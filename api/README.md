# BomFilme - serviço principal (Java/Quarkus)

| Item      | Valor         |
| --------- | ------------- |
| Java      | 25            |
| Quarkus   | 3.28.2        |
| Banco     | PostgreSQL 17 |
| Migrações | Flyway        |
| Testes    | JUnit 5       |

Um módulo Maven por camada: `dominio`, `aplicacao` e `adaptadores`. As convenções de
cada uma e o roteiro para adicionar um caso de uso estão em
[ARCHITECTURE.md](ARCHITECTURE.md).

## Pré-requisitos

- JDK 25
- Docker Desktop **em execução** - os testes e o modo `dev` sobem um PostgreSQL em container

## Comandos

Da raiz do monorepo:

```bash
mise run build   # compila os três módulos
mise run test    # sobe o PostgreSQL, aplica as migrações e roda os testes
mise run dev     # modo de desenvolvimento em http://localhost:8080, com Live Coding
```

Direto pelo Maven, de `BomFilme/api`:

```bash
./mvnw package -DskipTests   # compila os três módulos
./mvnw verify                # sobe o PostgreSQL, aplica as migrações e roda os testes
```

O modo de desenvolvimento precisa de dois passos. `api/` é um agregador e o plugin do
Quarkus vive no módulo `adaptadores`, então `./mvnw quarkus:dev` aqui falha com
"No plugin found for prefix 'quarkus'":

```bash
# publica dominio e aplicacao no repositório local
./mvnw install -DskipTests -Dquarkus.build.skip=true
cd adaptadores && ../mvnw quarkus:dev
```

`mise run dev` encapsula esses dois passos.

## Endpoints

| Método   | Rota            | Descrição                                                   | Sucesso |
| -------- | --------------- | ----------------------------------------------------------- | ------- |
| `GET`    | `/redes`        | Lista as redes com paginação e filtro opcional por nome     | `200`   |
| `POST`   | `/redes`        | Cria uma rede                                               | `201`   |
| `GET`    | `/redes/{id}`   | Consulta uma rede                                           | `200`   |
| `PUT`    | `/redes/{id}`   | Substitui os dados de uma rede                              | `200`   |
| `DELETE` | `/redes/{id}`   | Remove uma rede sem cinemas                                 | `204`   |
| `GET`    | `/cinemas`      | Lista os cinemas com paginação e filtros opcionais          | `200`   |
| `POST`   | `/cinemas`      | Cria um cinema                                              | `201`   |
| `GET`    | `/cinemas/{id}` | Consulta um cinema                                          | `200`   |
| `PUT`    | `/cinemas/{id}` | Substitui os dados de um cinema, inclusive a rede           | `200`   |
| `DELETE` | `/cinemas/{id}` | Remove um cinema sem salas                                  | `204`   |
| `GET`    | `/salas`        | Lista as salas com paginação e filtros opcionais            | `200`   |
| `POST`   | `/salas`        | Cria uma sala num cinema                                    | `201`   |
| `GET`    | `/salas/{id}`   | Consulta uma sala                                           | `200`   |
| `PUT`    | `/salas/{id}`   | Substitui os dados de uma sala, inclusive o cinema          | `200`   |
| `DELETE` | `/salas/{id}`   | Remove uma sala sem assentos nem sessões                    | `204`   |
| `GET`    | `/salas/{salaId}/assentos`      | Lista os assentos da sala, com paginação e filtro por fileira | `200` |
| `POST`   | `/salas/{salaId}/assentos`      | Cria um assento na sala                     | `201`   |
| `GET`    | `/salas/{salaId}/assentos/{id}` | Consulta um assento da sala                 | `200`   |
| `PUT`    | `/salas/{salaId}/assentos/{id}` | Substitui a fileira e o número do assento   | `200`   |
| `DELETE` | `/salas/{salaId}/assentos/{id}` | Remove um assento nunca ocupado em sessão   | `204`   |
| `GET`    | `/sessoes`      | Lista as sessões com paginação e filtros opcionais          | `200`   |
| `POST`   | `/sessoes`      | Programa uma sessão de um filme numa sala                   | `201`   |
| `GET`    | `/sessoes/{id}` | Consulta uma sessão                                         | `200`   |
| `PUT`    | `/sessoes/{id}` | Substitui a sala, o filme ou o início de uma sessão         | `200`   |
| `DELETE` | `/sessoes/{id}` | Remove uma sessão sem assentos ocupados                     | `204`   |
| `GET`    | `/sessoes/{id}/assentos` | Lista os assentos da sala com a disponibilidade na sessão | `200` |
| `POST`   | `/sessoes/{id}/assentos/{assentoId}` | Ocupa um assento na sessão             | `201`   |

`POST` responde com o cabeçalho `Location` apontando para o recurso criado. `PUT` recebe o mesmo corpo do `POST` e é idempotente.

## Respostas de erro

Os corpos de `POST` e `PUT` e os parâmetros de consulta são validados com Hibernate Validator antes de chegar ao banco.

| Status | Quando acontece |
| ------ | --------------- |
| `400`  | Corpo ausente, campo obrigatório em branco, texto acima de 120 caracteres, `redeId` ausente ou não positivo, UF diferente de duas letras ou parâmetro de paginação fora dos limites |
| `404`  | Rede ou cinema inexistente, inclusive `redeId` inexistente ao criar ou atualizar um cinema; o corpo traz `{"mensagem": "..."}` |
| `409`  | Nome de rede repetido ou nome de cinema repetido dentro da mesma rede; remoção de rede que ainda tem cinemas; remoção de cinema que ainda tem salas; o corpo traz `{"mensagem": "..."}` |

