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
        return fetchPage(username, page)
                .expand(repos -> {
                    if (repos.isEmpty() || page >= MAX_PAGES) {
                        return Mono.empty();
                    }
                    return fetchPage(username, page + 1);
                })
                .flatMapIterable(repos -> repos)
                .filter(repo -> !Boolean.TRUE.equals(repo.get("fork")))
                .flatMap(repo -> {
                    Repository repository = new Repository();
                    repository.setName((String) repo.get("name"));
                    repository.setOwnerLogin((String) ((Map) repo.get("owner")).get("login"));

                    return getBranches(username, repository.getName())
                            .map(branches -> {
                                repository.setBranches(branches);
                                result.add(repository);
                                return repository;
                            });
                })
                .collectList()
                .thenReturn(result)
                .onErrorResume(WebClientResponseException.class, e -> handleWebClientResponseException(e, username));
    }

    private Mono<List<Map>> fetchPage(String username, int page) {
        String url = String.format(BASE_URL, username) + "?page=" + page;

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
                .collectList();
    }

    protected Mono<List<Repository>> handleWebClientResponseException(WebClientResponseException e, String username) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return Mono.error(new UserNotFoundException(username));
        }
        return Mono.error(e);
    }
}
