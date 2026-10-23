package br.com.aps.meteorologia.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Funções de data/hora usadas na coleta. */
public final class DataHoraUtil {

    /** Fuso usado na consulta à Open-Meteo (parâmetro timezone) e no horário da coleta. */
    public static final ZoneId FUSO_PADRAO = ZoneId.of("America/Sao_Paulo");

    private DataHoraUtil() {
    }

    /** Instante atual no fuso informado, com precisão de milissegundos (dataHoraColeta). */
    public static LocalDateTime agora(ZoneId fuso) {
        return LocalDateTime.now(fuso).truncatedTo(ChronoUnit.MILLIS);
    }

    /**
     * Converte o {@code current.time} da Open-Meteo (formato ISO local, ex.: "2026-10-20T22:15")
     * em {@link LocalDateTime} (dataHoraFonte).
     *
     * @return a data/hora, ou {@code null} se o texto estiver vazio
     * @throws IllegalArgumentException se o texto não estiver no formato esperado
     */
    public static LocalDateTime converterHorarioFonte(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Horário em formato inesperado: '" + texto + "'", e);
        }
    }
}
