package br.edu.unip.aps.meteorologia.dto;

import java.time.LocalDateTime;

/** Corpo JSON devolvido quando ocorre um erro, para a página exibir uma mensagem adequada. */
public record ErroResposta(int status, String erro, LocalDateTime dataHora) {

    public static ErroResposta de(int status, String erro) {
        return new ErroResposta(status, erro, LocalDateTime.now());
    }
}
