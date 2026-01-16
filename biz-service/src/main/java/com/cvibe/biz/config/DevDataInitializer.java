package com.cvibe.biz.config;

import com.cvibe.biz.user.entity.User;
import com.cvibe.biz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

/**
 * Development data initializer - creates admin user on startup
 */
@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDevData() {
        return args -> {
            // Create admin user if not exists
            if (userRepository.findByEmail("admin@cvibe.com").isEmpty()) {
                User admin = User.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .email("admin@cvibe.com")
                        .passwordHash(passwordEncoder.encode("Admin123456!"))
                        .fullName("System Admin")
                        .role(User.UserRole.ROLE_ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(admin);
                log.info("Created default admin user: admin@cvibe.com");
            }
        };
    }
}
