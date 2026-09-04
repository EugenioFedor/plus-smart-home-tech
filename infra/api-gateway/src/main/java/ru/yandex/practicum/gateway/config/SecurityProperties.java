package ru.yandex.practicum.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        List<User> users
) {
    public record User(
            String username,
            String password,
            List<String> roles
    ) {
    }
}