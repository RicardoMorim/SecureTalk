package ricardo.messagingapp.domain.message.DomainEvents;

import com.ricardo.auth.domain.user.Username;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MessageRead(
        @NotNull @Positive
        UUID messageId,
        @NotNull
        Username userWhoReadUsername,
        @NotNull
        Username userWhoSentUsername,
        @NotNull
        Instant readAt
) {
    @Override
    public String toString() {
        return "\nMessage Read: " + messageId.toString() + "\nReceiver: " + userWhoReadUsername.toString()+ "\nSent By: " + userWhoSentUsername.toString() + "\nRead at: " + readAt;
    }
}