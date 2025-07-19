package ricardo.messagingapp.repositories;


import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ricardo.messagingapp.domain.message.*;

@Repository
public class MessageRepository {
    private JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        // Ensure the messages table exists
        // sender_id and receiver id come from UserId, which is a Long, we should link them to the users table
        String createTableSql = "CREATE TABLE IF NOT EXISTS messages (" +
                "message_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "conversation_id VARCHAR(255) NOT NULL, " +
                "sender_id BIGINT NOT NULL, " +
                "receiver_id BIGINT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "sent_at TIMESTAMP NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "is_edited BOOLEAN NOT NULL, " +
                "last_edited_time TIMESTAMP, " +
                "FOREIGN KEY (sender_id) REFERENCES users(user_id), " +
                "FOREIGN KEY (receiver_id) REFERENCES users(user_id)" +
                ")";

        jdbcTemplate.execute(createTableSql);
    }

    public void save(Message message) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, receiver_id, content, sent_at, status, is_edited, last_edited_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                message.getConversationId().getId(),
                message.getSenderId().getId(),
                message.getReceiverId().getId(),
                message.getContent().getContent(),
                message.getSentAt(),
                message.getStatus().name(),
                message.isEdited(),
                message.getLastEditedTime());
    }

    public void update(Message message) {
        Long id = message.getMessageId();

        String sql = "UPDATE messages SET content = ?, sent_at = ?, status = ?, is_edited = ?, last_edited_time = ? " +
                "WHERE message_id = ?";

        jdbcTemplate.update(sql,
                message.getContent().getContent(),
                message.getSentAt(),
                message.getStatus(),
                message.isEdited(),
                message.getLastEditedTime(),
                id);
    }


    public void delete(Message message) {
        Long id = message.getMessageId();

        String sql = "DELETE FROM messages WHERE message_id = ?";

        jdbcTemplate.update(sql, id);
    }


    public Message findById(Long messageId) {
        String sql = "SELECT * FROM messages WHERE message_id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{messageId}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return new Message(
                    rs.getLong("message_id"),
                    ConversationId.fromConversationId(rs.getString("conversation_id")),
                    UserId.valueOf(rs.getLong("sender_id")),
                    UserId.valueOf(rs.getLong("receiver_id")),
                    MessageContent.valueOf(rs.getString("content")),
                    rs.getTimestamp("sent_at").toLocalDateTime(),
                    MessageStatus.valueOf(rs.getString("status")),
                    rs.getBoolean("is_edited"),
                    rs.getTimestamp("last_edited_time") != null ? rs.getTimestamp("last_edited_time").toLocalDateTime() : null
            );
        });
    }

    public Iterable<Message> findAllFromConversation(ConversationId conversationId) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC";
        return jdbcTemplate.query(sql, new Object[]{conversationId.getId()}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return new Message(
                    rs.getLong("message_id"),
                    ConversationId.fromConversationId(rs.getString("conversation_id")),
                    UserId.valueOf(rs.getLong("sender_id")),
                    UserId.valueOf(rs.getLong("receiver_id")),
                    MessageContent.valueOf(rs.getString("content")),
                    rs.getTimestamp("sent_at").toLocalDateTime(),
                    MessageStatus.valueOf(rs.getString("status")),
                    rs.getBoolean("is_edited"),
                    rs.getTimestamp("last_edited_time") != null ? rs.getTimestamp("last_edited_time").toLocalDateTime() : null
            );
        });
    }

    public Iterable<Message> findPagedByConversation(ConversationId conversationId, int page, int size) {
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new Object[]{conversationId.getId(), size, page * size}, (rs, rowNum) -> {
            // Map the result set to a Message object
            return new Message(
                    rs.getLong("message_id"),
                    ConversationId.fromConversationId(rs.getString("conversation_id")),
                    UserId.valueOf(rs.getLong("sender_id")),
                    UserId.valueOf(rs.getLong("receiver_id")),
                    MessageContent.valueOf(rs.getString("content")),
                    rs.getTimestamp("sent_at").toLocalDateTime(),
                    MessageStatus.valueOf(rs.getString("status")),
                    rs.getBoolean("is_edited"),
                    rs.getTimestamp("last_edited_time") != null ? rs.getTimestamp("last_edited_time").toLocalDateTime() : null
            );
        });
    }

    public void deleteAllFromConversation(ConversationId conversationId) {
        String sql = "DELETE FROM messages WHERE conversation_id = ?";
        jdbcTemplate.update(sql, conversationId.getId());
    }

    public Iterable<UserId> findAllUsersWithConversationWithUser(UserId userId)  {
        String sql = "SELECT DISTINCT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS user_id " +
                     "FROM messages WHERE sender_id = ? OR receiver_id = ?";
        return jdbcTemplate.query(sql, new Object[]{userId.getId(), userId.getId(), userId.getId()}, (rs, rowNum) -> {
            return UserId.valueOf(rs.getLong("user_id"));
        });
    }

}
