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
        // Parse URI to get host and port
        String host = extractHost(milvusUri);
        int port = extractPort(milvusUri);

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
        // Remove protocol if present
        String cleanUri = uri.replaceFirst("^https?://", "");
        // Get host part before port
        int colonIndex = cleanUri.indexOf(':');
        return colonIndex > 0 ? cleanUri.substring(0, colonIndex) : cleanUri;
    }

    private int extractPort(String uri) {
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

