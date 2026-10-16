# Arquitetura da API

Leia antes de adicionar uma entidade, um caso de uso ou um endpoint. Este documento
descreve as convenções que todo recurso novo segue, e marca as decisões que foram
deliberadas — para ninguém "consertar" depois o que foi escolhido de propósito.

A visão geral do sistema, com o serviço Go e o fluxo de compra, está em
[`../docs/arquitetura.md`](../docs/arquitetura.md).

## Módulos e o que mora em cada um

| Módulo | Pacote | Contém | Alcança |
| --- | --- | --- | --- |
| `dominio` | `br.ufrn.bomfilme.dominio` | Registros do negócio (`Cinema`, `Sala`, `Sessao`, `Assento`, `Filme`, `Genero`, `Pessoa`, `Participacao`), as portas de repositório, a porta `CatalogoExternoDeFilmes`, os filtros de consulta, `Pagina`, `Busca` e `ErroDeDominio` | **Nada.** O `<dependencies>` do pom está vazio |
| `aplicacao` | `br.ufrn.bomfilme.aplicacao` | Um caso de uso por agregado: `ServicoDeRedes`, `ServicoDeCinemas`, `ServicoDeSalas`, `ServicoDeSessoes`, `ServicoDeFilmes`, `ServicoDeGeneros` | `dominio` + as anotações de injeção |
| `adaptadores` | `br.ufrn.bomfilme.adaptadores.http` | Recursos REST, `Problema`/`Problemas`, os mapeadores de exceção, DTOs de entrada (`Nova*`/`Novo*`) e de saída (`FilmeResponse`, ...), `ConfiguracaoOpenApi` | `aplicacao`, `dominio` |
| | `…adaptadores.banco` | Entidades JPA (`*Entidade`), repositórios Panache, `ViolacoesDoBanco`, `Paginas` | `dominio` |
| | `…adaptadores.tmdb` | `ClienteTMDB`, os DTOs do TMDB e `AdaptadorDoTMDB` | `dominio` |

Direção da dependência: `adaptadores → aplicacao → dominio`. A seta só aponta para
dentro, e **o build garante isso** — não é convenção. Um `import jakarta.persistence`
em `dominio` não compila, porque o módulo não tem essa dependência no classpath.

O que o Maven não cobre é a regra *entre pacotes* dentro de `adaptadores`: que `http`
não fale com `banco`, por exemplo. Isso fica em `ArquiteturaTest`, que lê o bytecode
com ArchUnit.

## Registros de domínio

São `record`s. Não há entidade JPA no domínio: `dominio.Cinema` e
`adaptadores.banco.CinemaEntidade` são classes diferentes, e a entidade é
package-private — não é visível nem do adaptador HTTP nem dos testes.

Relacionamentos aparecem como **id da chave estrangeira**, não como referência de
objeto: `Sala` tem `cinemaId`, não um `Cinema`. Isso é deliberado. Evita carregar meio
grafo para responder uma listagem, e é também o formato que a API já expõe.

Os `record`s guardam `criadoEm`. Quem gera é o banco (`default now()`), e o mapeamento
é `insertable = false, updatable = false` na entidade.

Comportamento que é do negócio mora no registro, não no serviço: `Filme.temDuracaoConhecida()`,
`Filme.elencoPorOrdem()`, `Sessao.sobrepoe(...)`.

## Portas de repositório

Declaradas no domínio, implementadas no adaptador de banco. Falam em registros de
domínio e em `long`, nunca em entidade JPA:

```java
Cinema criar(long redeId, String nome, String cidade, String uf);
```

O `redeId` entra como id porque o caso de uso já confirmou que a rede existe; o
adaptador usa `getReference` e não gasta uma consulta para carregá-la.

Não declare método de busca que ninguém chama ainda — adicione quando o caso de uso
que precisa dele for escrito.

## Casos de uso

Um por agregado, `@ApplicationScoped`. Recebem as portas por `@Inject`, aplicam a
regra e devolvem registro de domínio.

**Não devolvem `Response` e não conhecem status HTTP.** Para recusar uma operação,
lançam `ErroDeDominio`:

| Exceção | Vira | Quando |
| --- | --- | --- |
| `NaoEncontrado` | `404` | o registro pedido não existe |
| `Conflito` | `409` | vínculo existente, duplicidade, horário ocupado |
| `RegraViolada` | `422` | entrada bem formada que uma regra de negócio recusa |
| `ServicoExternoIndisponivel` | `502` | o TMDB falhou ou está fora |

`ErroDeDominio` é `sealed`, então `MapeadorDeErroDeDominio` cobre os casos com um
`switch` sem `default` — adicionar uma subclasse sem tratá-la não compila.

