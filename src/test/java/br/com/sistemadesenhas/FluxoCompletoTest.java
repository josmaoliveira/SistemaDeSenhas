package br.com.sistemadesenhas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.sistemadesenhas.service.PreenchimentoDeSeriais;
import br.com.sistemadesenhas.suporte.TokensDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Testa o sistema de ponta a ponta, chamando a API como o navegador faria:
 * cadastro de empresa, login, equipe, fila de senhas, ISOLAMENTO entre empresas, licença e painel do GM.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TokensDeTeste.class)
class FluxoCompletoTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PreenchimentoDeSeriais preenchimentoDeSeriais;

    // Tokens das pessoas do teste
    final String adminA = TokensDeTeste.email("uid-admin-a", "admin@empresa-a.com", true);
    final String adminB = TokensDeTeste.google("uid-admin-b", "admin@empresa-b.com");
    final String gm = TokensDeTeste.email("uid-gm", "gm@teste.com", true);

    /** Cada teste começa com o banco vazio. */
    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE empresas, usuarios, registros_login, pagamentos, servicos, prioridades, "
                + "guiches, senhas, atendimentos RESTART IDENTITY CASCADE");
        jdbc.update("UPDATE configuracoes SET valor_mensal_padrao = 99.90");
    }

    // ------------------------------------------------------------------ cadastro e login

    @Test
    void cadastroDeEmpresaCriaAdminComNoventaDiasESerial() throws Exception {
        JsonNode cadastro = cadastrarEmpresa(adminA, "Padaria Central", "11222333000181");

        assertThat(cadastro.at("/empresa/slug").asText()).isEqualTo("padaria-central");
        assertThat(cadastro.at("/licenca/situacao").asText()).isEqualTo("EM_TESTE");
        assertThat(cadastro.at("/licenca/diasRestantes").asLong()).isEqualTo(90);
        assertThat(cadastro.at("/licenca/dataVencimento").asText()).isEqualTo(LocalDate.now().plusDays(90).toString());
        assertThat(cadastro.at("/licenca/serial").asText()).matches("[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}");

        JsonNode me = corpo(chamar(get("/api/auth/me?login=true"), adminA, 200));
        assertThat(me.get("perfil").asText()).isEqualTo("ADMIN_EMPRESA");
        assertThat(me.at("/empresa/nomeFantasia").asText()).isEqualTo("Padaria Central");
        assertThat(me.at("/licenca/liberada").asBoolean()).isTrue();

        // O login ficou registrado no histórico (com o provedor usado)
        assertThat(jdbc.queryForObject("SELECT provedor FROM registros_login", String.class)).isEqualTo("password");
    }

    @Test
    void validacoesDoCadastro() throws Exception {
        String naoVerificado = TokensDeTeste.email("uid-x", "x@x.com", false);
        assertThat(codigo(chamar(get("/api/auth/me"), naoVerificado, 403))).isEqualTo("EMAIL_NAO_VERIFICADO");
        chamar(post("/api/cadastro/empresa").content(dadosEmpresa("X", "11222333000181")), naoVerificado, 403);

        // Sem cadastro ainda: 404 SEM_CADASTRO (a tela leva para o cadastro)
        assertThat(codigo(chamar(get("/api/auth/me"), adminA, 404))).isEqualTo("SEM_CADASTRO");

        // CNPJ inválido e CNPJ repetido
        assertThat(codigo(chamar(post("/api/cadastro/empresa").content(dadosEmpresa("A", "11222333000182")), adminA, 400)))
                .isEqualTo("CNPJ_INVALIDO");
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        assertThat(codigo(chamar(post("/api/cadastro/empresa").content(dadosEmpresa("B", "11.222.333/0001-81")), adminB, 409)))
                .isEqualTo("CNPJ_JA_CADASTRADO");
        // Quem já tem cadastro não cadastra outra empresa
        chamar(post("/api/cadastro/empresa").content(dadosEmpresa("A2", "45723174000110")), adminA, 409);
    }

    @Test
    void atendenteConvidadoFazPrimeiroAcessoPorEmailOuSms() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        chamar(post("/api/empresa/equipe").content(texto(Map.of(
                "nome", "Joao", "email", "joao@empresa-a.com", "perfil", "ATENDENTE"))), adminA, 201);
        chamar(post("/api/empresa/equipe").content(texto(Map.of(
                "nome", "Maria", "email", "maria@empresa-a.com", "telefone", "(83) 98888-7777", "perfil", "ATENDENTE"))), adminA, 201);

        // João entra pela primeira vez com e-mail/senha: é vinculado ao cadastro feito pelo admin
        String joao = TokensDeTeste.email("uid-joao", "joao@empresa-a.com", true);
        assertThat(corpo(chamar(get("/api/auth/me"), joao, 200)).get("perfil").asText()).isEqualTo("ATENDENTE");

        // Maria entra por SMS: é encontrada pelo telefone
        String maria = TokensDeTeste.sms("uid-maria", "+5583988887777");
        assertThat(corpo(chamar(get("/api/auth/me"), maria, 200)).get("nome").asText()).isEqualTo("Maria");

        // Atendente lista guichês, mas não mexe nos cadastros nem no painel do GM
        chamar(get("/api/empresa/guiches"), joao, 200);
        chamar(post("/api/empresa/servicos").content(texto(Map.of("nome", "X", "sigla", "X"))), joao, 403);
        chamar(get("/api/empresa/equipe"), joao, 403);
        chamar(get("/api/gm/empresas"), joao, 403);

        // Admin desativa o João: ele perde o acesso
        long idJoao = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = 'joao@empresa-a.com'", Long.class);
        chamar(put("/api/empresa/equipe/" + idJoao).content(texto(Map.of(
                "nome", "Joao", "email", "joao@empresa-a.com", "perfil", "ATENDENTE", "ativo", false))), adminA, 200);
        assertThat(codigo(chamar(get("/api/auth/me"), joao, 403))).isEqualTo("USUARIO_INATIVO");
        chamar(get("/api/empresa/guiches"), joao, 403);
    }

    // ------------------------------------------------------------------ fila

    @Test
    void filaRespeitaPrioridadeEOrdemDeChegada() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        long servico = criarServico(adminA, "Caixa", "CX");
        long normal = id(chamar(post("/api/empresa/prioridades").content(texto(Map.of("nome", "Normal", "peso", 1))), adminA, 201));
        long preferencial = id(chamar(post("/api/empresa/prioridades").content(texto(Map.of("nome", "Preferencial", "peso", 2))), adminA, 201));
        long guiche = criarGuiche(adminA, servico);

        assertThat(gerarSenha("empresa-a", servico, normal)).isEqualTo("CX001");
        assertThat(gerarSenha("empresa-a", servico, normal)).isEqualTo("CX002");
        assertThat(gerarSenha("empresa-a", servico, preferencial)).isEqualTo("CX003");

        assertThat(chamarProxima(adminA, guiche)).isEqualTo("CX003"); // preferencial passa na frente
        assertThat(chamarProxima(adminA, guiche)).isEqualTo("CX001");
        assertThat(chamarProxima(adminA, guiche)).isEqualTo("CX002");

        // O painel público mostra a senha em atendimento
        JsonNode painel = corpo(chamar(get("/api/publico/empresa-a/painel"), null, 200));
        assertThat(painel.get(0).get("codigo").asText()).isEqualTo("CX002");

        // Fila vazia: finaliza a atual e responde 204
        chamar(post("/api/atendimentos/chamar/" + guiche), adminA, 204);
        assertThat(corpo(chamar(get("/api/publico/empresa-a/painel"), null, 200))).isEmpty();
    }

    // ------------------------------------------------------------------ relatório do dia

    @Test
    void relatorioDoDiaMostraGuichesLigadosQuantidadesETempoMedio() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        cadastrarEmpresa(adminB, "Empresa B", "45723174000110");
        long servico = criarServico(adminA, "Caixa", "CX");
        long normal = id(chamar(post("/api/empresa/prioridades").content(texto(Map.of("nome", "Normal", "peso", 1))), adminA, 201));
        long guiche1 = criarGuiche(adminA, servico);
        long guiche2 = id(chamar(post("/api/empresa/guiches").content(texto(Map.of("nome", "Guiche 2", "numero", 2, "servicoId", servico))), adminA, 201));
        String atendente = cadastrarAtendente(adminA, "joao@empresa-a.com");
        for (int i = 0; i < 3; i++) {
            gerarSenha("empresa-a", servico, normal);
        }

        // Sem ninguém nos guichês: tudo zerado
        JsonNode vazio = corpo(chamar(get("/api/empresa/relatorios/atendimento-hoje"), adminA, 200));
        assertThat(vazio.get("totalGuiches").asInt()).isEqualTo(2);
        assertThat(vazio.get("guichesLigados").asLong()).isZero();
        assertThat(vazio.get("totalAtendimentos").asLong()).isZero();
        assertThat(vazio.get("tempoMedioSegundos").isNull()).isTrue();

        // O atendente abre a tela do guichê 1, atende uma senha e chama a próxima
        chamar(post("/api/atendimentos/presenca/" + guiche1), atendente, 204);
        chamarProxima(atendente, guiche1);
        chamarProxima(atendente, guiche1); // finaliza a primeira e abre a segunda

        JsonNode hoje = corpo(chamar(get("/api/empresa/relatorios/atendimento-hoje"), adminA, 200));
        assertThat(hoje.get("guichesLigados").asLong()).isEqualTo(1);
        assertThat(hoje.get("guichesQueAtenderam").asLong()).isEqualTo(1);
        assertThat(hoje.get("totalAtendimentos").asLong()).isEqualTo(2);
        assertThat(hoje.get("emAndamento").asLong()).isEqualTo(1);
        assertThat(hoje.get("tempoMedioSegundos").asLong()).isGreaterThanOrEqualTo(0);
        JsonNode linha1 = hoje.get("guiches").get(0);
        assertThat(linha1.get("id").asLong()).isEqualTo(guiche1);
        assertThat(linha1.get("ligado").asBoolean()).isTrue();
        assertThat(linha1.get("atendendo").asBoolean()).isTrue();
        assertThat(linha1.get("atendente").asText()).isEqualTo("Atendente");
        assertThat(linha1.get("atendimentos").asLong()).isEqualTo(2);
        assertThat(hoje.get("guiches").get(1).get("atendimentos").asLong()).isZero();

        // Saiu do guichê: aparece desligado na hora
        chamar(delete("/api/atendimentos/presenca/" + guiche1), atendente, 204);
        assertThat(corpo(chamar(get("/api/empresa/relatorios/atendimento-hoje"), adminA, 200))
                .get("guichesLigados").asLong()).isZero();

        // Só o administrador vê o relatório, e cada empresa vê só os seus números
        chamar(get("/api/empresa/relatorios/atendimento-hoje"), atendente, 403);
        chamar(post("/api/atendimentos/presenca/" + guiche2), adminB, 404);
        JsonNode outraEmpresa = corpo(chamar(get("/api/empresa/relatorios/atendimento-hoje"), adminB, 200));
        assertThat(outraEmpresa.get("totalGuiches").asInt()).isZero();
        assertThat(outraEmpresa.get("totalAtendimentos").asLong()).isZero();
    }

    // ------------------------------------------------------------------ isolamento entre empresas

    @Test
    void umaEmpresaNaoEnxergaNemAlteraDadosDeOutra() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        cadastrarEmpresa(adminB, "Empresa B", "45723174000110");

        long servicoA = criarServico(adminA, "Caixa A", "CA");
        long prioridadeA = id(chamar(post("/api/empresa/prioridades").content(texto(Map.of("nome", "Normal", "peso", 1))), adminA, 201));
        long guicheA = criarGuiche(adminA, servicoA);
        gerarSenha("empresa-a", servicoA, prioridadeA);
        criarServico(adminB, "Caixa B", "CB");

        // Listas: cada um só vê o seu
        JsonNode servicosB = corpo(chamar(get("/api/empresa/servicos"), adminB, 200));
        assertThat(servicosB).hasSize(1);
        assertThat(servicosB.get(0).get("nome").asText()).isEqualTo("Caixa B");

        // B tentando usar ids da A: sempre "não encontrado"
        chamar(put("/api/empresa/servicos/" + servicoA).content(texto(Map.of("nome", "Hack", "sigla", "HK"))), adminB, 404);
        chamar(delete("/api/empresa/servicos/" + servicoA + "?confirmado=true"), adminB, 404);
        chamar(post("/api/atendimentos/chamar/" + guicheA), adminB, 404);
        chamar(get("/api/atendimentos/atual/" + guicheA), adminB, 404);
        // B não consegue criar guichê apontando para o serviço da A
        chamar(post("/api/empresa/guiches").content(texto(Map.of("nome", "G", "servicoId", servicoA))), adminB, 404);
        // Totem da B não gera senha para serviço da A
        chamar(post("/api/publico/empresa-b/senhas").content(texto(Map.of("servicoId", servicoA, "prioridadeId", prioridadeA))), null, 404);

        // O reset de fila da B não apaga as senhas da A
        chamar(delete("/api/empresa/senhas"), adminB, 204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM senhas", Long.class)).isEqualTo(1);

        // E o serviço da A continua intacto
        assertThat(corpo(chamar(get("/api/empresa/servicos"), adminA, 200)).get(0).get("nome").asText()).isEqualTo("Caixa A");
    }

    // ------------------------------------------------------------------ licença

    @Test
    void licencaVencidaOuBloqueadaParaOSistemaMasDeixaVerALicenca() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        String atendente = cadastrarAtendente(adminA, "joao@empresa-a.com");
        long servico = criarServico(adminA, "Caixa", "CX");

        // Vencida (venceu ontem): gestão, atendimento e totem param; a licença continua visível
        jdbc.update("UPDATE empresas SET data_vencimento = CURRENT_DATE - 1");
        assertThat(codigo(chamar(get("/api/empresa/servicos"), adminA, 402))).isEqualTo("LICENCA_VENCIDA");
        chamar(get("/api/empresa/guiches"), atendente, 402);
        chamar(get("/api/publico/empresa-a/servicos"), null, 402);
        JsonNode licenca = corpo(chamar(get("/api/empresa/licenca"), adminA, 200));
        assertThat(licenca.get("situacao").asText()).isEqualTo("VENCIDA");
        assertThat(licenca.get("liberada").asBoolean()).isFalse();
        // O login continua funcionando (para a tela mostrar o aviso)
        assertThat(corpo(chamar(get("/api/auth/me"), adminA, 200)).at("/licenca/situacao").asText()).isEqualTo("VENCIDA");

        // No dia do vencimento ainda funciona
        jdbc.update("UPDATE empresas SET data_vencimento = CURRENT_DATE");
        chamar(get("/api/empresa/servicos"), adminA, 200);

        // Bloqueada manualmente: para mesmo dentro do prazo
        jdbc.update("UPDATE empresas SET status = 'BLOQUEADA', data_vencimento = CURRENT_DATE + 30");
        assertThat(codigo(chamar(get("/api/empresa/servicos"), adminA, 402))).isEqualTo("EMPRESA_BLOQUEADA");
        chamar(post("/api/publico/empresa-a/senhas").content(texto(Map.of("servicoId", servico, "prioridadeId", 1))), null, 402);
    }

    @Test
    void empresasAntigasRecebemSerialAoSubir() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        String serialOriginal = jdbc.queryForObject("SELECT serial FROM empresas", String.class);
        jdbc.update("UPDATE empresas SET serial = NULL");

        preenchimentoDeSeriais.run(null);

        // O serial recalculado é o mesmo: depende só do CNPJ, da versão e do segredo
        assertThat(jdbc.queryForObject("SELECT serial FROM empresas", String.class)).isEqualTo(serialOriginal);
    }

    // ------------------------------------------------------------------ painel do GM

    @Test
    void gmControlaEmpresasPagamentosEBloqueio() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");

        // GM é criado no primeiro login (e-mail listado em app.gm-emails) e não tem empresa nem licença
        JsonNode meGm = corpo(chamar(get("/api/auth/me?login=true"), gm, 200));
        assertThat(meGm.get("perfil").asText()).isEqualTo("GM");
        assertThat(meGm.get("empresa").isNull()).isTrue();
        // Admin de empresa não acessa o painel do GM, e o GM não usa as rotas de empresa
        chamar(get("/api/gm/empresas"), adminA, 403);
        chamar(get("/api/empresa/servicos"), gm, 403);

        JsonNode empresas = corpo(chamar(get("/api/gm/empresas"), gm, 200));
        assertThat(empresas).hasSize(1);
        long idA = empresas.get(0).get("id").asLong();
        String serialAntigo = empresas.get(0).get("serial").asText();

        // Pagamento: sai do período grátis e soma 30 dias ao vencimento atual (90 + 30)
        chamar(post("/api/gm/empresas/" + idA + "/pagamentos").content(texto(Map.of(
                "valor", 99.90, "dias", 30, "forma", "PIX", "observacao", "Mensalidade"))), gm, 201);
        JsonNode depois = corpo(chamar(get("/api/gm/empresas/" + idA), gm, 200));
        assertThat(depois.get("situacao").asText()).isEqualTo("ATIVA");
        assertThat(depois.get("dataVencimento").asText()).isEqualTo(LocalDate.now().plusDays(120).toString());
        JsonNode pagamentos = corpo(chamar(get("/api/gm/pagamentos"), gm, 200));
        assertThat(pagamentos.get(0).get("registradoPor").asText()).isEqualTo("Administrador Geral");

        // Mensalidade: começa no valor padrão; o GM reajusta o padrão e pode dar um valor próprio à empresa
        assertThat(depois.get("valorMensal").decimalValue()).isEqualByComparingTo("99.90");
        assertThat(depois.get("valorMensalProprio").asBoolean()).isFalse();
        chamar(put("/api/gm/mensalidade").content(texto(Map.of("valorMensal", 0))), gm, 400);
        chamar(put("/api/gm/mensalidade").content(texto(Map.of("valorMensal", 119.90))), adminA, 403);
        chamar(put("/api/gm/mensalidade").content(texto(Map.of("valorMensal", 119.90))), gm, 200);
        assertThat(corpo(chamar(get("/api/empresa/licenca"), adminA, 200)).get("valorMensal").decimalValue()).isEqualByComparingTo("119.90");
        JsonNode propria = corpo(chamar(put("/api/gm/empresas/" + idA + "/valor-mensal").content(texto(Map.of("valorMensal", 79.90))), gm, 200));
        assertThat(propria.get("valorMensal").decimalValue()).isEqualByComparingTo("79.90");
        assertThat(propria.get("valorMensalProprio").asBoolean()).isTrue();
        Map<String, Object> voltarAoPadrao = new java.util.HashMap<>();
        voltarAoPadrao.put("valorMensal", null);
        JsonNode padrao = corpo(chamar(put("/api/gm/empresas/" + idA + "/valor-mensal").content(texto(voltarAoPadrao)), gm, 200));
        assertThat(padrao.get("valorMensal").decimalValue()).isEqualByComparingTo("119.90");
        assertThat(padrao.get("valorMensalProprio").asBoolean()).isFalse();

        // Corrigir dados: CNPJ inválido é recusado; CNPJ novo recalcula o serial
        Map<String, Object> dados = new java.util.HashMap<>(Map.of("razaoSocial", "Empresa A LTDA", "nomeFantasia", "Empresa A Nova",
                "cnpj", "00000000000000", "cpfResponsavel", "52998224725", "nomeResponsavel", "Resp", "email", "a@a.com"));
        chamar(put("/api/gm/empresas/" + idA).content(texto(dados)), gm, 400);
        dados.put("cnpj", "45723174000110");
        JsonNode corrigida = corpo(chamar(put("/api/gm/empresas/" + idA).content(texto(dados)), gm, 200));
        assertThat(corrigida.get("nomeFantasia").asText()).isEqualTo("Empresa A Nova");
        assertThat(corrigida.get("serial").asText()).isNotEqualTo(serialAntigo);

        // Novo serial para o mesmo CNPJ
        JsonNode novo = corpo(chamar(post("/api/gm/empresas/" + idA + "/novo-serial"), gm, 200));
        assertThat(novo.get("serial").asText()).isNotEqualTo(corrigida.get("serial").asText());

        // Bloqueio: a empresa para de funcionar, mas ainda vê a licença
        chamar(put("/api/gm/empresas/" + idA + "/status").content(texto(Map.of("status", "BLOQUEADA"))), gm, 200);
        assertThat(codigo(chamar(get("/api/empresa/servicos"), adminA, 402))).isEqualTo("EMPRESA_BLOQUEADA");
        chamar(get("/api/empresa/licenca"), adminA, 200);

        // Desbloqueia, mas com vencimento ontem: licença vencida
        chamar(put("/api/gm/empresas/" + idA + "/status").content(texto(Map.of("status", "ATIVA"))), gm, 200);
        chamar(put("/api/gm/empresas/" + idA + "/vencimento").content(texto(Map.of(
                "dataVencimento", LocalDate.now().minusDays(1).toString()))), gm, 200);
        assertThat(codigo(chamar(get("/api/empresa/servicos"), adminA, 402))).isEqualTo("LICENCA_VENCIDA");

        // Resumo e histórico de logins
        JsonNode resumo = corpo(chamar(get("/api/gm/resumo"), gm, 200));
        assertThat(resumo.get("totalEmpresas").asLong()).isEqualTo(1);
        assertThat(resumo.get("vencidas").asLong()).isEqualTo(1);
        assertThat(resumo.get("recebidoNoMes").decimalValue()).isEqualByComparingTo("99.90");
        assertThat(corpo(chamar(get("/api/gm/logins"), gm, 200))).hasSize(1);

        // Excluir a empresa apaga os usuários e pagamentos dela
        chamar(delete("/api/gm/empresas/" + idA), gm, 204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE perfil <> 'GM'", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pagamentos", Long.class)).isZero();
    }

    @Test
    void gmCorrigeDesativaEExcluiUsuariosDeQualquerEmpresa() throws Exception {
        cadastrarEmpresa(adminA, "Empresa A", "11222333000181");
        cadastrarEmpresa(adminB, "Empresa B", "45723174000110");
        String joao = cadastrarAtendente(adminA, "joao@empresa-a.com");
        chamar(get("/api/auth/me"), gm, 200);

        // Lista todos, ou só de uma empresa
        assertThat(corpo(chamar(get("/api/gm/usuarios"), gm, 200))).hasSize(4);
        long idEmpresaA = jdbc.queryForObject("SELECT id FROM empresas WHERE slug = 'empresa-a'", Long.class);
        assertThat(corpo(chamar(get("/api/gm/usuarios?empresaId=" + idEmpresaA), gm, 200))).hasSize(2);

        long idJoao = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = 'joao@empresa-a.com'", Long.class);
        long idGm = jdbc.queryForObject("SELECT id FROM usuarios WHERE perfil = 'GM'", Long.class);

        // Promove a administrador e depois desativa: ele perde o acesso
        chamar(put("/api/gm/usuarios/" + idJoao).content(texto(Map.of("nome", "João Silva", "email", "joao@empresa-a.com",
                "perfil", "ADMIN_EMPRESA", "ativo", true))), gm, 200);
        chamar(get("/api/empresa/equipe"), joao, 200);
        chamar(put("/api/gm/usuarios/" + idJoao).content(texto(Map.of("nome", "João Silva", "email", "joao@empresa-a.com",
                "perfil", "ADMIN_EMPRESA", "ativo", false))), gm, 200);
        assertThat(codigo(chamar(get("/api/auth/me"), joao, 403))).isEqualTo("USUARIO_INATIVO");

        // Regras: usuário de empresa não vira GM; GM não se desativa nem se exclui
        chamar(put("/api/gm/usuarios/" + idJoao).content(texto(Map.of("nome", "J", "email", "joao@empresa-a.com",
                "perfil", "GM", "ativo", true))), gm, 400);
        chamar(put("/api/gm/usuarios/" + idGm).content(texto(Map.of("nome", "GM", "email", "gm@teste.com",
                "perfil", "GM", "ativo", false))), gm, 400);
        chamar(delete("/api/gm/usuarios/" + idGm), gm, 400);

        // Sem a conta de serviço do Firebase, trocar senha direto é recusado com mensagem clara
        assertThat(corpo(chamar(get("/api/gm/capacidades"), gm, 200)).get("alteracaoDireta").asBoolean()).isFalse();
        assertThat(codigo(chamar(put("/api/gm/usuarios/" + idJoao + "/senha").content(texto(Map.of("novaSenha", "123456"))), gm, 400)))
                .isEqualTo("SEM_CREDENCIAIS_FIREBASE");

        chamar(delete("/api/gm/usuarios/" + idJoao), gm, 204);
        assertThat(corpo(chamar(get("/api/gm/usuarios"), gm, 200))).hasSize(3);
    }

    @Test
    void semTokenNaoAcessaAreaRestrita() throws Exception {
        chamar(get("/api/empresa/servicos"), null, 401);
        chamar(get("/api/gm/resumo"), null, 401);
        chamar(get("/api/auth/me"), "token-falso", 401);
        chamar(get("/api/publico/config"), null, 200);
    }

    // ------------------------------------------------------------------ auxiliares

    private JsonNode cadastrarEmpresa(String token, String nome, String cnpj) throws Exception {
        return corpo(chamar(post("/api/cadastro/empresa").content(dadosEmpresa(nome, cnpj)), token, 201));
    }

    private String dadosEmpresa(String nome, String cnpj) throws Exception {
        return texto(Map.of("razaoSocial", nome + " LTDA", "nomeFantasia", nome, "cnpj", cnpj,
                "cpfResponsavel", "529.982.247-25", "nomeResponsavel", "Responsavel " + nome));
    }

    /** Admin cadastra o e-mail e a pessoa faz o primeiro acesso; devolve o token dela. */
    private String cadastrarAtendente(String tokenAdmin, String email) throws Exception {
        chamar(post("/api/empresa/equipe").content(texto(Map.of("nome", "Atendente", "email", email, "perfil", "ATENDENTE"))), tokenAdmin, 201);
        String token = TokensDeTeste.email("uid-" + email, email, true);
        chamar(get("/api/auth/me"), token, 200);
        return token;
    }

    private long criarServico(String token, String nome, String sigla) throws Exception {
        return id(chamar(post("/api/empresa/servicos").content(texto(Map.of("nome", nome, "sigla", sigla))), token, 201));
    }

    private long criarGuiche(String token, long servicoId) throws Exception {
        return id(chamar(post("/api/empresa/guiches").content(texto(Map.of("nome", "Guiche 1", "numero", 1, "servicoId", servicoId))), token, 201));
    }

    private String gerarSenha(String slug, long servico, long prioridade) throws Exception {
        return corpo(chamar(post("/api/publico/" + slug + "/senhas").content(texto(Map.of(
                "servicoId", servico, "prioridadeId", prioridade))), null, 201)).get("codigo").asText();
    }

    private String chamarProxima(String token, long guiche) throws Exception {
        return corpo(chamar(post("/api/atendimentos/chamar/" + guiche), token, 200)).at("/senha/codigo").asText();
    }

    /** Faz a chamada (com token, se houver) e confere o status HTTP esperado. */
    private MvcResult chamar(MockHttpServletRequestBuilder requisicao, String token, int statusEsperado) throws Exception {
        requisicao.contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        MvcResult resultado = mvc.perform(requisicao).andReturn();
        assertThat(resultado.getResponse().getStatus())
                .as("Status de %s %s. Resposta: %s", resultado.getRequest().getMethod(),
                        resultado.getRequest().getRequestURI(), resultado.getResponse().getContentAsString())
                .isEqualTo(statusEsperado);
        return resultado;
    }

    private JsonNode corpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long id(MvcResult r) throws Exception {
        return corpo(r).get("id").asLong();
    }

    private String codigo(MvcResult r) throws Exception {
        return corpo(r).get("codigo").asText();
    }

    private String texto(Object o) throws Exception {
        return json.writeValueAsString(o);
    }
}
