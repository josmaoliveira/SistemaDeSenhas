# Padrões do projeto

Regras combinadas para o código ficar fácil de entender e de manter.

## Comentários

- **Toda classe** tem um comentário `/** ... */` em cima dizendo **para que ela serve**.
- **Todo método público com regra de negócio** tem `/** ... */` explicando **o que faz** e, quando ajudar, os passos.
- Comentário explica o **porquê**, não repete o código.
  - Ruim: `// soma 1` em cima de `total + 1`
  - Bom: `// Sequência: quantas senhas já existem para este serviço + 1`
- Comentários e nomes em **português**.

## Organização das pastas

Pacote base: `br.com.sistemadesenhas` (nomes de pacote sempre em minúsculas).

| Pasta | O que vai nela |
|---|---|
| `entity` | Classes que representam tabelas do banco |
| `repository` | Consultas ao banco |
| `service` | Regras de negócio |
| `controller` | Rotas da API (recebem e devolvem JSON) |
| `dto` | Formatos dos dados que entram e saem da API |
| `security` | Login (token do Firebase) e `UsuarioLogado` |
| `config` | Configurações (segurança, Firebase) |
| `util` | Funções de apoio (validar CPF/CNPJ, telefone, slug) |
| `exception` | Erros padronizados (`ErroNegocio`) e o `TratadorDeErros` |
| `resources/db/migration` | Arquivos do Flyway que criam/alteram tabelas |
| `resources/static` | Telas (HTML). Estilo em `css/estilo.css`, funções comuns em `js/app.js` |

## Multiempresa (MUITO IMPORTANTE)

- Toda tabela de dados tem `empresa_id`.
- A empresa vem **sempre** de `usuarioLogado.empresaId()` (que lê o token do login), **nunca** da URL ou do JSON.
- Toda busca por id usa o método que também filtra pela empresa, ex.: `findByIdAndEmpresaId(id, usuarioLogado.empresaId())`.
  Assim, se alguém mandar o id de um registro de outra empresa, recebe "não encontrado".
- Ao criar um registro, a empresa é definida no service (`setEmpresa(usuarioLogado.empresa())`).
- Toda regra nova de isolamento ganha um teste em `FluxoCompletoTest`.

## Licença

- Rotas de `/api/empresa/**` e `/api/atendimentos/**` passam pelo `VerificadorLicenca`: licença vencida ou bloqueada responde **402**.
- Rotas públicas (totem/painel) chamam `licencaService.exigirLiberada(empresa)` no service.
- A situação (EM_TESTE, ATIVA, VENCIDA, BLOQUEADA) é **calculada** pelo `LicencaService`, não é salva no banco.

## Padrões de projeto usados

| Padrão | Onde | Para quê |
|---|---|---|
| Factory | `SenhaFactory` | Criar senhas num lugar só |
| Facade/Adapter | `ImpressaoService` + `ReportPrinter` | Esconder os detalhes do JasperReports |
| Strategy | `NextSenhaStrategy` | Trocar a regra de "qual a próxima senha" |
| Strategy | `GerenciadorContas` | Trocar como as contas do Firebase são gerenciadas (básico ou completo) |
| Adapter (interface) | `LeitorDeIdentidade` + `Identidade` | Os services não conhecem o formato do token do Firebase |
| Adapter (módulo JS) | `js/autenticacao.js` | Só esse arquivo das telas usa o Firebase |

## Baixo acoplamento com o Firebase

O Firebase fica "isolado" em poucos arquivos. **Regra:** nenhum outro arquivo pode usar o Firebase direto.

- **Telas:** só `js/autenticacao.js` importa o SDK do Firebase. As telas chamam funções em português
  (`entrarComEmail`, `criarConta`, `enviarCodigoSms`...) e recebem erros já traduzidos (`ErroAutenticacao`).
- **Back-end:** os services recebem uma `Identidade` (uid, e-mail, telefone...), nunca o token.
  Quem lê o token é `LeitorDeIdentidadeFirebase`; quem valida é o `JwtDecoder` de `FirebaseConfig`.
- Se o Firebase mudar ou for trocado, veja `docs/FIREBASE.md`, seção "Trocar o serviço de login".

## Telas

- Use `api()` do `js/app.js` para chamar o servidor: ele já manda o token do login.
- Login (entrar, criar conta, SMS...): use as funções de `js/autenticacao.js`, nunca o Firebase direto.
- Texto que a pessoa pode querer copiar (links, serial): coloque `data-copiar` no elemento e chame
  `ativarBotoesCopiar()` do `js/app.js`; ele cria o botão "Copiar".
- Ao colocar texto vindo do banco dentro de `innerHTML`, use `esc(texto)` (evita HTML malicioso).

## Erros

- Para avisar um problema esperado, lance `ErroNegocio` (ex.: `throw ErroNegocio.naoEncontrado("Guichê")`).
- O navegador recebe sempre o mesmo formato: `{ "status": 404, "codigo": "NAO_ENCONTRADO", "mensagem": "..." }`.
- Nas telas, mostre `erro.mensagem` para o usuário.

## Banco de dados (Flyway)

- **Nunca** altere um arquivo `V1__...sql` que já rodou.
- Para mudar o banco, crie o próximo: `V2__descricao.sql`, `V3__descricao.sql`...
- O Hibernate só confere (`ddl-auto=validate`); quem cria tabela é o Flyway.

## Testes

- Rodar: `.\mvnw.cmd test` (precisa do banco `sistemadesenhas_test` criado).
- Ao criar uma regra nova, crie um teste para ela.
