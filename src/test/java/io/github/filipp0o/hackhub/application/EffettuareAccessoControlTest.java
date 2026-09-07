package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffettuareAccessoControlTest {

    private static final String EMAIL = "Nome@example.com";
    private static final String PASSWORD = "  Segrèto!42  ";
    private static final String HASH = "hash-memorizzato";

    private UtenteRepository repository;
    private CodificatorePassword codificatore;
    private EffettuareAccessoControl control;

    @BeforeEach
    void prepara() {
        repository = mock(UtenteRepository.class);
        codificatore = mock(CodificatorePassword.class);
        control = new EffettuareAccessoControl(repository, codificatore);
    }

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new EffettuareAccessoControl(null, codificatore)
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new EffettuareAccessoControl(repository, null)
                )
        );
    }

    @Test
    void restituisceLAccountVerificandoLaPasswordConLHashMemorizzato() {
        Utente utente = Utente.ricostruisci(1L, EMAIL, HASH);
        when(repository.recuperaPerEmail(EMAIL)).thenReturn(utente);
        when(codificatore.verifica(PASSWORD, HASH)).thenReturn(true);

        assertSame(utente, control.richiediAccesso(EMAIL, PASSWORD));
        verify(codificatore).verifica(PASSWORD, HASH);
        verify(codificatore, never()).codifica(anyString());
        verify(repository, never()).salva(any());
    }

    @Test
    void utenteAssenteProduceCredenzialiNonValideSenzaVerificarePassword() {
        when(repository.recuperaPerEmail(EMAIL)).thenReturn(null);

        assertThrows(
                EffettuareAccessoControl.CredenzialiNonValideException.class,
                () -> control.richiediAccesso(EMAIL, PASSWORD)
        );
        verifyNoInteractions(codificatore);
    }

    @Test
    void passwordErrataProduceLoStessoErroreDellUtenteAssente() {
        when(repository.recuperaPerEmail(EMAIL))
                .thenReturn(Utente.ricostruisci(1L, EMAIL, HASH));
        when(codificatore.verifica(PASSWORD, HASH)).thenReturn(false);

        var passwordErrata = assertThrows(
                EffettuareAccessoControl.CredenzialiNonValideException.class,
                () -> control.richiediAccesso(EMAIL, PASSWORD)
        );
        var utenteAssente = assertThrows(
                EffettuareAccessoControl.CredenzialiNonValideException.class,
                () -> control.richiediAccesso("assente@example.com", PASSWORD)
        );

        assertEquals("Credenziali non valide", passwordErrata.getMessage());
        assertEquals(passwordErrata.getMessage(), utenteAssente.getMessage());
    }

    @Test
    void rifiutaDatiAssentiPrimaDiInterrogareLeDipendenze() {
        for (String valore : Arrays.asList(null, "", "   ", "\t\n")) {
            assertAll(
                    () -> assertThrows(
                            EffettuareAccessoControl.CredenzialiNonValideException.class,
                            () -> control.richiediAccesso(valore, PASSWORD)
                    ),
                    () -> assertThrows(
                            EffettuareAccessoControl.CredenzialiNonValideException.class,
                            () -> control.richiediAccesso(EMAIL, valore)
                    )
            );
        }
        verifyNoInteractions(repository, codificatore);
    }

    @Test
    void propagaErroreDelRepositorySenzaConfonderloConCredenzialiErrate() {
        IllegalStateException errore = new IllegalStateException("Repository indisponibile");
        when(repository.recuperaPerEmail(EMAIL)).thenThrow(errore);

        assertSame(errore, assertThrows(
                IllegalStateException.class,
                () -> control.richiediAccesso(EMAIL, PASSWORD)
        ));
        verifyNoInteractions(codificatore);
    }

    @Test
    void propagaErroreOperativoDelCodificatore() {
        when(repository.recuperaPerEmail(EMAIL))
                .thenReturn(Utente.ricostruisci(1L, EMAIL, HASH));
        IllegalStateException errore = new IllegalStateException("Codificatore indisponibile");
        when(codificatore.verifica(PASSWORD, HASH)).thenThrow(errore);

        assertSame(errore, assertThrows(
                IllegalStateException.class,
                () -> control.richiediAccesso(EMAIL, PASSWORD)
        ));
    }

    @Test
    void ogniRichiestaVerificaDiNuovoLeCredenziali() {
        Utente utente = Utente.ricostruisci(1L, EMAIL, HASH);
        when(repository.recuperaPerEmail(EMAIL)).thenReturn(utente);
        when(codificatore.verifica(PASSWORD, HASH)).thenReturn(true);
        when(codificatore.verifica("errata", HASH)).thenReturn(false);

        assertSame(utente, control.richiediAccesso(EMAIL, PASSWORD));
        assertThrows(
                EffettuareAccessoControl.CredenzialiNonValideException.class,
                () -> control.richiediAccesso(EMAIL, "errata")
        );
        assertSame(utente, control.richiediAccesso(EMAIL, PASSWORD));
        verify(repository, times(3)).recuperaPerEmail(EMAIL);
    }
}
