package ricardo.messagingapp.domain.message.DomainEvents;

import com.ricardo.auth.domain.user.Username;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ricardo.messagingapp.domain.message.MessageContent;

import java.time.Instant;
import java.util.UUID;

public record MessageEdited(
        @NotNull @Positive
        UUID messageId,
        @NotNull
        Username receiverUsername,
        @NotNull
        Username userWhoEditedUsername,
        @NotNull
        Instant editedAt,
        @NotNull @NotBlank
        MessageContent newEncryptedContent
) {

    @Override
    public String toString() {
        return "\nMessage Edited: " + messageId.toString() + "\nReceiver: " + receiverUsername.toString()+ "\nSent By: " + userWhoEditedUsername.toString() + "\nEdited at: " + editedAt;
    }
}