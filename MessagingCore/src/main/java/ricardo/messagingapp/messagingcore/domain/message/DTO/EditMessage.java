package ricardo.messagingapp.messagingcore.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;

import java.time.Instant;
import java.util.UUID;

public record EditMessage(
        @NotNull @Positive
        UUID messageId,
        @NotNull @NotBlank
        MessageContent newDecryptedContent,
        @NotNull @NotBlank
        Username receiverUsername,
        @NotNull @NotBlank
        Instant editedAt
) {
}