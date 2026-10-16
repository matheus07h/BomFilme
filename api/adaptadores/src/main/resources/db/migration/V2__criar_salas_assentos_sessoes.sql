-- Cria as tabelas de salas, assentos, sessões e ocupação de assentos por sessão.

create table if not exists sala (
    id        bigint generated always as identity primary key,
    cinema_id bigint      not null references cinema (id),
    nome      varchar(60) not null,
    criado_em timestamptz not null default now(),
    constraint sala_nome_unico_por_cinema unique (cinema_id, nome)
);

create table if not exists assento (
    id        bigint generated always as identity primary key,
    sala_id   bigint      not null references sala (id),
    fileira   varchar(2)  not null,
    numero    int         not null,
    criado_em timestamptz not null default now(),
    constraint assento_unico_por_sala unique (sala_id, fileira, numero)
);

create table if not exists sessao (
    id        bigint generated always as identity primary key,
    sala_id   bigint      not null references sala (id),
    filme_id  bigint      not null,
    inicio    timestamptz not null,
    criado_em timestamptz not null default now()
);

-- Presença de uma linha aqui = assento ocupado (reservado ou vendido) naquela sessão.
create table if not exists assento_ocupado (
    id         bigint generated always as identity primary key,
    sessao_id  bigint      not null references sessao (id),
    assento_id bigint      not null references assento (id),
    criado_em  timestamptz not null default now(),
    constraint assento_ocupado_unico unique (sessao_id, assento_id)
);

create index if not exists sala_cinema_id_idx on sala (cinema_id);
create index if not exists assento_sala_id_idx on assento (sala_id);
create index if not exists sessao_sala_id_idx on sessao (sala_id);
create index if not exists assento_ocupado_sessao_id_idx on assento_ocupado (sessao_id);
