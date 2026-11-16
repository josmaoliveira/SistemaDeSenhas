# Antes de colocar o sistema online

Lista do que **precisa** ser feito antes de publicar o sistema num servidor (DigitalOcean, Amazon etc.).
Marque cada item conforme for fazendo.

## 1. Segredos e senhas (obrigatório)

- [ ] **Trocar o segredo do serial.** Defina a variável de ambiente `SERIAL_SEGREDO` no servidor com um texto
      grande e aleatório (ex.: 40 caracteres misturados).
  - ⚠️ Faça isso **antes** de ter clientes: se trocar depois, os seriais de todas as empresas mudam.
  - Guarde esse segredo num lugar seguro: sem ele não dá para conferir os seriais.
- [ ] **Senha forte no PostgreSQL** do servidor (nada de `12345`) e usuário próprio do banco, não o `postgres`.
- [ ] **Configurar por variáveis de ambiente**, não por arquivo: `DB_URL`, `DB_USER`, `DB_PASSWORD`,
      `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`, `FIREBASE_AUTH_DOMAIN`, `FIREBASE_APP_ID`, `GM_EMAILS`,
      `SERIAL_SEGREDO` (e, se usar, `FIREBASE_CREDENCIAIS`). O `application-local.properties` é só para o seu PC.
- [ ] **Conta de serviço do Firebase** (arquivo JSON, se for usar): guardar **fora** da pasta do projeto,
      com acesso restrito, e **nunca** enviar ao GitHub. O `.gitignore` já bloqueia os nomes comuns.
- [ ] **`GM_EMAILS`** com apenas o(s) e-mail(s) de quem deve ser GM.

## 2. Firebase

- [ ] Em **Authentication > Configurações > Domínios autorizados**, adicionar o domínio do site (ex.: `meusistema.com.br`).
- [ ] Em **Google Cloud Console > APIs e serviços > Credenciais**, restringir a chave da API (`apiKey`) ao domínio do site.
- [ ] SMS de verdade exige o plano **Blaze** (pago por uso). Os "números de teste" não servem para clientes reais.
- [ ] Modelos de e-mail (Authentication > Modelos) em português e com o nome do sistema.

## 3. Servidor

- [ ] **HTTPS obrigatório** (certificado gratuito com Let's Encrypt, ou o HTTPS do provedor).
      Sem HTTPS o login do Google e o token podem ser bloqueados ou interceptados.
- [ ] Se usar proxy (Nginx etc.), repassar o cabeçalho `X-Forwarded-For` (o histórico de logins usa o IP real).
- [ ] Liberar no firewall só as portas necessárias (443/80). O PostgreSQL **não** deve ficar aberto para a internet.
- [ ] O sistema deve rodar como serviço (reinicia sozinho se o servidor reiniciar).

## 4. Banco de dados

- [ ] **Backup automático diário** do PostgreSQL, guardado fora do servidor, e testar uma restauração pelo menos uma vez.
- [ ] Não mexer nos arquivos `V1`, `V2`... que já rodaram. Mudança no banco = arquivo novo (`V5__...sql`).

## 5. Impressão do ticket

- [ ] A impressão atual (`JasperReports`) imprime na impressora **do servidor**. Online, a impressora fica no
      estabelecimento, então será preciso outra solução (ex.: imprimir pelo navegador do totem). Avaliar antes de publicar.

## 6. Conferência final

- [ ] Rodar os testes automáticos (`.\mvnw.cmd test`) e confirmar que todos passam.
- [ ] Testar no servidor: cadastro de empresa, login (e-mail, Google), totem, painel, atendimento, licença e painel do GM.
