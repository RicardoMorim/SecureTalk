package ricardo.messagingapp.domain.message.DomainEvents;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.UserId;

import java.time.LocalDateTime;

public record MessageRead(
        @NotNull @Positive
        Long messageId,
        @NotNull
        ConversationId conversationId,
        @NotNull
        UserId userWhoReadId,
        @NotNull
        LocalDateTime readAt
) {
}