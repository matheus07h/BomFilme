package main

import (
	"context"
	"log"
	"net"
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
	cenario := os.Getenv("BOMFILME_PAGAMENTOS_CENARIO")
	if cenario == "" {
		cenario = "aprovacao"
	}

	switch cenario {
	case "aprovacao", "recusa", "demora", "falha":
	default:
		log.Fatalf("cenário de pagamento inválido: %q", cenario)
	}

	listener, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("não foi possível abrir a porta gRPC: %v", err)
	}

	servidor := grpc.NewServer()
	pagamentosv1.RegisterPagamentoServiceServer(servidor, servidorPagamentos{cenario: cenario})
	log.Printf("Servidor gRPC de pagamentos escutando na porta 50051 (cenário: %s)", cenario)
	if err := servidor.Serve(listener); err != nil {
		log.Fatalf("falha no servidor gRPC: %v", err)
	}
}
