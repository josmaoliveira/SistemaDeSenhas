package br.com.sistemadesenhas.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Erro "esperado" das regras do sistema (ex.: guichê não encontrado, senha já em atendimento).
 * <p>
 * Quando é lançado, o {@link TratadorDeErros} devolve ao navegador um JSON assim:
 * <pre>{ "status": 404, "codigo": "NAO_ENCONTRADO", "mensagem": "Guichê não encontrado(a)." }</pre>
 * O {@code codigo} serve para o JavaScript tomar decisões; a {@code mensagem} é para mostrar ao usuário.
 */
@Getter
public class ErroNegocio extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ErroNegocio(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    /** 404 - o registro não existe. */
    public static ErroNegocio naoEncontrado(String oQue) {
        return new ErroNegocio(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", oQue + " não encontrado(a).");
    }

    /** 400 - dado enviado está errado. */
    public static ErroNegocio invalido(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }

    /** 409 - conflito com algo que já existe. */
    public static ErroNegocio conflito(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.CONFLICT, codigo, mensagem);
    }

    /** 403 - usuário identificado, mas sem permissão. */
    public static ErroNegocio proibido(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.FORBIDDEN, codigo, mensagem);
    }
}
