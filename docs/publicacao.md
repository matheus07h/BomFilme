# Publicação do BomFilme

O arquivo `render.yaml` descreve a API pública, o serviço privado de pagamentos e o
PostgreSQL gerenciado na mesma região. A API conecta ao banco pela rede interna e o
Flyway aplica as migrações na inicialização. O serviço Go só é acessível pela rede
privada do Render.

## Antes de criar os recursos

1. Revise os planos de computação e banco em `render.yaml`: a configuração usa
   recursos pagos. Escolha a região e os planos com a equipe.
2. Conecte o repositório GitHub a um workspace Render e crie um Blueprint a partir do
   `render.yaml` da branch `main`.
3. Informe `TMDB_API_TOKEN` no formulário inicial do Blueprint. Ele não fica salvo
   no repositório.
4. Após a primeira implantação, copie a URL `onrender.com` da API e acesse
   `/q/health/ready` e `/q/openapi`. Registre a URL pública no README.

O Blueprint usa `checksPass` para implantar após a aprovação das verificações do CI.
No `main`, o mesmo CI publica `ghcr.io/matheus07h/bomfilme-api` e
`ghcr.io/matheus07h/bomfilme-pagamentos` com as tags `latest` e o SHA do commit.
Essas imagens permitem reproduzir a implantação fora do Render. O `render.yaml`
constrói as imagens diretamente do Git para sincronizar os três recursos num único
Blueprint.

## Operação

- API: `/q/health/live` verifica o processo; `/q/health/ready` verifica também a
  conexão com o banco. O Render usa o segundo endpoint como health check.
- Pagamentos: `/health/live` e `/health/ready` na porta 8081; o gRPC escuta na 50051.
  Como serviço privado, o Render verifica a porta por TCP. O Compose usa o endpoint HTTP.
- Os dois serviços escrevem logs JSON em stdout, disponíveis nos logs da plataforma.
- O `BOMFILME_DB_URL` do Render é convertido para URL JDBC pelo `entrypoint.sh` da API.
  Usuário e senha vêm de referências separadas ao banco no Blueprint.

Para desenvolvimento local, `docker compose up --build` continua usando o PostgreSQL
do Compose. Não use o banco gerenciado para testes locais.
