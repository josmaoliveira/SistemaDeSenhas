# Histórico das etapas

Registro de tudo o que foi feito em cada etapa: o que mudou para quem usa, o que foi feito por dentro
e quais arquivos foram criados, modificados ou apagados.

Caminhos curtos: `java/...` = `src/main/java/br/com/sistemadesenhas/...`,
`resources/...` = `src/main/resources/...`, `test/...` = `src/test/java/br/com/sistemadesenhas/...`.
(Até a etapa 4 o pacote se chamava `duck.com.example.SistemaDeSenhas`; veja "Organização final".)

---

## ETAPA 0: Preparação

**O que mudou para quem usa:** por fora, nada. Por dentro, o sistema ficou mais organizado e mais seguro.

**O que foi feito:**
- **Flyway:** antes o Hibernate criava as tabelas sozinho e sem controle. Agora cada mudança no banco é um
  arquivo numerado (V1, V2, V3...). Em bancos que já tinham as tabelas, o Flyway só marca como "versão 1",
  sem apagar nada.
- **Hibernate só confere** (`ddl-auto=validate`): não cria nem altera tabelas, só avisa se o Java não bater com o banco.
- **Erros padronizados:** todo erro volta no formato `{ status, codigo, mensagem }`, ex.: "Guichê não encontrado(a)".
- **Segurança:** o login antigo devolvia a senha do atendente para o navegador; parou de devolver.
- **Correções:** nome do relatório de impressão (`senha.jasper` → `Senha.jasper`) e erro com sigla de 1 letra.
- **Código comentado:** todas as classes e métodos explicados.
- **Testes automáticos** da fila, dos erros e do login.
- **Aviso do Lombok** no `pom.xml` removido.

**Arquivos criados:**

| Arquivo | Para quê |
|---|---|
| `resources/db/migration/V1__estrutura_inicial.sql` | Tabelas iniciais (as mesmas que já existiam) |
| `java/exception/ErroNegocio.java` | Erro "esperado" com código e mensagem |
| `java/exception/TratadorDeErros.java` | Transforma erros em JSON padrão |
| `resources/application-local.properties.exemplo` | Modelo das configurações locais |
| `docs/PADROES.md` | Regras de comentários e organização |
| `test/FilaDeSenhasTest.java`, `src/test/resources/application-test.properties` | Testes automáticos |

**Arquivos modificados:** `pom.xml` (Flyway e Lombok), `application.properties` (Flyway e banco), `README.md`,
`atendente.html` (mostrar as mensagens de erro novas) e todas as classes de `entity`, `repository`, `service`
e `controller` (comentários e erros claros).

---

## ETAPAS 1 e 2: Multiempresa + Login pelo Firebase

Feitas juntas porque o sistema só sabe de qual empresa é cada pessoa depois do login.

**O que mudou para quem usa:**
- Várias empresas usam o mesmo sistema e o mesmo banco; cada uma só vê os próprios dados.
- Login pelo Firebase: e-mail e senha (com e-mail de confirmação), Google, "esqueci minha senha" e SMS (opcional).
- Cadastro de empresa (`/cadastro.html`) com validação de CNPJ e CPF; quem cadastra vira o administrador.
- O administrador cadastra a equipe só pelo e-mail; a pessoa faz o "Primeiro acesso" na tela de login.
- Totem e painel com link próprio de cada empresa (`?empresa=slug`).
- Telas refeitas com visual novo.
- Os dados de teste antigos foram apagados (não pertenciam a nenhuma empresa).

**O que foi feito:**
- Tabela `empresas` e coluna `empresa_id` em todas as outras.
- Tabela `atendentes` (senha em texto puro) trocada por `usuarios`. A senha agora fica no Firebase.
- **Isolamento:** a empresa vem sempre do login (token do Firebase), nunca da URL. Pedir dados de outra
  empresa resulta em "não encontrado".
- **Spring Security** confere se o token foi emitido pelo Google para o nosso projeto.
- Rotas reorganizadas: `/api/publico` (totem/painel), `/api/empresa` (gestão), `/api/atendimentos` (guichê),
  `/api/auth` (quem sou eu), `/api/cadastro`.
- Proteção contra HTML malicioso nas telas.

**Arquivos criados (44):**

