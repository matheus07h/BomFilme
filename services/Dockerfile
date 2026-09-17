FROM golang:1.27.1-alpine AS build
WORKDIR /projeto
COPY go.mod ./
COPY . .
RUN CGO_ENABLED=0 go build -o /pagamentos ./cmd/servidor

FROM alpine:3.22
RUN addgroup -S bomfilme && adduser -S -G bomfilme bomfilme
COPY --from=build --chown=bomfilme:bomfilme /pagamentos /usr/local/bin/pagamentos
USER bomfilme
ENTRYPOINT ["pagamentos"]
