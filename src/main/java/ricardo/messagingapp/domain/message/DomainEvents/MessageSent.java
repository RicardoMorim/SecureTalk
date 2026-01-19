package ricardo.messagingapp.domain.message.DomainEvents;

import com.ricardo.auth.domain.user.Username;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageSent(
        UUID messageId,
        Username receiverUsername,
        Username senderUsername,
        LocalDateTime sentAt,
        String encryptedContent
) {
    @Override
    public String toString() {
        return "\nMessage Sent: " + messageId.toString() + "\nReceiver: " + receiverUsername.toString()+ "\nSent by: " + senderUsername + "\nSent at: " + sentAt;
    }
}