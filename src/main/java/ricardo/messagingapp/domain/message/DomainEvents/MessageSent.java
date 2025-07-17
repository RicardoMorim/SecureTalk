package ricardo.messagingapp.domain.message.DomainEvents;

import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

import java.time.LocalDateTime;

public record MessageSent(
        ConversationId conversationId,
        UserId senderId,
        UserId receiverId,
        LocalDateTime sentAt
) {
}