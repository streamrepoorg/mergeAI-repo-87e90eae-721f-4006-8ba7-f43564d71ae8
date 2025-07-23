package com.backend.controller.auth;

import com.backend.config.security.JwtTokenProvider;
import com.backend.dto.UserDTO;
import com.backend.dto.request.MagicLinkRequest;
import com.backend.dto.response.JwtResponse;
import com.backend.model.user.User;
import com.backend.service.auth.AuthServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceImpl authService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthServiceImpl authService, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        User user = authService.registerUser(userDTO);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDTO.getUsername(), userDTO.getPassword()));
        String token = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @PostMapping("/magic-link")
    public ResponseEntity<?> requestMagicLink(@RequestBody MagicLinkRequest request) {
        authService.requestMagicLink(request.getEmail());
        return ResponseEntity.ok("Magic link sent to email");
    }

    @GetMapping("/magic-login")
    public ResponseEntity<?> magicLogin(@RequestParam String token) {
        User user = authService.validateMagicLink(token);
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), null, Collections.emptyList());
        String jwt = jwtTokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @GetMapping("/oauth2/success")
    public ResponseEntity<?> oauth2Success(OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String providerId = authentication.getPrincipal().getAttribute("sub") != null ? authentication.getPrincipal().getAttribute("sub") : authentication.getPrincipal().getAttribute("id");
        String email = authentication.getPrincipal().getAttribute("email");
        String name = authentication.getPrincipal().getAttribute("name");
        String picture = authentication.getPrincipal().getAttribute("picture");

        User user = authService.handleOAuth2User(provider, providerId, email, name, picture);
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authentication.getAuthorities());
        String token = jwtTokenProvider.generateToken(auth);
        return ResponseEntity.ok(new JwtResponse(token));
    }
}
