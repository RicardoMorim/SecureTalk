package ricardo.messagingapp.messagingcore.repositories;


import com.ricardo.auth.domain.user.Username;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ricardo.messagingapp.messagingcore.domain.message.Message;
import ricardo.messagingapp.messagingcore.domain.message.MessageContent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepository {
    private JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int save(Message message) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, sent_at, edited, seen, edited_at) " +
                "VALUES (?,?,?,?,?,?,?)";

        return jdbcTemplate.update(sql,
                message.getConversationId(),
                message.getSenderId(),
                message.getContent().getContent(),
                message.getSentAt(),
                message.isEdited(),
                message.isSeen(),
                message.getEditedAt());
    }

    public int update(Message message) {
        UUID id = message.getId();

        String sql = "UPDATE messages SET content = ?, edited = ?, edited_at = ? " +
                "WHERE id = ?";

        return jdbcTemplate.update(sql,
                message.getContent().getContent(),
                message.isEdited(),
                message.getEditedAt(),
                id);
    }

    public int delete(UUID messageId) {

        String sql = "DELETE FROM messages WHERE id = ?";

        return jdbcTemplate.update(sql, messageId);
    }


    public Message findById(UUID messageId) {
        String sql = "SELECT * FROM messages WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{messageId}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return rowMapper(rs);
        });
    }

    public List<Message> findAllFromConversation(UUID conversationId) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
        return jdbcTemplate.query(sql, new Object[]{conversationId}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return rowMapper(rs);
        });
    }

    public List<Message> findPagedByConversation(UUID conversationId, int page, int size) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new Object[]{conversationId, size, page * size}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return rowMapper(rs);
        });
    }

    public void deleteAllFromConversation(UUID conversationId) {
        String sql = "DELETE FROM messages WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId);
    }

    public List<UUID> findAllUsersWithConversationWithUser(UUID userId) {
        String sql = """
            SELECT DISTINCT cp_other.user_id AS user_id
            FROM conversation_participants cp_self
            JOIN conversation_participants cp_other
              ON cp_other.conversation_id = cp_self.conversation_id
            WHERE cp_self.user_id = ?
              AND cp_other.user_id <> ?
            """;
        return jdbcTemplate.query(sql, new Object[]{userId, userId}, (rs, rowNum) -> (UUID) rs.getObject("user_id"));
    }


    public List<UUID> findLatestConversations(UUID userId, int limit) {
        String sql = """
            SELECT m.conversation_id
            FROM messages m
            JOIN conversation_participants cp
              ON cp.conversation_id = m.conversation_id
            WHERE cp.user_id = ?
            GROUP BY m.conversation_id
            ORDER BY MAX(m.sent_at) DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(sql, new Object[]{userId, limit}, (rs, rowNum) -> (UUID) rs.getObject("conversation_id"));
    }

    public List<Message> findLatestMessagesFromConversation(UUID conversationId, int limit) {
        String sql = "SELECT * FROM messages " +
                "WHERE conversation_id = ? " +
                "ORDER BY sent_at DESC LIMIT ?";

        return jdbcTemplate.query(sql, new Object[]{conversationId, limit}, (rs, rowNum) -> rowMapper(rs));
    }

    public Message rowMapper(ResultSet rs) throws SQLException {
        return new Message(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("conversation_id"),
                (UUID) rs.getObject("sender_id"),
                MessageContent.valueOf(rs.getString("content")),
                rs.getTimestamp("sent_at").toInstant(),
                rs.getBoolean("edited"),
                rs.getBoolean("seen"),
                rs.getTimestamp("edited_at") != null ? rs.getTimestamp("edited_at").toInstant() : null
        );
    }

    public List<Username> findLatestConversationNames(UUID userId, int limit) {
        String sql = """
                SELECT u.username AS name
                FROM conversations c
                JOIN conversation_participants cp_self ON cp_self.conversation_id = c.id
                JOIN conversation_participants cp_other ON cp_other.conversation_id = c.id
                JOIN users u ON u.id = cp_other.user_id
                LEFT JOIN messages m ON m.conversation_id = c.id
                WHERE c.is_group = false
                  AND cp_self.user_id = ?
                  AND cp_other.user_id <> ?
                GROUP BY u.username
                ORDER BY MAX(m.sent_at) DESC NULLS LAST
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, new Object[]{userId, userId, limit}, (rs, rowNum) -> Username.valueOf(rs.getString("name")));
    }

    public int markAsRead(UUID messageId) {
        String sql = "UPDATE messages SET seen = true WHERE id = ?";
        return jdbcTemplate.update(sql, messageId);
    }
}
