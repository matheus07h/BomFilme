# BomFilme - serviço principal (Java/Quarkus)

| Item      | Valor         |
| --------- | ------------- |
| Java      | 25            |
| Quarkus   | 3.28.2        |
| Banco     | PostgreSQL 17 |
| Migrações | Flyway        |
| Testes    | JUnit 5       |

## Pré-requisitos

- JDK 25
- Docker Desktop **em execução** - os testes e o modo `dev` sobem um PostgreSQL em container

## Comandos

```bash
cd BomFilme/api

./mvnw package -DskipTests   # compila
./mvnw verify                # sobe o PostgreSQL, aplica as migrações e roda os testes
./mvnw quarkus:dev           # modo de desenvolvimento em http://localhost:8080
```

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

`POST` responde com o cabeçalho `Location` apontando para o recurso criado. `PUT` recebe o mesmo corpo do `POST` e é idempotente.

## Respostas de erro

Os corpos de `POST` e `PUT` e os parâmetros de consulta são validados com Hibernate Validator antes de chegar ao banco.

| Status | Quando acontece |
| ------ | --------------- |
| `400`  | Corpo ausente, campo obrigatório em branco, texto acima de 120 caracteres, `redeId` ausente ou não positivo, UF diferente de duas letras ou parâmetro de paginação fora dos limites |
| `404`  | Rede ou cinema inexistente, inclusive `redeId` inexistente ao criar ou atualizar um cinema; o corpo traz `{"mensagem": "..."}` |
| `409`  | Nome de rede repetido ou nome de cinema repetido dentro da mesma rede; remoção de rede que ainda tem cinemas; remoção de cinema que ainda tem salas; o corpo traz `{"mensagem": "..."}` |

A unicidade é garantida pelas restrições da migração `V1`, e o `MapeadorDeErrosDePersistencia` converte a violação (SQLSTATE `23505`) em `409`. Assim, duas requisições simultâneas com o mesmo nome também resultam em uma criação e um conflito. A UF é gravada em maiúsculas e os espaços nas bordas dos textos são removidos.

A especificação OpenAPI fica em `/q/openapi` e o Swagger UI em `/q/swagger-ui`.

### Remoção e vínculos

Uma rede só é removida quando não tem cinemas, e um cinema só é removido quando não tem salas. A API verifica o vínculo antes de remover e responde `409` com uma mensagem específica. As chaves estrangeiras do banco continuam como garantia final: se um vínculo surgir entre a verificação e a remoção, a violação (SQLSTATE `23503`) também vira `409`. Para remover uma rede, remova antes os cinemas dela.

### Listagens

`GET /redes` e `GET /cinemas` paginam no banco (`LIMIT`/`OFFSET`) e ordenam por `id`, para que a navegação entre páginas seja estável. Uma página sem resultados retorna `[]`.

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

Parâmetros fora dessas regras retornam `400`.
