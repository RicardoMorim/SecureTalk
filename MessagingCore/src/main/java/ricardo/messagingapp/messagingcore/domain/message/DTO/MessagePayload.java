package ricardo.messagingapp.messagingcore.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import lombok.AllArgsConstructor;
import lombok.Getter;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;

@AllArgsConstructor
@Getter
public class MessagePayload {
    private final Username otherUser;
    private final MessageContent content;
}
