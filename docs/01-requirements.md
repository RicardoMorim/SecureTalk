# Requirements do SecureTalk

## 1. Âmbito da primeira versão funcional

A primeira versão funcional suporta:

- autenticação de utilizadores;
- ligação STOMP sobre WebSocket;
- conversas DM;
- envio de mensagens;
- consulta e paginação de mensagens;
- edição de mensagens;
- eliminação de mensagens;
- confirmação de entrega;
- confirmação de leitura;
- rate limiting;
- armazenamento cifrado;
- notificações privadas em tempo real.

Não fazem parte desta versão:

- conversas de grupo;
- anexos;
- chamadas;
- pesquisa global;
- presença distribuída;
- E2E;
- federação entre servidores.

---

## 2. Requirements funcionais

### FR-001 — Autenticação HTTP

O Auth deve autenticar o utilizador e emitir um JWT assinado.

O token deve incluir, no mínimo:

- userId;
- emissor;
- audiência;
- data de emissão;
- data de expiração;
- roles necessárias.

O userId deve ser um UUID e deve ser a identidade canónica do utilizador.

### FR-002 — Autenticação WebSocket

Uma ligação WebSocket só pode ser aceite quando possui um token:

- presente;
- corretamente assinado;
- não expirado;
- emitido pelo emissor esperado;
- destinado à audiência esperada.

A sessão STOMP deve possuir um Principal.

`Principal.getName()` deve devolver o UUID do utilizador em formato textual.

### FR-003 — Criação de DM

Quando um utilizador envia a primeira mensagem a outro utilizador:

1. MessagingCore procura uma DM para o par;
2. se não existir, cria-a;
3. persiste a mensagem nessa conversa;
4. garante que não são criadas DMs duplicadas.

Uma DM deve ter exatamente dois participantes distintos.

### FR-004 — Envio de mensagem

Um utilizador autenticado pode enviar uma mensagem a outro utilizador existente.

O servidor deve:

1. obter o remetente através do Principal;
2. validar o destinatário;
3. rejeitar uma mensagem para o próprio remetente;
4. validar o conteúdo;
5. localizar ou criar a DM;
6. persistir a mensagem;
7. confirmar SENT ao remetente;
8. notificar o destinatário.

O cliente não pode escolher o senderId.

### FR-005 — Consulta de conversa

Um utilizador só pode consultar uma conversa da qual seja participante.

As mensagens devem ser devolvidas:

- numa ordem determinística;
- com paginação;
- através de DTOs;
- sem expor detalhes internos de persistência.

### FR-006 — Consulta de mensagem

Um utilizador só pode consultar uma mensagem se pertencer à respetiva conversa.

A autorização deve acontecer antes de desencriptar o conteúdo.

### FR-007 — Edição de mensagem

Apenas o remetente pode editar a mensagem.

A edição deve respeitar as regras de produto configuradas, incluindo:

- limite temporal;
- regra de ser a última mensagem, enquanto esta regra for mantida.

Depois da edição:

1. a alteração é persistida;
2. o autor recebe confirmação;
3. o outro participante recebe MESSAGE_EDITED.

### FR-008 — Eliminação de mensagem

Apenas o remetente pode eliminar a mensagem.

A eliminação deve respeitar as mesmas regras temporais aplicáveis.

É recomendada eliminação lógica para:

- preservar a posição;
- manter o messageId;
- sincronizar clientes;
- apresentar “mensagem eliminada”.

O outro participante deve receber MESSAGE_DELETED.

### FR-009 — Entrega

Depois de receber uma mensagem, o cliente destinatário deve enviar uma confirmação.

DELIVERED significa que pelo menos um dispositivo autenticado do destinatário confirmou a receção.

Confirmações repetidas devem ser idempotentes.

O estado não pode regredir.

### FR-010 — Leitura

O cliente deve informar até que mensagem a conversa foi lida.

MessagingCore deve manter um cursor de leitura por utilizador e conversa.

O cursor:

- só pode avançar;
- não pode apontar para outra conversa;
- não pode ser atualizado por alguém que não seja participante;
- deve aceitar repetições sem produzir inconsistências.

O outro participante deve receber uma notificação de leitura.

### FR-011 — Múltiplas sessões

Um utilizador pode ter várias sessões:

- vários separadores;
- vários browsers;
- vários dispositivos.

Por defeito, todos os dispositivos ativos do utilizador recebem os eventos privados.

### FR-012 — Rate limiting

O servidor deve aplicar rate limiting pelo UUID autenticado.

Os limites podem ser diferentes por operação:

