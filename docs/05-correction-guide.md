# Guia detalhado de correção

Este documento explica o que está errado, por que está errado e como corrigir. A ordem é importante: corrigir lógica de mensagens antes da identidade WebSocket pode produzir testes enganadores.

---

## 1. Estabelecer uma linha de base

Antes de alterar código:

1. inicia as dependências;
2. inicia Auth;
3. inicia MessagingCore;
4. cria dois utilizadores;
5. autentica ambos;
6. estabelece duas ligações STOMP;
7. regista os destinos subscritos;
8. tenta enviar uma mensagem;
9. verifica a base de dados;
10. verifica os logs.

Cria uma tabela manual:

| Passo | Esperado | Resultado atual | Erro |
|---|---|---|---|
| Login A | JWT |  |  |
| Connect A | CONNECTED |  |  |
| Connect B | CONNECTED |  |  |
| Send A → B | SENT |  |  |
| Persistência | Linha criada |  |  |
| Evento B | MESSAGE_RECEIVED |  |  |

Isto evita corrigir sintomas sem perceber onde o fluxo falha.

---

## 2. Corrigir a identidade WebSocket

### Problema

Guardar email, username e roles nos atributos do handshake não garante que a sessão STOMP possui um Principal reconhecido pelo Spring.

Os controllers dependem de Principal e os destinos privados também.

### Como corrigir

1. inclui userId no JWT;
2. valida o token durante o handshake ou CONNECT;
3. extrai o UUID;
4. associa um Principal à sessão;
5. garante que getName devolve o UUID;
6. rejeita a ligação quando não for possível criar identidade.

Não uses uma pesquisa pelo email no Auth durante cada mensagem.

### Como verificar

Dentro de um teste de integração, envia um comando autenticado e confirma:
```
text
principal != null
principal.getName() == UUID esperado
```yaml
Depois envia uma notificação privada para esse mesmo texto. O cliente deve recebê-la.

---

## 3. Uniformizar a identidade dos destinos privados

### Problema

O sistema mistura username, UUID e email. O Spring envia uma mensagem privada procurando sessões cujo Principal tenha exatamente o nome indicado.

Se o Principal se chama UUID e o handler envia para username, nenhuma sessão corresponde.

### Como corrigir

Escolhe esta regra:
```
text
Todos os destinos privados usam userId.toString().
```typescript
Revê todas as chamadas de envio privado e todos os eventos. Substitui campos como:

- receiverUsername;
- senderUsername;
- userWhoReadUsername;

por UUIDs quando forem usados para routing ou autorização.

Username pode continuar a existir num DTO de apresentação, mas não pode controlar routing.

### Como verificar

Pesquisa globalmente por envios privados. Cada primeiro argumento deve resultar num UUID.

Testa A e B e confirma que:

- evento para A não chega a B;
- evento para B não chega a A;
- duas sessões de A recebem o comportamento previsto.

---

## 4. Remover o registry manual de sessões

### Problema

Um mapa manual de WebSocketSession não está integrado naturalmente com o broker STOMP e só guarda uma sessão por chave.

Um segundo dispositivo substitui o primeiro. Numa segunda instância da aplicação, o mapa também deixa de representar todas as sessões.

### Como corrigir

1. identifica todas as utilizações;
2. se não estiver no fluxo, remove-o;
3. usa SimpMessagingTemplate e destinos /user;
4. se precisares de presença, trata eventos de conexão e desconexão separadamente;
5. não uses presença como garantia de entrega.

### Como verificar

Liga duas sessões com o mesmo utilizador e confirma que ambas podem receber os eventos definidos.

---

## 5. Desacoplar MessagingCore do Auth

### Problema

Usar UserService e entidades do Auth diretamente transforma os microserviços em módulos acoplados.

MessagingCore passa a depender:

- da implementação do Auth;
- do modelo de dados do Auth;
- possivelmente da base de dados do Auth;
- da disponibilidade síncrona do Auth.

### Como corrigir gradualmente

#### Passo 1

Remove a procura do utilizador no controller. O userId vem do Principal.

#### Passo 2

Altera os comandos para receber recipientId e não username.

#### Passo 3

Nos serviços, guarda apenas UUIDs de participantes.

#### Passo 4

