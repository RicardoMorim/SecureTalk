package ricardo.messagingapp.websocket;

import com.google.gson.Gson;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AuthUser;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ricardo.messagingapp.domain.message.*;
import ricardo.messagingapp.domain.message.DTO.ConversationPayload;
import ricardo.messagingapp.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.services.MessageService;

import java.io.IOException;
import java.util.List;


@Component
public class WebSocketHandler extends TextWebSocketHandler {

    public static final int PAGE_SIZE = 10;
    public static final int CONVERSATIONS_PAGE_SIZE = 5;
    public static int FIRST_PAGE = 0;

    private final WebSocketSessionRegistry sessionRegistry;
    private final UserService userService;
    private final MessageService messageService;

    public WebSocketHandler(WebSocketSessionRegistry sessionRegistry, UserService userService, MessageService messageService) {
        this.sessionRegistry = sessionRegistry;
        this.userService = userService;
        this.messageService = messageService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = (String) session.getAttributes().get("email");
        sessionRegistry.addSession(email, session);
        sendInitialData(session, email);
    }

    private void sendInitialData(WebSocketSession session, String email) {
        // send the latest 5 conversations to the user
        // for each conversation, we will send up to 10 messages, and a unread count
        AuthUser user = userService.getUserByEmail(email);
        List<ConversationId> conversations = messageService.getLatestXConversations(UserId.valueOf(Long.valueOf(user.getId().toString())), CONVERSATIONS_PAGE_SIZE);
        for (ConversationId conversationId : conversations) {
            List<Message> messages = messageService.getPagedConversation(conversationId, FIRST_PAGE, PAGE_SIZE);

            int unreadCount = 0;
            // as messages are sorted by date, we count from the newest to the oldest, and stop as soon as one is read or the sender is the current user (he has readed all the messages above one sent by him)
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message.getStatus() == MessageStatus.READ || message.getSenderId().equals(UserId.valueOf(Long.valueOf(user.getId().toString())))) {
                    break;
                }
                unreadCount++;
            }

            List<String> messageContents = messages.stream()
                    .map(Message::getContent)
                    .map(MessageContent::getContent)
                    .toList();
            try {

            session.sendMessage(new TextMessage(new Gson().toJson(
                    new ConversationPayload(conversationId.getId(), String.valueOf(ConversationId.extractTheOtherUserId(conversationId.getId(), UserId.valueOf(Long.valueOf((String) user.getId()))).getId()), messageContents, unreadCount, FIRST_PAGE, PAGE_SIZE)
            )));

            }catch (IOException e) {
                e.printStackTrace();
                try {
                    session.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }
}
