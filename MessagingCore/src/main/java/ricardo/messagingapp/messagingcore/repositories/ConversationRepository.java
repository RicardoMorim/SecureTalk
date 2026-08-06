package ricardo.messagingapp.messagingcore.repositories;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ricardo.messagingapp.messagingcore.domain.conversation.Conversation;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Conversation findByMessageId(UUID messageId){
        String sql = """
                SELECT c.id, c.name, c.is_group, c.created_at
                FROM conversations c
                JOIN messages m ON m.conversation_id = c.id
                WHERE m.id = ?
                """;
        try {
            ConversationRow row = jdbcTemplate.queryForObject(sql, this::mapConversationRow, messageId);
            if (row == null) {
                return null;
            }
            Set<UUID> participants = findParticipants(row.id());
            return buildConversation(row, participants);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Conversation findById(UUID conversationId) {
        String sql = """
                SELECT id, name, is_group, created_at
                FROM conversations
                WHERE id = ?
                """;
        try {
            ConversationRow row = jdbcTemplate.queryForObject(sql, this::mapConversationRow, conversationId);
            if (row == null) {
                return null;
            }
            Set<UUID> participants = findParticipants(row.id());
            return buildConversation(row, participants);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Conversation findDMByNameAndUUID(String name, UUID userId) {
        String sql = """
                SELECT c.id, c.name, c.is_group, c.created_at
                FROM conversations c
                JOIN conversation_participants cp_self ON cp_self.conversation_id = c.id
                JOIN conversation_participants cp_other ON cp_other.conversation_id = c.id
                JOIN users u ON u.id = cp_other.user_id
                WHERE c.is_group = false
                  AND cp_self.user_id = ?
                  AND cp_other.user_id <> ?
                  AND u.username = ?
                LIMIT 1
                """;
        try {
            ConversationRow row = jdbcTemplate.queryForObject(sql, this::mapConversationRow, userId, userId, name);
            if (row == null) {
                return null;
            }
            Set<UUID> participants = findParticipants(row.id());
            return buildConversation(row, participants);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public Conversation findByParticipants(Set<UUID> participants, boolean isGroup) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }

        for (Conversation conversation : findByParticipant(participants.iterator().next())) {
            if (conversation.isGroup() == isGroup && participants.equals(conversation.getParticipants())) {
                return conversation;
            }
        }

        return null;
    }

    public void save(Conversation conversation) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation cannot be null");
        }
        if (conversation.getParticipants() == null || conversation.getParticipants().isEmpty()) {
            throw new IllegalArgumentException("Conversation must have participants");
        }

        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID());
        }
        if (conversation.getCreatedAt() == null) {
            conversation.setCreatedAt(Instant.now());
        }

        String conversationSql = """
                INSERT INTO conversations (id, name, is_group, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;

        jdbcTemplate.update(
                conversationSql,
                conversation.getId(),
                conversation.getName(),
                conversation.isGroup(),
                Timestamp.from(conversation.getCreatedAt()),
                Timestamp.from(Instant.now())
        );

        String participantSql = """
                INSERT INTO conversation_participants (conversation_id, user_id)
                VALUES (?, ?)
                ON CONFLICT (conversation_id, user_id) DO NOTHING
                """;
        for (UUID participantId : conversation.getParticipants()) {
            jdbcTemplate.update(participantSql, conversation.getId(), participantId);
        }
    }

    public void delete(UUID conversationId) {
        // Implementation to delete a conversation by its ID
        // This could involve removing the conversation from a database
    }

    public void update(Conversation conversation) {
        String sql = """
                UPDATE conversations
                SET name = ?, is_group = ?, updated_at = NOW()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, conversation.getName(), conversation.isGroup(), conversation.getId());
    }

    public boolean exists(UUID conversationId) {
        String sql = "SELECT COUNT(1) FROM conversations WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, conversationId);
        return count != null && count > 0;
    }


    public List<Conversation> findAll() {
        String sql = "SELECT id, name, is_group, created_at FROM conversations";
        List<ConversationRow> rows = jdbcTemplate.query(sql, this::mapConversationRow);
        List<Conversation> conversations = new ArrayList<>();
        for (ConversationRow row : rows) {
            conversations.add(buildConversation(row, findParticipants(row.id())));
        }
        return conversations;
    }


    public List<Conversation> findByParticipant(UUID userId) {
        String sql = """
                SELECT c.id, c.name, c.is_group, c.created_at
                FROM conversations c
                JOIN conversation_participants cp ON cp.conversation_id = c.id
                WHERE cp.user_id = ?
                ORDER BY c.created_at DESC
                """;
            List<ConversationRow> rows = jdbcTemplate.query(sql, this::mapConversationRow, userId);
            List<Conversation> conversations = new ArrayList<>();
            for (ConversationRow row : rows) {
                conversations.add(buildConversation(row, findParticipants(row.id())));
            }
        return conversations;
    }

    public List<Conversation> findByGroupName(String groupName) {
        String sql = """
                SELECT id, name, is_group, created_at
                FROM conversations
                WHERE is_group = true AND name = ?
                ORDER BY created_at DESC
                """;
            List<ConversationRow> rows = jdbcTemplate.query(sql, this::mapConversationRow, groupName);
            List<Conversation> conversations = new ArrayList<>();
            for (ConversationRow row : rows) {
                conversations.add(buildConversation(row, findParticipants(row.id())));
            }
        return conversations;
    }

    public Conversation findByParticipants(Set<UUID> participants) {
        return findByParticipants(participants, participants != null && participants.size() > 2);
    }

    private ConversationRow mapConversationRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        UUID id = (UUID) rs.getObject("id");
        String name = rs.getString("name");
        boolean isGroup = rs.getBoolean("is_group");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ConversationRow(id, name, isGroup, createdAt != null ? createdAt.toInstant() : Instant.now());
    }

    private Conversation buildConversation(ConversationRow row, Set<UUID> participants) {
        Conversation conversation = row.isGroup()
                ? new Conversation(participants, row.name() == null ? "Group" : row.name())
                : new Conversation(participants);

        conversation.setId(row.id());
        conversation.setName(row.name());
        conversation.setGroup(row.isGroup());
        conversation.setCreatedAt(row.createdAt());
        return conversation;
    }

    private Set<UUID> findParticipants(UUID conversationId) {
        String sql = "SELECT user_id FROM conversation_participants WHERE conversation_id = ?";
        List<UUID> participants = jdbcTemplate.query(sql, (rs, rowNum) -> (UUID) rs.getObject("user_id"), conversationId);
        return new HashSet<>(participants);
    }

    private record ConversationRow(UUID id, String name, boolean isGroup, Instant createdAt) {}
}
