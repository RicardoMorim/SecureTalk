package ricardo.messagingapp.messagingcore.services;

import com.ricardo.auth.core.UserService;
import com.ricardo.auth.domain.user.AppRole;
import com.ricardo.auth.domain.user.User;
import com.ricardo.auth.domain.user.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ricardo.messagingapp.messagingcore.domain.conversation.Conversation;
import ricardo.messagingapp.messagingcore.domain.message.DTO.MessagePayload;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageDeleted;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageEdited;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageRead;
import ricardo.messagingapp.messagingcore.domain.message.DomainEvents.MessageSent;
import ricardo.messagingapp.messagingcore.domain.message.Message;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;
import ricardo.messagingapp.messagingcore.repositories.ConversationRepository;
import ricardo.messagingapp.messagingcore.repositories.MessageRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int MESSAGE_EDIT_DELETE_TIME_LIMIT_SECONDS = 900; // 15 minutes

    private final EncryptionService encryptionService;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserService<User, AppRole, UUID> userService;
    private final ApplicationEventPublisher eventPublisher;


    public boolean sendMessage(MessagePayload payload, UUID currentUserId) {
        if (payload == null || payload.getContent() == null || payload.getContent().getContent().isEmpty() || payload.getOtherUser() == null) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }

        User otherUser = userService.getUserByUserName(payload.getOtherUser().getUsername());

        Conversation conversation = conversationRepository.findByParticipants(Set.of(currentUserId, otherUser.getId()));

        if (conversation == null) {
            // If conversation does not exist, create a new one
            conversation = Conversation.createDM(Set.of(currentUserId, otherUser.getId()));
            conversationRepository.save(conversation);
        }

        validateMessageCreation(currentUserId, MessageContent.valueOf(payload.getContent().getContent()), conversation);

        Message message = Message.create(
                currentUserId,
                MessageContent.valueOf(payload.getContent().getContent()),
                conversation.getId()
        );

        return sendMessage(message);
    }

    private static void validateMessageCreation(UUID senderId, MessageContent content, Conversation conversation) {
        if (senderId == null || content == null) {
            throw new IllegalArgumentException("Sender, and content cannot be null");
        }

        if (!conversation.getParticipants().contains(senderId)) {
            throw new IllegalArgumentException("Both sender and receiver must be part of the conversation");
        }

        if (content.getContent() == null || content.getContent().isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or empty");
        }
    }

    public boolean sendMessage(Message message) {
        if (message == null || message.getContent() == null || message.getContent().getContent().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        Conversation conversation = conversationRepository.findById(message.getConversationId());
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found.");
        }

        String encryptedContent = encryptionService.encrypt(message.getContent().getContent());

        message.setContent(MessageContent.valueOf(encryptedContent));

        int saved = messageRepository.save(message);
        if (saved <= 0) {
            return false;
        }

        UUID receiverId = conversation.getParticipants()
                .stream()
                .filter(id -> !id.equals(message.getSenderId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found in conversation"));

        User sender = userService.getUserById(message.getSenderId());
        User receiver = userService.getUserById(receiverId);
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("Sender or receiver not found.");
        }

        eventPublisher.publishEvent(new MessageSent(
                message.getId(),
                Username.valueOf(receiver.getUsername()),
                Username.valueOf(sender.getUsername()),
                message.getSentAt().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                encryptedContent
        ));

        return true;
    }

    public boolean updateMessage(UUID messageId, MessageContent newContent, UUID currentUserId) {
        Message message = findMessageAndValidateOwnership(messageId, currentUserId);
        validateMessageEditDeleteTimeLimit(message);
        validateIsLastMessageInConversation(messageId, message.getConversationId());

        message.editContent(newContent, currentUserId);
        String encryptedContent = encryptionService.encrypt(newContent.getContent());
        message.setContent(MessageContent.valueOf(encryptedContent));

        User userWhoEdited = userService.getUserById(currentUserId);
        User otherUser = findOtherUserInConversation(message.getConversationId(), currentUserId);

        int updatedRows = messageRepository.update(message);
        if (updatedRows > 0){
            if (userWhoEdited == null) {
                throw new IllegalArgumentException("User not found.");
            }
            eventPublisher.publishEvent(
                    new MessageEdited(message.getId(), Username.valueOf(otherUser.getUsername()), Username.valueOf(userWhoEdited.getUsername()), Instant.now(), MessageContent.valueOf(encryptedContent))
            );
        }
        return updatedRows > 0;
    }

    public boolean deleteMessage(UUID messageId, UUID currentUserId) {
        Message message = findMessageAndValidateOwnership(messageId, currentUserId);
        validateMessageEditDeleteTimeLimit(message);
        validateIsLastMessageInConversation(messageId, message.getConversationId());

        User userWhoDeleted = userService.getUserById(currentUserId);
        User otherUser = findOtherUserInConversation(message.getConversationId(), currentUserId);

        if (messageRepository.delete(messageId) > 0){
            eventPublisher.publishEvent(
                    new MessageDeleted(messageId, Username.valueOf(otherUser.getUsername()), Username.valueOf(userWhoDeleted.getUsername()), Instant.now())
            );
            return true;
        }

        return false;
    }

    private Message findMessageAndValidateOwnership(UUID messageId, UUID currentUserId) {
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        if (!message.getSenderId().equals(currentUserId)) {
            throw new SecurityException("You are not allowed to modify this message.");
        }

        return message;
    }

    private void validateMessageEditDeleteTimeLimit(Message message) {
        if (message.getSentAt().plusSeconds(MESSAGE_EDIT_DELETE_TIME_LIMIT_SECONDS).isBefore(Instant.now())) {
            throw new IllegalArgumentException("You can only edit/delete messages within 15 minutes of sending");
        }
    }

    private void validateIsLastMessageInConversation(UUID messageId, UUID conversationId) {
        List<Message> messages = messageRepository.findLatestMessagesFromConversation(conversationId, 1);

        if (messages.isEmpty() || !messages.getFirst().getId().equals(messageId)) {
            throw new IllegalArgumentException("You can only edit/delete the last message of the conversation");
        }
    }

    private User findOtherUserInConversation(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found.");
        }

        UUID otherUserId = conversation.getParticipants().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Other participant not found in conversation."));

        User otherUser = userService.getUserById(otherUserId);
        if (otherUser == null) {
            throw new IllegalArgumentException("Other user not found.");
        }

        return otherUser;
    }

    /**
     * @param userWhoSent - User that the logged user is conversating with (the one that sent the message he is seeing)
     * @param loggedUser - The logged user (seen the message)
     *
     * @return true if the conversation was marked as read, false otherwise
     */
    public boolean markConversationAsRead(Username userWhoSent, UUID loggedUser) {
        if (userWhoSent == null || userWhoSent.getUsername() == null || userWhoSent.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Validate userId
        if (loggedUser == null) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }

        // Retrieve the conversation
        Conversation conversation = conversationRepository.findDMByNameAndUUID(userWhoSent.getUsername(), loggedUser);
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found.");
        }

        // Mark all messages in the conversation as read
        List<Message> messages = messageRepository.findAllFromConversation(conversation.getId());

        User userWhoRead = userService.getUserById(loggedUser);

        if (messages.isEmpty()) {
            // No messages to mark as read
            return false;
        }

        if (userWhoRead == null) {
            throw new IllegalArgumentException("User not found.");
        }

        for (Message message : messages) {
            if (message.getSenderId().equals(loggedUser) || message.isSeen()) {
                // If the message is sent by the user or already read, we can stop
                break;
            }

            markMessageAsRead(message.getId(), loggedUser);
            eventPublisher.publishEvent(
                    new MessageRead(message.getId(), Username.valueOf(userWhoRead.getUsername()), userWhoSent, Instant.now())
            );
            break; // the logic always checks only for one message read, so we mark only the latest one seen, and we know all the previous sent ones, are also read.
        }

        return true;
    }

    public Message getMessage(UUID messageId) {
        // Validate messageId
        if (messageId == null) {
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

    public List<Message> getConversation(Username otherUser, UUID userId) {
        // Validate conversationId
        if (otherUser == null || userId == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        Conversation conversation = conversationRepository.findDMByNameAndUUID(otherUser.getUsername(), userId);

        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found.");
        }

        if (!conversation.hasParticipant(userId)) {
            throw new IllegalArgumentException("User is not a participant in this conversation.");
        }

        // Retrieve messages for the conversation from repository
        List<Message> messages = messageRepository.findAllFromConversation(conversation.getId());

        return decryptMessages(messages);
    }

    public List<Username> getLatestXConversationNames(UUID userId, int limit) {
        // Validate userId
        if (userId == null) {
            throw new IllegalArgumentException("User ID must be a positive number.");
        }

        // Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number.");
        }

        // Retrieve latest conversations for the user from repository
        return messageRepository.findLatestConversationNames(userId, limit);
    }

    public List<Message> getLatestXMessagesFromConversationName(Username otherUser, UUID currentUser, int limit) {
        // Validate conversationId
        if (otherUser == null || otherUser.getUsername() == null || otherUser.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Validate currentUser
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user ID must be a positive number.");
        }

        // Validate limit
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive number.");
        }

        Conversation conversation = conversationRepository.findDMByNameAndUUID(otherUser.getUsername(), currentUser);

        // Retrieve latest messages for the conversation from repository
        Iterable<Message> messages = messageRepository.findLatestMessagesFromConversation(conversation.getId(), limit);
        return decryptMessages(messages);
    }

    public List<Message> getPagedConversation(Username otherUser, UUID currentUser, int page, int size) {
        // Validate conversationId
        if (otherUser == null || currentUser == null || otherUser.getUsername().isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        // Validate pagination parameters
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("Page must be non-negative and size must be positive.");
        }

        Conversation conversation = conversationRepository.findDMByNameAndUUID(otherUser.getUsername(), currentUser);

        if (conversation == null) {
            throw new IllegalArgumentException("Conversation not found.");
        }

        if (!conversation.hasParticipant(currentUser)) {
            throw new IllegalArgumentException("User is not a participant in this conversation.");
        }

        // Retrieve paged messages for the conversation from repository
        Iterable<Message> messages = messageRepository.findPagedByConversation(conversation.getId(), page, size);
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


    private void markMessageAsRead(UUID messageId, UUID userId) {
        // Validate messageId
        if (messageId == null) {
            throw new IllegalArgumentException("Message ID must be a UUID.");
        }

        // Retrieve the message
        Message message = messageRepository.findById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("Message not found.");
        }

        // Update the status to read
        message.markAsRead();
        messageRepository.markAsRead(messageId);
    }
}
