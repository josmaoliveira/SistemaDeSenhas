package br.com.sistemadesenhas.service;

import br.com.sistemadesenhas.entity.Senha;

/** Abstração da impressão do ticket (o ImpressaoService só conhece esta interface). */
public interface ReportPrinter {

    void printSenha(Senha senha);
}
