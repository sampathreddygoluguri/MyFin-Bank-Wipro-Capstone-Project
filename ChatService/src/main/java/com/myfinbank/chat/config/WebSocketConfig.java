package com.myfinbank.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Autowired
	private StompAuthChannelInterceptor authInterceptor;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// clients subscribe to /topic/*
		config.enableSimpleBroker("/topic");
		// server mappings for client messages
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
	    registry.addEndpoint("/ws")
	            .setAllowedOriginPatterns(
	                    "http://localhost:8081",
	                    "http://localhost:8082"
	            )
	            .withSockJS();
	}



	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}



}
