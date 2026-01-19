package ricardo.messagingapp.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;

import java.util.UUID;

public record ReadNotification(
        UUID messageId,
        Username userWhoReadId
) {
}