Quando a verificação e a gravação precisam dos mesmos dados, carregue uma vez e passe
adiante. `ServicoDeSessoes.planejar()` é o padrão: valida sala, filme e horário e
devolve o intervalo calculado. Antes disso, o recurso revalidava e rebuscava, o que
custava 4 consultas por criação e abria uma janela para 500 quando o registro
desaparecia no meio.

## Adaptador HTTP

- **Erros** em *problem details* da RFC 9457 (`application/problem+json`), com `type`,
  `title`, `status`, `detail` e, na validação, o membro de extensão `violacoes`. As
  fábricas estão em `Problemas`, para a mesma classe de erro não ganhar URNs
  diferentes em cada recurso.
- **Nunca `try/catch` num recurso.** Se um caso de uso introduzir uma falha nova, ela
  entra como subclasse de `ErroDeDominio`.
- **Validação** com Bean Validation nos parâmetros e nos DTOs de entrada, sempre com
  `message` — a mensagem padrão ("deve corresponder a...") não ajuda quem consome.
- **Listagens** devolvem `Pagina<T>`: `{itens, pagina, tamanho, total, totalPaginas}`.
  Todas, sem exceção — `PaginacaoTest` percorre as cinco e falha se alguma voltar a
  devolver array cru.
- **OpenAPI**: `@Operation` em toda rota, `@Parameter` em todo parâmetro de consulta e
  um `@APIResponse` por status, inclusive os de erro, apontando para o schema de
  `Problema`. `OpenApiTest` falha se uma rota ficar sem resumo.
- Os recursos devolvem registro de domínio direto quando o formato serve
  (`Cinema`, `Sala`, `Sessao`). A família `Filme` tem DTO próprio porque o contrato
  difere do registro: gêneros e diretores saem como lista de nomes.

## Adaptador de banco

As entidades carregam só mapeamento — sem `@JsonIgnore`, porque entidade nunca é
serializada. Relacionamentos são `LAZY`.

A tradução de restrição do banco fica em `ViolacoesDoBanco`, **não num
`ExceptionMapper`**. Reconhecer um SQLSTATE é conhecimento de persistência; deixar
isso na camada web obrigaria o adaptador HTTP a importar JPA e Hibernate — e o teste
de arquitetura reprova.

Toda gravação passa por `ViolacoesDoBanco.gravando(...)`, e o envelope cobre o
`persist` além do `flush`: com `IDENTITY`, o insert dispara já no persist, e é ali que
a violação aparece.

As regras de unicidade, de chave estrangeira e de sobreposição de horário ficam no
**banco**, não só na aplicação. A verificação na aplicação existe para dar uma
mensagem melhor; a garantia contra requisições simultâneas é a restrição.

## Migrações

Flyway em `adaptadores/src/main/resources/db/migration`, `V1__...`, `V2__...`. Nunca
edite uma migração já aplicada: crie a próxima. O schema vem só das migrações
(`quarkus.hibernate-orm.schema-management.strategy=none`).

## Como adicionar um caso de uso

1. **Registro de domínio**, em `dominio`: um `record` com os campos e os ids das FKs.
   Comportamento de negócio vem como método dele.
2. **Migração**, em `adaptadores/src/main/resources/db/migration`: a próxima `V*`, com
   as restrições que o banco deve garantir.
3. **Porta**, em `dominio`: a interface do repositório, em termos do registro e de
   `long`, só com os métodos que o caso de uso vai usar.
4. **Entidade e repositório**, em `adaptadores/banco`: a `*Entidade` package-private
   com `paraDominio()`, e a implementação Panache da porta. Gravações dentro de
   `ViolacoesDoBanco.gravando(...)`; listagens via `Paginas.de(...)`.
5. **Caso de uso**, em `aplicacao`: `@ApplicationScoped`, recebendo as portas,
   lançando `ErroDeDominio` para recusar.
6. **DTO de entrada**, em `adaptadores/http`, se houver corpo: um `record` com as
   anotações de Bean Validation e `message` em cada uma.
7. **Recurso**, em `adaptadores/http`: `@Tag` na classe, e por método `@Operation`,
   `@Parameter` e um `@APIResponse` por status — incluindo os de erro.
8. **Exceção**, se a falha não couber nas quatro existentes: adicione a subclasse de
   `ErroDeDominio` e trate no `switch` de `MapeadorDeErroDeDominio` (que não compila
   sem o caso novo).
9. **Testes**, em `adaptadores/src/test`: monte os dados pelas portas, via `Fixtures`
   — nunca persistindo entidade JPA, que nem é visível de lá.
