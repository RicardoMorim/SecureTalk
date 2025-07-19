package ricardo.messagingapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ricardo.messagingapp.domain.message.ConversationId;
import ricardo.messagingapp.domain.message.Message;
import ricardo.messagingapp.domain.message.MessageStatus;
import ricardo.messagingapp.domain.message.UserId;
import ricardo.messagingapp.repositories.MessageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    public boolean sendMessage(Message message) {
        // Validate message
        if (message == null || message.getContent() == null || message.getContent().getContent().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        // Save message to repository
        messageRepository.save(message);

        return true;
    }

    public boolean updateMessage(Message message) {
        // Validate message
        if (message == null || message.getMessageId() == null || message.getContent() == null || message.getContent().getContent().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        // Update message in repository
        int updatedRows = messageRepository.update(message);

        return updatedRows > 0;
    }

    public boolean deleteMessage(Long messageId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Delete message from repository
        int deletedRows = messageRepository.delete(messageId);

        return deletedRows > 0;
    }

    public Message getMessage(Long messageId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Retrieve message from repository
        return messageRepository.findById(messageId);
    }

    public List<Message> getConversation(ConversationId conversationId) {
        // Validate conversationId
        if (conversationId == null || conversationId.getId() == null || conversationId.getId().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Retrieve messages for the conversation from repository
        return (List<Message>) messageRepository.findAllFromConversation(conversationId);
    }

    public List<UserId> getUserWithConversationWithUser(UserId userId) {
        // Validate userId
        if (userId == null || userId.getId() == null || userId.getId() <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }

        // Retrieve users with conversations with the specified user
        return (List<UserId>) messageRepository.findAllUsersWithConversationWithUser(userId);
    }

    public List<Message> getPagedConversation(ConversationId conversationId, int page, int size) {
        // Validate conversationId
        if (conversationId == null || conversationId.getId() == null || conversationId.getId().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Validate pagination parameters
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be non-negative and size must be positive.");
        }

        // Retrieve paged messages for the conversation from repository
        return (List<Message>) messageRepository.findPagedByConversation(conversationId, page, size);
    }



    public boolean markMessageAsRead(Long messageId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Retrieve the message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        // Update the status to read
        message.markAsRead();

        // Save the updated message
        return updateMessage(message);
    }
}

