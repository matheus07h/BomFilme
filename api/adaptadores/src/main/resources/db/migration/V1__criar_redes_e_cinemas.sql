-- Cria as tabelas das redes de cinema e dos cinemas que pertencem a elas.

create table if not exists rede_cinema (
    id        bigint generated always as identity primary key,
    nome      varchar(120) not null unique,
    criado_em timestamptz  not null default now()
);

create table if not exists cinema (
    id             bigint generated always as identity primary key,
    rede_cinema_id bigint       not null references rede_cinema (id),
    nome           varchar(120) not null,
    cidade         varchar(120) not null,
    uf             char(2)      not null,
    criado_em      timestamptz  not null default now(),
    constraint cinema_nome_unico_por_rede unique (rede_cinema_id, nome)
);

create index if not exists cinema_rede_cinema_id_idx on cinema (rede_cinema_id);
