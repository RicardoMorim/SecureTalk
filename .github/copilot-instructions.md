# SecureTalk MessagingCore - AI Agent Instructions

⚠️ **IMPORTANT**: This file is a static reference guide. For evolving documentation, architecture decisions, and dependency tracking, see the **Obsidian brain** under `docs/FILE|FOLDER/`:
- **docs/FILE|FOLDER/INDEX.md** — Central hub for all documentation
- **docs/FILE|FOLDER/projects/Auth/** — Auth library documentation
- **docs/FILE|FOLDER/projects/MessagingCore/** — MessagingCore documentation  
- **docs/FILE|FOLDER/decisions-and-discoveries.md** — Architectural decisions & error log

**Keep both in sync**: When implementing features, update Obsidian docs to reflect discoveries and share with team.

---

## Project Overview
SecureTalk is a **Spring Boot 4.0.3** messaging backend (Java 21) with PostgreSQL, custom Auth library, STOMP/WebSocket real-time messaging, and rate limiting. The goal is to implement **true End-to-End Encryption (E2EE)** with real-time updates.

## Tech Stack
- **Framework**: Spring Boot 4.0.3 + Spring WebSocket (STOMP over SockJS fallback)
- **Database**: PostgreSQL 17+ (raw JDBC, no JPA repositories — manual schema init via `MessageSchemaInitializer`)
- **Auth**: Custom `io.github.ricardomorim:auth-spring-boot-starter:4.0.2` (`JwtService`, `UserService<User, AppRole, UUID>`, `RateLimiter`)
- **Serialization**: Gson (not Jackson)
- **Config**: `@ConfigurationProperties` prefix `secure-talk`, dotenv via `dotenv-java`
- **Build**: Maven (`./mvnw spring-boot:run`)

## Project Structure
```
MessagingCore/src/main/java/ricardo/messagingapp/messagingcore/
├── config/                    # WebSocketConfig, JwtHandshakeInterceptor, Properties, MessageSchemaInitializer
├── controller/                # MessageController (@MessageMapping endpoints)
├── domain/
│   ├── conversation/Conversation.java    # Aggregate root (UUID participants, isGroup, name)
│   └── message/
│       ├── Message.java                  # Domain entity (id, conversationId, senderId, content, sentAt, edited, seen)
│       ├── MessageContent.java           # Value object (immutable wrapper around String)
│       ├── MessageStatus.java
│       ├── DTO/                          # MessagePayload, EditMessage, ConversationPayload, MessageNotification, ReadNotification
│       └── DomainEvents/                 # MessageSent, MessageDelivered, MessageRead, MessageEdited, MessageDeleted (records)
├── eventhandlers/MessageEventHandler.java  # @EventListener + @Async → SimpMessagingTemplate broadcasts
├── exceptionhandler/GlobalExceptionHandler.java
├── repositories/                         # Raw JDBC via JdbcTemplate
│   ├── MessageRepository.java
│   └── ConversationRepository.java       # ⚠️ ALL METHODS RETURN NULL — stubs
├── services/
│   ├── MessageService.java               # Core business logic (send/edit/delete/markRead)
│   ├── EncryptionService.java            # ⚠️ SERVER-SIDE AES — needs E2EE replacement
│   ├── ConversationService.java
│   ├── NotificationService.java          # ⚠️ ALL METHODS EMPTY — stubs
│   └── MetricsService.java               # ⚠️ ALL METHODS EMPTY — stubs
├── websocket/
│   ├── WebSocketHandler.java             # TextWebSocketHandler, sends initial data on connect
│   └── WebSocketSessionRegistry.java     # ConcurrentHashMap<email, WebSocketSession>
```

## Key Conventions
1. **No JPA repositories** — all DB access is raw JDBC with `JdbcTemplate`. Domain entities are NOT JPA entities.
2. **Gson for JSON** — not Jackson. Use `new Gson().toJson()` / `fromJson()`.
3. **Auth integration**: `UserService<User, AppRole, UUID>` with generics. Get user by `getUserByEmail(email)`, `getUserByUserName(username.getUsername())`, `getUserById(uuid)`.
4. **Rate limiting**: Custom `RateLimiter` interface from auth library. WebSocket prefix: `"ws:"`.
5. **Domain events**: Published via `ApplicationEventPublisher`, handled async with `@EventListener @Async`.
6. **WebSocket paths**: STOMP broker at `/ws` (SockJS fallback). Destinations: `/topic`, `/queue`. App prefix: `/app`.
7. **Message edit/delete**: 15-minute time limit, only last message in conversation can be edited/deleted.
8. **Encryption key**: Stored in `secure-talk.encryption.secret` property (currently server-side AES).

## Critical Issues to Fix First
- `ConversationRepository.java` — ALL methods are stubs returning null. Must implement all queries against PostgreSQL.
- `NotificationService.java` — All methods empty. Must wire up WebSocket push notifications.
- `MetricsService.java` — All methods empty.
- `EncryptionService.java` — Uses server-side AES with shared key. **This is NOT E2EE.**

## Building & Running
```bash
cd MessagingCore
./mvnw clean spring-boot:run
# or with Maven wrapper from root: ./mvnw --also-make -pl MessagingCore spring-boot:run
```

Database runs via Docker Compose at `compose.yaml` (PostgreSQL on port 5433).

## E2EE Implementation Plan (Priority Order)

### Phase 1: Foundation — Fix Stubs & Architecture
**Goal**: Get the existing codebase working before adding E2EE.

1. **Implement ConversationRepository** — SQL queries for:
   - `findByParticipants(Set<UUID>)` — JOIN conversation_participants
   - `findById(UUID)` — SELECT FROM conversations
   - `findDMByNameAndUUID(String, UUID)` — DM lookup via participants
   - `save(Conversation)` — INSERT INTO conversations + conversation_participants
   - `findByMessageId(UUID)` — JOIN messages → conversations

2. **Implement MessageRepository missing methods**:
   - `findLatestMessagesFromConversation(conversationId, limit)`
   - `getLatestXConversationNames(userId, limit)` 
   - `findAllUsersWithConversationWithUser` (needs conversation lookup)

3. **Wire up NotificationService** — Basic WebSocket push for new messages using `SimpMessagingTemplate`.

### Phase 2: E2EE Architecture Design
**Goal**: Replace server-side AES with true client-side E2E encryption.

#### Cryptographic Protocol: X3DH + Double Ratchet (Signal Protocol inspired)
```
┌─────────────────────────────────────────────────────┐
│ Client-Side (NOT in this backend):                  │
│ 1. Generate Curve25519 key pair (identity keys)     │
│ 2. Exchange public keys via server (stored encrypted) │
│ 3. Derive session key using X3DH/DH handshake       │
│ 4. Encrypt messages with AES-GCM + random nonce     │
│ 5. Send ONLY ciphertext to server                    │
└─────────────────────────────────────────────────────┐
                                                      │
┌─────────────────────────────────────────────────────┐
│ Backend (MessagingCore) — What we implement:        │
│ 1. KeyStorageService — store public keys, fingerprints │
│ 2. SessionManager — derive/rotate session keys      │
│ 3. Replace EncryptionService → E2EEncryptionService │
│ 4. Server acts as KEY RELAY only (never sees plaintext) │
└─────────────────────────────────────────────────────┘
```

#### New Database Tables Needed:
```sql
-- User identity keys (public keys only)
CREATE TABLE IF NOT EXISTS user_keys (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    public_key BYTEA NOT NULL,           -- X25519 public key (32 bytes)
    signature_key BYTEA NOT NULL,        -- Ed25519 signing key (for identity verification)
    fingerprint VARCHAR(64) NOT NULL,    -- SHA-256 of public key (for client verification)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- E2E encrypted session keys (one-time use, ratchet-based)
CREATE TABLE IF NOT EXISTS session_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    receiver_id UUID NOT NULL REFERENCES users(id),
    encrypted_session_key BYTEA NOT NULL,  -- Recipient's public key encrypts this
    ratchet_index BIGINT NOT NULL DEFAULT 0,
    nonce BYTEA NOT NULL,                  -- AES-GCM nonce (12 bytes)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Message keys for double ratchet
CREATE TABLE IF NOT EXISTS message_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_key_id UUID REFERENCES session_keys(id),
    direction VARCHAR(10) NOT NULL,        -- 'sent' or 'received'
    chain_hash BYTEA NOT NULL,             -- HKDF chain hash
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Key verification / trust
CREATE TABLE IF NOT EXISTS key_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    conversation_id UUID NOT NULL,
    trusted BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP WITH TIME ZONE,
    UNIQUE(user_id, conversation_id)
);
```

### Phase 3: E2EE Implementation — Backend Services

#### 3.1 `KeyStorageService` (NEW)
- `storePublicKey(userId, publicKeyBytes)` — Store user's X25519 public key
- `getPublicKey(userId)` → `byte[]` — Retrieve for DH handshake
- `getPublicKeys(conversationIds)` → `Map<UUID, byte[]>` — Batch fetch for message recipients
- `storeFingerprint(userId, fingerprint)` — For client-side verification

#### 3.2 `E2EEncryptionService` (REPLACES EncryptionService)
```java
// Server NEVER decrypts messages. This service only:
// 1. Encryptes session keys with recipient's public key (X25519 DH)
// 2. Generates random nonces for AES-GCM
// 3. Validates ciphertext format

public record EncryptedMessage(
    String ciphertext,        // Base64-encoded AES-GCM ciphertext
    String nonce,             // Base64-encoded 12-byte random nonce  
    String sessionKeyNonce,   // Base64-encoded encrypted session key
    long ratchetIndex         // For double ratchet synchronization
) {}

public EncryptedMessage encryptForConversation(String plaintext, UUID conversationId, UUID senderId);
public byte[] deriveSessionKey(byte[] theirPublicKey, byte[] myPrivateKey); // X25519 DH
```

**Important**: The actual message encryption/decryption happens on the CLIENT side. The backend only:
- Stores encrypted ciphertext (never decrypts)
- Relays session key material between clients
- Manages key rotation via ratchet index

#### 3.3 Update `MessageService` for E2EE
```java
// sendMessage(): 
//   → Client sends ALREADY ENCRYPTED content (ciphertext + nonce)
//   → Server stores ciphertext as-is (no decryption)
//   → Publish MessageSent event with encrypted payload

// updateMessage(): Same pattern — client sends new encrypted content

// getPagedConversation() / findAllFromConversation():
//   → Return CIPHERTEXT to client (client decrypts locally)
//   → NEVER call encryptionService.decrypt() on message content
```

### Phase 4: Real-Time WebSocket Enhancements

#### 4.1 Message Delivery Notifications
In `MessageEventHandler.handleMessageSent()`:
- After storing encrypted message, push `MessageNotification` to receiver's `/queue/messages`
- Include: messageId, senderUsername, timestamp, **encrypted content preview** (first 50 chars of ciphertext)
- Client decrypts locally when displaying

#### 4.2 Typing Indicators
```java
// New WebSocket destination: /app/typing
// Payload: { conversationId, isTyping: boolean }

@MessageMapping("/typing")
public void handleTyping(TypingPayload payload, Principal principal) {
    UUID userId = getUserId(principal);
    messagingTemplate.convertAndSendToUser(
        payload.receiverUsername().toString(),
        "/queue/typing",
        new TypingNotification(payload.conversationId(), userId, payload.isTyping())
    );
}
```

#### 4.3 Online/Offline Status
- Track connection/disconnection in `WebSocketHandler.afterConnectionClosed()` / `afterConnectionEstablished()`
- Broadcast presence changes via `/topic/presence`
- Store last seen timestamp in a new `user_presence` table or Redis cache

#### 4.4 Message Reactions (NEW FEATURE)
```sql
CREATE TABLE IF NOT EXISTS message_reactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES messages(id),
    user_id UUID NOT NULL REFERENCES users(id),
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(message_id, user_id)
);
```

### Phase 5: Redis Caching Layer (NEW Dependency)
Add `spring-boot-starter-data-redis` for:
1. **Online users cache**: `SET online:{userId} 1 EX 300` with TTL heartbeat
2. **Typing indicators**: Short-lived keys, auto-expire
3. **Rate limiter backend**: Replace in-memory rate limiter with Redis-based (distributed)
4. **Conversation list cache**: Cache latest conversations per user for 30s

```java
@Service
@RequiredArgsConstructor
public class PresenceService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public void markOnline(UUID userId) {
        redisTemplate.opsForValue().set("online:" + userId, "1", Duration.ofSeconds(300));
    }
    
    public Set<UUID> getOnlineUsersInConversation(Set<UUID> participantIds) {
        List<String> keys = participantIds.stream()
            .map(id -> "online:" + id)
            .toList();
        return redisTemplate.hasKey(keys); // returns online user IDs
    }
}
```

### Phase 6: API Endpoints (REST for non-realtime operations)

#### New REST Controllers Needed:
1. **`KeyManagementController`** — `/api/keys/*`
   - `POST /api/keys/upload` — Upload public key + signature
   - `GET /api/keys/{userId}/fingerprint` — Get fingerprint for verification
   - `GET /api/keys/conversation/{conversationId}` — Get all participants' public keys

2. **`ConversationController`** — `/api/conversations/*`
   - `POST /api/conversations/groups` — Create group conversation
   - `PATCH /api/conversations/{id}/rename` — Rename group
   - `POST /api/conversations/{id}/participants` — Add/remove participants
   - `GET /api/conversations/{id}/members` — List members with online status

3. **`SearchController`** — `/api/search/*`
   - `GET /api/search/messages?conversationId=&query=` — Server-side search on encrypted content is NOT possible with E2EE
   - Alternative: Client-side search, server returns paginated messages

## Security Considerations for E2EE
1. **Server never sees plaintext** — Messages stored as Base64-encoded AES-GCM ciphertext
2. **Forward secrecy** — Double ratchet ensures past messages can't be decrypted if session key is compromised
3. **Key verification** — Clients compare fingerprints out-of-band (QR code, safety number)
4. **Replay protection** — Nonce + ratchet index prevents message replay attacks
5. **Auth integration** — JWT validates user identity; E2EE validates message integrity

## Testing Strategy
- Unit tests for `E2EEncryptionService` with known test vectors (use Bouncy Castle test keys)
- Integration tests for WebSocket flow using `SimpMessageType.TEST_MESSAGE`
- Domain event tests with `ApplicationEventPublisher` mocking
- Use H2 in-memory DB for fast unit tests, PostgreSQL container for integration tests

## Files to Create/Modify (Summary)

### NEW FILES:
- `services/E2EEncryptionService.java` — Replaces server-side encryption
- `services/KeyStorageService.java` — Public key management
- `services/PresenceService.java` — Online/offline tracking with Redis
- `controller/KeyManagementController.java` — REST API for key operations
- `domain/message/DTO/TypingPayload.java`, `TypingNotification.java` — Typing indicators
- `config/RedisConfig.java` — Redis connection configuration

### MODIFY FILES:
- `MessageSchemaInitializer.java` — Add E2EE tables (user_keys, session_keys, message_keys, key_verifications, message_reactions)
- `EncryptionService.java` — Deprecate or wrap as legacy (server-side AES for non-E2E features)
- `MessageService.java` — Remove decryption calls, work with ciphertext
- `ConversationRepository.java` — **CRITICAL: Implement all stub methods**
- `NotificationService.java` — Wire up WebSocket notifications
- `WebSocketHandler.java` — Add typing, presence, delivery receipts handlers
- `MessageEventHandler.java` — Update to handle encrypted payloads
- `pom.xml` — Add Bouncy Castle (`org.bouncycastle:bcprov-jdk18on`), Redis starter

### CONFIG CHANGES:
- `application.yml` — Add Redis config, E2EE settings (key rotation interval)

---

## Documentation Evolution System

### How to Keep Docs Updated

When implementing features, **update both**:

1. **This file** (`.github/copilot-instructions.md`)
   - High-level reference
   - Phase summaries
   - File structure
   - Maven dependencies

2. **Obsidian brain** (`docs/FILE|FOLDER/INDEX.md` + linked files)
   - Detailed implementation notes
    - Architecture decisions (`docs/FILE|FOLDER/decisions-and-discoveries.md`)
   - Dependency tracking with [[ligações]]
   - Error patterns and solutions
   - Status updates per phase

### When to Update

**During Development**:
- 🔴 **Critical**: Found bug/issue → Add to `docs/FILE|FOLDER/decisions-and-discoveries.md`
- 🟡 **Important**: New pattern discovered → Document in phase file
- 🟢 **Nice**: Update phase status after completing task

**After Completion**:
- [ ] Mark phase as COMPLETE in E2EE-Implementation-Plan.md
- [ ] Document what worked and what didn't
- [ ] Note any breaking changes or surprises
- [ ] Update Dependencies.md if new libraries added
- [ ] Update this file if architecture changed

### Example Flow

```
1. Start Phase 1: ConversationRepository implementation
    → Update `docs/FILE|FOLDER/INDEX.md` status: "Phase 1: In Progress"

2. Hit issue: Complex SQL query for participant matching
    → Add to `docs/FILE|FOLDER/decisions-and-discoveries.md`: "Error: ConversationRepository.findByParticipants() Complexity"
   → Document solution: "Use Redis cache for DM lookups in Phase 5"

3. Complete ConversationRepository
   → Mark checkboxes in E2EE-Implementation-Plan.md
    → Update `docs/FILE|FOLDER/INDEX.md`: "Phase 1: 40% Complete"

4. Deploy Phase 1
    → Move issue solutions to `docs/FILE|FOLDER/decisions-and-discoveries.md`
    → Update both `docs/FILE|FOLDER/INDEX.md` and `copilot-instructions.md` with new status
   → Note for next phase: "RateLimiter needs Redis backend for distributed systems"
```

### Version Tracking

Keep track of which version of this documentation is current:

- **copilot-instructions.md**: Static reference (update once per major phase)
- **Obsidian docs**: Living documentation (update during development)
- **docs/FILE|FOLDER/decisions-and-discoveries.md**: Error log (update as discovered)

### Obsidian Graph View

Use Obsidian's graph view to visualize:
- Which files depend on which
- Circular dependencies to avoid
- Clusters of related decisions
- Knowledge gaps (orphaned files)

**Command**: Open Obsidian → Ctrl+G (Graph view)

---

## Anti-Patterns to Avoid

❌ **Don't do this**:
- Update only Obsidian, forget to update this file
- Create documentation but don't use it (defeats the purpose)
- Document AFTER implementation (memory fades)
- Ignore errors in `docs/FILE|FOLDER/decisions-and-discoveries.md` (repeat mistakes)
- Forget to link related files (lose context)

✅ **Do this instead**:
- Update Obsidian IMMEDIATELY when discovering something
- Link related decisions with [[ligações]]
- Document errors even if you fix them quickly (helps team)
- Review `docs/FILE|FOLDER/decisions-and-discoveries.md` before each phase
- Use Obsidian graph to find related documentation

---

## AI Agent Instructions for Using This Brain

When completing tasks:

1. **Read `docs/FILE|FOLDER/INDEX.md` first** to understand project structure
2. **Check `docs/FILE|FOLDER/decisions-and-discoveries.md`** for known issues/patterns
3. **Review E2EE-Implementation-Plan.md** for current phase tasks
4. **Update FILES DURING WORK** (don't wait until end)
5. **Add new discoveries** to relevant Obsidian files
6. **Use [[ligações]]** to link related concepts
7. **Mark completed tasks** with checkboxes in E2EE-Implementation-Plan.md

### Example: Implementing ConversationRepository

```
Agent Workflow:
1. Read `docs/FILE|FOLDER/INDEX.md` → Understand project
2. Read Repositories.md → Understand what's stubbed
3. Read `docs/FILE|FOLDER/decisions-and-discoveries.md` → Check for known SQL patterns
4. Implement ConversationRepository methods
5. Document any issues found in `docs/FILE|FOLDER/decisions-and-discoveries.md`
6. Add testing patterns to Repositories.md
7. Update E2EE-Implementation-Plan.md: Mark checkboxes ✅
8. Update INDEX.md: Phase 1 progress
```

---

## Auth Library Version Compatibility

**Current**: io.github.ricardomorim:auth-spring-boot-starter:5.0.0

If build fails with auth dependency:
1. Check `pom.xml` for actual version
2. Run `./mvnw dependency:tree | grep auth`
3. Verify bean injection: Look for `JwtService`, `UserService` bean definitions
4. Update version in pom.xml and this file if needed
5. Document version change in [[projects/Auth/Configuration|Auth Configuration]]

See: [[decisions-and-discoveries#error-auth-starter-version-compatibility|Version Compatibility Decision]]
