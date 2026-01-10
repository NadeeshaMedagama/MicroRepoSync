package com.reposync.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.reposync.common.dto.DocumentContent;
import com.reposync.common.dto.RepositoryInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final WebClient gitHubWebClient;

    private static final List<String> README_PATTERNS = Arrays.asList(
            "README.md", "README.MD", "readme.md", "Readme.md", "README", "README.txt"
    );

    private static final List<String> API_DEFINITION_PATTERNS = Arrays.asList(
            "openapi.yaml", "openapi.yml", "swagger.yaml", "swagger.yml",
            "openapi.json", "swagger.json", "api.yaml", "api.yml"
    );

    public List<RepositoryInfo> getRepositories(String organization, String filterKeyword) {
        log.info("Fetching repositories for organization: {}", organization);

        return gitHubWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orgs/{org}/repos")
                        .queryParam("per_page", 100)
                        .queryParam("type", "all")
                        .build(organization))
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(this::mapToRepositoryInfo)
                .filter(repo -> filterKeyword == null ||
                        repo.getName().toLowerCase().contains(filterKeyword.toLowerCase()) ||
                        (repo.getDescription() != null &&
                         repo.getDescription().toLowerCase().contains(filterKeyword.toLowerCase())))
                .collectList()
                .block();
    }

    public List<DocumentContent> getRepositoryDocuments(String owner, String repo) {
        log.info("Fetching documents for repository: {}/{}", owner, repo);

        List<DocumentContent> documents = new ArrayList<>();

        // Fetch README files
        README_PATTERNS.forEach(pattern -> {
            try {
                DocumentContent readme = getFileContent(owner, repo, pattern, "README");
                if (readme != null) {
                    documents.add(readme);
                }
            } catch (Exception e) {
                log.debug("README pattern {} not found in {}/{}", pattern, owner, repo);
            }
        });

        // Fetch API definition files
        API_DEFINITION_PATTERNS.forEach(pattern -> {
            try {
                DocumentContent apiDef = getFileContent(owner, repo, pattern, "API_DEFINITION");
                if (apiDef != null) {
                    documents.add(apiDef);
                }
            } catch (Exception e) {
                log.debug("API definition pattern {} not found in {}/{}", pattern, owner, repo);
            }
        });

        // Search for additional API definitions in common directories
        searchInDirectory(owner, repo, "docs", "API_DEFINITION").forEach(documents::add);
        searchInDirectory(owner, repo, "api", "API_DEFINITION").forEach(documents::add);

        log.info("Found {} documents for repository {}/{}", documents.size(), owner, repo);
        return documents;
    }

    private DocumentContent getFileContent(String owner, String repo, String path, String fileType) {
        try {
            JsonNode response = gitHubWebClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response != null && response.has("content")) {
                String encodedContent = response.get("content").asText().replaceAll("\\s", "");
                String decodedContent = new String(Base64.getDecoder().decode(encodedContent), StandardCharsets.UTF_8);

                return DocumentContent.builder()
                        .repositoryName(owner + "/" + repo)
                        .filePath(path)
                        .fileName(response.get("name").asText())
                        .fileType(fileType)
                        .content(decodedContent)
                        .sha(response.get("sha").asText())
                        .build();
            }
        } catch (Exception e) {
            log.debug("Could not fetch file {} from {}/{}: {}", path, owner, repo, e.getMessage());
        }
        return null;
    }

    private List<DocumentContent> searchInDirectory(String owner, String repo, String directory, String fileType) {
        try {
            return gitHubWebClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{dir}", owner, repo, directory)
                    .retrieve()
                    .bodyToFlux(JsonNode.class)
                    .filter(item -> item.get("type").asText().equals("file"))
                    .filter(item -> {
                        String name = item.get("name").asText().toLowerCase();
                        return name.endsWith(".yaml") || name.endsWith(".yml") ||
                               name.endsWith(".json") || name.contains("openapi") ||
                               name.contains("swagger");
                    })
                    .map(item -> getFileContent(owner, repo, item.get("path").asText(), fileType))
                    .filter(Objects::nonNull)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.debug("Directory {} not found in {}/{}", directory, owner, repo);
            return Collections.emptyList();
        }
    }

    private RepositoryInfo mapToRepositoryInfo(JsonNode node) {
        return RepositoryInfo.builder()
                .name(node.get("name").asText())
                .fullName(node.get("full_name").asText())
                .description(node.has("description") && !node.get("description").isNull()
                        ? node.get("description").asText() : "")
                .url(node.get("html_url").asText())
                .defaultBranch(node.get("default_branch").asText())
                .language(node.has("language") && !node.get("language").isNull()
                        ? node.get("language").asText() : "")
                .stargazersCount(node.get("stargazers_count").asLong())
                .forksCount(node.get("forks_count").asLong())
                .updatedAt(node.get("updated_at").asText())
                .build();
    }
}

