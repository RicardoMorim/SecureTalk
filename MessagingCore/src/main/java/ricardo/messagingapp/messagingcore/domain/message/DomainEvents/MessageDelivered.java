package ricardo.messagingapp.messagingcore.domain.message.DomainEvents;

import com.ricardo.auth.domain.user.Username;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageDelivered(
        UUID messageId,
        Username receiverUsername,
        LocalDateTime deliveredAt
) {
    @Override
    public String toString() {
        return "\nMessage Delivered: " + messageId.toString() + "\nReceiver: " + receiverUsername.toString() + "\nDelivered at: " + deliveredAt;
    }
}