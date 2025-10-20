package com.yjshz.service;

import reactor.core.publisher.Flux;

/**
 * AI咨询服务接口
 * 用于调用consultant模块的AI功能
 */
public interface IAiConsultantService {
    
    /**
     * 与AI助手对话
     * @param memoryId 会话记忆ID
     * @param message 用户消息
     * @return AI回复流
     */
    Flux<String> chatWithAI(String memoryId, String message);
    
    /**
     * 获取AI助手服务状态
     * @return 服务状态
     */
    boolean isAIServiceAvailable();
}