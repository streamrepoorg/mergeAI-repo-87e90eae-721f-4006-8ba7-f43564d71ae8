package com.backend.service.auth;

import com.backend.dto.UserDTO;
import com.backend.model.email.MagicLink;
import com.backend.model.email.PasswordResetLink;
import com.backend.model.user.User;
import com.backend.repository.mail.MagicLinkRepository;
import com.backend.repository.mail.PasswordResetRepository;
import com.backend.repository.user.UserRepository;
import com.backend.service.email.EmailServiceImpl;
import com.backend.config.PasswordUtil;
import com.backend.shared.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
    private RestTemplate restTemplate;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private EmailServiceImpl emailService;

    private final ModelMapper modelMapper = new ModelMapper();

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${jwt.password-reset-expiration}")
    private Long passwordResetExpiration;

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
        return !Pattern.compile(passwordRegex).matcher(password).matches();
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
            throw new InvalidInputException("Username cannot be empty");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().isEmpty()) {
            throw new InvalidInputException("Email cannot be empty");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().isEmpty()) {
            throw new InvalidInputException("Password cannot be empty");
        }
        if (isValidEmail(userDTO.getEmail())) {
            throw new UserNotFoundException("User email is invalid.");
        }
        if (isStrongPassword(userDTO.getPassword())) {
            if (userDTO.getPassword().length() < 5) {
                throw new PasswordOrEmailException("Password should be at least 5 characters.", new Throwable("Invalid password length"));
            }
            throw new PasswordOrEmailException("Password is too weak.", new Throwable("Invalid password strength"));
        }
        if (existsByUsername(userDTO.getUsername())) {
            throw new AlreadyExistException("Username already exists");
        }
        if (existByEmail(userDTO.getEmail())) {
            User existingUser = userRepository.findByEmail(userDTO.getEmail()).orElseThrow(() -> new RuntimeException("User with email exists but could not be retrieved"));
            if (existingUser.getIsVerified() == true) {
                throw new AlreadyExistException("User is already verified. Please log in");
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
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setName(name != null ? name : user.getName());
            user.setEmail(email);
            user.setPicture(picture != null ? picture : user.getPicture());
        } else {
            user = new User();
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setEmail(email);
            user.setName(name != null ? name : "GitHub User");
            user.setPicture(picture);
            user.setUsername(generateUniqueUsername(email));
            user.setIsVerified(true);
        }

        try {
            User savedUser = userRepository.save(user);
            log.info("Successfully saved/updated OAuth2 user --> id={}, email={}, provider={}", savedUser.getId(), savedUser.getEmail(), provider);
            return modelMapper.map(savedUser, UserDTO.class);
        } catch (Exception e) {
            log.error("Failed to save/update OAuth2 user for email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to process OAuth2 user registration", e);
        }
    }

    @Override
    public UserDTO handleOAuth2Redirect(OAuth2AuthenticationToken authentication) {
        log.info("Processing OAuth2 redirect for provider: {}", authentication.getAuthorizedClientRegistrationId());
        String provider = authentication.getAuthorizedClientRegistrationId();
        String providerId = authentication.getPrincipal().getAttribute("id");
        String email = authentication.getPrincipal().getAttribute("email");
        String name = authentication.getPrincipal().getAttribute("name");
        String picture = authentication.getPrincipal().getAttribute("avatar_url");

        if (email == null) {
            String accessToken = authentication.getPrincipal().getAttribute("access_token");
            email = fetchGitHubEmail(accessToken);
        }

        return handleOAuth2User(provider, providerId, email, name, picture);
    }

    private String fetchGitHubEmail(String accessToken) {
        String url = "https://api.github.com/user/emails";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> emails = response.getBody();
        if (emails != null) {
            for (Map<String, Object> emailData : emails) {
                if (Boolean.TRUE.equals(emailData.get("primary")) && Boolean.TRUE.equals(emailData.get("verified"))) {
                    return (String) emailData.get("email");
                }
            }
        }
        throw new IllegalArgumentException("No verified email found for GitHub user");
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

        String securedLink = generateSecureLink();
        MagicLink magicLink = new MagicLink();
        magicLink.setUserId(userOpt.get().getId());
        magicLink.setLink(securedLink);
        magicLink.setExpiresAt(Instant.now().plusMillis(magicLinkExpiration));
        magicLinkRepository.save(magicLink);

        String link = String.format("%s/auth/magic-link/validatelink?magic-link=%s", frontendUrl, securedLink);
        emailService.sendMagicLink(email, link);
        log.info("Magic link sent to {} with securedLink: {}", email, securedLink);
    }

    @Override
    public void validateMagicLink(String link) {
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
    }

    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with email: " + email);
        }
        User user = userOpt.get();
        if (!user.getIsVerified()) {
            throw new UserNotVerified("User account is not verified");
        }

        String token = generateSecureLink();
        PasswordResetLink resetToken = new PasswordResetLink();
        resetToken.setUserId(user.getId());
        resetToken.setLink(token);
        resetToken.setExpiresAt(Instant.now().plusMillis(passwordResetExpiration));
        passwordResetRepository.save(resetToken);

        String link = String.format("%s/auth/reset-password?token=%s", frontendUrl, token);
        emailService.sendPasswordResetLink(email, link);
        log.info("Password reset link sent to {} with token: {}", email, token);
    }

    public void resetPassword(String link, String newPassword) {
        Optional<PasswordResetLink> resetTokenOpt = passwordResetRepository.findByLink(link);
        if (resetTokenOpt.isEmpty() || resetTokenOpt.get().getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Password reset token expired or not found");
        }
        String userId = resetTokenOpt.get().getUserId();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
        if (isStrongPassword(newPassword)) {
            if (newPassword.length() < 5) {
                throw new PasswordOrEmailException("Password should be at least 5 characters.", new Throwable("Invalid password length"));
            }
            throw new PasswordOrEmailException("Password is too weak.", new Throwable("Invalid password strength"));
        }

        User user = userOpt.get();
        user.setPassword(PasswordUtil.encryptPassword(newPassword));
        userRepository.save(user);
        passwordResetRepository.delete(resetTokenOpt.get());
        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}