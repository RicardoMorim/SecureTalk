package ricardo.messagingapp.domain.message.DTO;

import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

public record ReadNotification(
        Long messageId,
        UserId userWhoReadId,
        ConversationId conversationId
) {
}
