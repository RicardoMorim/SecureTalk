package ricardo.messagingapp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ricardo.messagingapp.domain.message.*;
import ricardo.messagingapp.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.repositories.MessageRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final EncryptionService encryptionService;
    private final MessageRepository messageRepository;

    public boolean sendMessage(MessagePayload payload, UserId currentUserId) {
        // Validate payload
        if (payload == null || payload.getContent() == null || payload.getContent().isEmpty() || payload.getConversationId() == null || payload.getConversationId().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }

        if(!ConversationId.validateConversationId(payload.getConversationId(), currentUserId)) {
            throw new IllegalArgumentException("Invalid conversation ID.");
        }

        // Create a new message
        Message message = Message.create(
                currentUserId,
                ConversationId.extractTheOtherUserId(payload.getConversationId(), currentUserId),
                MessageContent.valueOf(payload.getContent())
        );

        return sendMessage(message);
    }

    public boolean sendMessage(Message message) {
        // Validate message
        if (message == null || message.getContent() == null || message.getContent().getContent().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        String encryptedContent = encryptionService.encrypt(message.getContent().getContent());

        message.setContent(MessageContent.valueOf(encryptedContent));
        message.markAsDelivered();

        // Save message to repository
        messageRepository.save(message);

        return true;
    }

    public boolean updateMessage(Long messageId, MessageContent newContent, UserId currentUserId) {
        Message message = messageRepository.findById(messageId);
        if (message == null) throw new IllegalArgumentException("Message not found.");

        // Only the sender can edit
        if (!message.getSenderId().equals(currentUserId)) {
            throw new SecurityException("You are not allowed to edit this message.");
        }

        message.editContent(newContent, currentUserId);
        String encryptedContent = encryptionService.encrypt(newContent.getContent());
        message.setContent(MessageContent.valueOf(encryptedContent));
        return messageRepository.update(message) > 0;
    }


    public boolean deleteMessage(Long messageId, UserId currentUserId) {
        Message message = messageRepository.findById(messageId);
        if (message == null) throw new IllegalArgumentException("Message not found.");

        // Only the sender can delete
        if (!message.getSenderId().equals(currentUserId)) {
            throw new SecurityException("You are not allowed to delete this message.");
        }

        return messageRepository.delete(messageId) > 0;
    }

    public Message getMessage(Long messageId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Retrieve message from repository
        Message message = messageRepository.findById(messageId);

        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        String decryptedContent = encryptionService.decrypt(message.getContent().getContent());

        message.setContent(MessageContent.valueOf(decryptedContent));

        return message;
    }

    public List<Message> getConversation(ConversationId conversationId) {
        // Validate conversationId
        if (conversationId == null || conversationId.getId() == null || conversationId.getId().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Retrieve messages for the conversation from repository
        Iterable<Message> messages = messageRepository.findAllFromConversation(conversationId);

        return decryptMessages(messages);
    }

    public List<ConversationId> getLatestXConversations(UserId userId, int limit) {
        // Validate userId
        if (userId == null || userId.getId() == null || userId.getId() <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }

        // Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number.");
        }

        // Retrieve latest conversations for the user from repository
        return (List<ConversationId>) messageRepository.findLatestConversations(userId, limit);
    }

    public List<Message> getLatestXMessagesFromConversation(ConversationId conversationId, int limit) {
        // Validate conversationId
        if (conversationId == null || conversationId.getId() == null || conversationId.getId().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number.");
        }

        // Retrieve latest messages for the conversation from repository
        Iterable<Message> messages = messageRepository.findLatestMessagesFromConversation(conversationId, limit);
        return decryptMessages(messages);
    }

    public List<UserId> getUsersWithConversationWithUser(UserId userId) {
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
        Iterable<Message> messages = messageRepository.findPagedByConversation(conversationId, page, size);
        return decryptMessages(messages);
    }

    private List<Message> decryptMessages(Iterable<Message> messages) {
        List<Message> messageList = new ArrayList<>();
        for (Message message : messages) {
            String decryptedContent = encryptionService.decrypt(message.getContent().getContent());
            message.setContent(MessageContent.valueOf(decryptedContent));
            messageList.add(message);
        }

        return messageList;
    }


    public boolean markMessageAsRead(Long messageId, UserId userId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Retrieve the message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        // Validate userId
        ConversationId conversationId = message.getConversationId();

        if (conversationId == null || conversationId.getId() == null || conversationId.getId().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        if (!ConversationId.validateConversationId(conversationId.getId(), userId)) {
            throw new IllegalArgumentException("Invalid conversation ID for the given user ID.");
        }

        // Update the status to read
        message.markAsRead();

        // Save the updated message
        return messageRepository.update(message) > 0;
    }


    public ConversationId getMessageConversationId(Long messageId){
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Retrieve the message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        // Return the conversation ID
        return message.getConversationId();
    }

    public boolean canUserEditMessage(Long messageId, UserId userId) {
        // Validate messageId
        if (messageId == null || messageId <= 0) {
            throw new IllegalArgumentException("Message ID must be a positive number.");
        }

        // Validate userId
        if (userId == null || userId.getId() == null || userId.getId() <= 0) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }

        // Retrieve the message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        // Check if the user is the sender of the message
        return message.getSenderId().equals(userId);
    }
}

