package ricardo.messagingapp.domain.message.DomainEvents;

import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

import java.time.LocalDateTime;

public record MessageEdited(
        Long messageId,
        ConversationId conversationId,
        UserId editedBy,
        LocalDateTime editedAt
) {
}