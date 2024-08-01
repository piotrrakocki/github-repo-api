package com.example.githubapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf((csfr -> csfr.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/github/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