| Pasta | Arquivos | Para quê |
|---|---|---|
| `resources/db/migration` | `V2__multiempresa.sql` | Cria empresas e usuários; coloca `empresa_id` em tudo |
| `java/entity` | `Empresa`, `Usuario`, `Enums/Perfil` | Novas tabelas e perfis |
| `java/repository` | `EmpresaRepository`, `UsuarioRepository` | Consultas, sempre filtrando pela empresa |
| `java/config` | `SecurityConfig`, `FirebaseProperties` | Regras de acesso e configuração do Firebase |
| `java/security` | `TokenFirebase`, `ConversorTokenFirebase`, `UsuarioLogado` | Leem o token e descobrem quem está logado e de qual empresa (depois trocados por `LeitorDeIdentidade`/`ConversorDeToken`, veja a última seção) |
| `java/service` | `AuthService`, `EmpresaService`, `EquipeService`, `CadastrosService` | Login, cadastro de empresa, equipe, serviços/prioridades/guichês |
| `java/controller` | `AuthController`, `CadastroController`, `publico/ConfigController`, `publico/TotemController`, `empresa/CadastrosController`, `empresa/EquipeController`, `empresa/MinhaEmpresaController` | Novas rotas da API |
| `java/dto` | 12 arquivos (`ServicoRequest`, `MeResponse`, `PainelItem`...) | Formato dos dados que entram e saem |
| `java/util` | `Documentos`, `Telefones`, `Slugs` | Validar CPF/CNPJ, padronizar telefone, gerar o link da empresa |
| `resources/static` | `cadastro.html`, `css/estilo.css`, `js/app.js` | Cadastro, visual comum e funções comuns (Firebase, API) |
| `docs` | `FIREBASE.md` | Passo a passo do Firebase |
| testes | `FluxoCompletoTest`, `suporte/TokensDeTeste`, `util/DocumentosTest` | Testes de ponta a ponta, inclusive isolamento |

**Arquivos apagados (9):** `Atendente`, `AtendenteRepository`, `AtendenteController`, `ServicoController`,
`PrioridadeController`, `GuicheController`, `SenhaController`, `SenhaRequestDTO`, `FilaDeSenhasTest`
(substituídos pelos novos).

**Arquivos modificados:**
- Telas reescritas: `login.html`, `admin.html`, `selecionar-guiche.html`, `atendente.html`, `index.html` (totem), `painel.html`.
- Classes: entidades `Servico`, `Prioridade`, `Guiche`, `Senha`, `Atendimento` (ganharam a empresa), seus
  repositórios, `AtendimentoService`, `SenhaService`, `SenhaFactory`, `TratadorDeErros`.
- Configuração: `pom.xml` (Spring Security), `application.properties` (Firebase), `README.md`.

---

## ETAPA 3: Licença

**O que mudou para quem usa:**
- Toda empresa ganha **90 dias grátis** e um **serial** ligado ao CNPJ.
- A gestão mostra o cartão da licença e avisa quando faltam 7 dias ou menos.
- No vencimento (ou se a empresa for bloqueada) totem, painel e atendimento param; o administrador continua vendo a licença.
- O fim do cadastro mostra o serial e a data de vencimento.

**O que foi feito:**
- **Serial:** HMAC-SHA256 do CNPJ com um segredo do servidor. O mesmo CNPJ gera sempre o mesmo serial e,
  sem o segredo, ninguém consegue inventar um válido.
- **Situação calculada:** Período grátis, Ativa, Vencida ou Bloqueada.
- **Bloqueio automático:** chamadas da empresa recusadas com status 402 quando a licença não está liberada.
- **Empresas antigas:** ganham as colunas novas e o serial é calculado ao subir o sistema.

**Arquivos criados (11):**

| Arquivo | Para quê |
|---|---|
| `resources/db/migration/V3__licenca.sql` | Colunas da licença na tabela empresas |
| `java/service/SerialService.java` | Calcula e confere o serial |
| `java/service/LicencaService.java` | Situação, dias restantes, bloqueio |
| `java/service/PreenchimentoDeSeriais.java` | Gera o serial das empresas antigas ao subir |
| `java/security/VerificadorLicenca.java` | Barra as rotas da empresa com licença vencida |
| `java/config/WebConfig.java` | Liga o verificador às rotas certas |
| `java/config/AppProperties.java` | Lê os dias grátis e o segredo do serial |
| `java/entity/Enums/SituacaoLicenca.java`, `StatusEmpresa.java` | Situações da licença e status Ativa/Bloqueada |
| `java/dto/LicencaResponse.java` | Dados da licença para as telas |
| `test/service/SerialServiceTest.java` | Testes do serial |

