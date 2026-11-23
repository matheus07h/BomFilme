package main

import (
	"context"
	"log"
	"net"

	pagamentosv1 "github.com/matheus07h/BomFilme/services/gen/bomfilme/pagamentos/v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type servidorPagamentos struct {
	pagamentosv1.UnimplementedPagamentoServiceServer
}

func (servidorPagamentos) ProcessarPagamento(
	_ context.Context,
	req *pagamentosv1.ProcessarPagamentoRequest,
) (*pagamentosv1.ProcessarPagamentoResponse, error) {
	if req.GetCompraId() <= 0 || req.GetValorCentavos() <= 0 {
		return nil, status.Error(
			codes.InvalidArgument,
			"compra_id e valor_centavos devem ser positivos",
		)
	}

	return &pagamentosv1.ProcessarPagamentoResponse{
		Resultado: pagamentosv1.ResultadoPagamento_RESULTADO_PAGAMENTO_APROVADO,
	}, nil
}

func main() {
	listener, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("não foi possível abrir a porta gRPC: %v", err)
	}

	servidor := grpc.NewServer()
	pagamentosv1.RegisterPagamentoServiceServer(servidor, servidorPagamentos{})

	log.Print("Servidor gRPC de pagamentos escutando na porta 50051")
	if err := servidor.Serve(listener); err != nil {
		log.Fatalf("falha no servidor gRPC: %v", err)
	}
}
