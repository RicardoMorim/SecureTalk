# Arquitetura e design

## 1. Arquitetura pretendida

```mermaid
flowchart LR
    ClientA[Cliente A]
    ClientB[Cliente B]
    Auth[Auth]
    Messaging[MessagingCore]
    AuthDB[(Auth DB)]
    MessagingDB[(Messaging DB)]
    RateLimit[(Rate limiter)]
    Broker[Broker STOMP]
    Outbox[Outbox processor]

    ClientA -->|HTTPS: login| Auth
    Auth --> AuthDB
    Auth -->|JWT com userId| ClientA

    ClientA -->|STOMP/WSS + JWT| Messaging
    ClientB -->|STOMP/WSS + JWT| Messaging

    Messaging --> MessagingDB
    Messaging --> RateLimit
    Messaging --> Broker
    Broker --> ClientA
    Broker --> ClientB

    MessagingDB --> Outbox
    Outbox --> Broker
````

O broker simples incorporado pode ser usado inicialmente. A Outbox e um broker externo são evoluções de fiabilidade, não requisitos para a primeira correção funcional.

---

## 2. Fronteiras dos serviços

### Auth

Responsável por:

- identidade;
- credenciais;
- passwords;
- emissão de JWT;
- roles;
- perfis de utilizador;
- revogação, quando implementada.

Auth não deve conhecer mensagens nem conversas.

### MessagingCore

Responsável por:

- DMs;
- participantes;
- mensagens;
- estados de mensagens;
- cursores de leitura;
- protocolo STOMP;
- notificações relacionadas com mensagens.

MessagingCore guarda IDs de utilizador, não entidades do Auth.

---

## 3. Comunicação Auth → MessagingCore

### Autenticação normal

```mermaid
sequenceDiagram
    participant C as Cliente
    participant A as Auth
    participant M as MessagingCore

    C->>A: Autenticar
    A-->>C: JWT assinado com userId
    C->>M: STOMP CONNECT com JWT
    M->>M: Validar assinatura, issuer, audience e expiração
    M->>M: Criar Principal(userId)
    M-->>C: CONNECTED
```

MessagingCore deve validar o token localmente. Não deve chamar UserService durante cada handshake.

### Informação de perfil

Quando for necessário apresentar username ou avatar, existem três possibilidades:

1. o cliente consulta Auth;
2. MessagingCore usa um cliente HTTP interno limitado;
3. MessagingCore mantém uma projeção local alimentada por eventos.

A primeira solução é suficiente para a recuperação inicial.

---

## 4. Camadas do MessagingCore

```mermaid
flowchart TD
    Transport[Controller STOMP / Transporte]
    Application[Casos de uso / Application services]
    Domain[Domínio]
    Repositories[Interfaces de repositório]
    Adapters[Adaptadores de infraestrutura]
    DB[(Base de dados)]
    WS[SimpMessagingTemplate]
    External[Cliente externo opcional]

    Transport --> Application
    Application --> Domain
    Application --> Repositories
    Application --> WS
    Repositories --> Adapters
    Adapters --> DB
    Adapters --> External
```

### Transporte

Responsável por:

- desserializar;
- validar a forma básica do comando;
- obter Principal;
- aplicar ou chamar rate limiting;
- invocar o caso de uso;
- devolver confirmação estruturada.

Não contém regras de negócio.

### Aplicação

Coordena:

- autorização;
- repositórios;
- domínio;
- transações;
- publicação de eventos.

### Domínio

Contém invariantes como:

- uma DM tem dois participantes distintos;
- apenas o remetente edita;
- estado de leitura não recua;
- estado de entrega não recua.

### Infraestrutura

Implementa:

- persistência;
- cifra em repouso;
- WebSocket;
- integração externa;
- rate limiter;
- outbox.

---

## 5. Modelo de domínio atual

```mermaid
classDiagram
    class Conversation {
        UUID id
        ConversationType type
        Set~UUID~ participants
        Instant createdAt
        hasParticipant(userId)
    }

    class Message {
        UUID id
        UUID conversationId
        UUID senderId
        EncryptedContent content
        Instant sentAt
        Instant editedAt
        MessageStatus status
        long version
    }

    class ConversationParticipant {
        UUID conversationId
        UUID userId
        UUID lastReadMessageId
        Instant readAt
    }

    Conversation "1" --> "2" ConversationParticipant
    Conversation "1" --> "*" Message
```

Mesmo que a implementação atual guarde participantes de outra forma, este é o modelo lógico pretendido.

---

## 6. Fluxo de envio

```mermaid
sequenceDiagram
    participant A as Cliente A
    participant MC as MessagingCore
    participant DB as Messaging DB
    participant B as Cliente B

    A->>MC: SEND /app/messages.send
    MC->>MC: Obter senderId do Principal
    MC->>MC: Validar payload e rate limit
    MC->>DB: Localizar ou criar DM
    MC->>DB: Persistir mensagem cifrada
    DB-->>MC: Mensagem persistida
    MC-->>A: COMMAND_ACK / SENT
    MC-->>B: MESSAGE_RECEIVED
    B->>MC: MESSAGE_DELIVERED command
    MC->>DB: Avançar estado para DELIVERED
    MC-->>A: MESSAGE_DELIVERED
