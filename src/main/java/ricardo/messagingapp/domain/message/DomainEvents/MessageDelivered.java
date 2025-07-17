package ricardo.messagingapp.domain.message.DomainEvents;

import ricardo.messagingapp.domain.message.ConversationId;

import java.time.LocalDateTime;

public record MessageDelivered(
        Long messageId,
        ConversationId conversationId,
        LocalDateTime deliveredAt
) {
}