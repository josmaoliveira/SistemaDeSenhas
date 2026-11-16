# Sistema de Senhas

Sistema web de **gestão de filas por senha** para várias empresas ao mesmo tempo (multiempresa).
O cliente retira a senha no **totem**, o **painel da TV** chama com aviso sonoro e o **atendente** opera o guichê.
Cada empresa tem sua própria gestão, e o **GM** (administrador geral do sistema) controla empresas, licenças e pagamentos.

---

## Funcionalidades

- **Multiempresa:** várias empresas no mesmo sistema e no mesmo banco; cada uma só enxerga os próprios dados.
- **Login pelo Firebase:** e-mail e senha (com confirmação por e-mail), Google, SMS e "esqueci minha senha".
- **Cadastro de empresa** com validação de CNPJ e CPF.
- **Licença por empresa:** 90 dias grátis, serial ligado ao CNPJ e bloqueio automático no vencimento.
- **Três perfis com telas próprias:** GM, administrador da empresa e atendente.
- **Fila com prioridade:** maior peso primeiro e, no empate, ordem de chegada; trava contra chamada duplicada.
- **Painel multi-guichê** com destaque e som para a chamada mais recente.
- **Impressão do ticket** com JasperReports.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA, Spring Security (OAuth2 Resource Server) |
| Banco de dados | PostgreSQL 16, Flyway (versionamento das tabelas) |
| Login | Firebase Authentication (+ Firebase Admin SDK, opcional) |
| Frontend | HTML, CSS e JavaScript puro (módulos ES) |
| Relatórios | JasperReports |
| Testes | JUnit 5, Spring Boot Test, MockMvc |

---

## Como rodar

### Pré-requisitos
- JDK 21
- PostgreSQL 16
- Um projeto no Firebase (grátis): veja [`docs/FIREBASE.md`](docs/FIREBASE.md)

### 1. Banco de dados
No pgAdmin, crie o banco (uma vez só):
```sql
CREATE DATABASE sistemadesenhas_db;
```
As tabelas são criadas automaticamente pelo Flyway.

### 2. Configuração local
Copie `src/main/resources/application-local.properties.exemplo` para `application-local.properties`
(mesma pasta) e preencha a senha do PostgreSQL, as chaves do Firebase e o e-mail do GM.
Esse arquivo fica só no seu computador (está no `.gitignore`).

### 3. Executar
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```
Acesse `http://localhost:8080/login.html`.

### 4. Testes automáticos
Crie o banco de testes uma vez (`CREATE DATABASE sistemadesenhas_test;`) e rode:
```bash
.\mvnw.cmd test
```

---

## Telas

| Endereço | Quem usa | Para quê |
|---|---|---|
| `/login.html` | Todos | Entrar, primeiro acesso (criar senha) e "esqueci minha senha" |
| `/cadastro.html` | Novo cliente | Cadastrar a empresa (vira o administrador dela) |
| `/admin.html` | Administrador da empresa | Licença, serviços, prioridades, guichês, equipe e links do totem/painel |
| `/selecionar-guiche.html`, `/atendente.html` | Atendente | Escolher o guichê e chamar senhas |
| `/gm.html` | GM | Empresas, pagamentos, usuários, logins e bloqueios |
| `/index.html?empresa=SLUG` | Clientes | Totem: retirar senha |
| `/painel.html?empresa=SLUG` | TV | Painel de chamadas |

Os links do totem e do painel de cada empresa aparecem na tela de gestão.

### Primeiros passos
1. Abra `/cadastro.html`, crie seu acesso e cadastre a empresa.
2. Na gestão, cadastre serviços, prioridades e guichês.
3. Em **Equipe**, cadastre o e-mail dos atendentes. Eles entram em `/login.html` > **Primeiro acesso: criar minha senha** (ou com o Google).
4. Para ser **GM**, coloque seu e-mail em `app.gm-emails` e entre pelo **Primeiro acesso** (ou Google) com ele.
   Use um e-mail diferente do da sua empresa.

---

## Estrutura do projeto

```
src/main/java/br/com/sistemadesenhas
├── config        Segurança, Firebase e regras do sistema (properties)
├── controller    Rotas da API: publico/ (totem e painel), empresa/, gm/, auth, cadastro, atendimentos
├── dto           Formatos dos dados que entram e saem da API
├── entity        Tabelas do banco (e enums em entity/enums)
├── exception     Erros padronizados e tratador global
├── repository    Consultas ao banco (sempre filtrando pela empresa)
├── security      Identidade de quem fez login (o Firebase fica isolado aqui), usuário logado e licença
├── service       Regras de negócio (e service/conta: Strategy das contas do Firebase)
└── util          Validação de CPF/CNPJ, telefone e slug

src/main/resources
├── db/migration  Arquivos do Flyway (V1, V2, ...)
├── reports       Ticket da senha (JasperReports)
└── static        Telas HTML, css/estilo.css, js/app.js e js/autenticacao.js (único arquivo que usa o Firebase)
```

### API (resumo)

| Prefixo | Acesso | Conteúdo |
|---|---|---|
| `/api/publico/**` | Livre | Configuração do Firebase, totem e painel (por slug da empresa) |
| `/api/auth/me`, `/api/cadastro/**` | Logado no Firebase | Quem sou eu, cadastro de empresa |
| `/api/empresa/**` | Administrador da empresa | Serviços, prioridades, guichês, equipe, licença, atendimento de hoje, reset da fila |
| `/api/atendimentos/**` | Administrador e atendente | Chamar, rechamar e finalizar senhas; presença no guichê |
| `/api/gm/**` | GM | Empresas, pagamentos, usuários, logins e resumo |

Erros sempre no formato `{ "status": 404, "codigo": "NAO_ENCONTRADO", "mensagem": "..." }`.
Licença vencida ou bloqueada responde **402**.

---

## Licença das empresas

- Empresa nova ganha **90 dias grátis** (`app.licenca.dias-gratis`).
- O **serial** (`XXXX-XXXX-XXXX-XXXX`) é calculado a partir do CNPJ com um segredo do servidor (`SERIAL_SEGREDO`).
- Vencida ou bloqueada: totem, painel e atendimento param; o administrador continua vendo a licença.
- O GM registra pagamentos, que somam dias ao vencimento.

## Manutenção diária

O administrador da empresa tem o botão **Resetar fila** na gestão, para reiniciar a numeração ao fim do expediente.

---

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/FIREBASE.md`](docs/FIREBASE.md) | Configurar o login, o GM e a conta de serviço |
| [`docs/ANTES-DE-COLOCAR-ONLINE.md`](docs/ANTES-DE-COLOCAR-ONLINE.md) | Lista obrigatória antes de publicar |
| [`docs/ETAPAS.md`](docs/ETAPAS.md) | Histórico do que foi feito em cada etapa |
| [`docs/PADROES.md`](docs/PADROES.md) | Padrões de código, comentários e multiempresa |
| [`docs/RELATORIO-PADROES-DE-PROJETO.md`](docs/RELATORIO-PADROES-DE-PROJETO.md) | Relatório da refatoração com Factory, Facade e Strategy |
