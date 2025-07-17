package ricardo.messagingapp.domain.message;

public class MessageContent {
    private String content;

    private MessageContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }
        this.content = content;
    }

    public static MessageContent valueOf(String content) {
        return new MessageContent(content);
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "MessageContent{" +
                "content='" + content + '\'' +
                '}';
    }
}
