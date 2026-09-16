# Registro de uso de IA

Usamos o Codex para ler os materiais que criamos no Notion e consultar o repositório do professor, comparando nosso planejamento com os requisitos da Sprint 0. Pedimos uma revisão para identificar possíveis problemas, ambiguidades e requisitos que faltavam.

Quando foram identificados pontos de atenção, a ferramenta nos explicou o problema e sugeriu alternativas. Revisamos as explicações para decidir quais correções eram necessárias. Nesse processo, confirmamos o pagamento simulado e a divisão de responsabilidades entre Java e Go.

Também usamos a ferramenta para ajudar a organizar e redigir os documentos a partir do nosso material: proposta, backlog, checklist, roteiro do vídeo e README. Revisamos e aprovamos os complementos do backlog e as três estimativas iniciais para o quadro. Também usamos a ferramenta para conferir a estrutura exigida pelo professor e ajustar a redação e a coerência da proposta.

| Data | Ferramenta | Atividades |
| --- | --- | --- |
| 2026-09-15 | Codex | Leitura do nosso material do Notion; consulta ao repositório do professor; revisão dos requisitos e explicação dos pontos de atenção; apoio na organização e redação dos documentos. |
| 2026-09-15 | Claude Code | Planejamento e escrita da base do serviço Java/Quarkus em `api/`: `pom.xml`, configuração de Flyway e Testcontainers, primeira migração, fatia vertical de redes e cinemas, testes e README do serviço. Revisamos as decisões de stack e a modelagem antes de aceitar, e verificamos o build e os testes localmente. |
| 2026-09-15 | Claude Code | Revisão do código de redes e cinemas em `api/`; identificação de respostas 500 para `redeId` ausente, campos inválidos e nomes duplicados; sugestão de validação com Hibernate Validator, mapeamento de violação de unicidade para 409 e ampliação dos testes de ponta a ponta. Revisamos as mudanças e verificamos o build e os testes localmente com `./mvnw verify` antes do envio. |

Mantemos neste documento o registro das ferramentas e tarefas em que usamos IA. Cada um de nós deve compreender e conseguir explicar o conteúdo que submeter.
