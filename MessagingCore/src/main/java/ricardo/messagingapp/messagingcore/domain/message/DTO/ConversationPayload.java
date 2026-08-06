package ricardo.messagingapp.messagingcore.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationPayload {
    private final Username otherUserName;
    private final List<MessageContent> messages;
    private final int unreadCount;
    private final int page;
    private final int pageSize;
}