Salas, assentos e sessões seguem as mesmas regras, com estes casos próprios:

| Status | Quando acontece |
| ------ | --------------- |
| `400`  | Nome de sala em branco ou acima de 60 caracteres; fileira diferente de uma ou duas letras; número de assento fora de `1` a `999`; sessão sem `salaId`, `filmeId` ou `inicio`, ou com `inicio` no passado |
| `404`  | Sala, assento, sessão, cinema ou filme inexistente; assento consultado por uma sala que não é a dele |
| `409`  | Nome de sala repetido no cinema; fileira e número repetidos na sala; sessão que se sobrepõe a outra da mesma sala; remoção de sala com assentos ou sessões, de assento já ocupado e de sessão com assentos ocupados |
| `422`  | Sessão de um filme sem duração cadastrada, pois o fim da sessão não pode ser calculado |

A unicidade é garantida pelas restrições da migração `V1`, e `ViolacoesDoBanco`, no adaptador de banco, converte a violação (SQLSTATE `23505`) em conflito de domínio, que o adaptador HTTP responde como `409`. Assim, duas requisições simultâneas com o mesmo nome também resultam em uma criação e um conflito. A UF é gravada em maiúsculas e os espaços nas bordas dos textos são removidos.

A especificação OpenAPI fica em `/q/openapi` e o Swagger UI em `/q/swagger-ui`.

### Remoção e vínculos

Uma rede só é removida quando não tem cinemas, e um cinema só é removido quando não tem salas. A API verifica o vínculo antes de remover e responde `409` com uma mensagem específica. As chaves estrangeiras do banco continuam como garantia final: se um vínculo surgir entre a verificação e a remoção, a violação (SQLSTATE `23503`) também vira `409`. Para remover uma rede, remova antes os cinemas dela.

O mesmo vale para a programação: uma sala só é removida sem assentos e sem sessões, um assento só é removido se nunca foi ocupado, e uma sessão só é removida sem assentos ocupados.

### Conflito de horários

A sessão recebe `salaId`, `filmeId` e `inicio` (ISO 8601 com fuso, por exemplo `2026-11-20T19:00:00-03:00`). O `fim` é calculado pela duração do filme e devolvido na resposta. Duas sessões da mesma sala não podem ocupar o mesmo intervalo `[inicio, fim)`: uma sessão pode começar exatamente quando a anterior termina.

A API confere o conflito antes de gravar e responde `409` com uma mensagem específica. A migração `V6` cria a restrição de exclusão `sessao_sem_sobreposicao_na_sala` (extensão `btree_gist`), que garante a regra também para requisições simultâneas; a violação (SQLSTATE `23P01`) também vira `409`. Ao editar uma sessão, ela não conflita consigo mesma.

### Listagens

Todas as listagens paginam no banco (`LIMIT`/`OFFSET`) com ordenação estável: redes, cinemas e salas por `id`; assentos por fileira e número; sessões por início e `id`.

Toda listagem devolve o mesmo envelope, com o total para o cliente saber quantas páginas existem:

```json
{
  "itens": [],
  "pagina": 0,
  "tamanho": 20,
  "total": 0,
  "totalPaginas": 0
}
```

Uma página além do fim devolve `itens` vazio sem perder o `total`. O `total` vem de um `count` separado, não do tamanho da lista carregada.

Parâmetros comuns:

| Parâmetro | Padrão | Regra |
| --- | --- | --- |
| `pagina` | `0` | Índice da página, começando em zero |
| `tamanho` | `20` | Quantidade de itens por página, entre `1` e `100` |

Filtros de `GET /redes`:

| Parâmetro | Regra |
| --- | --- |
| `nome` | Busca parcial, sem diferenciar maiúsculas de minúsculas; até 120 caracteres |

Filtros de `GET /cinemas`, combináveis entre si:

| Parâmetro | Regra |
| --- | --- |
| `redeId` | Retorna apenas os cinemas da rede informada; deve ser positivo |
| `nome` | Busca parcial, sem diferenciar maiúsculas de minúsculas; até 120 caracteres |
| `cidade` | Busca parcial, sem diferenciar maiúsculas de minúsculas; até 120 caracteres |
| `uf` | Igualdade exata, sem diferenciar maiúsculas de minúsculas; duas letras |

Filtros de `GET /salas`, combináveis entre si:

| Parâmetro | Regra |
| --- | --- |
| `cinemaId` | Retorna apenas as salas do cinema informado; deve ser positivo |
| `nome` | Busca parcial, sem diferenciar maiúsculas de minúsculas; até 60 caracteres |

Filtro de `GET /salas/{salaId}/assentos`:

| Parâmetro | Regra |
| --- | --- |
| `fileira` | Igualdade exata, sem diferenciar maiúsculas de minúsculas; uma ou duas letras |

Filtros de `GET /sessoes`, combináveis entre si e todos positivos:

| Parâmetro | Regra |
| --- | --- |
| `cinemaId` | Retorna apenas as sessões das salas do cinema informado |
| `salaId` | Retorna apenas as sessões da sala informada |
| `filmeId` | Retorna apenas as sessões do filme informado |

Parâmetros fora dessas regras retornam `400`.
