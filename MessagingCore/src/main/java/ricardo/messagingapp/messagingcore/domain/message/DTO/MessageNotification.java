package ricardo.messagingapp.messagingcore.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;


import java.time.Instant;

public record MessageNotification(MessageContent content, Username senderName, Username receiverName, Instant timestamp) {
}
