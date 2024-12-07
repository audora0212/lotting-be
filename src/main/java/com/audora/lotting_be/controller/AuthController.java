// src/main/java/com/audora/lotting_be/controller/AuthController.java
package com.audora.lotting_be.controller;

import com.audora.lotting_be.model.manager.Manager;
import com.audora.lotting_be.payload.request.LoginRequest;
import com.audora.lotting_be.payload.request.SignupRequest;
import com.audora.lotting_be.payload.response.JwtResponse;
import com.audora.lotting_be.payload.response.MessageResponse;
import com.audora.lotting_be.repository.ManagerRepository;
import com.audora.lotting_be.security.JwtUtils;
import com.audora.lotting_be.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                          ManagerRepository managerRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.managerRepository = managerRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateManager(@RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken((UserDetailsImpl) authentication.getPrincipal());

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                roles));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerManager(@RequestBody SignupRequest signUpRequest) {
        if (managerRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // Create new manager's account
        Manager manager = new Manager();
        manager.setUsername(signUpRequest.getUsername());
        manager.setPassword(encoder.encode(signUpRequest.getPassword()));
        manager.setRoles(signUpRequest.getRoles());

        managerRepository.save(manager);

        return ResponseEntity.ok(new MessageResponse("Manager registered successfully!"));
    }
}
