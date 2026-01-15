package com.cvibe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CVibe Backend Service Main Application
 * 
 * @author CVibe Team
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class CVibeApplication {

    public static void main(String[] args) {
        SpringApplication.run(CVibeApplication.class, args);
    }

}
