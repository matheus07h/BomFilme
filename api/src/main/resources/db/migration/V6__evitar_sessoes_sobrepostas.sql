-- Guarda o fim de cada sessão e impede que duas sessões da mesma sala se sobreponham.
-- A restrição de exclusão vale também para requisições simultâneas, o que uma
-- verificação feita só na aplicação não garante.

create extension if not exists btree_gist;

alter table sessao add column fim timestamptz;

-- Sessões antigas recebem a duração do filme ou, sem ela, duas horas.
update sessao s
   set fim = s.inicio + make_interval(mins => coalesce(f.duracao_minutos, 120))
  from filme f
 where f.id = s.filme_id;

alter table sessao alter column fim set not null;

alter table sessao
    add constraint sessao_fim_apos_inicio check (fim > inicio);

alter table sessao
    add constraint sessao_sem_sobreposicao_na_sala
        exclude using gist (sala_id with =, tstzrange(inicio, fim, '[)') with &&);

create index if not exists sessao_filme_id_idx on sessao (filme_id);
