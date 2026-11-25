-- A mesma chave (sessao_id, assento_id) protege reservas e vendas entre instâncias.
-- As ocupações existentes representam assentos já confirmados.
alter table assento_ocupado
    add column reserva_id uuid,
    add column estado varchar(12) not null default 'CONFIRMADO',
    add constraint assento_ocupado_estado_valido
        check (estado in ('RESERVADO', 'CONFIRMADO')),
    add constraint assento_ocupado_reserva_obrigatoria
        check (estado <> 'RESERVADO' or reserva_id is not null),
    add constraint assento_ocupado_reserva_unica unique (reserva_id);
