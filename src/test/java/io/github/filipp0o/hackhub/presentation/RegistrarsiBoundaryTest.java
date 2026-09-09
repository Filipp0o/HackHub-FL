package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import io.github.filipp0o.hackhub.application.RegistrarsiControl;
import io.github.filipp0o.hackhub.infrastructure.InMemoryUtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrarsiBoundaryTest {

    private InMemoryUtenteRepository repository;
    private CodificatorePassword codificatore;
    private RegistrarsiBoundary boundary;

    @BeforeEach
    void prepara() {
        repository = new InMemoryUtenteRepository(List.of());

        codificatore = new CodificatorePassword() {

            @Override
            public String codifica(String password) {
                return "hash-finto";
            }

            @Override
            public boolean verifica(String password, String hash) {
                throw new UnsupportedOperationException();
            }
        };

        boundary = new RegistrarsiBoundary(
                new RegistrarsiControl(repository, codificatore)
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new RegistrarsiBoundary(null)
        );
    }

    @Test
    void selezioneRichiedeDatiSenzaCreareAccount() {
        var risposta = boundary.selezionaRegistrazione();

        assertEquals(HttpStatus.OK, risposta.getStatusCode());
        assertEquals(
                "Inserire email e password",
                risposta.getBody().messaggio()
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void registrazioneRestituisceCreatedESalvaHash() {
        var risposta = boundary.registra(
                new RegistrarsiBoundary.RichiestaRegistrazione(
                        "uno@example.com",
                        "segreto"
                )
        );

        assertEquals(HttpStatus.CREATED, risposta.getStatusCode());
        assertEquals(
                "Registrazione completata",
                risposta.getBody().messaggio()
        );

        var utenti = repository.recuperaUtentiAssegnabili();

        assertEquals(1, utenti.size());
        assertEquals("hash-finto", utenti.get(0).recuperaPasswordHash());
        assertNotNull(utenti.get(0).getId());
    }

    @Test
    void datiAssentiRestituisconoBadRequest() {
        for (String valore : new String[]{null, "", " "}) {
            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    boundary.inserisciDatiRegistrazione(
                            valore,
                            "segreto"
                    ).getStatusCode()
            );

            assertEquals(
                    HttpStatus.BAD_REQUEST,
                    boundary.inserisciDatiRegistrazione(
                            "uno@example.com",
                            valore
                    ).getStatusCode()
            );
        }

        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void richiestaNullaRestituisceBadRequest() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                boundary.registra(null).getStatusCode()
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void emailDuplicataRestituisceConflict() {
        boundary.inserisciDatiRegistrazione(
                "uno@example.com",
                "segreto"
        );

        var risposta = boundary.inserisciDatiRegistrazione(
                "uno@example.com",
                "altro"
        );

        assertEquals(HttpStatus.CONFLICT, risposta.getStatusCode());
        assertEquals(
                "L'email è già registrata",
                risposta.getBody().messaggio()
        );
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void erroreOperativoNonEsponeDettagliInterni() {
        RegistrarsiControl controlFallibile = new RegistrarsiControl(
                repository,
                codificatore
        ) {
            @Override
            public void richiediRegistrazione(
                    String email,
                    String password
            ) {
                throw new IllegalStateException(
                        "SQL: dettaglio interno"
                );
            }
        };

        var altraBoundary = new RegistrarsiBoundary(controlFallibile);

        var risposta = altraBoundary.inserisciDatiRegistrazione(
                "uno@example.com",
                "segreto"
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                risposta.getStatusCode()
        );
        assertEquals(
                "Registrazione non completata",
                risposta.getBody().messaggio()
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void datiErratiIndicanoLeCorrezioniEConsentonoUnNuovoTentativo() {
        var risposta = boundary.inserisciDatiRegistrazione("non-email", " ");

        assertEquals(HttpStatus.BAD_REQUEST, risposta.getStatusCode());
        assertEquals(
                "Il formato dell'email non è valido; La password è obbligatoria",
                risposta.getBody().messaggio()
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());

        assertEquals(
                HttpStatus.CREATED,
                boundary.inserisciDatiRegistrazione(
                        "uno@example.com", "segreto"
                ).getStatusCode()
        );
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void hashInvalidoDelCodificatoreProduceErroreInterno() {
        CodificatorePassword difettoso = new CodificatorePassword() {

            @Override
            public String codifica(String password) {
                return null;
            }

            @Override
            public boolean verifica(String password, String hash) {
                throw new UnsupportedOperationException();
            }
        };

        RegistrarsiBoundary altra = new RegistrarsiBoundary(
                new RegistrarsiControl(repository, difettoso)
        );

        var risposta = altra.inserisciDatiRegistrazione(
                "uno@example.com", "segreto"
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, risposta.getStatusCode());
        assertEquals(
                "Registrazione non completata",
                risposta.getBody().messaggio()
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }
}