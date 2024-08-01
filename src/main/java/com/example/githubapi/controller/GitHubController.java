package com.example.githubapi.controller;

import com.example.githubapi.exceptions.UserNotFoundException;
import com.example.githubapi.model.Repository;
import com.example.githubapi.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/user/{username}/repositories")
    public Mono<ResponseEntity<?>> getUserRepositories(@PathVariable String username) {
        return gitHubService.getUserRepositories(username, 1, new ArrayList<>())
                .<ResponseEntity<?>>map(repositories -> new ResponseEntity<>(repositories, HttpStatus.OK))
                .onErrorResume(UserNotFoundException.class, e -> handleUserNotFoundException(e, username));
    }

    private Mono<ResponseEntity<List<Repository>>> handleUserNotFoundException(UserNotFoundException e, String username) {
        Map<String, Object> errorResponse = Map.of(
                "status", HttpStatus.NOT_FOUND.value(),
                "message", e.getMessage()
        );
        return Mono.just(new ResponseEntity(errorResponse, HttpStatus.NOT_FOUND));
    }
}
