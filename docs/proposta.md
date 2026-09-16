# BomFilme — Proposta do produto

Neste documento apresentamos a visão, o escopo e as decisões de arquitetura do BomFilme. O escopo da Sprint 0 é o planejamento e a base dos serviços compilando; as funcionalidades serão desenvolvidas nas próximas sprints.

## 1. Visão do produto

Para entusiastas de cinema e administradores de redes de cinema,
que buscam centralizar a compra de ingressos e consultar a opinião do público,
o BomFilme é um ecossistema integrado de ingressos e avaliações de filmes
que unifica o catálogo de múltiplas redes, simplifica a reserva de assentos e reúne avaliações.
Diferente das plataformas exclusivas de cada rede de cinema,
nosso produto consolida as redes em uma única interface e reúne uma comunidade de espectadores.

Acreditamos que espectadores vão consultar sessões e avaliações e adquirir ingressos no mesmo sistema porque isso reduz a necessidade de alternar entre plataformas.

## 2. MVP

| Dentro do escopo | Fora do escopo |
| --- | --- |
| Gerenciamento de ingressos, cinemas, salas, horários e assentos | Criação de filmes |
| Gerenciamento de redes de cinema | Venda de combos |
| Catálogo de filmes obtido de API externa | Outros tipos promocionais de ingressos |
| Avaliações com nota e texto | Nota fiscal e QR Code |
| Autenticação de usuários | Reserva de sala |
| Administração de avaliações | Integração com provedores de pagamento e cobranças reais |
| Pagamento simulado | |

Definimos que o pagamento será simulado, sem cobrança real. Planejamos permitir cenários reproduzíveis de aprovação e recusa e confirmar a compra e gerar o ingresso somente após aprovação simulada.

## 3. Backlog inicial

Organizamos nosso backlog com histórias de usuário e tarefas técnicas, prioridades e critérios de aceitação em [backlog.md](backlog.md). O endereço do repositório público está pendente da criação na conta de um integrante do grupo, devido à suspensão da conta que hospedava o projeto. As estimativas iniciais aprovadas são Pequena para consulta de filmes, Pequena para consulta de cinemas e Média para consulta e seleção de assentos. A publicação dos itens e dessas estimativas no GitHub Projects e a inclusão do link do quadro estão pendentes.

## 4. Entidades principais

Usuario, Cinema, RedeCinema, Filme, Sessao, Sala, Assento, Ingresso, Compra, Avaliacao e Papel.

## 5. Escolha do serviço principal

Escolhemos Java com Quarkus pela nossa familiaridade com Java e orientação a objetos e pela adequação ao desenvolvimento de APIs REST e integração com banco de dados. Consideramos Kotlin/Ktor como alternativa, mas preferimos aproveitar o conhecimento que já temos em Java para concentrar o aprendizado na arquitetura, na integração entre serviços e na infraestrutura do projeto.

## 6. Divisão de responsabilidades

| Serviço principal — Java/Quarkus | Microsserviço Go — pagamento simulado |
| --- | --- |
| Redes, cinemas, salas, assentos e sessões | Receber solicitações de pagamento via gRPC |
| Catálogo de filmes e avaliações | Processar pagamento simulado, sem cobrança real |
| Usuários, autenticação e autorização | Retornar aprovação ou recusa simulada |
| Compras, reservas de assentos e geração de ingressos | Simular demora e falhas para testar a integração |
| Persistência, migrações e exclusividade de assentos por sessão | |

Definimos que Java concentra as entidades e regras do negócio. A exclusividade de assentos fica no Java e no banco, junto das compras, com garantia entre instâncias e teste de concorrência. Escolhemos Go para o processador de pagamento simulado porque pretendemos tratar várias solicitações independentes enquanto elas aguardam uma resposta com demora simulada. Esse trabalho permite explorar concorrência, cancelamento e limites de tempo em uma responsabilidade isolada. A separação permite exercitar chamadas gRPC e tratamento de erros sem integração financeira real; a confirmação da compra continua sendo uma regra do Java.

Fluxo: Java cria uma compra pendente e reserva o assento com segurança; solicita pagamento ao Go via gRPC; após aprovação, confirma a compra e gera o ingresso; após recusa, libera a reserva. Timeout ou falha de comunicação não confirma a compra automaticamente. Detalharemos recuperação e expiração durante a implementação desse fluxo na Sprint 2.

Distribuímos a responsabilidade principal pela infraestrutura a Hugo: Docker Compose, tasks, CI e documentação. Também dividimos as contribuições e revisões entre os integrantes.

## 7. Equipe

| Nome | Matrícula | Papel | GitHub | E-mail dos commits |
| --- | --- | --- | --- | --- |
| Fernando Simonetti Meira Pires de Araújo | 20240017897 | Compras, usuários e testes | @Nandosmpa | Simonettifernando2@gmail.com |
| Gabriel Eugênio Vitalino da Silva | 20240061349 | Microsserviço em Go | @geugenio | geugnio12@gmail.com |
| Hugo José de Lima Nunes | 20240062319 | Infraestrutura e processo | @Hugojoseof | hugoliman4@gmail.com |
| Matheus Henrique Ferreira da Silva | 20240009311 | API Java/Quarkus | @matheus07h | matheushenriquefs07@gmail.com |

Na Sprint 0, Matheus e Fernando são responsáveis pela base Java/Quarkus; Gabriel, pela base Go; Hugo, pela infraestrutura e pelo processo. Todos participamos das contribuições, revisões e preparação do vídeo.

## 8. Coorte e integração

- Coorte B: apresentações online.
- Integração com outra disciplina: nenhuma.

## Referências

- [Guia da Sprint 0](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/SPRINT-0.md)
- [Rúbricas](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/RUBRICAS.md)
