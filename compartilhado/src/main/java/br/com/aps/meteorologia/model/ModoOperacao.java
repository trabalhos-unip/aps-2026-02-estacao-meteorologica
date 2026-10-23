package br.com.aps.meteorologia.model;

/**
 * Origem de uma leitura: consulta real à API ou cenário simulado para testes/demonstração.
 * Gravado no banco como texto ("REAL" ou "SIMULADO").
 */
public enum ModoOperacao {

    /** Dados obtidos da API Open-Meteo. */
    REAL,

    /** Dados gerados pelo simulador do coletor (cenários de teste). */
    SIMULADO;

    /**
     * Converte um texto (ex.: parâmetro da URL) em modo, sem diferenciar maiúsculas/minúsculas.
     *
     * @return o modo, ou {@code null} se o texto estiver vazio (= todos os modos)
     * @throws IllegalArgumentException se o texto não for REAL nem SIMULADO
     */
    public static ModoOperacao deTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Modo inválido: '" + texto + "' (use REAL ou SIMULADO)");
        }
    }
}