```

A confirmação SENT significa persistência, não entrega ao destinatário.

---

## 7. Fluxo de leitura

```mermaid
sequenceDiagram
    participant B as Cliente B
    participant MC as MessagingCore
    participant DB as Messaging DB
    participant A as Cliente A

    B->>MC: CONVERSATION_READ até messageId
    MC->>MC: Validar participante e mensagem
    MC->>DB: Avançar cursor de B
    DB-->>MC: Cursor atualizado ou inalterado
    MC-->>B: COMMAND_ACK
    MC-->>A: MESSAGE_READ até messageId
```

O cursor nunca deve recuar. Repetir o mesmo comando não deve causar erro nem eventos inconsistentes.

---

## 8. Fluxo de edição

```mermaid
sequenceDiagram
    participant A as Cliente A
    participant MC as MessagingCore
    participant DB as Messaging DB
    participant B as Cliente B

    A->>MC: EDIT_MESSAGE
    MC->>MC: Validar ownership, tempo e versão
    MC->>DB: Atualizar conteúdo e incrementar versão
    DB-->>MC: Sucesso
    MC-->>A: COMMAND_ACK
    MC-->>B: MESSAGE_EDITED
```

---

## 9. Fluxo de eliminação

```mermaid
sequenceDiagram
    participant A as Cliente A
    participant MC as MessagingCore
    participant DB as Messaging DB
    participant B as Cliente B

    A->>MC: DELETE_MESSAGE
    MC->>MC: Validar ownership e regras
    MC->>DB: Marcar mensagem como eliminada
    DB-->>MC: Sucesso
    MC-->>A: COMMAND_ACK
    MC-->>B: MESSAGE_DELETED
```

---

## 10. Destinos privados

Todos os envios privados devem usar:

```text
convertAndSendToUser(userId.toString(), destination, payload)
```

O valor deve coincidir exatamente com:

```text
Principal.getName()
```

É proibido misturar:

- UUID;
- username;
- email.

---

## 11. Gestão de sessões

A gestão deve ficar a cargo do Spring STOMP.

Um mapa manual de WebSocketSession:

- não é necessário para destinos STOMP;
- falha com múltiplas sessões;
- é local a uma instância;
- duplica a responsabilidade do framework.

Se for necessária presença online, deve ser implementada separadamente através de eventos de conexão/desconexão.

---

## 12. Consistência e fiabilidade

### Fase inicial

O evento pode ser processado após a persistência através de eventos transacionais.

### Fase robusta

Usar Outbox:

```mermaid
flowchart LR
    Command[Comando]
    Transaction[Transação]
    Message[(Mensagem)]
    OutboxTable[(Outbox)]
    Worker[Outbox worker]
    Broker[Broker]
    Client[Cliente]

    Command --> Transaction
    Transaction --> Message
    Transaction --> OutboxTable
    OutboxTable --> Worker
    Worker --> Broker
    Broker --> Client
```

Mensagem e evento Outbox são gravados na mesma transação. O worker pode repetir o evento, pelo que consumidores e clientes devem tolerar duplicados.

---

## 13. Evolução para grupos

A versão atual deve validar exatamente dois participantes, mas evitar pressupostos espalhados pelo sistema.

Para preparar grupos:

- eventos devem incluir conversationId;
- leitura deve usar cursor por participante;
- destinos privados devem usar userId;
- descoberta do “outro utilizador” deve ficar limitada ao caso de uso DM;
- o domínio deve possuir ConversationType;
- o protocolo não deve chamar a conversa de “otherUser”.

A expansão para grupos exigirá:

- lista variável de participantes;
- permissões;
- eventos de entrada e saída;
- fan-out para vários utilizadores;
- alteração de chaves E2E;
- recibos de entrega/leitura por participante.

---

## 14. Evolução para E2E

Com E2E, MessagingCore deixa de desencriptar mensagens.

```mermaid
flowchart LR
    DeviceA[Dispositivo A]
    Server[MessagingCore]
    DB[(Messaging DB)]
    DeviceB[Dispositivo B]

    DeviceA -->|Ciphertext| Server
    Server --> DB
    Server -->|Ciphertext| DeviceB
    DeviceB -->|Desencriptação local| DeviceB
```

Antes de E2E devem estar estáveis:

- identidade por dispositivo;
- sincronização;
- múltiplas sessões;
- mensagens offline;
- versionamento de payload;
- edição e eliminação;
- ordem dos eventos;
- recuperação de estado.
