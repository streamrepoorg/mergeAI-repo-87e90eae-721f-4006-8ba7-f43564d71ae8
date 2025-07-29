package com.backend.service.auth;

import com.backend.config.exception.EmptyFieldException;
import com.backend.config.exception.PasswordOrEmailException;
import com.backend.config.exception.UserAlreadyExistException;
import com.backend.config.exception.UserNotFoundException;
import com.backend.dto.UserDTO;
import com.backend.model.email.MagicLink;
import com.backend.model.user.User;
import com.backend.repository.mail.MagicLinkRepository;
import com.backend.repository.user.UserRepository;
import com.backend.service.email.EmailServiceImpl;
import com.backend.shared.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MagicLinkRepository magicLinkRepository;

    @Autowired
    private EmailServiceImpl emailService;

    private final ModelMapper modelMapper = new ModelMapper();

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${jwt.magic-link-expiration}")
    private Long magicLinkExpiration;

    private boolean isValidEmail(String email) {
        return !email.contains("@") || !email.contains(".");
    }

    private String generateSecureLink() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] linkBytes = new byte[32];
        secureRandom.nextBytes(linkBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(linkBytes);
    }

    private boolean isStrongPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return Pattern.compile(passwordRegex).matcher(password).matches();
    }

    public boolean existsByUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean existByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    @Override
    public void registerUser(UserDTO userDTO) {
        if (userDTO.getUsername() == null || userDTO.getUsername().isEmpty()) {
            throw new EmptyFieldException("Username cannot be empty");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
            throw new EmptyFieldException("Email cannot be empty");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            throw new EmptyFieldException("Password cannot be empty");
        }
        if (isValidEmail(userDTO.getEmail())) {
            throw new UserNotFoundException("User email is invalid.");
        }
        if (!isStrongPassword(userDTO.getPassword())) {
            if (userDTO.getPassword().length() < 5) {
                throw new PasswordOrEmailException("Password should be at least 5 characters.", new Throwable("Invalid password length"));
            }
            throw new PasswordOrEmailException("Password is too weak.", new Throwable("Invalid password strength"));
        }
        if (existsByUsername(userDTO.getUsername())) {
            throw new UserAlreadyExistException("Username already exists");
        }
        if (existByEmail(userDTO.getEmail())) {
            User existingUser = userRepository.findByEmail(userDTO.getEmail()).orElseThrow(() -> new RuntimeException("User with email exists but could not be retrieved"));

            if (existingUser.getIsVerified() == true) {
                throw new UserAlreadyExistException("User is already verified. Please log in");
            } else {
                try {
                    requestMagicLink(existingUser.getEmail());
                    log.info("Sent new magic link to unverified user: email={}", existingUser.getEmail());
                    return;
                } catch (Exception e) {
                    log.error("Failed to send new magic link: {}", e.getMessage(), e);
                    throw new RuntimeException("Failed to send new magic link", e);
                }
            }
        }

        User user = new User();
        modelMapper.map(userDTO, user);
        user.setPassword(PasswordUtil.encryptPassword(userDTO.getPassword()));
        user.setProvider("manual system");
        user.setIsVerified(false);
        try {
            User savedUser = userRepository.save(user);
            log.info("Successfully saved user --> id={}, email={}", savedUser.getId(), savedUser.getEmail());
            requestMagicLink(savedUser.getEmail());
        } catch (Exception e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save user or send magic link", e);
        }
    }

    @Override
    public UserDTO handleOAuth2User(String provider, String providerId, String email, String name, String picture) {
        if (provider == null || providerId == null) {
            throw new IllegalArgumentException("Provider or providerId cannot be null");
        }
        if (email == null || isValidEmail(email)) {
            throw new IllegalArgumentException("Valid email is required for OAuth2 registration");
        }

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(name != null ? name : user.getName());
            user.setPicture(picture != null ? picture : user.getPicture());
            userRepository.save(user);
            return modelMapper.map(user, UserDTO.class);
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setName(name != null ? name : "Unknown");
        user.setPicture(picture);
        user.setUsername(generateUniqueUsername(email));
        user.setIsVerified(true);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    private String generateUniqueUsername(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int counter = 1;
        while (existsByUsername(username)) {
            username = baseUsername + counter++;
        }
        return username;
    }


    @Override
    public void requestMagicLink(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        String token = generateSecureLink();
        MagicLink magicLink = new MagicLink();
        magicLink.setUserId(userOpt.get().getId());
        magicLink.setLink(token);
        magicLink.setExpiresAt(Instant.now().plusMillis(magicLinkExpiration));
        magicLinkRepository.save(magicLink);

        String link = String.format("%s/auth/login?magic-link=%s", frontendUrl, token);
        emailService.sendMagicLink(email, link);
        log.info("Magic link sent to {} with token: {}", email, token);
    }

    @Override
    public String validateMagicLink(String link) {
        Optional<MagicLink> magicLinkOpt = magicLinkRepository.findByLink(link);
        if (magicLinkOpt.isEmpty() || magicLinkOpt.get().getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Magic link expired or not found");
        }

        String userId = magicLinkOpt.get().getUserId();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        User user = userOpt.get();
        user.setIsVerified(true);
        userRepository.save(user);

        magicLinkRepository.delete(magicLinkOpt.get());
        return link;
    }
}
