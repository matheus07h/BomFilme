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

A especificação OpenAPI fica em `/q/openapi` e o Swagger UI em `/q/swagger-ui`.
