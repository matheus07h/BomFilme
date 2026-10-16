-- V3__criar_filmes_generos_pessoas_elenco.sql

CREATE TABLE genero (
                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        tmdb_id BIGINT NOT NULL,
                        nome VARCHAR(100) NOT NULL,
                        CONSTRAINT uq_genero_tmdb_id UNIQUE (tmdb_id)
);

CREATE TABLE pessoa (
                        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        tmdb_id BIGINT NOT NULL,
                        nome VARCHAR(255) NOT NULL,
                        CONSTRAINT uq_pessoa_tmdb_id UNIQUE (tmdb_id)
);

CREATE TABLE filme (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       tmdb_id BIGINT NOT NULL,
                       titulo VARCHAR(255) NOT NULL,
                       titulo_original VARCHAR(255) NOT NULL,
                       sinopse TEXT NOT NULL,
                       poster_path VARCHAR(255),
                       duracao_minutos INTEGER,
                       data_lancamento DATE NOT NULL,
                       nota_media_tmdb DOUBLE PRECISION NOT NULL,
                       nota_media_sistema DOUBLE PRECISION,
                       CONSTRAINT uq_filme_tmdb_id UNIQUE (tmdb_id)
);

CREATE TABLE filme_genero (
                              filme_id BIGINT NOT NULL REFERENCES filme(id) ON DELETE CASCADE,
                              genero_id BIGINT NOT NULL REFERENCES genero(id) ON DELETE CASCADE,
                              PRIMARY KEY (filme_id, genero_id)
);

CREATE TABLE filme_diretor (
                               filme_id BIGINT NOT NULL REFERENCES filme(id) ON DELETE CASCADE,
                               pessoa_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
                               PRIMARY KEY (filme_id, pessoa_id)
);

CREATE TABLE elenco_filme (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              filme_id BIGINT NOT NULL REFERENCES filme(id) ON DELETE CASCADE,
                              pessoa_id BIGINT NOT NULL REFERENCES pessoa(id) ON DELETE CASCADE,
                              personagem VARCHAR(255) NOT NULL,
                              ordem_bilheteria INTEGER NOT NULL
);

CREATE INDEX idx_filme_genero_genero_id ON filme_genero(genero_id);
CREATE INDEX idx_filme_diretor_pessoa_id ON filme_diretor(pessoa_id);
CREATE INDEX idx_elenco_filme_filme_id ON elenco_filme(filme_id);
CREATE INDEX idx_elenco_filme_pessoa_id ON elenco_filme(pessoa_id);