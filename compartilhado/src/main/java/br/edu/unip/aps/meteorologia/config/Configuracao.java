package br.edu.unip.aps.meteorologia.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê as configurações do sistema (banco, coordenadas, intervalo de coleta...).
 *
 * <p>Ordem de prioridade de cada chave:
 * <ol>
 *   <li>propriedade de sistema ({@code -DCHAVE=valor});</li>
 *   <li>variável de ambiente;</li>
 *   <li>arquivo {@code .env} na pasta atual ou na pasta acima (raiz do projeto);</li>
 *   <li>valor padrão informado no código.</li>
 * </ol>
 * Assim as senhas ficam só no {@code .env}, que não vai para o GitHub.
 */
public final class Configuracao {

    private static final Map<String, String> ARQUIVO_ENV = carregarArquivoEnv();

    private Configuracao() {
    }

    public static String texto(String chave, String padrao) {
        String valor = System.getProperty(chave);
        if (valor == null) {
            valor = System.getenv(chave);
        }
        if (valor == null) {
            valor = ARQUIVO_ENV.get(chave);
        }
        return (valor == null || valor.isBlank()) ? padrao : valor.trim();
    }

    public static int inteiro(String chave, int padrao) {
        String valor = texto(chave, null);
        try {
            return valor == null ? padrao : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Configuração " + chave + " deve ser um número inteiro: " + valor, e);
        }
    }

    public static double decimal(String chave, double padrao) {
        String valor = texto(chave, null);
        try {
            return valor == null ? padrao : Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Configuração " + chave + " deve ser um número: " + valor, e);
        }
    }

    /** URL JDBC do MySQL montada a partir de MYSQL_HOST, MYSQL_PORT e MYSQL_DATABASE. */
    public static String urlBanco() {
        return "jdbc:mysql://" + texto("MYSQL_HOST", "localhost") + ":" + texto("MYSQL_PORT", "3306")
                + "/" + texto("MYSQL_DATABASE", "estacao_meteorologica") + "?serverTimezone=America/Sao_Paulo";
    }

    public static String usuarioBanco() {
        return texto("MYSQL_USER", "aps");
    }

    public static String senhaBanco() {
        return texto("MYSQL_PASSWORD", "");
    }

    /** Lê linhas no formato CHAVE=valor do .env, ignorando comentários (#) e linhas vazias. */
    private static Map<String, String> carregarArquivoEnv() {
        Map<String, String> valores = new HashMap<>();
        for (Path caminho : List.of(Path.of(".env"), Path.of("..", ".env"))) {
            if (!Files.isRegularFile(caminho)) {
                continue;
            }
            try {
                for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
                    String limpa = linha.trim();
                    int igual = limpa.indexOf('=');
                    if (limpa.isEmpty() || limpa.startsWith("#") || igual <= 0) {
                        continue;
                    }
                    valores.putIfAbsent(limpa.substring(0, igual).trim(), limpa.substring(igual + 1).trim());
                }
            } catch (IOException e) {
                System.err.println("Não foi possível ler " + caminho.toAbsolutePath() + ": " + e.getMessage());
            }
        }
        return valores;
    }
}
