# Sprints de implementação

Cada sprint deve terminar com testes executáveis e uma demonstração pequena. Não avances apenas porque “o código parece pronto”.

---

## Sprint 0 — Recuperar o ambiente

### Objetivo

Conseguir iniciar e observar o projeto atual.

### Trabalho

- criar branch de recuperação;
- levantar dependências do compose;
- documentar portas e variáveis;
- compilar Auth e MessagingCore;
- executar testes existentes;
- criar dois utilizadores de teste;
- testar login e handshake;
- registar o primeiro erro de cada fluxo;
- confirmar bases de dados usadas;
- remover dependências de caminhos locais.

### Definition of Done

- ambos os módulos compilam;
- dependências iniciam;
- existem instruções de execução;
- é possível reproduzir o estado atual;
- não existem credenciais reais no repositório.

---

## Sprint 1 — Contratos e identidade

### Objetivo

Fixar UUID como identidade canónica.

### Trabalho

- documentar ADRs;
- alterar JWT para conter userId;
- definir issuer e audience;
- definir contratos STOMP;
- definir DTO de resposta;
- definir códigos de erro;
- definir correlationId e eventId.

### Definition of Done

- o contrato não usa username/email como identidade;
- todos os comandos estão documentados;
- todos os eventos estão documentados;
- existem exemplos JSON.

---

## Sprint 2 — Autenticação STOMP

### Objetivo

Criar uma sessão realmente autenticada.

### Trabalho

- validar JWT no handshake ou CONNECT;
- tratar token/cookie ausente;
- criar Principal com UUID;
- rejeitar token inválido;
- restringir origins;
- testar Principal no controller;
- decidir se SockJS será mantido.

### Testes

- token válido;
- token ausente;
- token expirado;
- token manipulado;
- cookies nulos;
- origin inválida;
- Principal correto.

### Definition of Done

`Principal.getName()` devolve o UUID em todos os comandos autenticados.

---

## Sprint 3 — Rotas privadas consistentes

### Objetivo

Garantir que mensagens privadas chegam à pessoa correta.

### Trabalho

- substituir username/email por UUID nos eventos;
- utilizar UUID em todos os convertAndSendToUser;
- criar destinos commands, messages e errors;
- testar dois utilizadores;
- testar duas sessões do mesmo utilizador;
- remover ou desativar o registry manual.

### Definition of Done

Nenhum envio privado usa username ou email.

---

## Sprint 4 — Desacoplar Auth

### Objetivo

Fazer MessagingCore deixar de depender do código interno do Auth.

### Trabalho

- remover UserService do controller;
- obter userId do Principal;
- substituir User por IDs/DTOs locais;
- remover dependências do Auth nos serviços;
- remover dependências do Auth nos handlers;
- criar uma porta UserDirectory apenas se necessária;
- mudar payloads para recipientId;
- confirmar compilação independente.

### Definition of Done

MessagingCore não importa entidades ou serviços internos do Auth.

---

## Sprint 5 — Respostas e erros estruturados

### Objetivo

Eliminar strings livres no protocolo.

### Trabalho

- criar envelope comum;
- criar confirmações;
- criar erros;
- implementar tratamento central de exceções STOMP;
- adicionar correlationId;
- mapear exceções para códigos;
- remover detalhes internos das respostas.

### Definition of Done

Todos os comandos produzem confirmação ou erro JSON estruturado.

---

## Sprint 6 — Domínio DM e concorrência

### Objetivo

Garantir DMs válidas e únicas.

### Trabalho

- validar dois participantes distintos;
- impedir mensagem para o próprio;
- normalizar o par de participantes;
- criar restrição única na base de dados;
- tratar corrida durante criação;
- substituir procura por username;
- centralizar criação/localização da DM.

### Testes

- A cria DM com B;
- B encontra a mesma DM com A;
- A não cria DM consigo;
- dois pedidos concorrentes criam uma única DM.

### Definition of Done

Existe exatamente uma DM por par.

---

## Sprint 7 — Autorização e validação

### Objetivo

Fechar acessos indevidos.

### Trabalho

- passar userId autenticado para todos os casos de uso;
- validar participante ao consultar mensagem;
- autorizar antes de desencriptar;
- validar ownership na edição e eliminação;
- validar paginação e tamanho;
- normalizar exceções;
- tratar valores nulos;
- usar isBlank para texto;
- impor tamanho máximo.

### Definition of Done

Um terceiro utilizador não consegue observar nem modificar recursos da conversa.

---

## Sprint 8 — Leitura por cursor

### Objetivo

Substituir a lógica atual de leitura.

### Trabalho

- criar cursor por conversa e participante;
- criar comando conversations.read;
- garantir cursor monotónico;
- publicar MESSAGE_READ;
- tornar repetição idempotente;
- remover ciclo que marca apenas uma mensagem;
- definir recuperação depois de reconexão.

### Definition of Done

Marcar leitura até uma mensagem produz um estado consistente e não recua.

