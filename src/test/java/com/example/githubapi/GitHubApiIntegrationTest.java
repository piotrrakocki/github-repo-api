package com.example.githubapi;

import com.example.githubapi.model.Branch;
import com.example.githubapi.model.Repository;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GitHubApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    public void setUp() {
        WireMock.configureFor("localhost", wireMockServer.getPort());
        wireMockServer.resetAll();
    }

    @Test
    public void testGetUserRepositories() {
        // Mock GitHub API response for repositories
        stubFor(get(urlPathEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\": \"repo1\", \"owner\": {\"login\": \"testuser\"}, \"fork\": false}]")));

        // Mock GitHub API response for branches
        stubFor(get(urlPathEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\": \"main\", \"commit\": {\"sha\": \"abc123\"}}]")));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/github/user/testuser/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Repository.class)
                .consumeWith(response -> {
                    List<Repository> repositories = response.getResponseBody();
                    assertNotNull(repositories);
                    assertEquals(1, repositories.size());

                    Repository repository = repositories.get(0);
                    assertEquals("repo1", repository.name());
                    assertEquals("testuser", repository.ownerLogin());
                    assertEquals(1, repository.branches().size());

                    Branch branch = repository.branches().get(0);
                    assertEquals("main", branch.name());
                    assertEquals("abc123", branch.lastCommitSha());
                });
    }

    @Test
    public void testUserNotFound() {
        // Mock GitHub API response for user not found
        stubFor(get(urlPathEqualTo("/users/nonexistentuser/repos"))
                .willReturn(aResponse().withStatus(404)));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/github/user/nonexistentuser/repositories")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("User not found: nonexistentuser");
    }

    @Test
    public void testUserHasNoRepositories() {
        String user = "testuser";
        // Mock GitHub API response for user with no repositories
        stubFor(get(urlPathEqualTo("/users/" + user + "/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/github/user/" + user + "/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Repository.class)
                .consumeWith(response -> {
                    List<Repository> repositories = response.getResponseBody();
                    assertNotNull(repositories);
                    assertTrue(repositories.isEmpty());
                });
    }

    @Test
    public void testRepositoryHasNoBranches() {
        // Mock GitHub API response for repositories
        stubFor(get(urlPathEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\": \"repo1\", \"owner\": {\"login\": \"testuser\"}, \"fork\": false}]")));

        // Mock GitHub API response for branches with no branches
        stubFor(get(urlPathEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/github/user/testuser/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Repository.class)
                .consumeWith(response -> {
                    List<Repository> repositories = response.getResponseBody();
                    assertNotNull(repositories);
                    assertEquals(1, repositories.size());

                    Repository repository = repositories.get(0);
                    assertEquals("repo1", repository.name());
                    assertEquals("testuser", repository.ownerLogin());
                    assertTrue(repository.branches().isEmpty());
                });
    }

    @Test
    public void testBranchHasNoLastCommit() {
        // Mock GitHub API response for repositories
        stubFor(get(urlPathEqualTo("/users/testuser/repos"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\": \"repo1\", \"owner\": {\"login\": \"testuser\"}, \"fork\": false}]")));

        // Mock GitHub API response for branches with no last commit
        stubFor(get(urlPathEqualTo("/repos/testuser/repo1/branches"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"name\": \"main\", \"commit\": {}}]")));

        webTestClient.get()
                .uri("http://localhost:" + port + "/api/github/user/testuser/repositories")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Repository.class)
                .consumeWith(response -> {
                    List<Repository> repositories = response.getResponseBody();
                    assertNotNull(repositories);
                    assertEquals(1, repositories.size());

                    Repository repository = repositories.get(0);
                    assertEquals("repo1", repository.name());
                    assertEquals("testuser", repository.ownerLogin());
                    assertEquals(1, repository.branches().size());

                    Branch branch = repository.branches().get(0);
                    assertEquals("main", branch.name());
                    assertNull(branch.lastCommitSha());
                });
    }
}