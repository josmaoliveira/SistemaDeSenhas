package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.config.AppProperties;
import br.com.sistemadesenhas.dto.CadastroEmpresaRequest;
import br.com.sistemadesenhas.dto.EmpresaGmRequest;
import br.com.sistemadesenhas.dto.EmpresaGmResponse;
import br.com.sistemadesenhas.entity.Empresa;
import br.com.sistemadesenhas.entity.enums.Perfil;
import br.com.sistemadesenhas.entity.enums.StatusEmpresa;
import br.com.sistemadesenhas.entity.Usuario;
import br.com.sistemadesenhas.exception.ErroNegocio;
import br.com.sistemadesenhas.repository.EmpresaRepository;
import br.com.sistemadesenhas.repository.UsuarioRepository;
import br.com.sistemadesenhas.security.Identidade;
import br.com.sistemadesenhas.util.Documentos;
import br.com.sistemadesenhas.util.Slugs;
import br.com.sistemadesenhas.util.Telefones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cadastro de empresas (feito pelo cliente) e manutenção dos dados da empresa (feita pelo GM).
 */
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final SerialService serialService;
    private final LicencaService licencaService;
    private final AppProperties app;

    /**
     * Cadastra uma empresa nova e transforma quem está logado no ADMINISTRADOR dela.
     * <p>
     * Passos: valida CPF/CNPJ, garante que a pessoa ainda não tem cadastro, cria a empresa com
     * os dias grátis (app.licenca.dias-gratis), gera o serial a partir do CNPJ e cria o usuário admin.
     *
     * @param identidade quem está se cadastrando (vem do login)
     * @return a empresa criada
     */
    @Transactional
    public Empresa cadastrar(CadastroEmpresaRequest dados, Identidade identidade) {
        if (identidade.emailPendente()) {
            throw ErroNegocio.proibido("EMAIL_NAO_VERIFICADO", "Confirme seu e-mail antes de cadastrar a empresa.");
        }
        String email = identidade.email();
        if (email == null) {
            throw ErroNegocio.invalido("SEM_EMAIL", "Para cadastrar uma empresa entre com e-mail ou Google.");
        }
        if (usuarioRepository.findByFirebaseUid(identidade.uid()).isPresent()
                || usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ErroNegocio.conflito("JA_CADASTRADO", "Este e-mail já está cadastrado no sistema. Faça login.");
        }

        String cnpj = validarCnpj(dados.cnpj(), null);
        String cpf = validarCpf(dados.cpfResponsavel());

        // O telefone vale como "verificado" se for o mesmo que a pessoa confirmou por SMS no login
        String telefone = Telefones.normalizar(dados.telefone());
        String telefoneSms = identidade.telefone();
        if (telefone == null && telefoneSms != null) {
            telefone = telefoneSms;
        }

        Empresa empresa = new Empresa();
        empresa.setRazaoSocial(dados.razaoSocial().trim());
        empresa.setNomeFantasia(dados.nomeFantasia().trim());
        empresa.setCnpj(cnpj);
        empresa.setCpfResponsavel(cpf);
        empresa.setNomeResponsavel(dados.nomeResponsavel().trim());
        empresa.setEmail(email);
        empresa.setTelefone(telefone);
        empresa.setTelefoneVerificado(telefone != null && telefone.equals(telefoneSms));
        empresa.setSlug(gerarSlugUnico(dados.nomeFantasia()));
        empresa.setVersaoSerial(1);
        empresa.setSerial(serialService.gerar(cnpj, 1));
        empresa.setStatus(StatusEmpresa.ATIVA);
        empresa.setEmPeriodoGratis(true);
        empresa.setDataCadastro(LocalDateTime.now());
        empresa.setDataVencimento(LocalDate.now().plusDays(app.licenca().diasGratis()));
        empresaRepository.save(empresa);

        Usuario admin = new Usuario();
        admin.setEmpresa(empresa);
        admin.setFirebaseUid(identidade.uid());
        admin.setNome(dados.nomeResponsavel().trim());
        admin.setEmail(email);
        // Telefone único entre usuários: só guarda se ninguém mais usa
        if (telefoneSms != null && !usuarioRepository.existsByTelefone(telefoneSms)) {
            admin.setTelefone(telefoneSms);
        }
        admin.setPerfil(Perfil.ADMIN_EMPRESA);
        admin.setAtivo(true);
        admin.setDataCadastro(LocalDateTime.now());
        usuarioRepository.save(admin);

        return empresa;
    }

    /** GM: corrige os dados cadastrais. Se o CNPJ mudar, o serial é recalculado. */
    @Transactional
    public Empresa atualizar(Long id, EmpresaGmRequest dados) {
        Empresa empresa = buscar(id);
        String cnpj = validarCnpj(dados.cnpj(), empresa);
        boolean cnpjMudou = !cnpj.equals(empresa.getCnpj());

        empresa.setRazaoSocial(dados.razaoSocial().trim());
        empresa.setNomeFantasia(dados.nomeFantasia().trim());
        empresa.setCnpj(cnpj);
        empresa.setCpfResponsavel(validarCpf(dados.cpfResponsavel()));
        empresa.setNomeResponsavel(dados.nomeResponsavel().trim());
        empresa.setEmail(dados.email().trim().toLowerCase());
        String telefone = Telefones.normalizar(dados.telefone());
        if (telefone == null || !telefone.equals(empresa.getTelefone())) {
            empresa.setTelefoneVerificado(false);
        }
        empresa.setTelefone(telefone);
        if (cnpjMudou) {
            empresa.setSerial(serialService.gerar(cnpj, empresa.getVersaoSerial()));
        }
        return empresaRepository.save(empresa);
    }

    /** GM: gera um serial novo (o antigo deixa de valer). */
    @Transactional
    public Empresa gerarNovoSerial(Long id) {
        Empresa empresa = buscar(id);
        empresa.setVersaoSerial(empresa.getVersaoSerial() + 1);
        empresa.setSerial(serialService.gerar(empresa.getCnpj(), empresa.getVersaoSerial()));
        return empresaRepository.save(empresa);
    }

    /** GM: bloqueia ou desbloqueia a empresa. */
    @Transactional
    public Empresa alterarStatus(Long id, StatusEmpresa status) {
        Empresa empresa = buscar(id);
        empresa.setStatus(status);
        return empresaRepository.save(empresa);
    }

    /** GM: define manualmente a data de vencimento (ex.: dar mais dias de teste). */
    @Transactional
    public Empresa alterarVencimento(Long id, LocalDate novaData) {
        Empresa empresa = buscar(id);
        empresa.setDataVencimento(novaData);
        return empresaRepository.save(empresa);
    }

    /** GM: define uma mensalidade própria para a empresa. null = volta a pagar o valor padrão. */
    @Transactional
    public Empresa alterarValorMensal(Long id, BigDecimal valor) {
        Empresa empresa = buscar(id);
        empresa.setValorMensal(valor);
        return empresaRepository.save(empresa);
    }

    /** GM: exclui a empresa e, pelo ON DELETE CASCADE do banco, TODOS os dados dela. */
    @Transactional
    public void excluir(Long id) {
        empresaRepository.delete(buscar(id));
    }

    public Empresa buscar(Long id) {
        return empresaRepository.findById(id).orElseThrow(() -> ErroNegocio.naoEncontrado("Empresa"));
    }

    /** Converte para a resposta do painel do GM (inclui a situação calculada da licença). */
    public EmpresaGmResponse paraGm(Empresa e) {
        return new EmpresaGmResponse(e.getId(), e.getRazaoSocial(), e.getNomeFantasia(), e.getCnpj(),
                e.getCpfResponsavel(), e.getNomeResponsavel(), e.getEmail(), e.getTelefone(), e.isTelefoneVerificado(),
                e.getSlug(), e.getSerial(), e.getStatus(), licencaService.situacao(e), e.isEmPeriodoGratis(),
                e.getDataCadastro(), e.getDataVencimento(), licencaService.diasRestantes(e),
                licencaService.valorMensal(e), e.getValorMensal() != null);
    }

    // ------------------------------------------------------------------ auxiliares

    /** Valida o CNPJ e confere se já não existe em OUTRA empresa. Devolve só os números. */
    private String validarCnpj(String valor, Empresa atual) {
        String cnpj = Documentos.somenteNumeros(valor);
        if (!Documentos.cnpjValido(cnpj)) {
            throw ErroNegocio.invalido("CNPJ_INVALIDO", "CNPJ inválido. Confira os números.");
        }
        boolean mesmoDaEmpresaAtual = atual != null && cnpj.equals(atual.getCnpj());
        if (!mesmoDaEmpresaAtual && empresaRepository.existsByCnpj(cnpj)) {
            throw ErroNegocio.conflito("CNPJ_JA_CADASTRADO", "Este CNPJ já está cadastrado.");
        }
        return cnpj;
    }

    private String validarCpf(String valor) {
        String cpf = Documentos.somenteNumeros(valor);
        if (!Documentos.cpfValido(cpf)) {
            throw ErroNegocio.invalido("CPF_INVALIDO", "CPF inválido. Confira os números.");
        }
        return cpf;
    }

    /** "Padaria Central" -> "padaria-central"; se já existir, tenta "padaria-central-2", "-3"... */
    private String gerarSlugUnico(String nomeFantasia) {
        String base = Slugs.gerar(nomeFantasia);
        String slug = base;
        int numero = 2;
        while (empresaRepository.existsBySlug(slug)) {
            slug = base + "-" + numero++;
        }
        return slug;
    }
}
