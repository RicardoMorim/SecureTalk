package ricardo.messagingapp.websocket;

import com.google.gson.Gson;
import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.AuthUser;
import com.ricardo.auth.domain.user.User;
import com.ricardo.auth.domain.user.Username;
import lombok.extern.slf4j.Slf4j;
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
import java.util.UUID;


@Component
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    public static final int PAGE_SIZE = 10;
    public static final int CONVERSATIONS_PAGE_SIZE = 5;
    public static int FIRST_PAGE = 0;

    private final WebSocketSessionRegistry sessionRegistry;
    private final UserService<User, AppRole, UUID> userService;
    private final MessageService messageService;

    public WebSocketHandler(WebSocketSessionRegistry sessionRegistry, UserService<User, AppRole, UUID> userService, MessageService messageService) {
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
        User user = userService.getUserByEmail(email);

        List<Username> conversations = messageService.getLatestXConversationNames(user.getId(), CONVERSATIONS_PAGE_SIZE);
        for (Username conversationName : conversations) {
            List<Message> messages = messageService.getPagedConversation(conversationName, user.getId(), FIRST_PAGE, PAGE_SIZE);

            int unreadCount = 0;
            // as messages are sorted by date, we count from the newest to the oldest, and stop as soon as one is read or the sender is the current user (he has readed all the messages above one sent by him)
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (message.isSeen() || message.getSenderId().equals(user.getId())) {
                    break;
                }
                unreadCount++;
            }

            List<MessageContent> messageContents = messages.stream()
                    .map(Message::getContent)
                    .map(MessageContent::getContent).map(MessageContent::valueOf)
                    .toList();
            try {

            session.sendMessage(new TextMessage(new Gson().toJson(
                    new ConversationPayload(conversationName, messageContents, unreadCount, FIRST_PAGE, PAGE_SIZE)
            )));

            }catch (IOException e) {
                log.error("There was an error in sending initial data. closing connnection.\n Email of the user trying to get the data: {}. Exception: ", email, e);
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
