package com.reposync.milvus.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    // Default to empty string to avoid "Could not resolve placeholder" startup crashes
    @Value("${milvus.uri:}")
    private String milvusUri;

    @Value("${milvus.token:}")
    private String milvusToken;

    @Bean
    public MilvusServiceClient milvusClient() {
        // 1. Fail fast with clear error message if configuration is missing
        if (milvusUri == null || milvusUri.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "❌ Fatal: 'milvus.uri' is not configured. " +
                "Check that MILVUS_URI environment variable is set in docker-compose.yml or application.properties"
            );
        }

        System.out.println("✅ Connecting to Milvus URI: " + milvusUri);

        // 2. Use the SDK's built-in URI parser (Safe & Standard)
        // This automatically handles http://host:port format
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withUri(milvusUri)
                .withConnectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .withKeepAliveTime(60, java.util.concurrent.TimeUnit.SECONDS)
                .withKeepAliveTimeout(30, java.util.concurrent.TimeUnit.SECONDS);

        // 3. Add token only if provided and not empty
        if (milvusToken != null && !milvusToken.trim().isEmpty()) {
            System.out.println("✅ Using Milvus authentication token");
            builder.withToken(milvusToken);
        } else {
            System.out.println("ℹ️  No Milvus token provided (using unauthenticated connection)");
        }

        try {
            MilvusServiceClient client = new MilvusServiceClient(builder.build());
            System.out.println("✅ Milvus client created successfully");
            return client;
        } catch (Exception e) {
            // Catch SDK-specific errors (like bad URI format) and rethrow clearly
            System.err.println("❌ Failed to initialize Milvus Client: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException(
                "Failed to initialize Milvus Client with URI: " + milvusUri +
                ". Error: " + e.getMessage(),
                e
            );
        }
    }
}

