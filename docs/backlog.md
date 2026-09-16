# BomFilme — Backlog inicial

Nosso backlog reúne as histórias de usuário e as tarefas técnicas do projeto, organizadas por prioridade. P1 é essencial, P2 é importante e P3 é desejável. A coluna Sprint indica a etapa de implementação; na Sprint 0, esses itens compõem o planejamento.

| Prio | História ou tarefa | Critérios de aceitação | Sprint |
| --- | --- | --- | --- |
| P1 | Como usuário, quero consultar filmes em cartaz para escolher o que assistir. | Listagem paginada, ordenada por popularidade; resposta vazia quando não houver filmes. | 1 |
| P1 | Como usuário, quero consultar cinemas para encontrar onde assistir. | Listagem paginada com informações básicas do cinema. | 1 |
| P1 | Como usuário, quero consultar assentos de uma sessão para escolher um lugar disponível. | Identifica disponíveis e ocupados; rejeita seleção de assento indisponível. Mapa visual depende de interface ainda não definida. | 1 |
| P1 | Como operador, quero impedir vendas duplicadas de um assento para garantir ingressos válidos. | Java e banco garantem exclusividade por sessão e assento; em compras concorrentes, no máximo uma é confirmada; teste reproduz a disputa entre instâncias; reserva impede compras conflitantes durante o pagamento. | 2 |
| P1 | Como usuário, quero concluir uma compra com pagamento simulado para receber meu ingresso. | Java cria compra pendente e reserva assento; solicita pagamento ao Go via gRPC, sem cobrança real; aprovação confirma compra e gera ingresso; recusa libera reserva e não gera ingresso confirmado; timeout ou falha não confirma compra automaticamente. Política de recuperação e expiração pendente. | 2 |
| P1 | Como administrador, quero gerenciar redes e cinemas para organizar os locais disponíveis. | CRUD de duas entidades relacionadas, paginação, filtros e validação; exclusão respeita vínculos. | 1 |
| P1 | Como administrador, quero gerenciar salas, assentos e sessões para disponibilizar a programação. | Sala vinculada a cinema; sessão vinculada a sala e filme; assento pertence à sala; conflito de horários tratado. | 1–2 |
| P1 | Integrar Java ao processador Go de pagamento simulado via gRPC. | Go recebe solicitações e simula aprovação, recusa, demora e falha; contratos versionados e stubs gerados; lint e compatibilidade; Java aplica deadline e trata erros; teste de integração cobre os cenários sem confirmar compras em falhas. | 2 |
| P1 | Implementar cache com política documentada. | Dados cacheados e TTL definidos; invalidação, stampede e métricas de hit/miss tratados. Dados elegíveis ainda pendentes. | 3 |
| P1 | Publicar o sistema e configurar banco gerenciado. | URL pública, migrações automatizadas, imagens publicadas pelo CI, logs estruturados e health checks. | 3 |
| P1 | Completar segurança e documentação. | Autenticação e autorização por recurso testadas; registro de mitigação OWASP; pipeline final; documentação e referência de API públicas. | Final |
| P2 | Como usuário, quero me autenticar para acessar minhas funcionalidades protegidas. | Credenciais válidas permitem acesso; inválidas são rejeitadas; rotas protegidas rejeitam acesso sem autenticação. | 2, com evolução na final |
| P2 | Como espectador, quero consultar e publicar avaliações para compartilhar minha opinião. | Publicação exige autenticação e ingresso para o filme; avaliação possui nota e texto; escala da nota e comprovação de sessão assistida pendentes. | 3 |
| P2 | Como administrador, quero atribuir permissões por papel para restringir operações administrativas. | Permissões diferenciadas; usuário comum não acessa operações administrativas; evolução de autorização por recurso na final. | 3, com evolução na final |
| P2 | Como administrador, quero moderar avaliações e gerenciar o catálogo em cartaz para manter informações adequadas. | Permite consultar, editar ou remover avaliações e administrar filmes em cartaz; política de edição pendente. | 3 |

As estimativas iniciais aprovadas são Pequena para consulta de filmes, Pequena para consulta de cinemas e Média para consulta e seleção de assentos. A publicação do quadro e dessas estimativas no GitHub Projects está pendente.
