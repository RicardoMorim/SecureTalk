package ricardo.messagingapp.domain.message;

import jakarta.persistence.Entity;
import lombok.Getter;
import org.springframework.data.domain.AbstractAggregateRoot;
import ricardo.messagingapp.domain.message.DomainEvents.MessageDelivered;
import ricardo.messagingapp.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.domain.message.DomainEvents.MessageSent;

import java.time.LocalDateTime;

@Getter
public class Message extends AbstractAggregateRoot<Message> {

    private Long messageId;
    private ConversationId conversationId;
    private UserId senderId;
    private UserId receiverId;
    private MessageContent content;
    private LocalDateTime sentAt;
    private MessageStatus status;
    private boolean isEdited;
    private LocalDateTime lastEditedTime;

    protected Message() {
    }



    /**
     * Constructor for creating a new message.
     * This constructor is only to be used when loading from the database.
     * * It should not be used for creating new messages, as it does not register any events.
     *
     * @param messageId
     * @param conversationId
     * @param senderId
     * @param receiverId
     * @param content
     * @param sentAt
     * @param status
     * @param isEdited
     * @param lastEditedTime
     */
    public Message(Long messageId, ConversationId conversationId, UserId senderId, UserId receiverId, MessageContent content, LocalDateTime sentAt, MessageStatus status, boolean isEdited, LocalDateTime lastEditedTime) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = sentAt;
        this.status = status;
        this.isEdited = isEdited;
        this.lastEditedTime = lastEditedTime;

        // DO NOT register events in this constructor, as it is used for loading from the database
    }

    private Message(UserId senderId, UserId receiverId, MessageContent content) {
        this.messageId = null; // Will be set by repository
        this.conversationId = ConversationId.valueOf(senderId, receiverId);
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sentAt = LocalDateTime.now();
        this.status = MessageStatus.create();
        this.isEdited = false;
        this.lastEditedTime = null;

        // Publish domain event
        registerEvent(new MessageSent(this.conversationId, this.senderId, this.receiverId, this.sentAt));
    }

    public static Message create(UserId senderId, UserId receiverId, MessageContent content) {
        validateMessageCreation(senderId, receiverId, content);
        return new Message(senderId, receiverId, content);
    }

    public void markAsDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = this.status.next();
            registerEvent(new MessageDelivered(this.messageId, this.conversationId, LocalDateTime.now()));
        }
    }

    public void markAsRead(UserId readBy) {
        if (this.status == MessageStatus.DELIVERED && readBy.equals(this.receiverId)) {
            this.status = this.status.next();
            registerEvent(new MessageRead(this.messageId, this.conversationId, readBy, LocalDateTime.now()));
        }
    }

    public void editContent(MessageContent newContent, UserId editedBy) {
        if (!editedBy.equals(this.senderId)) {
            throw new IllegalArgumentException("Only the sender can edit their message");
        }

        this.content = newContent;
        this.isEdited = true;
        this.lastEditedTime = LocalDateTime.now();

        registerEvent(new MessageEdited(this.messageId, this.conversationId, editedBy, this.lastEditedTime));
    }

    private static void validateMessageCreation(UserId senderId, UserId receiverId, MessageContent content) {
        if (senderId == null || receiverId == null || content == null) {
            throw new IllegalArgumentException("Sender, receiver, and content cannot be null");
        }

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender must be different from receiver");
        }
    }
}