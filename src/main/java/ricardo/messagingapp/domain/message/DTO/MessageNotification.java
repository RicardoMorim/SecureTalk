package ricardo.messagingapp.domain.message.DTO;

import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.DomainEvents.MessageSent;
import ricardo.messagingapp.domain.message.UserId;

import java.time.Instant;

public record MessageNotification(String content, UserId senderId, Instant timestamp, ConversationId conversationId) {
}
