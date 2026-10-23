package br.com.aps.meteorologia.util;

/**
 * Traduz o código WMO ({@code weather_code}) da Open-Meteo para uma descrição em português.
 * Tabela de referência: documentação da Forecast API da Open-Meteo.
 */
public final class CodigoClimaWMO {

    private CodigoClimaWMO() {
    }

    public static String descrever(Integer codigo) {
        if (codigo == null) {
            return "Sem informação";
        }
        return switch (codigo) {
            case 0 -> "Céu limpo";
            case 1 -> "Predominantemente limpo";
            case 2 -> "Parcialmente nublado";
            case 3 -> "Nublado";
            case 45, 48 -> "Neblina";
            case 51, 53, 55 -> "Garoa";
            case 56, 57 -> "Garoa congelante";
            case 61, 63, 65 -> "Chuva";
            case 66, 67 -> "Chuva congelante";
            case 71, 73, 75, 77 -> "Neve";
            case 80, 81, 82 -> "Pancadas de chuva";
            case 85, 86 -> "Pancadas de neve";
            case 95 -> "Trovoada";
            case 96, 99 -> "Trovoada com granizo";
            default -> "Código desconhecido (" + codigo + ")";
        };
    }
}
