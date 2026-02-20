package com.oyun.magnolia.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/oda");
        config.setApplicationDestinationPrefixes("/oyun");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // İNTERNETTEN GELEN BAĞLANTILARA (CORS) İZİN VERİYORUZ:
        registry.addEndpoint("/magnolia-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}