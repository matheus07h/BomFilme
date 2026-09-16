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

| Método | Rota            | Descrição                |
| ------ | --------------- | ------------------------ |
| `GET`  | `/redes`        | Lista as redes de cinema |
| `POST` | `/redes`        | Cria uma rede            |
| `GET`  | `/redes/{id}`   | Consulta uma rede        |
| `GET`  | `/cinemas`      | Lista os cinemas         |
| `POST` | `/cinemas`      | Cria um cinema           |
| `GET`  | `/cinemas/{id}` | Consulta um cinema       |

## Respostas de erro

As entradas de `POST /redes` e `POST /cinemas` são validadas com Hibernate Validator antes de chegar ao banco.

| Status | Quando acontece |
| ------ | --------------- |
| `400`  | Corpo ausente, campo obrigatório em branco, texto acima de 120 caracteres, `redeId` ausente ou UF diferente de duas letras |
| `404`  | Rede ou cinema inexistente, inclusive `redeId` inexistente ao criar um cinema; o corpo traz `{"mensagem": "..."}` |
| `409`  | Nome de rede repetido ou nome de cinema repetido dentro da mesma rede; o corpo traz `{"mensagem": "..."}` |

A unicidade é garantida pelas restrições da migração `V1`, e o `MapeadorDeErrosDePersistencia` converte a violação (SQLSTATE `23505`) em `409`. Assim, duas requisições simultâneas com o mesmo nome também resultam em uma criação e um conflito. A UF é gravada em maiúsculas e os espaços nas bordas dos textos são removidos.

A especificação OpenAPI fica em `/q/openapi` e o Swagger UI em `/q/swagger-ui`.
