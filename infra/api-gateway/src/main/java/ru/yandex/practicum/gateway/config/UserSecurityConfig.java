package ru.yandex.practicum.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class UserSecurityConfig {

    @Bean
    public ReactiveUserDetailsService userDetailsService(
            SecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        var users = properties.users().stream()
                .map(user -> User.builder()
                        .username(user.username())
                        .password(passwordEncoder.encode(user.password()))
                        .roles(user.roles().toArray(String[]::new))
                        .build())
                .toList();

        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}