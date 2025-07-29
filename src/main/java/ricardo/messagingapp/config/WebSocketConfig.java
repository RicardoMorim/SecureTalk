package ricardo.messagingapp.config;

import com.ricardo.auth.core.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ricardo.messagingapp.websocket.WebSocketHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final JwtService jwtService;


    public WebSocketConfig(WebSocketHandler webSocketHandler, JwtService jwtService) {
        this.webSocketHandler = webSocketHandler;
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS()
                .setInterceptors(new JwtHandshakeInterceptor(jwtService));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
}