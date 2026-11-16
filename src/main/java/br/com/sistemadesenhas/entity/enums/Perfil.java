package br.com.sistemadesenhas.entity.enums;

/**
 * Perfis de acesso. Cada perfil enxerga telas diferentes:
 * <ul>
 *   <li>{@link #GM}: administrador geral do sistema (dono do software). Vê todas as empresas.</li>
 *   <li>{@link #ADMIN_EMPRESA}: administrador de uma empresa. Cadastra serviços, guichês e atendentes.</li>
 *   <li>{@link #ATENDENTE}: opera um guichê chamando senhas.</li>
 * </ul>
 */
public enum Perfil {
    GM,
    ADMIN_EMPRESA,
    ATENDENTE
}
