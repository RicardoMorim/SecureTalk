package ricardo.messagingapp.config;

import com.ricardo.auth.core.JwtService;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ricardo.messagingapp.websocket.WebSocketHandler;

import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final JwtService jwtService;
    private final UserService<User, AppRole, UUID> userService;


    public WebSocketConfig(WebSocketHandler webSocketHandler, JwtService jwtService, UserService<User, AppRole, UUID> userService) {
        this.webSocketHandler = webSocketHandler;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS()
                .setInterceptors(new JwtHandshakeInterceptor(jwtService, userService));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
}