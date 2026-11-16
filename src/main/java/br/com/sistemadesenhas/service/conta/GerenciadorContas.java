package br.com.sistemadesenhas.service.conta;

/**
 * PADRÃO STRATEGY: operações nas contas de login (que ficam no Firebase).
 * <p>
 * Existem duas "estratégias" (implementações), escolhidas na inicialização pelo {@link ContasConfig}:
 * <ul>
 *   <li>{@link FirebaseRestGerenciadorContas}: usa só a chave pública da API. Consegue apenas
 *       ENVIAR o e-mail de redefinição de senha.</li>
 *   <li>{@link FirebaseAdminGerenciadorContas}: usa a conta de serviço (firebase.credenciais).
 *       Consegue também trocar a senha direto, desativar e excluir contas.</li>
 * </ul>
 * Quem usa (o GmUsuarioService) não sabe qual das duas está ativa: só conversa com esta interface.
 * Amanhã dá para trocar o Firebase por outro provedor criando uma nova implementação, sem mexer no resto.
 */
public interface GerenciadorContas {

    /** @return nome da estratégia ativa (aparece no painel do GM) */
    String nome();

    /** @return true se esta estratégia consegue trocar senha, desativar e excluir contas */
    boolean suportaAlteracaoDireta();

    /** Envia para o e-mail o link padrão do Firebase para criar uma nova senha. */
    void enviarEmailRedefinicaoSenha(String email);

    /** Define uma nova senha para a conta (exige {@link #suportaAlteracaoDireta()}). */
    void definirSenha(String uid, String novaSenha);

    /** Ativa/desativa a conta no Firebase (exige {@link #suportaAlteracaoDireta()}). */
    void definirAtiva(String uid, boolean ativa);

    /** Exclui a conta no Firebase (exige {@link #suportaAlteracaoDireta()}). */
    void excluirConta(String uid);
}
