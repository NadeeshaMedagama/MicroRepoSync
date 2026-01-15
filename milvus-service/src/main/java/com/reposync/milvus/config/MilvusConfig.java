package com.reposync.milvus.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.uri}")
    private String milvusUri;

    @Value("${milvus.token:#{null}}")
    private String milvusToken;

    @Bean
    public MilvusServiceClient milvusClient() {
        // 1. Validation check - fail fast with clear error message
        if (milvusUri == null || milvusUri.isEmpty()) {
            throw new IllegalArgumentException(
                "❌ Fatal: 'milvus.uri' is missing in configuration. Cannot create Milvus client. "
                + "Please ensure MILVUS_URI secret is set in GitHub Actions or environment variables."
            );
        }

        // 2. Parse URI to get host and port
        String host = extractHost(milvusUri);
        int port = extractPort(milvusUri);

        System.out.println("✅ Connecting to Milvus at Host: " + host + ", Port: " + port);

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port);

        // Add token only if provided and not empty
        if (milvusToken != null && !milvusToken.trim().isEmpty()) {
            builder.withToken(milvusToken);
        }

        return new MilvusServiceClient(builder.build());
    }

    private String extractHost(String uri) {
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("URI cannot be null or empty");
        }
        // Remove protocol if present
        String cleanUri = uri.replaceFirst("^https?://", "");
        // Get host part before port
        int colonIndex = cleanUri.indexOf(':');
        return colonIndex > 0 ? cleanUri.substring(0, colonIndex) : cleanUri;
    }

    private int extractPort(String uri) {
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("URI cannot be null or empty");
        }
        // Remove protocol if present
        String cleanUri = uri.replaceFirst("^https?://", "");
        // Get port part
        int colonIndex = cleanUri.indexOf(':');
        if (colonIndex > 0) {
            String portStr = cleanUri.substring(colonIndex + 1);
            // Remove any path after port
            int slashIndex = portStr.indexOf('/');
            if (slashIndex > 0) {
                portStr = portStr.substring(0, slashIndex);
            }
            return Integer.parseInt(portStr);
        }
        // Default Milvus port
        return 19530;
    }
}

