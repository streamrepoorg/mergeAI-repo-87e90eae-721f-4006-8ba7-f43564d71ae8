package com.backend.service.auth;

import com.backend.config.exception.EmptyFieldException;
import com.backend.config.exception.PasswordOrEmailException;
import com.backend.config.exception.UserAlreadyExistException;
import com.backend.config.exception.UserNotFoundException;
import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.model.email.MagicLink;
import com.backend.model.user.User;
import com.backend.repository.mail.MagicLinkRepository;
import com.backend.repository.user.UserRepository;
import com.backend.service.email.EmailService;
import com.backend.service.email.EmailServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    private MongoTemplate mongoTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmailServiceImpl emailService;

    private final ModelMapper modelMapper = new ModelMapper();

    boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }

    private String encryptPassword(String password) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(password);
    }

    @Value("${jwt.magic-link-expiration}")
    private Long magicLinkExpiration;

    private boolean isStrongPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        return pattern.matcher(password).matches();
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
        if (!isValidEmail(userDTO.getEmail())) {
            throw new UserNotFoundException("User email is invalid.");
        }
        if (!isStrongPassword(userDTO.getPassword())) {
            if (userDTO.getPassword().length() < 5) {
                throw new PasswordOrEmailException("Password should be at least 5 characters.", new Throwable("Invalid password length"));
            }
            throw new PasswordOrEmailException("Password is too weak.", new Throwable("Invalid password strength"));
        }
        if (existByEmail(userDTO.getEmail())) {
            throw new UserAlreadyExistException("Email already exists");
        }
        User user = new User();
        modelMapper.map(userDTO, user);
        user.setPassword(encryptPassword(userDTO.getPassword()));
        user.setProvider("manual");
        User savedUser;
        try {
            savedUser = userRepository.save(user);
            log.info("Successfully saved user --> id={}, email={}", savedUser.getId(), savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to save user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save user to MongoDB", e);
        }
        try {
            requestMagicLink(savedUser.getEmail());
        } catch (Exception e) {
            log.error("Magic link email failed to send to {}: {}", savedUser.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to send magic link. User not saved.", e);
        }
    }

    @Override
    public UserDTO handleOAuth2User(String provider, String providerId, String email, String name, String picture) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existingUser.isPresent()) {
            return modelMapper.map(existingUser.get(), UserDTO.class);
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);
        user.setUsername(email.split("@")[0]);
        user = userRepository.save(user);
        return modelMapper.map(user, UserDTO.class);
    }

    @Override
    public void requestMagicLink(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        String token = jwtTokenProvider.generateMagicLinkToken(email);
        MagicLink magicLink = new MagicLink();
        magicLink.setUserId(userOpt.get().getId());
        magicLink.setToken(token);
        magicLink.setExpiresAt(Instant.now().plusMillis(magicLinkExpiration));
        magicLinkRepository.save(magicLink);

        String link = "http://localhost:8080/api/auth/magic-login?token=" + token;
        emailService.sendMagicLink(email, link);
        log.info("Magic link sent to {} with token: {}", email, token);
    }

    @Override
    public UserDTO validateMagicLink(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("Invalid or expired magic link");
        }

        Optional<MagicLink> magicLinkOpt = magicLinkRepository.findByToken(token);
        if (magicLinkOpt.isEmpty() || magicLinkOpt.get().getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Magic link expired or not found");
        }

        String email = jwtTokenProvider.getUsernameFromJWT(token);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }

        magicLinkRepository.delete(magicLinkOpt.get());
        return modelMapper.map(userOpt.get(), UserDTO.class);
    }
}