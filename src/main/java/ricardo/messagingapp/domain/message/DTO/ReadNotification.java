package ricardo.messagingapp.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

public record ReadNotification(
        Long messageId,
        Username userWhoReadId
) {
}
