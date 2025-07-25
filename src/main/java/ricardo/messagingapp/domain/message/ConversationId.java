package ricardo.messagingapp.domain.message;


import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ConversationId {

    public final String id;

    private ConversationId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        this.id = id;
    }

    public static ConversationId fromConversationId(String id){
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        return new ConversationId(id);
    }


    public static ConversationId valueOf(UserId senderId, UserId receiverId) {

        if (senderId == null || receiverId == null || senderId.getId() <= 0 || receiverId.getId() <= 0) {
            throw new IllegalArgumentException("Sender and receiver IDs must be positive numbers.");
        }

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver must be different users.");
        }

        String conversationId = senderId.getId() < receiverId.getId()
                ? senderId.getId() + "-" + receiverId.getId()
                : receiverId.getId() + "-" + senderId.getId();

        return new ConversationId(conversationId);
    }

    public static UserId extractTheOtherUserId(String conversationId, UserId userId) {
        // Validate conversationId
        if (conversationId == null || conversationId.isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        if (!validateConversationId(conversationId, userId)) {
            throw new IllegalArgumentException("Invalid conversation ID for the given user ID.");
        }

        String[] parts = conversationId.split("-");

        long otherUserId = parts[0].equals(userId.getId().toString()) ? Long.parseLong(parts[1]) : Long.parseLong(parts[0]);

        return UserId.valueOf(otherUserId);
    }

    public static boolean validateConversationId(String conversationId, UserId userId) {
        // Validate conversationId
        if (conversationId == null || conversationId.isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }

        String[] parts = conversationId.split("-");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid conversation ID format.");
        }

        if (!parts[0].equals(userId.getId().toString()) && !parts[1].equals(userId.getId().toString())) {
            return false;
        }

        if (parts[0].equals(parts[1])) {
            throw new IllegalArgumentException("Conversation ID cannot have the same user IDs.");
        }

        long id1;
        long id2;

        try {
            id1 = Long.parseLong(parts[0]);
            id2 = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Conversation ID must contain valid user IDs.");
        }

        if (id1 <= 0 || id2 <= 0) {
            throw new IllegalArgumentException("User IDs in conversation ID must be positive numbers.");
        }

        if (id1 > id2) {
            throw new IllegalArgumentException("User IDs in conversation ID must be in ascending order.");
        }

        return true;
    }

}
