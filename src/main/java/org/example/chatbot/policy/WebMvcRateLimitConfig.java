package org.example.chatbot.policy;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcRateLimitConfig implements WebMvcConfigurer {
    private final ChatbotRateLimitInterceptor chatbotRateLimitInterceptor;

    public WebMvcRateLimitConfig(ChatbotRateLimitInterceptor chatbotRateLimitInterceptor) {
        this.chatbotRateLimitInterceptor = chatbotRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(chatbotRateLimitInterceptor)
                .addPathPatterns("/api/chatbot", "/api/ai-guidance");
    }
}