- enviar;
- editar;
- apagar;
- marcar entregue;
- marcar lida.

Quando o limite é atingido, o cliente recebe um erro estruturado com o código RATE_LIMIT_EXCEEDED.

### FR-013 — Reconexão

Depois de uma reconexão, o cliente deve poder:

1. voltar a subscrever as filas privadas;
2. carregar mensagens posteriores ao último cursor local;
3. reenviar confirmações idempotentes;
4. recuperar o estado sem depender de eventos perdidos.

---

## 3. Requirements de segurança

### SR-001 — Identidade confiável

A identidade nunca pode ser obtida de:

- senderId enviado no payload;
- email enviado no payload;
- username enviado no payload;
- parâmetros controlados pelo cliente.

A identidade vem exclusivamente do Principal autenticado.

### SR-002 — Autorização

Todas as operações sobre mensagens e conversas devem verificar participação ou ownership.

Conhecer um UUID de mensagem não concede acesso à mensagem.

### SR-003 — Isolamento dos microserviços

MessagingCore não pode:

- importar UserService do Auth;
- importar entidades de utilizador do Auth;
- aceder diretamente à base de dados do Auth;
- depender de detalhes internos do Auth.

### SR-004 — Proteção do conteúdo

O conteúdo não pode aparecer em:

- logs;
- métricas;
- mensagens de erro;
- tracing;
- nomes de filas;
- chaves do rate limiter.

### SR-005 — Transporte

HTTP e WebSocket devem usar TLS em produção.

As origens WebSocket permitidas devem ser explícitas e configuráveis. Não deve ser utilizada uma origem global permissiva em produção.

### SR-006 — Erros

O cliente recebe códigos públicos estáveis. Stack traces, queries, nomes de tabelas e detalhes criptográficos não podem ser devolvidos.

### SR-007 — Limites

O servidor deve limitar:

- tamanho da mensagem;
- tamanho dos frames;
- número de mensagens por intervalo;
- valores de paginação;
- tamanho das filas de execução assíncrona.

---

## 4. Requirements de dados

### DR-001 — Identificadores

Devem ser utilizados UUIDs para:

- utilizador;
- conversa;
- mensagem;
- eventos relevantes.

### DR-002 — Unicidade da DM

A base de dados deve garantir uma única DM por par de participantes.

A regra não pode depender apenas de “procurar e depois criar”, devido a concorrência.

### DR-003 — Ordenação

As mensagens devem possuir uma ordenação total e determinística.

Um timestamp sozinho pode não ser suficiente. Deve existir um critério de desempate, como messageId ou uma sequência criada pelo servidor.

### DR-004 — Datas

Datas persistidas e transmitidas devem usar UTC.

Conversões para o fuso horário do utilizador pertencem ao cliente.

### DR-005 — Conteúdo cifrado

Enquanto não existir E2E:

- o conteúdo deve ser cifrado antes da persistência;
- deve ser desencriptado apenas depois de autorização;
- as chaves não podem estar no código-fonte;
- as chaves devem ser fornecidas através de configuração segura.

---

## 5. Requirements de qualidade

### QR-001 — Testabilidade

As regras de domínio devem ser testáveis sem iniciar o servidor WebSocket.

### QR-002 — Observabilidade

Cada comando deve ter correlationId e logs estruturados.

### QR-003 — Idempotência

As seguintes operações devem ser idempotentes:

- confirmação de entrega;
- atualização do cursor de leitura;
- processamento de eventos persistentes;
- repetição de comandos quando suportada.

### QR-004 — Transações

Persistência da alteração e criação de um evento durável devem ocorrer atomicamente quando for introduzida a Outbox.

### QR-005 — Disponibilidade

A indisponibilidade do Auth não deve impedir o MessagingCore de validar localmente tokens já emitidos, desde que a chave de validação esteja disponível.

---

## 6. Critérios de aceitação da versão DM

A versão DM está funcional quando:

- dois utilizadores estabelecem sessões autenticadas;
- o Principal de cada sessão possui o UUID correto;
- A envia uma mensagem e recebe SENT;
- B recebe MESSAGE_RECEIVED;
- B confirma e A recebe MESSAGE_DELIVERED;
- B lê e A recebe MESSAGE_READ;
- uma edição é sincronizada;
- uma eliminação é sincronizada;
- um terceiro utilizador não consegue consultar a conversa;
- eventos privados nunca são enviados ao utilizador errado;
- duas criações concorrentes não criam DMs duplicadas;
- uma reconexão recupera o estado através da API;
- conteúdo não aparece nos logs;
- MessagingCore não importa serviços ou entidades do Auth.
