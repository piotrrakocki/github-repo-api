package com.example.githubapi.exceptions;

import java.util.HashMap;
import java.util.Map;

public class UserNotFoundException extends RuntimeException {
    private final String username;

    public UserNotFoundException(String username) {
        super("User not found: " + username);
        this.username = username;
    }

    public Map<String, Object> getResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 404);
        response.put("message", "User not found: " + username);
        return response;
    }
}
