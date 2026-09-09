package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.application.RegistrarsiControl.RegistrazioneFallitaException;
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

            RegistrazioneFallitaException errore = assertThrows(
                    RegistrazioneFallitaException.class,
                    () -> control.richiediRegistrazione(
                            "uno@example.com", "segreto"
                    )
            );
            assertInstanceOf(IllegalArgumentException.class, errore.getCause());
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
            public Utente recuperaPerEmail(String email) {
                throw new UnsupportedOperationException(
                        "Non utilizzato in questo test"
                );
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
                        RegistrazioneFallitaException.class,
                        () -> altroControl.richiediRegistrazione(
                                "uno@example.com", "segreto"
                        )
                ).getCause()
        );
    }

    @Test
    void rifiutaEmailMalformatePrimaDiCodificareOSalvare() {
        for (String email : List.of(
                "non-email", "nome@", "@example.com", "nome@@example.com",
                "nome@example", "nome cognome@example.com",
                " nome@example.com", "nome@example.com ",
                ".nome@example.com", "nome..cognome@example.com",
                "nome.@example.com", "nome@example..com",
                "nome@-example.com", "nome@example-.com",
                "a".repeat(321) + "@example.com"
        )) {
            IllegalArgumentException errore = assertThrows(
                    IllegalArgumentException.class,
                    () -> control.richiediRegistrazione(email, "segreto"),
                    email
            );
            assertEquals(
                    "Il formato dell'email non è valido",
                    errore.getMessage()
            );
        }

        assertEquals(0, codificatore.chiamate);
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void accettaEmailConPuntiPlusApiciESottodominiSenzaModificarle() {
        for (String email : List.of(
                "Nome.Cognome+prova@example.com",
                "o'connor@sub.example.org",
                "a@b.it"
        )) {
            control.richiediRegistrazione(email, "segreto");
            assertNotNull(repository.recuperaPerEmail(email));
        }

        assertEquals(3, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void indicaTuttiICampiDaCorreggere() {
        assertEquals(
                "L'email è obbligatoria; La password è obbligatoria",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDatiRegistrazione(null, null)
                ).getMessage()
        );

        assertEquals(
                "Il formato dell'email non è valido; La password è obbligatoria",
                assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaDatiRegistrazione("non-email", " ")
                ).getMessage()
        );
    }

    @Test
    void erroreRicercaEmailConservaCausaENonAvviaLaCreazione() {
        RuntimeException causa = new IllegalArgumentException("Errore interno");

        InMemoryUtenteRepository fallibile =
                new InMemoryUtenteRepository(List.of()) {
                    @Override
                    public boolean esistePerEmail(String email) {
                        throw causa;
                    }
                };

        RegistrarsiControl altro = new RegistrarsiControl(
                fallibile, codificatore
        );

        RegistrazioneFallitaException errore = assertThrows(
                RegistrazioneFallitaException.class,
                () -> altro.richiediRegistrazione("uno@example.com", "segreto")
        );

        assertSame(causa, errore.getCause());
        assertEquals(0, codificatore.chiamate);
        assertTrue(fallibile.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void salvataggioFallitoConservaAccountEsistentiEConsenteNuovoTentativo() {
        for (RuntimeException causa : List.of(
                new RuntimeException("Errore interno"),
                new IllegalArgumentException("Errore interno"),
                new IllegalStateException("Errore interno")
        )) {
            Utente precedente = Utente.ricostruisci(
                    1L, "precedente@example.com", "hash-originale"
            );

            class RepositoryFallibile extends InMemoryUtenteRepository {

                private boolean fallisce = true;
                private Utente tentativo;

                RepositoryFallibile() {
                    super(List.of(precedente));
                }

                @Override
                public void salva(Utente utente) {
                    tentativo = utente;

                    if (fallisce) {
                        throw causa;
                    }

                    super.salva(utente);
                }
            }

            RepositoryFallibile fallibile = new RepositoryFallibile();
            RegistrarsiControl altro = new RegistrarsiControl(
                    fallibile, codificatore
            );

            RegistrazioneFallitaException errore = assertThrows(
                    RegistrazioneFallitaException.class,
                    () -> altro.richiediRegistrazione(
                            "uno@example.com", "segreto"
                    )
            );

            assertSame(causa, errore.getCause());
            assertEquals(
                    List.of(precedente),
                    fallibile.recuperaUtentiAssegnabili()
            );
            assertNull(fallibile.recuperaPerEmail("uno@example.com"));
            assertNull(fallibile.tentativo.getId());
            assertEquals(
                    "hash-originale",
                    precedente.recuperaPasswordHash()
            );

            fallibile.fallisce = false;
            altro.richiediRegistrazione("uno@example.com", "segreto");

            assertEquals(2, fallibile.recuperaUtentiAssegnabili().size());

            Utente salvato = fallibile.recuperaPerEmail("uno@example.com");
            assertEquals(2L, salvato.getId());
            assertEquals("hash-finto", salvato.recuperaPasswordHash());
        }
    }

    @Test
    void erroreRealeDelRepositoryNonPubblicaAccountParziali() {
        Utente precedente = new Utente(Long.MAX_VALUE);

        InMemoryUtenteRepository pieno = new InMemoryUtenteRepository(
                List.of(precedente)
        );
        RegistrarsiControl altro = new RegistrarsiControl(
                pieno, codificatore
        );

        RegistrazioneFallitaException errore = assertThrows(
                RegistrazioneFallitaException.class,
                () -> altro.richiediRegistrazione("uno@example.com", "segreto")
        );

        assertEquals(
                "Identificativi utente esauriti",
                errore.getCause().getMessage()
        );
        assertEquals(List.of(precedente), pieno.recuperaUtentiAssegnabili());
        assertNull(pieno.recuperaPerEmail("uno@example.com"));
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