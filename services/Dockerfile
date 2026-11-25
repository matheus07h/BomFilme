FROM bufbuild/buf:1.75.0 AS contratos
WORKDIR /projeto
COPY protos/ protos/
RUN buf generate protos --template protos/buf.gen.yaml

FROM golang:1.27.1-alpine AS build
WORKDIR /projeto
COPY services/go.mod services/go.sum ./
RUN go mod download
COPY services/ ./
COPY --from=contratos /projeto/services/gen/ gen/
RUN CGO_ENABLED=0 go build -o /pagamentos ./cmd/servidor

FROM alpine:3.22
RUN addgroup -S bomfilme && adduser -S -G bomfilme bomfilme
COPY --from=build --chown=bomfilme:bomfilme /pagamentos /usr/local/bin/pagamentos
USER bomfilme
ENTRYPOINT ["pagamentos"]