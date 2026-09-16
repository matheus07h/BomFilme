# BomFilme

Projeto da disciplina DIM0547 — Desenvolvimento de Sistemas Web II, UFRN, período 2026.2.

O BomFilme reúne o planejamento de um sistema de ingressos e avaliações de filmes para espectadores e administradores de redes de cinema. O MVP inclui catálogo de filmes, sessões, assentos, compras com pagamento simulado e avaliações.

## Documentação

- [Proposta do produto](docs/proposta.md): visão, MVP, entidades e decisões de arquitetura.
- [Backlog inicial](docs/backlog.md): histórias e tarefas organizadas por prioridade e sprint.
- [Registro de uso de IA](docs/uso-de-ia.md).

## Arquitetura

Definimos Java/Quarkus para o serviço principal e Go para o pagamento simulado. Java concentra as regras do negócio, a persistência, as compras e a exclusividade dos assentos. A integração entre os serviços via gRPC será implementada na Sprint 2.

| Diretório | Responsabilidade |
| --- | --- |
| `api/` | Serviço principal Java/Quarkus |
| `services/` | Serviço Go de pagamento simulado |
| `protos/` | Contratos entre serviços, previstos para a Sprint 2 |
| `docs/` | Proposta, backlog e registro de IA |

## Equipe

| Integrante | GitHub | Responsabilidade principal |
| --- | --- | --- |
| Fernando Simonetti Meira Pires de Araújo | [@Nandosmpa](https://github.com/Nandosmpa) | Compras, usuários e testes; apoio à base Java na Sprint 0 |
| Gabriel Eugênio Vitalino da Silva | [@geugenio](https://github.com/geugenio) | Microsserviço Go |
| Hugo José de Lima Nunes | [@hugojoseof2](https://github.com/hugojoseof2) | Infraestrutura e processo |
| Matheus Henrique Ferreira da Silva | [@matheus07h](https://github.com/matheus07h) | API Java/Quarkus |

Coorte B — apresentações online. Sem integração com outra disciplina.

## Execução local

A configuração de execução está pendente das bases Java e Go. Os comandos de build, testes e execução entram neste guia após a configuração e a verificação do ambiente com `mise` e Docker Compose.

## Sprint 0

O escopo desta etapa é o planejamento e a base dos dois serviços compilando, com CI passando. As funcionalidades do MVP pertencem às próximas sprints.

- Proposta e backlog: organizados para revisão final.
- Bases Java e Go, Docker Compose, tasks e CI: pendentes.
- GitHub Projects: criação e registro das estimativas pendentes.
- Vídeo de cinco minutos: gravação e link pendentes.

## Referências da disciplina

- [Guia da Sprint 0](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/SPRINT-0.md)
- [Rúbricas](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/RUBRICAS.md)
