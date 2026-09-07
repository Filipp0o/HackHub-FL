package io.github.filipp0o.hackhub.application;

public interface CodificatorePassword {

    /**
     * Codifica la password per conservarne l'hash.
     *
     * @throws IllegalArgumentException se la password è nulla, vuota
     *         o non supportata dal codificatore
     */
    String codifica(String passwordInChiaro);

    /**
     * Verifica la password rispetto all'hash memorizzato.
     * Restituisce false per credenziali assenti, non valide o non corrispondenti.
     */
    boolean verifica(String passwordInChiaro, String passwordHash);
}