---

## Sprint 9 — Entrega real

### Objetivo

Distinguir persistência de receção.

### Trabalho

- SENT depois da persistência;
- MESSAGE_RECEIVED para destinatário;
- comando delivered do cliente;
- validar destinatário;
- atualizar estado idempotentemente;
- emitir MESSAGE_DELIVERED para remetente.

### Definition of Done

SENT e DELIVERED têm significados diferentes e testados.

---

## Sprint 10 — Edição e eliminação

### Objetivo

Sincronizar alterações nos dois clientes.

### Trabalho

- criar DTOs finais;
- confirmar regra dos 15 minutos;
- externalizar limite;
- considerar Clock injetável;
- adicionar versão da mensagem;
- implementar evento de eliminação;
- preferir soft delete;
- tratar version conflict;
- enviar confirmações ao autor e eventos ao destinatário.

### Definition of Done

Edição e eliminação são consistentes mesmo com eventos repetidos ou fora de ordem.

---

## Sprint 11 — Transações e eventos

### Objetivo

Impedir eventos antes de commits e estados parciais.

### Trabalho

- delimitar transações;
- publicar eventos depois do commit;
- tornar handlers idempotentes;
- separar sucesso de persistência de sucesso da notificação;
- tratar falhas assíncronas;
- remover dependências não usadas dos handlers.

### Definition of Done

Uma falha de notificação não desfaz nem disfarça a persistência da mensagem.

---

## Sprint 12 — Paginação e sincronização

### Objetivo

Permitir recuperar estado sem depender apenas do WebSocket.

### Trabalho

- definir ordenação total;
- implementar paginação por cursor;
- impor limite máximo;
- obter mensagens posteriores a um cursor;
- testar chegada de mensagens durante paginação;
- documentar sincronização após reconnect.

### Definition of Done

O cliente recupera todas as mensagens sem duplicações ou omissões permanentes.

---

## Sprint 13 — Rate limiting e segurança operacional

### Objetivo

Tornar a exposição segura.

### Trabalho

- separar chaves por operação;
- configurar limites externamente;
- remover configuração do construtor do controller;
- retornar retryAfter;
- restringir origins;
- limitar frames e payloads;
- confirmar TLS;
- rever secrets;
- garantir que conteúdo não entra nos logs.

### Definition of Done

Os principais controlos operacionais estão testados e configuráveis.

---

## Sprint 14 — Observabilidade

### Objetivo

Tornar problemas diagnosticáveis.

### Trabalho

- logs estruturados;
- correlationId;
- métricas de handshake;
- métricas de comandos;
- métricas de eventos falhados;
- health checks;
- tempos de processamento;
- alertas básicos.

### Definition of Done

É possível seguir um comando sem registar conteúdo sensível.

---

## Sprint 15 — Testes end-to-end da versão DM

### Objetivo

Validar a versão completa.

### Cenários

- login e ligação;
- envio;
- entrega;
- leitura;
- edição;
- eliminação;
- utilizador offline;
- reconexão;
- duas sessões;
- acesso proibido;
- evento duplicado;
- evento fora de ordem;
- rate limit;
- concorrência na criação de DM;
- reinício do servidor.

### Definition of Done

Todos os critérios de aceitação de DM estão automatizados ou documentados como testes manuais repetíveis.

---

## Sprint 16 — Outbox e broker externo

### Objetivo

Evitar perda de eventos em falhas.

### Trabalho

- criar tabela outbox;
- guardar mensagem e evento atomicamente;
- criar worker;
- adicionar retries e backoff;
- deduplicar;
- criar dead-letter strategy;
- avaliar broker externo;
- testar restart entre persistência e envio.

### Definition of Done

Um reinício não perde permanentemente uma notificação persistida.

Esta sprint pode ser adiada se a primeira versão aceitar entrega best-effort.

---

## Sprint 17 — Preparação para grupos

### Objetivo

Remover pressupostos DM das interfaces genéricas sem implementar grupos.

### Trabalho

- garantir conversationId em eventos;
- limitar procura do outro participante ao caso DM;
- introduzir ConversationType;
- rever nomes baseados em otherUser;
- documentar permissões futuras;
- manter validação de exatamente dois participantes.

### Definition of Done

O sistema continua DM-only, mas a futura expansão não exige substituir todo o protocolo.

---

## Sprint 18 — E2E

### Pré-condições

Todas as sprints funcionais anteriores estão estáveis.

### Trabalho

- definir identidade criptográfica por dispositivo;
- definir distribuição de chaves;
- definir rotação;
- definir novos dispositivos;
- definir recuperação;
- versionar envelopes;
- mover cifra/decifra para clientes;
- tratar edição e eliminação;
- tratar mensagens offline;
- remover desencriptação do servidor;
- executar revisão de segurança especializada.

### Definition of Done

MessagingCore armazena e transmite apenas ciphertext e não possui as chaves para obter o conteúdo.
