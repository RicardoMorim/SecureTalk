# SecureTalk — Documentação técnica

## Estado do projeto

O SecureTalk é uma aplicação de mensagens que está a ser recuperada e reorganizada. A implementação atual contém os módulos Auth e MessagingCore, mas ainda existe acoplamento direto entre eles e o protocolo WebSocket precisa de ser estabilizado.

Esta documentação define o estado pretendido da aplicação, não garante que a implementação atual já cumpra todas as regras aqui descritas.

## Objetivo atual

A primeira versão funcional deve suportar exclusivamente mensagens diretas entre dois utilizadores.

O objetivo é obter um sistema:

- autenticado;
- corretamente autorizado;
- previsível;
- testável;
- separado por responsabilidades;
- preparado para futura expansão para grupos;
- preparado para futura encriptação ponta a ponta.

## Decisões principais

1. Uma conversa é, por enquanto, uma DM entre exatamente dois utilizadores distintos.
2. O UUID é a identidade técnica e canónica de um utilizador.
3. Email e username são dados de apresentação e não identificadores internos.
4. Auth e MessagingCore são microserviços separados.
5. MessagingCore não pode importar entidades nem serviços internos do Auth.
6. STOMP sobre WebSocket é usado para comandos e notificações em tempo real.
7. As respostas usam JSON estruturado e não strings livres.
8. O servidor obtém a identidade do remetente através do Principal autenticado.
9. A encriptação atual protege dados em repouso, mas ainda não é E2E.
10. A implementação de E2E será a última fase do projeto.

## Documentos

- [Requirements](01-requirements.md)
- [Arquitetura e design](02-architecture-and-design.md)
- [Protocolo WebSocket](03-websocket-protocol.md)
- [Sprints de implementação](04-implementation-sprints.md)
- [Guia detalhado de correção](05-correction-guide.md)

## Glossário

### Auth

Microserviço responsável por:

- registo;
- autenticação;
- credenciais;
- emissão e validação criptográfica de tokens;
- identidade e roles dos utilizadores.

### MessagingCore

Microserviço responsável por:

- conversas;
- mensagens;
- edição e eliminação;
- estado de entrega;
- estado de leitura;
- notificações WebSocket relacionadas com mensagens.

### DM

Conversa direta entre exatamente dois utilizadores distintos.

### Comando

Pedido enviado por um cliente ao servidor, como enviar, editar ou apagar uma mensagem.

### Evento

Facto ocorrido no sistema, como mensagem criada, editada, entregue ou lida.

### Confirmação

Resposta ao utilizador que executou um comando. Deve possuir um correlationId que a associe ao comando original.

### SENT

A mensagem foi validada e persistida pelo MessagingCore.

### DELIVERED

Pelo menos uma sessão autenticada do destinatário confirmou a receção da mensagem.

### READ

O cursor de leitura do destinatário avançou até à mensagem indicada.

### E2E

Encriptação ponta a ponta. Apenas os dispositivos participantes conseguem desencriptar o conteúdo. O servidor não possui o texto legível nem as chaves necessárias para o obter.
