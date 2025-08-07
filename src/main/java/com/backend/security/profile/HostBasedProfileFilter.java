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
        String host = request.getServerName() + ":" + request.getServerPort();
        switch (host) {
            case "localhost:3000" -> System.setProperty("spring.profiles.active", "dev");
            case "https://stream-repo-frontend.vercel.app/" -> System.setProperty("spring.profiles.active", "prod");
            case "localhost:9090" -> System.setProperty("spring.profiles.active", "local");
        }
        filterChain.doFilter(request, response);
    }
//    http://localhost:8080/
}