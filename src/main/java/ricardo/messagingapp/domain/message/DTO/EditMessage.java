package ricardo.messagingapp.domain.message.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ricardo.messagingapp.domain.message.UserId;

public record EditMessage(
        @NotNull @Positive
        Long messageId,
        @NotNull @NotBlank
        String newDecryptedContent,
        @NotNull
        UserId receiverId,
        @NotNull @NotBlank
        String editedAt
) {
}