Nos handlers, envia diretamente para o UUID presente no evento.

#### Passo 5

Se precisares de validar que o destinatário existe, cria uma interface local pequena, como UserDirectory. A implementação pode chamar um endpoint interno do Auth.

Não devolvas nem uses a entidade User do Auth dentro do domínio do MessagingCore.

#### Passo 6

Sempre que possível, deixa a apresentação do username para o frontend, que pode consultar Auth.

### Como verificar

Remove a dependência de código do Auth do módulo MessagingCore. O módulo deve compilar sem imports do domínio ou serviços do Auth.

---

## 6. Criar respostas JSON estruturadas

### Problema

Strings como “Message sent successfully” não permitem ao cliente:

- identificar a operação;
- distinguir erros;
- obter messageId;
- associar resposta ao comando;
- manter compatibilidade quando o texto muda.

Além disso, um retorno STOMP deve ter destino conhecido.

### Como corrigir

1. cria um envelope comum;
2. adiciona type;
3. adiciona correlationId;
4. adiciona timestamp;
5. separa data de error;
6. envia a confirmação para uma fila privada explícita;
7. cria tratamento central de exceções.

O cliente deve reagir a códigos e tipos, nunca ao texto humano.

### Como verificar

Cada comando deve resultar em exatamente uma confirmação:

- COMMAND_ACK;
- ou COMMAND_REJECTED.

---

## 7. Corrigir validações de payload

### Problema

Validações encadeadas podem aceder a objetos internos nulos. isEmpty também permite conteúdo formado apenas por espaços.

Mensagens de erro relativas a “ID positivo” são incorretas para UUID.

### Como corrigir

Valida por níveis:

1. comando não nulo;
2. correlationId presente;
3. recipientId presente;
4. content presente;
5. content não blank;
6. content dentro do tamanho máximo;
7. recipient diferente do sender.

Aplica Bean Validation aos DTOs para validação estrutural. Mantém regras de domínio no domínio ou serviço de aplicação.

Impõe um limite máximo de paginação mesmo que o cliente peça um valor maior.

---

## 8. Garantir DMs únicas

### Problema

A sequência “procurar conversa; se não existir, criar” sofre uma race condition.

Dois pedidos simultâneos podem não encontrar a conversa e criar duas.

### Como corrigir

1. cria uma representação normalizada do par;
2. ordena os dois UUIDs;
3. cria uma chave única para esse par;
4. adiciona uma restrição única na base de dados;
5. tenta criar;
6. se ocorrer conflito de unicidade, lê a conversa já criada;
7. executa a operação numa transação adequada.

A base de dados deve ser a autoridade final da unicidade.

### Como verificar

Executa dois envios simultâneos entre A e B quando ainda não existe conversa. A base de dados deve ficar com uma DM.

---

## 9. Impedir DM consigo próprio

### Problema

Um conjunto imutável de participantes pode falhar ao receber o mesmo UUID duas vezes, mas isso produz uma exceção técnica e não uma regra clara.

### Como corrigir

Valida explicitamente:
```
text
senderId != recipientId
```yaml
Devolve RECIPIENT_EQUALS_SENDER.

A entidade Conversation também deve proteger a própria invariante, para que nenhum outro caminho consiga criar uma DM inválida.

---

## 10. Corrigir autorização de leitura

### Problema

Obter uma mensagem por ID, desencriptá-la e devolvê-la sem verificar participação permite acesso indevido quando alguém conhece o UUID.

### Como corrigir

Todo o caso de uso deve receber:

- messageId;
- authenticatedUserId.

Fluxo:

1. procurar a mensagem;
2. se não existir, devolver MESSAGE_NOT_FOUND;
3. procurar a conversa;
4. confirmar que authenticatedUserId participa;
5. apenas depois desencriptar;
6. mapear para DTO;
7. devolver.

Não devolvas a entidade persistida diretamente.

### Como verificar

Cria A, B e C. A conversa pertence a A e B. C não pode consultar nenhuma mensagem, mesmo conhecendo messageId.

---

## 11. Corrigir autorização de edição e eliminação

### Problema

Verificar ownership em memória e atualizar depois pode permitir conflitos concorrentes. Também é necessário verificar conversa e estado.

