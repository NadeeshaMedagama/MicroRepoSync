package com.reposync.milvus.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
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

        log.info("✅ Connecting to Milvus URI: {}", milvusUri);

        // 2. Use the SDK's built-in URI parser (Safe & Standard)
        // This automatically handles http://host:port format
        // Extended timeouts for Zilliz Cloud which may have higher latency
        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withUri(milvusUri)
                .withConnectTimeout(120, TimeUnit.SECONDS)      // Increased from 60s
                .withKeepAliveTime(120, TimeUnit.SECONDS)       // Increased from 60s
                .withKeepAliveTimeout(60, TimeUnit.SECONDS)     // Increased from 30s
                .withIdleTimeout(300, TimeUnit.SECONDS)         // Added: 5 min idle timeout
                .withRpcDeadline(180, TimeUnit.SECONDS);        // Added: 3 min RPC deadline

        // 3. Add token only if provided and not empty
        if (milvusToken != null && !milvusToken.trim().isEmpty()) {
            log.info("✅ Using Milvus authentication token");
            builder.withToken(milvusToken);
        } else {
            log.info("ℹ️  No Milvus token provided (using unauthenticated connection)");
        }

        try {
            MilvusServiceClient client = new MilvusServiceClient(builder.build());
            log.info("✅ Milvus client created successfully");

            // Test connection by listing collections
            try {
                var response = client.showCollections(
                    io.milvus.param.collection.ShowCollectionsParam.newBuilder().build()
                );
                if (response.getStatus() == io.milvus.param.R.Status.Success.getCode()) {
                    log.info("✅ Milvus connection verified - found {} collections",
                        response.getData() != null ? response.getData().getCollectionNamesCount() : 0);
                } else {
                    log.warn("⚠️ Milvus connection test returned status: {} - {}",
                        response.getStatus(), response.getMessage());
                }
            } catch (Exception connTest) {
                log.warn("⚠️ Milvus connection test failed (may work later): {}", connTest.getMessage());
            }

            return client;
        } catch (Exception e) {
            // Catch SDK-specific errors (like bad URI format) and rethrow clearly
            log.error("❌ Failed to initialize Milvus Client: {}", e.getMessage(), e);
            throw new IllegalStateException(
                "Failed to initialize Milvus Client with URI: " + milvusUri +
                ". Error: " + e.getMessage(),
                e
            );
        }
    }
}

