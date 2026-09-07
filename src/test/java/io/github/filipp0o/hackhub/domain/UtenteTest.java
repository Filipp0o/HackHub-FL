package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtenteTest {

    private static final String EMAIL = "Mattia@example.test";
    private static final String PASSWORD_HASH = "hash-gia-codificato";

    @Test
    void creaUtenteConCredenzialiESenzaIdentificativo() {
        Utente utente = Utente.crea(EMAIL, PASSWORD_HASH);

        assertAll(
                () -> assertNull(utente.getId()),
                () -> assertEquals(EMAIL, utente.recuperaEmail()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        utente.recuperaPasswordHash()
                )
        );
    }

    @Test
    void rifiutaEmailAssenteOVuota() {
        for (String email : Arrays.asList(null, "", "   ", "\t\n")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Utente.crea(email, PASSWORD_HASH)
            );
        }
    }

    @Test
    void rifiutaHashAssenteOVuoto() {
        for (String hash : Arrays.asList(null, "", "   ", "\t\n")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Utente.crea(EMAIL, hash)
            );
        }
    }

    @Test
    void assegnaIdentificativoConservandoLeCredenziali() {
        Utente utente = Utente.crea(EMAIL, PASSWORD_HASH);

        utente.assegnaId(42L);

        assertAll(
                () -> assertEquals(42L, utente.getId()),
                () -> assertEquals(EMAIL, utente.recuperaEmail()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        utente.recuperaPasswordHash()
                )
        );
    }

    @Test
    void rifiutaIdentificativoNulloSenzaAssegnarlo() {
        Utente utente = Utente.crea(EMAIL, PASSWORD_HASH);

        assertThrows(
                NullPointerException.class,
                () -> utente.assegnaId(null)
        );
        assertNull(utente.getId());
    }

    @Test
    void rifiutaIdentificativoNonPositivoSenzaAssegnarlo() {
        Utente utente = Utente.crea(EMAIL, PASSWORD_HASH);

        for (Long id : List.of(0L, -1L)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> utente.assegnaId(id)
            );
            assertNull(utente.getId());
        }
    }

    @Test
    void impedisceUnaSecondaAssegnazioneDellIdentificativo() {
        Utente utente = Utente.crea(EMAIL, PASSWORD_HASH);
        utente.assegnaId(42L);

        for (Long id : List.of(42L, 43L)) {
            assertThrows(
                    IllegalStateException.class,
                    () -> utente.assegnaId(id)
            );
            assertEquals(42L, utente.getId());
        }
    }

    @Test
    void ricostruisceUtenteConIdentificativoECredenziali() {
        Utente utente = Utente.ricostruisci(42L, EMAIL, PASSWORD_HASH);

        assertAll(
                () -> assertEquals(42L, utente.getId()),
                () -> assertEquals(EMAIL, utente.recuperaEmail()),
                () -> assertEquals(
                        PASSWORD_HASH,
                        utente.recuperaPasswordHash()
                ),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> utente.assegnaId(43L)
                )
        );
    }

    @Test
    void rifiutaRicostruzioneConIdentificativoNonValido() {
        assertThrows(
                NullPointerException.class,
                () -> Utente.ricostruisci(null, EMAIL, PASSWORD_HASH)
        );

        for (Long id : List.of(0L, -1L)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Utente.ricostruisci(id, EMAIL, PASSWORD_HASH)
            );
        }
    }

    @Test
    void rifiutaRicostruzioneConCredenzialiAssentiOVuote() {
        for (String valore : Arrays.asList(null, "", "   ", "\t\n")) {
            assertAll(
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> Utente.ricostruisci(
                                    42L, valore, PASSWORD_HASH
                            )
                    ),
                    () -> assertThrows(
                            IllegalArgumentException.class,
                            () -> Utente.ricostruisci(42L, EMAIL, valore)
                    )
            );
        }
    }

    @Test
    void supportaIRiferimentiConSoloIdDeiFlussiEsistenti() {
        Utente riferimento = new Utente(42L);

        assertAll(
                () -> assertEquals(42L, riferimento.getId()),
                () -> assertNull(riferimento.recuperaEmail()),
                () -> assertNull(riferimento.recuperaPasswordHash()),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> riferimento.assegnaId(43L)
                )
        );
    }

    @Test
    void rifiutaRiferimentoConIdentificativoNonValido() {
        assertThrows(
                NullPointerException.class,
                () -> new Utente(null)
        );

        for (Long id : List.of(0L, -1L)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Utente(id)
            );
        }
    }
}
