-- Cria as compras e os ingressos emitidos para elas (issue #8).
--
-- O assento fica preso pela reserva da V7 (linha RESERVADO em assento_ocupado), criada
-- na mesma transação da compra pendente. A aprovação confirma essa reserva e a recusa a
-- libera. reserva_id não é chave estrangeira porque a liberação apaga a linha da reserva.

create table if not exists compra (
    id             bigint generated always as identity primary key,
    sessao_id      bigint      not null references sessao (id),
    assento_id     bigint      not null references assento (id),
    reserva_id     uuid        not null,
    valor_centavos bigint      not null,
    status         varchar(12) not null,
    criado_em      timestamptz not null default now(),
    constraint compra_reserva_unica unique (reserva_id),
    constraint compra_valor_positivo check (valor_centavos > 0),
    constraint compra_status_valido check (status in ('PENDENTE', 'CONFIRMADA', 'RECUSADA'))
);

-- No máximo um ingresso por compra, mesmo que a confirmação seja repetida.
create table if not exists ingresso (
    id         bigint generated always as identity primary key,
    compra_id  bigint      not null references compra (id),
    codigo     uuid        not null,
    emitido_em timestamptz not null default now(),
    constraint ingresso_unico_por_compra unique (compra_id),
    constraint ingresso_codigo_unico unique (codigo)
);

create index if not exists compra_sessao_assento_idx on compra (sessao_id, assento_id);
