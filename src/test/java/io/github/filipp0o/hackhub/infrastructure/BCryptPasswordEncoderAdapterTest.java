package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderAdapterTest {

    private static final String PASSWORD = "Password-di-prova!42";

    private final CodificatorePassword codificatore =
            new BCryptPasswordEncoderAdapter();

    @Test
    void produceUnHashVerificabileConBCrypt() {
        String hash = codificatore.codifica(PASSWORD);

        assertAll(
                () -> assertNotEquals(PASSWORD, hash),
                () -> assertTrue(
                        new BCryptPasswordEncoder().matches(PASSWORD, hash)
                )
        );
    }

    @Test
    void generaHashDiversiPerLaStessaPassword() {
        String primoHash = codificatore.codifica(PASSWORD);
        String secondoHash = codificatore.codifica(PASSWORD);
        CodificatorePassword altroCodificatore =
                new BCryptPasswordEncoderAdapter();

        assertAll(
                () -> assertNotEquals(primoHash, secondoHash),
                () -> assertTrue(
                        altroCodificatore.verifica(PASSWORD, primoHash)
                ),
                () -> assertTrue(
                        altroCodificatore.verifica(PASSWORD, secondoHash)
                )
        );
    }

    @Test
    void verificaUnHashProdottoDaBCryptEsterno() {
        String hash = new BCryptPasswordEncoder().encode(PASSWORD);

        assertTrue(codificatore.verifica(PASSWORD, hash));
    }

    @Test
    void rifiutaUnaPasswordDiversaDaQuellaCodificata() {
        String hash = codificatore.codifica(PASSWORD);

        assertFalse(codificatore.verifica("Password-errata!42", hash));
    }

    @Test
    void preservaSpaziMaiuscoleECaratteriUnicode() {
        String password = "  Pàss🔑Word!42  ";
        String hash = codificatore.codifica(password);

        assertAll(
                () -> assertTrue(codificatore.verifica(password, hash)),
                () -> assertFalse(
                        codificatore.verifica(password.strip(), hash)
                ),
                () -> assertFalse(
                        codificatore.verifica(
                                password.toLowerCase(Locale.ROOT), hash
                        )
                )
        );
    }

    @Test
    void rifiutaCodificaDiPasswordAssentiOVuote() {
        for (String password : Arrays.asList(null, "", "   ", "\t\n")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> codificatore.codifica(password)
            );
        }
    }

    @Test
    void fallisceLaVerificaDiPasswordAssentiOVuote() {
        String hash = codificatore.codifica(PASSWORD);

        for (String password : Arrays.asList(null, "", "   ", "\t\n")) {
            assertFalse(codificatore.verifica(password, hash));
        }
    }

    @Test
    void fallisceLaVerificaDiHashAssentiOMalformati() {
        for (String hash : Arrays.asList(
                null,
                "",
                "   ",
                PASSWORD,
                "hash-non-valido",
                "$2a$99$" + ".".repeat(53)
        )) {
            assertFalse(codificatore.verifica(PASSWORD, hash));
        }
    }

    @Test
    void rifiutaCodificaOltre72ByteAncheConCaratteriMultibyte() {
        for (String password : List.of(
                "a".repeat(73),
                "é".repeat(37),
                "🔑".repeat(19)
        )) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> codificatore.codifica(password)
            );
        }
    }

    @Test
    void accetta72ByteERifiutaPasswordConSuffissiOltreIlLimite() {
        for (String password : List.of(
                "a".repeat(72),
                "é".repeat(36),
                "🔑".repeat(18)
        )) {
            String hash = codificatore.codifica(password);

            assertAll(
                    () -> assertTrue(codificatore.verifica(password, hash)),
                    () -> assertFalse(
                            codificatore.verifica(password + "x", hash)
                    )
            );
        }
    }
}
