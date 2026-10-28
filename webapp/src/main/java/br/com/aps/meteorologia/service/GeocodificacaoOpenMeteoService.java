package br.com.aps.meteorologia.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.List;

/** Resolve exclusivamente municípios brasileiros pela API de geocodificação da Open-Meteo. */
@Service
public class GeocodificacaoOpenMeteoService {
    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("AC", "Acre"), Map.entry("AL", "Alagoas"), Map.entry("AP", "Amapá"),
            Map.entry("AM", "Amazonas"), Map.entry("BA", "Bahia"), Map.entry("CE", "Ceará"),
            Map.entry("DF", "Distrito Federal"), Map.entry("ES", "Espírito Santo"),
            Map.entry("GO", "Goiás"), Map.entry("MA", "Maranhão"), Map.entry("MT", "Mato Grosso"),
            Map.entry("MS", "Mato Grosso do Sul"), Map.entry("MG", "Minas Gerais"), Map.entry("PA", "Pará"),
            Map.entry("PB", "Paraíba"), Map.entry("PR", "Paraná"), Map.entry("PE", "Pernambuco"),
            Map.entry("PI", "Piauí"), Map.entry("RJ", "Rio de Janeiro"), Map.entry("RN", "Rio Grande do Norte"),
            Map.entry("RS", "Rio Grande do Sul"), Map.entry("RO", "Rondônia"), Map.entry("RR", "Roraima"),
            Map.entry("SC", "Santa Catarina"), Map.entry("SP", "São Paulo"), Map.entry("SE", "Sergipe"),
            Map.entry("TO", "Tocantins"));
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    public GeocodificacaoOpenMeteoService(ObjectMapper json) { this.json = json; }

    public Localizacao localizarBrasil(String cidade, String estado) {
        if (cidade == null || cidade.isBlank()) throw new IllegalArgumentException("Informe a cidade");
        if (estado == null || !estado.trim().matches("(?i)[a-z]{2}")) throw new IllegalArgumentException("Informe a sigla do estado, por exemplo SP");
        String uf = estado.trim().toUpperCase();
        String nomeEstado = ESTADOS.get(uf);
        if (nomeEstado == null) throw new IllegalArgumentException("Informe uma UF brasileira válida");
        String consulta = cidade.trim() + ", " + uf + ", Brasil";
        try {
            URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?count=10&language=pt&name=" + URLEncoder.encode(consulta, StandardCharsets.UTF_8));
            HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) throw new IllegalArgumentException("Não foi possível localizar a cidade agora");
            Resultado resultado = json.readValue(resposta.body(), Resultado.class);
            if (resultado.results == null) throw new IllegalArgumentException("Cidade não encontrada no Brasil");
            return resultado.results.stream().filter(r -> "BR".equalsIgnoreCase(r.countryCode))
                    .filter(r -> nomeEstado.equalsIgnoreCase(r.admin1))
                    .findFirst().map(r -> new Localizacao(r.name, uf, r.latitude, r.longitude, r.timezone))
                    .orElseThrow(() -> new IllegalArgumentException("Cidade não encontrada para o estado informado"));
        } catch (IllegalArgumentException e) { throw e;
        } catch (Exception e) { throw new IllegalArgumentException("Falha ao consultar a localização na Open-Meteo"); }
    }

    /** Sugestões para o Select2, sempre limitadas a localidades brasileiras. */
    public List<Sugestao> buscarSugestoesBrasil(String termo, String estado) {
        if (termo == null || termo.trim().length() < 2) return List.of();
        String uf = estado == null ? "" : estado.trim().toUpperCase();
        String nomeEstado = uf.isEmpty() ? null : ESTADOS.get(uf);
        if (!uf.isEmpty() && nomeEstado == null) return List.of();
        try {
            URI uri = URI.create("https://geocoding-api.open-meteo.com/v1/search?count=10&language=pt&name="
                    + URLEncoder.encode(termo.trim(), StandardCharsets.UTF_8));
            HttpResponse<String> resposta = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) return List.of();
            Resultado resultado = json.readValue(resposta.body(), Resultado.class);
            if (resultado.results == null) return List.of();
            return resultado.results.stream()
                    .filter(r -> "BR".equalsIgnoreCase(r.countryCode))
                    .filter(r -> nomeEstado == null || nomeEstado.equalsIgnoreCase(r.admin1))
                    .map(r -> new Sugestao(r.name, siglaDoEstado(r.admin1), r.admin1))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String siglaDoEstado(String nome) {
        return ESTADOS.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(nome))
                .map(Map.Entry::getKey).findFirst().orElse("");
    }

    public record Localizacao(String cidade, String estado, double latitude, double longitude, String fusoHorario) {}
    public record Sugestao(String cidade, String estado, String nomeEstado) {}
    @JsonIgnoreProperties(ignoreUnknown = true) static class Resultado { public java.util.List<Item> results; }
    @JsonIgnoreProperties(ignoreUnknown = true) static class Item { public String name, admin1, timezone; @com.fasterxml.jackson.annotation.JsonProperty("country_code") public String countryCode; public double latitude, longitude; }
}
