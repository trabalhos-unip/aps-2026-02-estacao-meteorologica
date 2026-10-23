package br.com.aps.meteorologia.dao;

/** Erro ao acessar o banco de dados (conexão, gravação ou consulta). */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
