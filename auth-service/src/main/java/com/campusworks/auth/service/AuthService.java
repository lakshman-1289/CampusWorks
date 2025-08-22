package com.campusworks.auth.service;

import com.campusworks.auth.domain.User;
import com.campusworks.auth.repo.UserRepository;
import com.campusworks.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public User register(String email, String password) {
        User user = User.builder()
                .email(email)
                .passwordHash(encoder.encode(password))
                .roles(Set.of("STUDENT"))
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public Optional<String> login(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .map(u -> jwtService.generateToken(String.valueOf(u.getId()), Map.of("roles", u.getRoles())));
    }
}


