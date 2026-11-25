# Publicação gratuita do BomFilme

O `render.yaml` cria somente a API como Web Service Free no Render. O banco é um
PostgreSQL gerenciado no plano Free do Neon, criado separadamente. O serviço Go
de pagamentos continua no Docker Compose: ele não precisa estar publicado para
a avaliação da Sprint 2, e a API deve responder mesmo quando ele está ausente.
O Flyway aplica as migrações na inicialização da API.

## Criar o banco e publicar a API

1. Crie um projeto PostgreSQL no plano Free do Neon e anote o host, o nome do
   banco, o usuário e a senha. Mantenha esses dados fora do repositório.
2. No terminal, forneça `BOMFILME_DB_URL`, `BOMFILME_DB_USER` e
   `BOMFILME_DB_PASSWORD` como variáveis de ambiente. A URL tem o formato
   `jdbc:postgresql://HOST/BANCO?sslmode=require`. Execute `mise run migrar`
   para aplicar as migrações no Neon por uma task. O Flyway do serviço também
   confere as migrações na inicialização.
3. Conecte o repositório GitHub ao Render e crie um Blueprint com o `render.yaml`
   da branch `main`, após a integração da PR. Confirme que o único recurso a
   criar é `bomfilme-api` com plano `free`; não aprove recursos pagos.
4. Preencha os segredos solicitados pelo Blueprint:
   - `BOMFILME_DB_URL`: `jdbc:postgresql://HOST/BANCO?sslmode=require`, com o
     host e o banco copiados do Neon, sem usuário ou senha na URL;
   - `BOMFILME_DB_USER` e `BOMFILME_DB_PASSWORD`: credenciais do Neon;
   - `TMDB_API_TOKEN`: token da API TMDB.
5. Aguarde a implantação e confira os logs do Flyway e o health check
   `/q/health/ready`. Faça uma requisição a `/q/openapi` sem o serviço Go no ar.
   Se a API ficar disponível, registre sua URL `onrender.com` no README.
6. Guarde evidências do plano e das variáveis configuradas (sem mostrar valores
   secretos), da saída da task de migração, do health check e da resposta da API. A
   rúbrica da Sprint 2 aceita essas evidências mesmo sem URL pública funcional.

O Blueprint usa `checksPass` para implantar após a aprovação das verificações
do CI. No `main`, o CI publica `ghcr.io/matheus07h/bomfilme-api` e
`ghcr.io/matheus07h/bomfilme-pagamentos` com as tags `latest` e o SHA do commit.
Confirme essa publicação após integrar a PR. As imagens publicadas também
servem como evidência da implantação.

## Limites do plano gratuito

O Render suspende o Web Service Free após 15 minutos sem tráfego. A primeira
requisição após a suspensão pode demorar cerca de um minuto. O PostgreSQL Free
do próprio Render expira após 30 dias, por isso o Blueprint usa Neon. Não há
serviço privado Go no plano gratuito. Se a API ultrapassar os recursos do
Render Free, reduza o consumo de memória ou apresente as evidências de
implantação aceitas pela rúbrica; não mude o plano para um pago sem decisão
da equipe.

## Operação local

- API: `/q/health/live` verifica o processo; `/q/health/ready` verifica também
  a conexão com o banco. O Render usa o segundo endpoint como health check.
- Pagamentos: `/health/live` e `/health/ready` na porta 8081; o gRPC escuta na
  50051 dentro do Compose.
- Os dois serviços escrevem logs JSON em stdout.
- `docker compose up --build` continua usando o PostgreSQL local. Não use o
  banco gerenciado para testes locais.
