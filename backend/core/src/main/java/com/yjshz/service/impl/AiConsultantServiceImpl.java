package com.yjshz.service.impl;

import com.yjshz.service.IAiConsultantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;

/**
 * AI咨询服务实现
 * 通过HTTP调用consultant模块的AI服务
 */
@Slf4j
@Service
public class AiConsultantServiceImpl implements IAiConsultantService {
    
    @Value("${ai.consultant.url:http://localhost:8080}")
    private String aiConsultantUrl;
    
    private WebClient webClient;
    
    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create();
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(aiConsultantUrl)
                .build();
    }
    
    @Override
    public Flux<String> chatWithAI(String memoryId, String message) {
        log.info("调用AI服务: memoryId={}, message={}", memoryId, message);
        
        return webClient.get()
                .uri("/chat?memoryId={memoryId}&message={message}", memoryId, message)
                .accept(MediaType.TEXT_HTML)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    log.error("AI服务调用失败: {}", response.statusCode());
                    return Mono.error(new RuntimeException("AI服务暂时不可用"));
                })
                .bodyToFlux(String.class)
                .doOnError(error -> log.error("AI服务调用异常", error))
                .onErrorResume(error -> Flux.just("抱歉，AI助手暂时无法响应，请稍后再试。"));
    }
    
    @Override
    public boolean isAIServiceAvailable() {
        try {
            String response = webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return response != null && response.contains("\"status\":\"UP\"");
        } catch (Exception e) {
            log.warn("AI服务健康检查失败", e);
            return false;
        }
    }
}