package br.com.sistemadesenhas.controller.empresa;

import br.com.sistemadesenhas.dto.MembroEquipeRequest;
import br.com.sistemadesenhas.dto.UsuarioResponse;
import br.com.sistemadesenhas.service.EquipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Atendentes e administradores da empresa (perfil ADMIN_EMPRESA). */
@RestController
@RequestMapping("/api/empresa/equipe")
@RequiredArgsConstructor
public class EquipeController {

    private final EquipeService equipeService;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return equipeService.listar().stream().map(UsuarioResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse adicionar(@RequestBody @Valid MembroEquipeRequest dados) {
        return UsuarioResponse.de(equipeService.adicionar(dados));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody @Valid MembroEquipeRequest dados) {
        return UsuarioResponse.de(equipeService.atualizar(id, dados));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        equipeService.remover(id);
    }
}