**Arquivos modificados:** `Empresa` (campos da licença), `EmpresaRepository`, `EmpresaService` (serial e 90 dias
no cadastro), `AuthService` e `MeResponse` (login devolve a licença), `CadastroController` e
`MinhaEmpresaController` (rota da licença), `SenhaService` (totem e painel param), `admin.html`, `cadastro.html`,
`js/app.js`, `application.properties`, `README.md`, `docs/PADROES.md`, `FluxoCompletoTest`.

---

## ETAPA 4: Painel do GM (administrador geral do sistema)

**O que mudou para quem usa:**
- Nova tela **`/gm.html`**, só para o GM, com 5 abas:
  - **Resumo:** empresas por situação, empresas que vencem em até 7 dias, total de usuários, logins de hoje e valor recebido no mês.
  - **Empresas:** buscar por nome/CNPJ, **registrar pagamento** (renova o vencimento), **editar dados**,
    **alterar vencimento**, **bloquear/desbloquear**, **gerar serial novo**, ver usuários e **excluir** (exige digitar o nome).
  - **Usuários:** de todas as empresas (com filtro), **editar** (nome, e-mail, celular, perfil),
    **ativar/desativar**, **enviar e-mail de redefinição de senha**, **definir senha** (com a conta de serviço do Firebase) e **excluir**.
  - **Pagamentos:** histórico com vencimento anterior → novo e quem registrou.
  - **Logins:** histórico de acessos (data, usuário, empresa, como entrou, IP).
- Quem tem o e-mail listado em `app.gm-emails` vira GM no primeiro login.
- Cada login fica registrado no histórico.

**O que foi feito:**
- O GM é um usuário **sem empresa**; só ele acessa `/api/gm/**`.
- **Padrão Strategy** para as contas do Firebase (`GerenciadorContas`): sem a conta de serviço, usa a estratégia
  básica (só envia e-mail de redefinição); com ela, a completa (troca senha, desativa e exclui contas no Firebase).
  O resto do sistema não sabe qual está ativa.
- **Pagamentos:** cada pagamento soma dias ao vencimento atual (ou a partir de hoje, se já venceu) e tira a empresa do período grátis.
- Ao trocar o CNPJ de uma empresa, o serial é recalculado.

**Arquivos criados (25):**

| Arquivo | Para quê |
|---|---|
| `resources/db/migration/V4__painel_gm.sql` | Tabelas `registros_login` e `pagamentos` |
| `java/entity/RegistroLogin.java`, `Pagamento.java`, `Enums/FormaPagamento.java` | Histórico de logins, pagamentos e formas de pagamento |
| `java/repository/RegistroLoginRepository.java`, `PagamentoRepository.java` | Consultas do histórico e dos pagamentos |
| `java/service/GmService.java` | Resumo e gestão de usuários de todas as empresas |
| `java/service/conta/GerenciadorContas.java` | Interface (Strategy) das operações de conta |
| `java/service/conta/FirebaseRestGerenciadorContas.java` | Estratégia básica: envia e-mail de redefinição |
| `java/service/conta/FirebaseAdminGerenciadorContas.java` | Estratégia completa: troca senha, desativa, exclui |
| `java/service/conta/ContasConfig.java` | Escolhe a estratégia ao subir o sistema |
| `java/controller/gm/GmEmpresaController.java` | Rotas de empresas e pagamentos do GM |
| `java/controller/gm/GmUsuarioController.java` | Rotas de resumo, usuários e logins do GM |
| `java/dto/` (9 arquivos) | `EmpresaGmRequest/Response`, `PagamentoRequest/Response`, `StatusEmpresaRequest`, `VencimentoRequest`, `NovaSenhaRequest`, `LoginResponse`, `ResumoGmResponse` |
| `resources/static/gm.html` | Tela do GM |
| `docs/ETAPAS.md` | Este histórico |
| `docs/ANTES-DE-COLOCAR-ONLINE.md` | Lista do que fazer antes de publicar |

