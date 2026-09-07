package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.InMemoryUtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrarsiControlTest {

    private InMemoryUtenteRepository repository;
    private CodificatoreFinto codificatore;
    private RegistrarsiControl control;

    @BeforeEach
    void prepara() {
        repository = new InMemoryUtenteRepository(List.of());
        codificatore = new CodificatoreFinto();
        control = new RegistrarsiControl(repository, codificatore);
    }

    @Test
    void rifiutaDipendenzeNulle() {
        assertThrows(
                NullPointerException.class,
                () -> new RegistrarsiControl(null, codificatore)
        );
        assertThrows(
                NullPointerException.class,
                () -> new RegistrarsiControl(repository, null)
        );
    }

    @Test
    void registraAccountSalvandoSoloHash() {
        control.richiediRegistrazione("uno@example.com", "segreto");

        List<Utente> utenti = repository.recuperaUtentiAssegnabili();

        assertEquals(1, utenti.size());
        assertNotNull(utenti.get(0).getId());
        assertEquals("uno@example.com", utenti.get(0).recuperaEmail());
        assertEquals("hash-finto", utenti.get(0).recuperaPasswordHash());
        assertEquals("segreto", codificatore.passwordRicevuta);
        assertEquals(1, codificatore.chiamate);
    }

    @Test
    void rifiutaDatiAssentiPrimaDiCodificareOSalvare() {
        for (String valore : new String[]{null, "", " "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> control.richiediRegistrazione(valore, "segreto")
            );
            assertThrows(
                    IllegalArgumentException.class,
                    () -> control.richiediRegistrazione(
                            "uno@example.com", valore
                    )
            );
        }

        assertEquals(0, codificatore.chiamate);
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void verificaDatiNonCreaAccount() {
        control.verificaDatiRegistrazione("uno@example.com", "segreto");

        assertEquals(0, codificatore.chiamate);
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void emailDuplicataNonVieneRicodificataENonModificaAccount() {
        Utente esistente = Utente.crea(
                "uno@example.com", "hash-originale"
        );
        repository.salva(esistente);

        IllegalStateException errore = assertThrows(
                IllegalStateException.class,
                () -> control.richiediRegistrazione(
                        "uno@example.com", "altro"
                )
        );

        assertEquals("L'email è già registrata", errore.getMessage());
        assertEquals(0, codificatore.chiamate);
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
        assertEquals("hash-originale", esistente.recuperaPasswordHash());
    }

    @Test
    void erroreCodificatoreNonCreaAccount() {
        codificatore.errore = new IllegalArgumentException(
                "Password non supportata"
        );

        assertSame(
                codificatore.errore,
                assertThrows(
                        IllegalArgumentException.class,
                        () -> control.richiediRegistrazione(
                                "uno@example.com", "segreto"
                        )
                )
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void hashAssenteNonVieneSalvato() {
        for (String valore : new String[]{null, "", " "}) {
            codificatore.hash = valore;

            assertThrows(
                    IllegalArgumentException.class,
                    () -> control.richiediRegistrazione(
                            "uno@example.com", "segreto"
                    )
            );
        }

        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void erroreSalvataggioVienePropagato() {
        IllegalStateException errore = new IllegalStateException(
                "Salvataggio fallito"
        );

        UtenteRepository repositoryFallibile = new UtenteRepository() {

            @Override
            public List<Utente> recuperaUtentiAssegnabili() {
                return List.of();
            }

            @Override
            public boolean esistePerEmail(String email) {
                return false;
            }

            @Override
            public void salva(Utente utente) {
                assertNull(utente.getId());
                assertEquals(
                        "hash-finto",
                        utente.recuperaPasswordHash()
                );
                throw errore;
            }
        };

        RegistrarsiControl altroControl = new RegistrarsiControl(
                repositoryFallibile, codificatore
        );

        assertSame(
                errore,
                assertThrows(
                        IllegalStateException.class,
                        () -> altroControl.richiediRegistrazione(
                                "uno@example.com", "segreto"
                        )
                )
        );
    }

    private static class CodificatoreFinto implements CodificatorePassword {

        private int chiamate;
        private String passwordRicevuta;
        private String hash = "hash-finto";
        private RuntimeException errore;

        @Override
        public String codifica(String passwordInChiaro) {
            chiamate++;
            passwordRicevuta = passwordInChiaro;

            if (errore != null) {
                throw errore;
            }

            return hash;
        }

        @Override
        public boolean verifica(
                String passwordInChiaro,
                String passwordHash
        ) {
            throw new UnsupportedOperationException(
                    "Non usato nella registrazione"
            );
        }
    }
}