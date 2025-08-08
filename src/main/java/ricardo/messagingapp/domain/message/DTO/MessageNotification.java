package ricardo.messagingapp.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;


import java.time.Instant;

public record MessageNotification(String content, Username senderName, Username receiverName, Instant timestamp) {
}
