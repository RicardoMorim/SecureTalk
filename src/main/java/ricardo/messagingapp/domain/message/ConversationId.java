package ricardo.messagingapp.domain.message;


public class ConversationId {

    public final String id;

    private ConversationId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty.");
        }
        this.id = id;
    }


    public static ConversationId valueOf(UserId senderId, UserId receiverId) {

        if (senderId == null || receiverId == null || senderId.getId() <= 0 || receiverId.getId() <= 0) {
            throw new IllegalArgumentException("Sender and receiver IDs must be positive numbers.");
        }

        String conversationId = senderId.getId() < receiverId.getId() ? senderId.getId() + "-" + receiverId.getId() : receiverId.getId() + "-" + senderId.getId();
        return new ConversationId(conversationId);
    }
}
