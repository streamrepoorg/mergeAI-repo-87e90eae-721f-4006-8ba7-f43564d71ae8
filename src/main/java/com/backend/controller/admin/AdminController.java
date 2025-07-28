package com.backend.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PostMapping("/write-docs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> writeDocs() {
        return ResponseEntity.ok("Welcome, mighty Admin! You may now write the sacred docs.");
    }
}
