package ricardo.messagingapp.config;

import com.ricardo.auth.core.JwtService;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.Message;
import ricardo.messagingapp.domain.message.MessageStatus;
import ricardo.messagingapp.domain.message.UserId;
import ricardo.messagingapp.services.MessageService;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class JwtHandshakeInterceptor implements HandshakeInterceptor {


    private final JwtService jwtService;


    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes
    ) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token != null && jwtService.isTokenValid(token)) {
                String email = jwtService.extractSubject(token);
                attributes.put("email", email);
                return true;
            }
        }
        return false; // Reject the connection
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // nothing to do here initial data is sent in afterConnectionEstablished
    }
}
