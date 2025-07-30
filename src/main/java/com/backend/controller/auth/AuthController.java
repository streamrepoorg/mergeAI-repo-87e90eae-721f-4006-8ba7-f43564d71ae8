package com.backend.controller.auth;

import com.backend.config.exception.EmptyFieldException;
import com.backend.config.exception.PasswordOrEmailException;
import com.backend.config.exception.UserAlreadyExistException;
import com.backend.config.exception.UserNotFoundException;
import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
import com.backend.dto.response.LoginResponse;
import com.backend.dto.response.ResponseDetails;
import com.backend.service.auth.AuthServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthServiceImpl authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        log.info("Registering user with email: {}", userDTO.getEmail());
        try {
            authService.registerUser(userDTO);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Your account has been created successfully", HttpStatus.CREATED.toString()
            );
            return ResponseEntity.status(201).body(responseDetails);
        } catch (EmptyFieldException | PasswordOrEmailException | UserAlreadyExistException e) {
            log.error("Registration failed: {}", e.getMessage());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), e.getMessage(), HttpStatus.BAD_REQUEST.toString());
            return ResponseEntity.status(400).body(responseDetails);
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Failed to save user", HttpStatus.INTERNAL_SERVER_ERROR.toString());
            return ResponseEntity.status(500).body(responseDetails);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserDTO userDTO) {
        try {
            log.info("Logging in user: {}", userDTO.getUsername());
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword()));
            String token = jwtTokenProvider.generateToken(authentication);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Login successful", HttpStatus.OK.toString());
            LoginResponse loginResponse = new LoginResponse(token, responseDetails);
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) {
            log.error("Login failed for user: {}", userDTO.getUsername(), e);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Invalid credentials", HttpStatus.UNAUTHORIZED.toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseDetails);
        }
    }

    @PostMapping("/magic-link")
    public ResponseEntity<?> requestMagicLink(@Valid @RequestBody MagicLinkRequest request){
        try {
            log.info("Requesting magic link for email: {}", request.getEmail());
            authService.requestMagicLink(request.getEmail());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Magic link sent to email", HttpStatus.OK.toString());
            return ResponseEntity.status(200).body(responseDetails);
        } catch (UserNotFoundException e) {
            log.error("Failed to send magic link: {}", e.getMessage());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "User not found", HttpStatus.BAD_REQUEST.toString());
            return ResponseEntity.status(400).body(responseDetails);
        }
    }

    @GetMapping("/validate-magic-link")
    public ResponseEntity<?> validateMagicLink(@RequestParam String link) {
        try {
            authService.validateMagicLink(link);
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Your account has been verified successfully", HttpStatus.OK.toString());
            return ResponseEntity.status(201).body(responseDetails);
        } catch (Exception e) {
            log.error("Magic link validation failed: {}", e.getMessage());
            ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Your magic link is expired already", HttpStatus.BAD_REQUEST.toString());
            return ResponseEntity.status(400).body(responseDetails);
        }
    }

    @GetMapping("/oauth2/success")
    public void oauth2Success(OAuth2AuthenticationToken authentication, HttpServletResponse response) throws IOException {
        try {
            log.info("Processing OAuth2 login for provider: {}", authentication.getAuthorizedClientRegistrationId());
            String provider = authentication.getAuthorizedClientRegistrationId();
            String providerId = authentication.getPrincipal().getAttribute("sub") != null ? authentication.getPrincipal().getAttribute("sub") : authentication.getPrincipal().getAttribute("id");
            String email = authentication.getPrincipal().getAttribute("email");
            String name = authentication.getPrincipal().getAttribute("name");
            String picture = authentication.getPrincipal().getAttribute("picture");

            UserDTO userDTO = authService.handleOAuth2User(provider, providerId, email, name, picture);
            Authentication auth = new UsernamePasswordAuthenticationToken(userDTO.getUsername(), null, authentication.getAuthorities());
            String token = jwtTokenProvider.generateToken(auth);
            String redirectUrl = String.format("%s/auth/callback?token=%s", "https://stream-repo-frontend.vercel.app", token);
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("OAuth2 login failed: {}", e.getMessage());
//            response.sendRedirect("https://stream-repo-frontend.vercel.app/auth/callback?error=OAuth2 login failed");
        }
    }
}