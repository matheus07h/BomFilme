# BomFilme

[![CI](https://github.com/matheus07h/BomFilme/actions/workflows/ci.yml/badge.svg)](https://github.com/matheus07h/BomFilme/actions/workflows/ci.yml)

Projeto da disciplina DIM0547 — Desenvolvimento de Sistemas Web II, UFRN, período 2026.2.

O BomFilme reúne o planejamento de um sistema de ingressos e avaliações de filmes para espectadores e administradores de redes de cinema. O MVP inclui catálogo de filmes, sessões, assentos, compras com pagamento simulado e avaliações.

## Documentação

- [Proposta do produto](docs/proposta.md): visão, MVP, entidades e decisões de arquitetura.
- [Arquitetura](docs/arquitetura.md): serviços, responsabilidades, fluxo e infraestrutura local.
- [Backlog inicial](docs/backlog.md): histórias e tarefas organizadas por prioridade e sprint.
- [GitHub Project](https://github.com/users/matheus07h/projects/3): quadro com prioridades, estimativas, sprints e responsáveis.

## Arquitetura

Definimos Java/Quarkus para o serviço principal e Go para o pagamento simulado. Java concentra as regras do negócio, a persistência, as compras e a exclusividade dos assentos. A integração entre os serviços via gRPC será implementada na Sprint 2. As decisões e os limites dos componentes estão detalhados em [docs/arquitetura.md](docs/arquitetura.md).

| Diretório | Responsabilidade |
| --- | --- |
| `api/` | Serviço principal Java/Quarkus |
| `services/` | Serviço Go de pagamento simulado |
| `protos/` | Contratos entre serviços, previstos para a Sprint 2 |
| `docs/` | Proposta, backlog e registro de IA |

## Equipe

| Integrante | Matrícula | GitHub | Responsabilidade principal |
| --- | --- | --- | --- |
| Fernando Simonetti Meira Pires de Araújo | 20240017897 | [@Nandosmpa](https://github.com/Nandosmpa) | Compras, usuários e testes; apoio à base Java na Sprint 0 |
| Gabriel Eugênio Vitalino da Silva | 20240061349 | [@geugenio](https://github.com/geugenio) | Microsserviço Go |
| Hugo José de Lima Nunes | 20240062319 | [@Hugojoseof](https://github.com/Hugojoseof) | Infraestrutura e processo |
| Matheus Henrique Ferreira da Silva | 20240009311 | [@matheus07h](https://github.com/matheus07h) | API Java/Quarkus |

Coorte B — apresentações online. Sem integração com outra disciplina.

## Execução local

Pré-requisitos: [mise](https://mise.jdx.dev/) e Docker Desktop.

```bash
mise install
mise run build
mise run test
```

Para construir as imagens e subir PostgreSQL, API e a base do serviço Go:

```bash
mise run up
```

A API fica disponível em `http://localhost:8080`. O serviço Go ainda encerra após validar sua inicialização, pois o servidor gRPC pertence à Sprint 2. Use `mise run down` para encerrar o ambiente. As portas e credenciais locais podem ser personalizadas copiando `.env.example` para `.env`.

A task `mise run ci` executa lint, build e testes, na mesma ordem usada pelo GitHub Actions.

## Sprint 0

O escopo desta etapa é o planejamento e a base dos dois serviços compilando, com CI passando. As funcionalidades do MVP pertencem às próximas sprints.

- Finalizamos a proposta e organizamos o backlog inicial.
- Configuramos e validamos as bases Java e Go, o Docker Compose e as tasks de build, testes, lint, ambiente local e CI.
- Mantemos o pipeline do GitHub Actions passando para os dois serviços.
- Publicamos as histórias no [GitHub Project](https://github.com/users/matheus07h/projects/3), com prioridades, sprints, responsáveis e três estimativas iniciais.

## Referências da disciplina

- [Guia da Sprint 0](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/SPRINT-0.md)
- [Rúbricas](https://github.com/fmarquesfilho/web2-2026-2/blob/main/docs/RUBRICAS.md)
