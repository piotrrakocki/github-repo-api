package com.example.githubapi.service;

import com.example.githubapi.exceptions.UserNotFoundException;
import com.example.githubapi.model.Branch;
import com.example.githubapi.model.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class GitHubServiceImpl implements GitHubService {

    private final WebClient webClient;
    private static final String BASE_URL = "https://api.github.com/users/%s/repos";
    private static final int MAX_PAGES = 500;

    public GitHubServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<List<Repository>> getUserRepositories(String username, int page, List<Repository> result) {
        String url = String.format(BASE_URL, username) + "?page=" + page;
        System.out.println("Fetching URL: " + url);

        return fetchPage(url)
                .expand(repos -> {
                    System.out.println("Fetched page: " + page + ", size: " + repos.size());
                    if (repos.isEmpty() || page >= MAX_PAGES) {
                        return Mono.empty();
                    }
                    return fetchPage(String.format(BASE_URL, username) + "?page=" + (page + 1));
                })
                .flatMapIterable(repos -> repos)
                .filter(repo -> !Boolean.TRUE.equals(repo.get("fork")))
                .flatMap(repo -> {
                    String name = (String) repo.get("name");
                    String ownerLogin = (String) ((Map) repo.get("owner")).get("login");

                    return getBranches(username, name)
                            .map(branches -> new Repository(name, ownerLogin, branches));
                })
                .collect(Collectors.toList())
                .onErrorResume(WebClientResponseException.class, e -> handleWebClientResponseException(e, username));
    }

    private Mono<List<Map>> fetchPage(String url) {
        System.out.println("Fetching URL: " + url);

        return webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToMono(Map[].class)
                .map(Arrays::asList);
    }

    @Override
    public Mono<List<Branch>> getBranches(String username, String repoName) {
        String url = String.format("https://api.github.com/repos/%s/%s/branches", username, repoName);
        System.out.println("Fetching branches URL: " + url);

        return webClient.get()
                .uri(url)
                .header("Accept", "application/json")
                .retrieve()
                .bodyToFlux(Map.class)
                .map(branchMap -> {
                    String name = (String) branchMap.get("name");
                    Map commitMap = (Map) branchMap.get("commit");
                    String lastCommitSha = (String) commitMap.get("sha");
                    return new Branch(name, lastCommitSha);
                })
                .collect(Collectors.toList());
    }

    protected Mono<List<Repository>> handleWebClientResponseException(WebClientResponseException e, String username) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.error(new UserNotFoundException(username));
        }
        return Mono.error(e);
    }
}