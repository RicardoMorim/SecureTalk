package ricardo.messagingapp.domain.message.DTO;

import com.ricardo.auth.domain.user.Username;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessagePayload {
    private final Username otherUser;
    private final String content;
}
