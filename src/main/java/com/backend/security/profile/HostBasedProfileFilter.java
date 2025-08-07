package com.backend.security.profile;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HostBasedProfileFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        String host = request.getServerName();
        int port = request.getServerPort();
        String fullHost = host + ":" + port;

        switch (fullHost) {
            case "localhost:3000" -> System.setProperty("spring.profiles.active", "dev");
            case "stream-repo-frontend.vercel.app:443" -> System.setProperty("spring.profiles.active", "prod"); // Default HTTPS port
            case "localhost:9090" -> System.setProperty("spring.profiles.active", "local");
        }
        filterChain.doFilter(request, response);
    }
}