package com.yjshz.controller;

import com.yjshz.dto.Result;
import com.yjshz.service.IAiConsultantService;
import com.yjshz.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI咨询服务控制器
 * 为用户提供AI助手功能
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiConsultantController {
    
    @Autowired
    private IAiConsultantService aiConsultantService;
    
    /**
     * 与AI助手对话
     * @param message 用户消息
     * @return AI回复流
     */
    @GetMapping("/chat")
    public Flux<String> chatWithAI(@RequestParam String message) {
        // 使用用户ID作为会话记忆ID，确保每个用户有独立的对话上下文
        String memoryId = getCurrentUserMemoryId();
        log.info("用户 {} 调用AI助手: {}", memoryId, message);
        
        return aiConsultantService.chatWithAI(memoryId, message)
                .doOnNext(response -> log.debug("AI回复: {}", response))
                .doOnError(error -> log.error("AI服务调用失败", error));
    }
    
    /**
     * 检查AI服务状态
     * @return 服务状态
     */
    @GetMapping("/status")
    public Result checkAIStatus() {
        boolean isAvailable = aiConsultantService.isAIServiceAvailable();
        if (isAvailable) {
            return Result.ok("AI服务正常运行");
        } else {
            return Result.fail("AI服务暂时不可用");
        }
    }
    
    /**
     * 获取当前用户的会话记忆ID
     * 如果用户已登录，使用用户ID；否则使用会话ID
     */
    private String getCurrentUserMemoryId() {
        try {
            // 尝试获取当前登录用户
            com.yjshz.dto.UserDTO user = UserHolder.getUser();
            if (user != null) {
                return "user_" + user.getId();
            }
        } catch (Exception e) {
            log.debug("用户未登录，使用默认会话ID");
        }
        
        // 未登录用户使用会话ID或时间戳
        return "guest_" + System.currentTimeMillis();
    }
}