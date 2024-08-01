package com.example.githubapi.service;

import com.example.githubapi.model.Branch;
import com.example.githubapi.model.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GitHubService {
    Mono<List<Repository>> getUserRepositories(String username, int page, List<Repository> result);

    Mono<List<Branch>> getBranches(String username, String repoName);

}
