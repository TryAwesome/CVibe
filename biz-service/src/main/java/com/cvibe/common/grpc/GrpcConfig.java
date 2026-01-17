package com.cvibe.common.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端配置
 * 
 * 配置连接到 ai-engine 和 search-service 的 gRPC 通道
 */
@Configuration
public class GrpcConfig {

    @Value("${grpc.ai-engine.host:localhost}")
    private String aiEngineHost;

    @Value("${grpc.ai-engine.port:50051}")
    private int aiEnginePort;

    @Value("${grpc.search-service.host:localhost}")
    private String searchServiceHost;

    @Value("${grpc.search-service.port:50052}")
    private int searchServicePort;

    @Value("${grpc.ai-engine.enabled:false}")
    private boolean aiEngineEnabled;

    @Value("${grpc.search-service.enabled:false}")
    private boolean searchServiceEnabled;

    private ManagedChannel aiEngineChannel;
    private ManagedChannel searchServiceChannel;

    @Bean
    public ManagedChannel aiEngineChannel() {
        if (!aiEngineEnabled) {
            return null;
        }
        aiEngineChannel = ManagedChannelBuilder
                .forAddress(aiEngineHost, aiEnginePort)
                .usePlaintext()  // 开发环境不使用 TLS
                .maxInboundMessageSize(50 * 1024 * 1024)  // 50MB
                .build();
        return aiEngineChannel;
    }

    @Bean
    public ManagedChannel searchServiceChannel() {
        if (!searchServiceEnabled) {
            return null;
        }
        searchServiceChannel = ManagedChannelBuilder
                .forAddress(searchServiceHost, searchServicePort)
                .usePlaintext()
                .maxInboundMessageSize(50 * 1024 * 1024)
                .build();
        return searchServiceChannel;
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (aiEngineChannel != null) {
                aiEngineChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
            if (searchServiceChannel != null) {
                searchServiceChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
