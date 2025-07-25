package ricardo.messagingapp.domain.message.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessagePayload {
    private final String conversationId;
    private final String content;
}
