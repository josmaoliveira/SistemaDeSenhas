# Configurar o Firebase (login)

O login do sistema (e-mail e senha, Google e SMS) é feito pelo **Firebase**, do Google.
É grátis para começar. Faça isto **uma vez só**.

## 1. Criar o projeto

1. Acesse <https://console.firebase.google.com> com a sua conta Google.
2. Clique em **Criar um projeto** (ou "Adicionar projeto").
3. Dê um nome, por exemplo `sistema-senhas`, e avance.
4. O Google Analytics pode ser **desativado**. Clique em **Criar projeto**.

## 2. Ativar as formas de login

1. No menu da esquerda: **Criação > Authentication** e clique em **Vamos começar**.
2. Na aba **Método de login**:
   - **E-mail/senha**: ative a primeira chave (não precisa do "link por e-mail") e salve.
   - **Google**: ative, escolha o seu e-mail de suporte e salve.
3. (Opcional) Na aba **Modelos**, clique no lápis e mude o idioma dos e-mails para **Português (Brasil)**.

## 3. Pegar as chaves do app Web

1. Clique na **engrenagem** (ao lado de "Visão geral do projeto") > **Configurações do projeto**.
2. Em **Seus apps**, clique no ícone **`</>`** (Web).
3. Dê um apelido (ex.: `site`) e clique em **Registrar app** (não precisa do Firebase Hosting).
4. Vai aparecer um bloco `firebaseConfig` parecido com este:
   ```js
   const firebaseConfig = {
     apiKey: "AIzaSy...",
     authDomain: "sistema-senhas.firebaseapp.com",
     projectId: "sistema-senhas",
     appId: "1:1234567890:web:abc123..."
   };
   ```

## 4. Colocar as chaves no sistema

1. Na pasta `src/main/resources`, copie o arquivo `application-local.properties.exemplo`
   e renomeie a cópia para **`application-local.properties`**.
2. Preencha com os valores do passo 3:
   ```properties
   firebase.project-id=sistema-senhas
   firebase.api-key=AIzaSy...
   firebase.auth-domain=sistema-senhas.firebaseapp.com
   firebase.app-id=1:1234567890:web:abc123...
   ```
3. Reinicie o sistema (`Ctrl + C` e `.\mvnw.cmd spring-boot:run`).

Esse arquivo **não vai para o GitHub** (está no `.gitignore`).

## 5. (Opcional) Login por SMS

1. Em **Authentication > Método de login**, ative **Telefone**.
2. Enviar SMS de verdade exige o plano pago do Firebase (Blaze).
   **Para testar de graça**, ainda na configuração do Telefone, abra
   **Números de telefone para teste** e cadastre, por exemplo:
   `+55 83 99999-0000` com o código `123456`. Esse número funciona sem enviar SMS.
3. No `application-local.properties`, coloque `firebase.sms-habilitado=true` e reinicie.

## 6. Virar GM (administrador geral do sistema)

1. No `application-local.properties`, coloque o seu e-mail:
   ```properties
   app.gm-emails=seu-email@gmail.com
   ```
   (Para mais de um GM, separe por vírgula.)
2. Reinicie o sistema e abra `/login.html`.
3. O GM ainda **não tem senha**. Escolha uma das opções:
   - **Entrar com o Google** usando esse e-mail; **ou**
   - **Primeiro acesso: criar minha senha** → digite o e-mail e crie a senha → confirme o e-mail que chegar →
     clique em **"Já confirmei meu e-mail"**.
4. Você vai direto para o **Painel Geral** (`/gm.html`).

⚠️ O e-mail do GM não pode já estar cadastrado como usuário de uma empresa. Use um e-mail diferente
do que você usou para cadastrar a sua empresa de teste.

## 7. (Opcional) Conta de serviço: GM troca senhas direto

Sem isto, o GM só consegue **enviar o e-mail de redefinição de senha**. Com isto, ele também consegue
**definir a senha** de um usuário, e desativar/excluir contas direto no Firebase.

1. No Firebase: ⚙️ **Configurações do projeto > Contas de serviço > Gerar nova chave privada**.
2. Vai baixar um arquivo `.json`. **Ele é um segredo**: guarde FORA da pasta do projeto, por exemplo
   em `C:\chaves\firebase-conta-servico.json`. Nunca mande para o GitHub nem para ninguém.
3. No `application-local.properties` (use barras normais `/`):
   ```properties
   firebase.credenciais=C:/chaves/firebase-conta-servico.json
   ```
4. Reinicie. No terminal deve aparecer `Gerenciador de contas: completo (Firebase Admin)`.

## Problemas comuns

| Mensagem | Solução |
|---|---|
| "O Firebase ainda não foi configurado no servidor" | Faltou o passo 4 ou o sistema não foi reiniciado. |
| "Este tipo de login não foi ativado no Firebase" | Faltou ativar o método no passo 2. |
| "Este endereço não está autorizado no Firebase" | Authentication > Configurações > Domínios autorizados: adicione o endereço (o `localhost` já vem). |
| E-mail de confirmação não chega | Veja a caixa de spam. |

## Trocar o serviço de login (ou adaptar a uma mudança do Firebase)

O sistema foi organizado para que o Firebase fique em poucos arquivos. Se o Firebase mudar a forma de uso
(nova versão do SDK, por exemplo) ou se um dia ele for trocado por outro serviço (Auth0, Keycloak, Amazon Cognito...),
só estes arquivos mudam:

| Onde | Arquivo | O que faz |
|---|---|---|
| Telas | `static/js/autenticacao.js` | Entrar, criar conta, confirmar e-mail, esqueci a senha, SMS, sair |
| Back-end | `config/FirebaseConfig.java` | Valida o token (assinatura, emissor e projeto) |
| Back-end | `security/LeitorDeIdentidadeFirebase.java` | Lê o token e devolve uma `Identidade` |
| Back-end | `service/conta/*GerenciadorContas.java` | Ações do GM nas contas (redefinir senha, desativar, excluir) |
| Configuração | `config/FirebaseProperties.java`, `controller/publico/ConfigController.java` | Chaves lidas do `application.properties` e enviadas às telas |

O resto do sistema (services, controllers, telas de gestão) não conhece o Firebase e não precisa mudar.

**Para atualizar a versão do SDK do Firebase:** troque o número `10.12.2` nas duas linhas `import` de
`static/js/autenticacao.js` (é o único lugar).

**Detalhe que fica:** a coluna `usuarios.firebase_uid` guarda o id da pessoa no serviço de login. Com outro serviço
ela continua servindo (é só um id); trocar o nome da coluna exigiria uma migração nova, e não é necessário.
