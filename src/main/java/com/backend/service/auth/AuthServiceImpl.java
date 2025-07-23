package com.backend.service.auth;

import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.model.email.MagicLink;
import com.backend.model.user.User;
import com.backend.repository.mail.MagicLinkRepository;
import com.backend.repository.user.UserRepository;
import com.backend.service.email.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final MagicLinkRepository magicLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public AuthServiceImpl(UserRepository userRepository, MagicLinkRepository magicLinkRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, EmailService emailService) {
        this.userRepository = userRepository;
        this.magicLinkRepository = magicLinkRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailService = emailService;
    }

    @Override
    public User registerUser(UserDTO userDTO) {
        if (userRepository.findByUsernameOrEmail(userDTO.getUsername(), userDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Username or email already exists");
        }
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        user.setBio(userDTO.getBio());
        user.setPicture(userDTO.getPicture());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setProvider("manual");
        return userRepository.save(user);
    }

    @Override
    public User handleOAuth2User(String provider, String providerId, String email, String name, String picture) {
        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);
        user.setUsername(email.split("@")[0]); // Default username from email
        return userRepository.save(user);
    }

    @Override
    public void requestMagicLink(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        String token = jwtTokenProvider.generateMagicLinkToken(email);
        MagicLink magicLink = new MagicLink();
        magicLink.setUserId(userOpt.get().getId());
        magicLink.setToken(token);
        magicLink.setExpiresAt(Instant.now().plusMillis(180000)); // 3 minutes
        magicLinkRepository.save(magicLink);

        String link = "http://localhost:8080/api/auth/magic-login?token=" + token;
        emailService.sendMagicLink(email, link);
    }

    @Override
    public User validateMagicLink(String token) {
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
            throw new RuntimeException("User not found");
        }

        magicLinkRepository.delete(magicLinkOpt.get()); // Invalidate link after use
        return userOpt.get();
    }
}