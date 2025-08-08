package ricardo.messagingapp.domain.message.DomainEvents;

import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

import java.time.LocalDateTime;

public record MessageDeleted (
        Long messageId,
        ConversationId conversationId,
        UserId userWhoDeletedId,
        LocalDateTime deletedAt
){
}
