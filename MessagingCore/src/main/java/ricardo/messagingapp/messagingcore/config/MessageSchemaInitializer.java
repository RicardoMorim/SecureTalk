package ricardo.messagingapp.messagingcore.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@DependsOn({"userSchemaInitializer", "RefreshTokenSchemaInitializer"})
public class MessageSchemaInitializer {
    private static final Logger logger = LoggerFactory.getLogger(MessageSchemaInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public MessageSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initializeSchema() {
        try {
            createTablesIfNotExists();
            createIndexes();
            logger.info("Message schema initialization completed successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Message schema", e);
            throw new RuntimeException("Message schema initialization failed", e);
        }
    }

    private void createTablesIfNotExists() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"pgcrypto\"");

        // Conversations table
        String createConversationsTable = """
            CREATE TABLE IF NOT EXISTS conversations (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                name VARCHAR(255),
                is_group BOOLEAN NOT NULL DEFAULT FALSE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
            )
            """;
        jdbcTemplate.execute(createConversationsTable);

        // Conversation participants (many-to-many)
        String createParticipantsTable = """
            CREATE TABLE IF NOT EXISTS conversation_participants (
                conversation_id UUID NOT NULL,
                user_id UUID NOT NULL,
                joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                PRIMARY KEY (conversation_id, user_id),
                FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        jdbcTemplate.execute(createParticipantsTable);

        // Messages table
        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                conversation_id UUID NOT NULL,
                sender_id UUID NOT NULL,
                content TEXT NOT NULL,
                sent_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                edited BOOLEAN NOT NULL DEFAULT FALSE,
                seen BOOLEAN NOT NULL DEFAULT FALSE,
                edited_at TIMESTAMP WITH TIME ZONE,
                FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
            )
            """;
        jdbcTemplate.execute(createMessagesTable);

        logger.debug("All message tables created or already exist");
    }

    private void createIndexes() {
        String[] indexStatements = {
            "CREATE INDEX IF NOT EXISTS idx_conversations_created_at ON conversations(created_at)",
            "CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_id ON conversation_participants(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation_id ON conversation_participants(conversation_id)",
            "CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON messages(conversation_id)",
            "CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id)",
            "CREATE INDEX IF NOT EXISTS idx_messages_sent_at ON messages(sent_at)",
            "CREATE INDEX IF NOT EXISTS idx_messages_conversation_sent_at ON messages(conversation_id, sent_at)",
            "CREATE INDEX IF NOT EXISTS idx_messages_seen ON messages(seen)"
        };

        for (String indexSql : indexStatements) {
            jdbcTemplate.execute(indexSql);
        }

        logger.debug("All message indexes created or already exist");
    }
}