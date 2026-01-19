package ricardo.messagingapp.domain.message.DomainEvents;

import com.ricardo.auth.domain.user.Username;

import java.time.Instant;
import java.util.UUID;

public record MessageDeleted (
        UUID messageId,
        Username receiverUsername,
        Username userWhoDeletedUsername,
        Instant deletedAt
){
    @Override
    public String toString() {
        return "\nMessage Deleted: " + messageId.toString() + "\nReceiver: " + receiverUsername.toString()+ "\nDeletedBy: " + userWhoDeletedUsername + "\nDeleted at: " + deletedAt;
    }
}
