# Protocolo WebSocket/STOMP

## 1. Endpoint

```text
/wss endpoint: /ws
application prefix: /app
private user prefix: /user
````

Em desenvolvimento pode ser usado `ws://`. Em produção deve ser utilizado `wss://`.

---

## 2. Identidade

Depois da autenticação:

```text
Principal.getName() = userId UUID
```

Nenhum comando aceita senderId como fonte de identidade.

---

## 3. Subscrições do cliente

Depois de ligar, o cliente deve subscrever:

```text
/user/queue/commands
/user/queue/messages
/user/queue/errors
```

### commands

Confirmações dos comandos enviados por esse cliente.

### messages

Eventos de mensagens e conversas.

### errors

Erros que não possam ser representados como confirmação de um comando.

---

## 4. Envelope

### Sucesso

```json
{
  "type": "COMMAND_ACK",
  "correlationId": "CORRELATION_ID",
  "eventId": "EVENT_ID",
  "success": true,
  "timestamp": "2026-08-06T12:00:00Z",
  "data": {
    "operation": "SEND_MESSAGE",
    "messageId": "MESSAGE_ID",
    "conversationId": "CONVERSATION_ID",
    "status": "SENT"
  },
  "error": null
}
```

### Erro

```json
{
  "type": "COMMAND_REJECTED",
  "correlationId": "CORRELATION_ID",
  "eventId": "EVENT_ID",
  "success": false,
  "timestamp": "2026-08-06T12:00:00Z",
  "data": null,
  "error": {
    "code": "FORBIDDEN",
    "message": "Operation not allowed",
    "retryable": false,
    "retryAfterMs": null
  }
}
```

`eventId` permite deduplicar eventos. `correlationId` associa a resposta ao comando.

---

## 5. Comandos

### Enviar mensagem

Destino:

```text
/app/messages.send
```

Payload:

```json
{
  "correlationId": "CORRELATION_ID",
  "recipientId": "RECIPIENT_USER_ID",
  "content": "Example message"
}
```

### Editar mensagem

Destino:

```text
/app/messages.edit
```

Payload:

```json
{
  "correlationId": "CORRELATION_ID",
  "messageId": "MESSAGE_ID",
  "expectedVersion": 1,
  "content": "Edited example"
}
```

### Eliminar mensagem

Destino:

```text
/app/messages.delete
```

Payload:

```json
{
  "correlationId": "CORRELATION_ID",
  "messageId": "MESSAGE_ID",
  "expectedVersion": 1
}
```

### Confirmar entrega

Destino:

```text
/app/messages.delivered
```

Payload:

```json
{
  "correlationId": "CORRELATION_ID",
  "messageId": "MESSAGE_ID"
}
```

### Atualizar leitura

Destino:

```text
/app/conversations.read
```

Payload:

```json
{
  "correlationId": "CORRELATION_ID",
  "conversationId": "CONVERSATION_ID",
  "lastReadMessageId": "MESSAGE_ID"
}
```

---

## 6. Eventos privados

### Mensagem recebida

```json
{
  "type": "MESSAGE_RECEIVED",
  "eventId": "EVENT_ID",
  "correlationId": null,
  "success": true,
  "timestamp": "2026-08-06T12:00:00Z",
  "data": {
    "messageId": "MESSAGE_ID",
    "conversationId": "CONVERSATION_ID",
    "senderId": "SENDER_USER_ID",
    "content": "Example message",
    "sentAt": "2026-08-06T12:00:00Z",
    "version": 1
  },
  "error": null
}
```

### Mensagem editada

```json
{
  "type": "MESSAGE_EDITED",
  "eventId": "EVENT_ID",
  "correlationId": null,
  "success": true,
  "timestamp": "2026-08-06T12:05:00Z",
  "data": {
    "messageId": "MESSAGE_ID",
    "conversationId": "CONVERSATION_ID",
    "content": "Edited example",
    "editedAt": "2026-08-06T12:05:00Z",
    "version": 2
  },
  "error": null
}
```

### Mensagem eliminada

```json
{
  "type": "MESSAGE_DELETED",
  "eventId": "EVENT_ID",
  "correlationId": null,
  "success": true,
  "timestamp": "2026-08-06T12:06:00Z",
  "data": {
    "messageId": "MESSAGE_ID",
    "conversationId": "CONVERSATION_ID",
    "deletedAt": "2026-08-06T12:06:00Z",
    "version": 3
  },
  "error": null
}
```

### Entrega

```json
{
  "type": "MESSAGE_DELIVERED",
  "eventId": "EVENT_ID",
  "correlationId": null,
  "success": true,
  "timestamp": "2026-08-06T12:01:00Z",
  "data": {
    "messageId": "MESSAGE_ID",
    "conversationId": "CONVERSATION_ID",
    "recipientId": "RECIPIENT_USER_ID",
    "deliveredAt": "2026-08-06T12:01:00Z"
  },
  "error": null
}
```

### Leitura

```json
{
  "type": "MESSAGE_READ",
  "eventId": "EVENT_ID",
  "correlationId": null,
  "success": true,
  "timestamp": "2026-08-06T12:02:00Z",
  "data": {
    "conversationId": "CONVERSATION_ID",
    "readerId": "READER_USER_ID",
    "lastReadMessageId": "MESSAGE_ID",
    "readAt": "2026-08-06T12:02:00Z"
  },
  "error": null
}
```

---

## 7. Códigos de erro

```text
INVALID_PAYLOAD
UNAUTHENTICATED
FORBIDDEN
USER_NOT_FOUND
CONVERSATION_NOT_FOUND
MESSAGE_NOT_FOUND
NOT_A_PARTICIPANT
RECIPIENT_EQUALS_SENDER
MESSAGE_TOO_LARGE
MESSAGE_EDIT_WINDOW_EXPIRED
MESSAGE_NOT_LAST_IN_CONVERSATION
VERSION_CONFLICT
RATE_LIMIT_EXCEEDED
INTERNAL_ERROR
```

---

## 8. Idempotência e ordenação

- Eventos devem possuir eventId.
- O cliente deve ignorar eventIds já processados.
- Mensagens editadas ou eliminadas devem possuir versão.
- O cliente deve ignorar versões anteriores à versão local.
- O cursor de leitura nunca recua.
- Uma confirmação DELIVERED repetida não altera o resultado.
- O cliente deve recuperar dados através da API depois de uma reconexão.

---

## 9. Compatibilidade futura com E2E

Quando E2E for implementado, `content` deve deixar de ser texto e passar a ser um envelope opaco versionado, por exemplo:

```json
{
  "cryptoVersion": 1,
  "senderDeviceId": "SENDER_DEVICE_ID",
  "ciphertext": "BASE64_CIPHERTEXT",
  "nonce": "BASE64_NONCE",
  "keyReference": "KEY_REFERENCE"
}
```

O servidor não deve interpretar nem desencriptar este conteúdo.
