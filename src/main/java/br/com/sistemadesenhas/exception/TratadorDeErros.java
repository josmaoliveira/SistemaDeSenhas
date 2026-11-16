package br.com.sistemadesenhas.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transforma exceções em respostas JSON padronizadas: { status, codigo, mensagem }.
 * Assim todas as telas tratam erro do mesmo jeito.
 */
@Slf4j
@RestControllerAdvice
public class TratadorDeErros {

    /** Erros das regras de negócio (lançados de propósito no código). */
    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<Map<String, Object>> negocio(ErroNegocio e) {
        return resposta(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    /** Campos com @NotBlank, @Size etc. que vieram errados. Junta todas as mensagens. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(" "));
        return resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem);
    }

    /** JSON mal formado (ex.: texto onde deveria ser número). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "JSON_INVALIDO", "Dados enviados em formato inválido.");
    }

    /** Violação de regra do banco (ex.: e-mail repetido). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException e) {
        log.warn("Violação de integridade no banco: {}", e.getMostSpecificCause().getMessage());
        return resposta(HttpStatus.CONFLICT, "CONFLITO",
                "Não foi possível salvar: já existe um registro com esses dados ou há registros ligados a ele.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AccessDeniedException e) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Você não tem permissão para isso.");
    }

    /** Arquivo/rota inexistente: deixa o 404 normal. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> naoExiste(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Endereço não encontrado.");
    }

    /** Qualquer outro erro: registra no log e não expõe detalhes técnicos para o navegador. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente.");
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String codigo, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status.value());
        corpo.put("codigo", codigo);
        corpo.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
