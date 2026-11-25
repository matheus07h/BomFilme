package main

import (
	"context"
	"log/slog"
	"net"
	"net/http"
	"os"
	"time"

	pagamentosv1 "github.com/matheus07h/BomFilme/services/gen/bomfilme/pagamentos/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type servidorPagamentos struct {
	pagamentosv1.UnimplementedPagamentoServiceServer
	cenario string
}

func (s servidorPagamentos) ProcessarPagamento(
	ctx context.Context,
	req *pagamentosv1.ProcessarPagamentoRequest,
) (*pagamentosv1.ProcessarPagamentoResponse, error) {
	if req.GetCompraId() <= 0 || req.GetValorCentavos() <= 0 {
		return nil, status.Error(
			codes.InvalidArgument,
			"compra_id e valor_centavos devem ser positivos",
		)
	}

	switch s.cenario {
	case "recusa":
		return &pagamentosv1.ProcessarPagamentoResponse{
			Resultado: pagamentosv1.ResultadoPagamento_RESULTADO_PAGAMENTO_RECUSADO,
		}, nil
	case "falha":
		return nil, status.Error(codes.Unavailable, "indisponibilidade simulada")
	case "demora":
		timer := time.NewTimer(3 * time.Second)
		defer timer.Stop()

		select {
		case <-ctx.Done():
			return nil, status.FromContextError(ctx.Err()).Err()
		case <-timer.C:
		}
	}

	return &pagamentosv1.ProcessarPagamentoResponse{
		Resultado: pagamentosv1.ResultadoPagamento_RESULTADO_PAGAMENTO_APROVADO,
	}, nil
}

func main() {
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, nil)))
	cenario := os.Getenv("BOMFILME_PAGAMENTOS_CENARIO")
	if cenario == "" {
		cenario = "aprovacao"
	}

	switch cenario {
	case "aprovacao", "recusa", "demora", "falha":
	default:
		slog.Error("cenário de pagamento inválido", "cenario", cenario)
		os.Exit(1)
	}

	listener, err := net.Listen("tcp", ":50051")
	if err != nil {
		slog.Error("não foi possível abrir a porta gRPC", "erro", err)
		os.Exit(1)
	}
	defer listener.Close()

	saude := http.NewServeMux()
	saude.HandleFunc("GET /health/live", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
	saude.HandleFunc("GET /health/ready", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
	servidorSaude := &http.Server{
		Addr:              ":8081",
		Handler:           saude,
		ReadHeaderTimeout: 5 * time.Second,
	}
	go func() {
		if err := servidorSaude.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("falha no servidor de saúde", "erro", err)
			os.Exit(1)
		}
	}()

	servidor := grpc.NewServer()
	pagamentosv1.RegisterPagamentoServiceServer(servidor, servidorPagamentos{cenario: cenario})
	slog.Info("servidor gRPC de pagamentos iniciado", "porta", 50051, "cenario", cenario)
	if err := servidor.Serve(listener); err != nil {
		slog.Error("falha no servidor gRPC", "erro", err)
		os.Exit(1)
	}
}
