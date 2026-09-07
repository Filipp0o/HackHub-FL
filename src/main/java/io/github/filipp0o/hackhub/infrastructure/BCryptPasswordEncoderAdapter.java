package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

public final class BCryptPasswordEncoderAdapter
        implements CodificatorePassword {

    private static final int LUNGHEZZA_MASSIMA_BYTE = 72;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    @Override
    public String codifica(String passwordInChiaro) {
        if (passwordInChiaro == null || passwordInChiaro.isBlank()) {
            throw new IllegalArgumentException(
                    "La password è obbligatoria"
            );
        }

        if (passwordInChiaro.getBytes(StandardCharsets.UTF_8).length
                > LUNGHEZZA_MASSIMA_BYTE) {
            throw new IllegalArgumentException(
                    "La password non può superare 72 byte in UTF-8"
            );
        }

        return encoder.encode(passwordInChiaro);
    }

    @Override
    public boolean verifica(
            String passwordInChiaro,
            String passwordHash
    ) {
        if (passwordInChiaro == null || passwordInChiaro.isBlank()
                || passwordHash == null || passwordHash.isBlank()
                || passwordInChiaro.getBytes(StandardCharsets.UTF_8).length
                > LUNGHEZZA_MASSIMA_BYTE) {
            return false;
        }

        try {
            return encoder.matches(passwordInChiaro, passwordHash);
        } catch (IllegalArgumentException eccezione) {
            // Un hash BCrypt malformato non può autenticare l'utente.
            return false;
        }
    }
}
