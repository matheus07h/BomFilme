-- Impede sessões sem filme e a exclusão de filmes com sessões vinculadas.
alter table sessao
    add constraint sessao_filme_fk foreign key (filme_id) references filme (id);
