package com.example.githubapi.service;

import com.example.githubapi.exceptions.UserNotFoundException;
import com.example.githubapi.model.Branch;
import com.example.githubapi.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GitHubServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private GitHubServiceImpl gitHubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getUserRepositories_UserNotFound() {
        // Arrange
        String username = "nonexistentuser";
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        // Act
        Mono<List<Repository>> result = gitHubService.getUserRepositories(username, 1, Collections.emptyList());

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.toFuture().get());
        assertTrue(exception.getCause() instanceof UserNotFoundException);
    }

    @Test
    void getBranches_Success() throws ExecutionException, InterruptedException {
        // Arrange
        String username = "testuser";
        String repoName = "testrepo";
        Branch branch = new Branch("main", "sha1");

        when(responseSpec.bodyToFlux(Map.class))
                .thenReturn(Flux.just(Map.of("name", "main", "commit", Map.of("sha", "sha1"))));

        // Act
        Mono<List<Branch>> result = gitHubService.getBranches(username, repoName);
        List<Branch> branches = result.toFuture().get();

        // Assert
        assertNotNull(branches);
        assertEquals(1, branches.size());
        assertEquals("main", branches.get(0).getName());
    }

    @Test
    void handleWebClientResponseException_NotFound() throws ExecutionException, InterruptedException {
        // Arrange
        String username = "testuser";
        WebClientResponseException exception = WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null);

        // Act
        Mono<List<Repository>> result = gitHubService.handleWebClientResponseException(exception, username);

        // Assert
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> result.toFuture().get());
        assertTrue(executionException.getCause() instanceof UserNotFoundException);
    }

    @Test
    void handleWebClientResponseException_OtherError() throws ExecutionException, InterruptedException {
        // Arrange
        String username = "testuser";
        WebClientResponseException exception = WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null);

        // Act
        Mono<List<Repository>> result = gitHubService.handleWebClientResponseException(exception, username);

        // Assert
        ExecutionException executionException = assertThrows(ExecutionException.class, () -> result.toFuture().get());
        assertTrue(executionException.getCause() instanceof WebClientResponseException);
    }

    @Test
    void getUserRepositories_EmptyRepositoryList() throws ExecutionException, InterruptedException {
        // Arrange
        String username = "emptyuser";
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(new Map[]{}));

        // Act
        Mono<List<Repository>> result = gitHubService.getUserRepositories(username, 1, Collections.emptyList());
        List<Repository> repositories = result.toFuture().get();

        // Assert
        assertNotNull(repositories);
        assertTrue(repositories.isEmpty());
    }

    @Test
    void getUserRepositories_HandleOtherErrors() {
        // Arrange
        String username = "testuser";
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.error(WebClientResponseException.create(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null)));

        // Act
        Mono<List<Repository>> result = gitHubService.getUserRepositories(username, 1, Collections.emptyList());

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.toFuture().get());
        assertTrue(exception.getCause() instanceof WebClientResponseException);
        WebClientResponseException webClientResponseException = (WebClientResponseException) exception.getCause();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, webClientResponseException.getStatusCode());
    }

    @Test
    void getBranches_EmptyBranchList() throws ExecutionException, InterruptedException {
        // Arrange
        String username = "testuser";
        String repoName = "emptybranches";
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.empty());

        // Act
        Mono<List<Branch>> result = gitHubService.getBranches(username, repoName);
        List<Branch> branches = result.toFuture().get();

        // Assert
        assertNotNull(branches);
        assertTrue(branches.isEmpty());
    }

    @Test
    void getUserRepositories_HandleNonWebClientResponseException() {
        // Arrange
        String username = "testuser";
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // Act
        Mono<List<Repository>> result = gitHubService.getUserRepositories(username, 1, Collections.emptyList());

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, () -> result.toFuture().get());
        assertTrue(exception.getCause() instanceof RuntimeException);
        assertEquals("Unexpected error", exception.getCause().getMessage());
    }


}