**Arquivos modificados:** `Perfil` (novo perfil GM), `Usuario`, `AppProperties` (`app.gm-emails`), `AuthService`
(cria o GM e grava o histórico de login), `AuthController` (IP), `EmpresaService` (edição, serial novo,
bloqueio, vencimento, exclusão), `LicencaService` (registro de pagamento), `EquipeService` (não permite criar GM),
`UsuarioRepository`, `EmpresaRepository`, `SecurityConfig` (`/api/gm/**` só GM), `pom.xml` (Firebase Admin),
`application.properties` e `.exemplo` (`gm-emails` e `credenciais`), `.gitignore` (arquivos de chave),
`js/app.js` (tela inicial do GM), `README.md`, `docs/FIREBASE.md`, `FluxoCompletoTest`, `SerialServiceTest`.

---

## Correção do acesso do GM + organização final do projeto

**O que mudou para quem usa:**
- **Acesso do GM:** o GM não conseguia entrar porque o e-mail dele ainda não tinha senha no Firebase.
  A tela de login agora tem o botão **"Primeiro acesso: criar minha senha"** bem visível (antes era um link
  escondido que falava só de atendente). Senha errada sugere o primeiro acesso; e-mail que já tem conta é
  levado para "esqueci minha senha".
- Ao subir, o sistema avisa no terminal se o e-mail do GM já pertence a uma empresa (esse caso faz a pessoa
  cair na tela da empresa em vez do painel do GM).

**Organização e limpeza:**
- **Pacote Java renomeado** de `duck.com.example.SistemaDeSenhas` para `br.com.sistemadesenhas`, no padrão
  de nomes do Java (tudo minúsculo). O pacote `entity.Enums` virou `entity.enums`.
- `pom.xml`: `groupId` `br.com.sistemadesenhas`, `artifactId` `sistema-de-senhas`, versão `1.0.0-SNAPSHOT`;
  removida a biblioteca `jasperreports-functions`, que o relatório não usa.
- Código sem uso removido: `UsuarioRepository.countByPerfil` e `SerialService.confere` (só os testes chamavam).
- Teste `SistemaDeSenhasApplicationTests` removido (o `FluxoCompletoTest` já sobe o sistema inteiro).
- `README.md` reescrito (funcionalidades, tecnologias, estrutura, API, telas e documentação).
- Relatório acadêmico de padrões de projeto movido do README para `docs/RELATORIO-PADROES-DE-PROJETO.md`.
- Novo `.editorconfig` (padrão de formatação para qualquer editor) e `.gitignore` organizado.

**Arquivos criados:** `java/service/ConferenciaDosGms.java`, `docs/RELATORIO-PADROES-DE-PROJETO.md`, `.editorconfig`.

**Arquivos modificados:** `login.html`, `pom.xml`, `README.md`, `.gitignore`, `SerialService`, `SerialServiceTest`,
`UsuarioRepository`, `docs/ETAPAS.md`, `docs/FIREBASE.md`, e todas as classes Java (só a linha `package`/`import`, pela troca do pacote).

**Arquivos apagados:** `test/SistemaDeSenhasApplicationTests.java`.

---

## Mensalidade das empresas (pedido do supervisor)

**Regra:** a empresa usa 90 dias grátis e, depois, paga um valor mensal (por Pix, na Etapa 5).
O valor é configurável, porque o preço pode subir.

**O que mudou para quem usa:**
- **Mensalidade padrão:** vale para todas as empresas. Começa em **R$ 99,90** e o GM altera em
  **Resumo > Mensalidade padrão > Alterar**. O novo valor vale para as próximas cobranças.
- **Valor próprio por empresa (opcional):** na aba Empresas, botão **"Mensalidade"**. Serve para dar um
  desconto ou manter um preço antigo. Marcando "Usar o valor padrão", a empresa volta a seguir o padrão.
- A lista de empresas mostra a mensalidade de cada uma (e se é "padrão" ou "valor próprio").
- Ao registrar um pagamento, o valor já vem preenchido com a mensalidade da empresa.
- O administrador da empresa vê na tela dele quanto vai pagar: "R$ 99,90/mês após o período grátis".

**Banco:** migração `V5__mensalidade.sql`, com a tabela `configuracoes` (uma linha só, com o valor padrão)
e a coluna `empresas.valor_mensal` (vazia = usa o padrão).

