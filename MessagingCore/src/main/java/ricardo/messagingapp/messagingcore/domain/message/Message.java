package ricardo.messagingapp.messagingcore.domain.message;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Message {

    private UUID id;

    private UUID conversationId;

    private UUID senderId;

    private MessageContent content;

    private Instant sentAt;

    private boolean edited;

    private boolean seen;

    private Instant editedAt;

    protected Message() {
    }

    private Message(UUID senderId, MessageContent content, UUID conversation) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.content = content;
        this.conversationId = conversation;
        this.sentAt = Instant.now();
        this.edited = false;
        this.seen = false;
        this.editedAt = null;
    }



    public static Message create(UUID senderId, MessageContent content, UUID conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation cannot be null");
        }
        return new Message(senderId, content, conversationId);
    }

    public void markAsRead() {
        if (this.seen) {
            throw new IllegalStateException("Message is already read");
        }
        this.seen = true;
    }

    public void editContent(MessageContent newContent, UUID editedBy) {
        if (!editedBy.equals(this.senderId)) {
            throw new IllegalArgumentException("Only the sender can edit their message");
        }

        this.content = newContent;
        this.edited = true;
        this.editedAt = Instant.now();
    }

    public void setContent(MessageContent content) {
        if (content == null || content.getContent() == null || content.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }
        this.content = content;
    }
}