### Como corrigir

Na mesma transação:

1. obter mensagem;
2. validar senderId;
3. validar regra temporal;
4. validar regra da última mensagem, caso seja mantida;
5. validar expectedVersion;
6. atualizar;
7. incrementar versão;
8. publicar evento depois do commit.

O authenticatedUserId vem do Principal, nunca do payload.

### Como verificar

- B não edita mensagem de A;
- A não edita depois do limite;
- versão antiga produz VERSION_CONFLICT;
- operação bem-sucedida produz evento para B.

---

## 12. Corrigir a lógica de leitura

### Problema atual

Percorrer mensagens e parar ao encontrar uma mensagem própria ou já lida depende da ordenação da consulta.

Marcar apenas uma mensagem e assumir que as anteriores ficaram lidas não corresponde ao estado persistido, caso cada mensagem tenha um booleano.

O método pode ainda devolver sucesso sem alterar nada.

### Correção recomendada

Substitui o booleano por um cursor por participante e conversa:
```
text
conversationId
userId
lastReadMessageId
readAt
```markdown
Fluxo:

1. receber conversationId e lastReadMessageId;
2. obter utilizador do Principal;
3. confirmar participação;
4. confirmar que a mensagem pertence à conversa;
5. comparar a posição com o cursor atual;
6. se for anterior ou igual, retornar sucesso idempotente sem regredir;
7. se for posterior, atualizar;
8. publicar MESSAGE_READ;
9. notificar o outro participante.

### Concorrência

A atualização deve garantir que um pedido atrasado não recua o cursor. Isso pode ser protegido por:

- comparação na query;
- optimistic locking;
- sequência monotónica.

### Como verificar

Envia atualizações fora de ordem:
```
text
M10, depois M7
```yaml
O cursor final deve continuar em M10.

---

## 13. Implementar DELIVERED corretamente

### Problema

Enviar pelo broker não prova que o cliente recebeu a mensagem.

### Como corrigir

1. servidor persiste;
2. servidor envia MESSAGE_RECEIVED;
3. cliente recebe;
4. cliente envia comando delivered;
5. servidor confirma que o utilizador é o destinatário;
6. servidor atualiza estado;
7. servidor notifica o remetente.

Se o mesmo comando for repetido, retorna sucesso sem duplicar efeitos.

### Como verificar

Sem confirmação do cliente, a mensagem permanece SENT. Depois da confirmação, passa para DELIVERED.

---

## 14. Completar eliminação

### Problema

Publicar um evento sem handler deixa o outro cliente desatualizado.

### Como corrigir

1. criar handler para MESSAGE_DELETED;
2. identificar o outro participante por UUID;
3. enviar para /user/queue/messages;
4. incluir messageId, conversationId, deletedAt e version;
5. preferir soft delete;
6. confirmar ao autor em /user/queue/commands.

### Como verificar

Depois de A apagar, A e B devem apresentar a mesma mensagem eliminada.

---

## 15. Corrigir datas

### Problema

Converter entre Instant, ZoneId do servidor e LocalDateTime pode alterar o significado e tornar o resultado dependente da máquina.

### Como corrigir

Usa Instant e UTC na persistência e no protocolo. O cliente converte para o fuso horário visual.

Evita ZoneId.systemDefault em eventos e DTOs de rede.

---

## 16. Evitar efeitos antes do commit

### Problema

Um evento assíncrono pode ser executado antes de a transação confirmar ou pode perder-se num reinício.

### Correção inicial

1. envolver escrita numa transação;
2. publicar eventos apenas depois do commit;
3. tornar handlers idempotentes;
4. registar falhas;
5. não dizer que a mensagem foi entregue só porque foi persistida.

### Correção robusta

Implementar Outbox:

1. guardar alteração e evento na mesma transação;
2. worker procura eventos pendentes;
3. envia;
4. marca como processado;
5. repete falhas com backoff;
6. move falhas permanentes para tratamento manual.

---

## 17. Rever tratamento assíncrono

### Problema

Capturar exceções e apenas escrever no log evita que o chamador saiba que a notificação falhou. Filas assíncronas sem limites também podem consumir memória sob carga.

### Como corrigir

