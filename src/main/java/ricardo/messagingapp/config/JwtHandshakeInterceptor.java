package ricardo.messagingapp.config;

import com.ricardo.auth.core.JwtService;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.User;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class JwtHandshakeInterceptor implements HandshakeInterceptor {


    private final JwtService jwtService;
    private final UserService<User, AppRole, UUID> userService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes
    ) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            String token = Arrays.stream(cookies)
                    .filter(c -> "access_token".equals(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);

            if (token != null && jwtService.isTokenValid(token)) {
                String email = jwtService.extractSubject(token);
                User user = userService.getUserByEmail(email);

                attributes.put("email", email);
                attributes.put("username", user.getUsername());
                attributes.put("roles", user.getRoles());
                return true;
            }
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false; // Reject the connection
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // nothing to do here initial data is sent in afterConnectionEstablished in the websocket handler
    }
}