**API nova (só GM):** `GET/PUT /api/gm/mensalidade` e `PUT /api/gm/empresas/{id}/valor-mensal`.
`GET /api/empresa/licenca` e a lista de empresas do GM passaram a trazer o `valorMensal`.

**Arquivos criados:** `db/migration/V5__mensalidade.sql`, `entity/Configuracao.java`,
`repository/ConfiguracaoRepository.java`, `dto/ValorMensalRequest.java`, `dto/MensalidadeResponse.java`.

**Arquivos modificados:** `Empresa`, `LicencaService`, `EmpresaService`, `GmEmpresaController`,
`LicencaResponse`, `EmpresaGmResponse`, `gm.html`, `admin.html`, `FluxoCompletoTest` (testa padrão,
reajuste, valor próprio e volta ao padrão).

---

## Baixo acoplamento com o Firebase + botões "Copiar" + Atendimento de hoje (pedidos do supervisor)

**1. Firebase isolado (para mudanças futuras)**
- **Telas:** novo `js/autenticacao.js`, o ÚNICO arquivo que usa o Firebase. Login, cadastro e `app.js` só chamam
  funções dele (`entrarComEmail`, `criarConta`, `enviarCodigoSms`...). Os erros saem já em português.
- **Back-end:** os services recebem uma `Identidade` (uid, e-mail, telefone, nome...) em vez do token do Firebase.
  - `LeitorDeIdentidade` (interface) + `LeitorDeIdentidadeFirebase` (lê o token do Firebase).
  - `FirebaseConfig`: a validação do token saiu do `SecurityConfig`, que agora não sabe qual serviço de login é usado.
  - `ConversorTokenFirebase` virou `ConversorDeToken`; `TokenFirebase` foi apagado.
- O passo a passo de "o que mudar se o Firebase mudar" está em `docs/FIREBASE.md` (última seção).
- Para quem usa, nada muda: o login funciona igual.

**2. Botões "Copiar"**
- Ao lado dos links do totem e do painel, do serial (tela do administrador e fim do cadastro) e do serial na lista do GM.
- Funciona em `localhost` e em `https` (link do VS Code); em `http` por IP usa um jeito alternativo.

**3. Atendimento de hoje (tela do administrador da empresa)**
- Novo quadro no topo da gestão: guichês ligados, quantos guichês atenderam, total de atendimentos do dia,
  quantos estão em atendimento agora e tempo médio de atendimento.
- Tabela por guichê: situação (Ligado, Atendendo, Desligado), quem está nele, quantidade, tempo médio e último atendimento.
- **Ligado** = a tela de atendimento daquele guichê está aberta. Ela avisa o servidor a cada minuto; sem aviso por
  2 minutos, aparece desligado. Ao clicar em "Trocar de guichê" ou "Sair", desliga na hora.
- **Tempo médio** = da chamada da senha até finalizar (só conta atendimentos finalizados).
- Atualiza sozinho a cada 30 segundos. O "Fim do dia" apaga os atendimentos, então os números zeram junto.

**Banco:** migração `V6__presenca_guiche.sql` (colunas `ultima_presenca` e `usuario_presente_id` em `guiches`).

**API nova:** `GET /api/empresa/relatorios/atendimento-hoje` (administrador),
`POST` e `DELETE /api/atendimentos/presenca/{guicheId}` (tela do atendente).

**Arquivos criados:** `js/autenticacao.js`, `V6__presenca_guiche.sql`, `security/Identidade.java`,
`security/LeitorDeIdentidade.java`, `security/LeitorDeIdentidadeFirebase.java`, `config/FirebaseConfig.java`,
`service/RelatorioService.java`, `controller/empresa/RelatorioController.java`, `dto/AtendimentoHojeResponse.java`.

**Arquivos modificados:** `js/app.js`, `login.html`, `cadastro.html`, `admin.html`, `atendente.html`, `gm.html`,
`css/estilo.css`, `SecurityConfig`, `UsuarioLogado`, `AuthService`, `EmpresaService`, `AuthController`,
`CadastroController`, `AtendimentoService`, `AtendimentoController`, `AtendimentoRepository`, `Guiche`,
`FluxoCompletoTest` (teste novo do relatório), `docs/FIREBASE.md`, `docs/PADROES.md`.

**Arquivos renomeados/apagados:** `ConversorTokenFirebase` → `ConversorDeToken`; `TokenFirebase` apagado.