- configurar executor com filas limitadas;
- medir tarefas rejeitadas;
- adicionar retry onde fizer sentido;
- não repetir erros permanentes;
- usar Outbox para operações que não podem ser perdidas;
- separar métricas, notificações externas e entrega WebSocket;
- não incluir conteúdo nos erros.

---

## 18. Corrigir rate limiting

### Problema

Configurar um componente partilhado no construtor do controller cria efeitos globais inesperados.

### Como corrigir

1. configurar através de propriedades e beans;
2. validar propriedades durante startup;
3. usar userId do Principal;
4. criar chave por operação;
5. devolver retryAfter;
6. tornar o armazenamento partilhado se existirem várias instâncias;
7. testar concorrência.

O rate limiter não substitui limites de payload ou autorização.

---

## 19. Corrigir paginação

### Problema

Paginação por página pode saltar ou duplicar mensagens quando chegam novas mensagens durante a navegação.

### Como corrigir

Prefere cursor:
```
text
beforeMessageId
limit
```markdown
A query deve ordenar de forma determinística e impor limite máximo no servidor.

Depois da reconexão, permite também pedir mensagens posteriores ao último ID conhecido.

### Como verificar

Carrega uma página, adiciona uma mensagem e carrega a seguinte. Nenhuma mensagem antiga deve desaparecer do percurso.

---

## 20. Rever a regra “apenas a última mensagem”

Esta regra pode ser válida, mas deve ser uma decisão de produto e não um detalhe acidental.

Perguntas:

- editar uma mensagem anterior é realmente proibido?
- apagar uma mensagem anterior é proibido?
- a leitura pelo destinatário altera essa permissão?
- a regra continua válida para grupos?
- a regra dos 15 minutos é suficiente?

Se mantida:

- documenta-a;
- testa-a;
- executa validação e escrita na mesma transação;
- trata concorrência;
- devolve MESSAGE_NOT_LAST_IN_CONVERSATION.

---

## 21. Logs e métricas

Cada comando deve registar:

- correlationId;
- operation;
- authenticatedUserId;
- conversationId;
- messageId;
- resultado;
- duração;
- errorCode.

Nunca registar:

- conteúdo;
- JWT;
- cookies;
- chaves;
- ciphertext completo;
- credenciais.

Métricas mínimas:

- conexões ativas;
- handshakes rejeitados;
- mensagens persistidas;
- notificações falhadas;
- comandos rejeitados;
- rate limits;
- latência;
- tamanho da outbox.

---

## 22. Ordem exata de correção

Segue esta ordem:

1. recuperar execução;
2. definir contratos;
3. introduzir userId no JWT;
4. criar Principal STOMP;
5. uniformizar routing por UUID;
6. remover registry manual;
7. remover UserService do controller;
8. remover Auth do restante MessagingCore;
9. criar respostas estruturadas;
10. corrigir validações;
11. garantir DM única;
12. corrigir autorização de leitura;
13. corrigir edição e eliminação;
14. substituir leitura por cursor;
15. implementar confirmação de entrega;
16. completar eventos;
17. estabilizar transações;
18. corrigir paginação;
19. melhorar rate limiting;
20. adicionar observabilidade;
21. automatizar testes;
22. introduzir Outbox se necessária;
23. preparar grupos;
24. implementar E2E no fim.

Não comeces pela E2E. A E2E altera o conteúdo, mas não resolve identidade, autorização, ordenação, entrega, leitura ou sincronização.

---

## 23. Teste final manual

Com A, B e C:

1. A e B autenticam-se;
2. ambos ligam dois dispositivos;
3. A envia a B;
4. A recebe SENT;
5. os dispositivos de B recebem a mensagem;
6. um dispositivo de B confirma entrega;
7. A recebe DELIVERED;
8. B lê até à mensagem;
9. A recebe READ;
10. A edita;
11. B recebe EDITED;
12. A elimina;
13. B recebe DELETED;
14. C tenta consultar e recebe erro;
15. B desliga;
16. A envia outra mensagem;
17. B volta a ligar;
18. B recupera a mensagem através da sincronização;
19. dois pedidos simultâneos não criam duas DMs;
20. nenhum log contém conteúdo.

Quando este cenário funcionar de forma repetível, a base DM estará suficientemente estável para começar a preparar E2E.
