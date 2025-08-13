package ricardo.messagingapp.domain.conversation;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Conversation {

    private UUID id;

    private boolean isGroup;

    private String name; // optional, only for groups

    private Instant createdAt;

    private Set<UUID> participants;

    protected Conversation() {
    }

    private void setStarterValues() {
        this.isGroup = false;
        this.name = null;
        this.createdAt = Instant.now();
    }

    /**
     * Constructor for a one-on-one conversation.
     *
     * @param participants the participants in the conversation, should contain exactly two UserIds.
     */
    public Conversation(Set<UUID> participants) {
        setStarterValues();
        if (participants == null || participants.size() != 2) {
            throw new IllegalArgumentException("One-on-one conversation must have exactly two participants.");
        }
        this.participants = participants;
    }

    /**
     * Constructor for a group conversation.
     *
     * @param participants the participants in the conversation, should contain at least two UserIds.
     * @param name         the name of the group chat.
     */
    public Conversation(Set<UUID> participants, String name) {
        setStarterValues();
        if (participants == null || participants.size() < 2) {
            throw new IllegalArgumentException("Group conversation must have at least two participants.");
        }
        this.isGroup = true;
        this.participants = participants;
        this.name = name;
    }


    public boolean hasParticipant(UUID userId) {
        return participants.contains(userId);
    }

    public boolean isBetween(UUID userA, UUID userB) {
        return !isGroup && participants.containsAll(Set.of(userA, userB));
    }

    public void rename(String newName) {
        if (!isGroup) throw new UnsupportedOperationException("Only group chats can be renamed.");
        this.name = newName;
    }

    /**
     * Create a group conversation with a name.
     *
     * @param participants the participants in the group conversation, should contain at least two UserIds.
     * @param name         the name of the group chat.
     * @return a new Conversation instance representing the group chat.
     */
    public static Conversation createGroupChat(Set<UUID> participants, String name) {
        if (participants == null || participants.isEmpty() || participants.size() < 2) {
            throw new IllegalArgumentException("Participants cannot be null or empty.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be null or blank.");
        }
        return new Conversation(participants, name);
    }

    /**
     * Create a direct message (DM) conversation between two users.
     *
     * @param participants
     * @return
     */
    public static Conversation createDM(Set<UUID> participants) {
        if (participants == null || participants.size() != 2) {
            throw new IllegalArgumentException("Participants cannot be null or empty.");
        }
        return new Conversation(participants);
    }
}
