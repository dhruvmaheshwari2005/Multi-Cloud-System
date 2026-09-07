package com.multicloud.costmonitor.controller;

import com.multicloud.costmonitor.model.User;
import com.multicloud.costmonitor.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is already in use!"));
        }

        User newUser = new User(
                user.getUsername(),
                passwordEncoder.encode(user.getPassword()),
                user.getEmail()
        );
        userRepository.save(newUser);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> loginRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username and password are required"));
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        User user = userRepository.findByUsername(username).orElse(null);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Logged in successfully!");
        if (user != null) {
            resp.put("username", user.getUsername());
            resp.put("email", user.getEmail());
            resp.put("role", user.getRole());
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully!"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("authenticated", false));
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("authenticated", true);
        if (user != null) {
            resp.put("username", user.getUsername());
            resp.put("email", user.getEmail());
            resp.put("role", user.getRole());
        }
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Error: Unauthorized"));
        }

        String currentUsername = auth.getName();
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Error: User not found"));
        }

        String newUsername = profileRequest.get("username");
        String newEmail = profileRequest.get("email");
        String newPassword = profileRequest.get("password");

        if (newUsername == null || newUsername.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username cannot be empty"));
        }
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email cannot be empty"));
        }

        // If username changes, check availability
        if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username is already taken"));
        }

        // If email changes, check availability
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Email is already in use"));
        }

        user.setUsername(newUsername);
        user.setEmail(newEmail);

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);

        // Update Authentication context with the new username if it changed
        if (!newUsername.equals(currentUsername)) {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(newUsername, auth.getCredentials(), auth.getAuthorities())
            );
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully!");
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }
}
