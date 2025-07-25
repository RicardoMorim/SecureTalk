package ricardo.messagingapp.domain.message.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationPayload {
    private final String conversationId;
    private final String otherUserName;
    private final List<String> messages;
    private final int unreadCount;
    private final int page;
    private final int pageSize;
}
