package ricardo.messagingapp.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionRegistry {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void addSession(String userEmail, WebSocketSession session) {
        sessions.put(userEmail, session);
    }

    public WebSocketSession getSession(String userEmail) {
        return sessions.get(userEmail);
    }

    public void removeSession(String userEmail) {
        sessions.remove(userEmail);
    }
}
