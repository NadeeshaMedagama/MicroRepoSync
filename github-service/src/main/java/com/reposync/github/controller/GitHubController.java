package com.reposync.github.controller;

import com.reposync.common.dto.DocumentContent;
import com.reposync.common.dto.RepositoryInfo;
import com.reposync.github.service.GitHubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositoryInfo>> getRepositories(
            @RequestParam String organization,
            @RequestParam(required = false) String filterKeyword) {
        log.info("Fetching repositories for organization: {}, filter: {}", organization, filterKeyword);
        List<RepositoryInfo> repositories = gitHubService.getRepositories(organization, filterKeyword);
        return ResponseEntity.ok(repositories);
    }

    @GetMapping("/documents/{owner}/{repo}")
    public ResponseEntity<List<DocumentContent>> getDocuments(
            @PathVariable String owner,
            @PathVariable String repo) {
        log.info("Fetching documents for repository: {}/{}", owner, repo);
        List<DocumentContent> documents = gitHubService.getRepositoryDocuments(owner, repo);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GitHub Service is running");
    }
}

