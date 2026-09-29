package com.booking.resourcebooking.service;

import com.booking.resourcebooking.dto.AuthResponse;
import com.booking.resourcebooking.dto.LoginRequest;
import com.booking.resourcebooking.dto.RegisterRequest;
import com.booking.resourcebooking.exception.BadRequestException;
import com.booking.resourcebooking.exception.ConflictException;
import com.booking.resourcebooking.model.Role;
import com.booking.resourcebooking.model.User;
import com.booking.resourcebooking.repository.UserRepository;
import com.booking.resourcebooking.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        String role = jwtTokenProvider.getRoleFromToken(jwt);

        return new AuthResponse(jwt, loginRequest.getUsername(), role, jwtTokenProvider.getExpirationMs());
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new ConflictException("Username is already taken: " + registerRequest.getUsername());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ConflictException("Email is already registered: " + registerRequest.getEmail());
        }

        Role role = registerRequest.getRole() != null ? registerRequest.getRole() : Role.ROLE_USER;

        User user = new User(
                registerRequest.getUsername(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getEmail(),
                role
        );

        userRepository.save(user);

        String jwt = jwtTokenProvider.generateTokenFromUsername(user.getUsername(), user.getRole().name());
        return new AuthResponse(jwt, user.getUsername(), user.getRole().name(), jwtTokenProvider.getExpirationMs());
    }
}
