package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
class SottomissioneRepositoryImplTest {

    @Test
    void rifiutaSottomissioneNullaDuranteIlSalvataggio() {
        SottomissioneRepositoryImpl repository =
                new SottomissioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void salvaSottomissioneValida() {
        SottomissioneRepositoryImpl repository =
                new SottomissioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon();

        Utente responsabile =
                new Utente(10L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        Sottomissione sottomissione =
                Sottomissione.crea(
                        partecipazione,
                        "Repository del progetto"
                );

        assertDoesNotThrow(
                () -> repository.salva(
                        sottomissione
                )
        );
    }
    @Test
    void recuperaSottomissioneDellaPartecipazione() {
        SottomissioneRepositoryImpl repository =
                new SottomissioneRepositoryImpl();

        Partecipazione partecipazione =
                creaPartecipazione();

        Sottomissione sottomissione =
                Sottomissione.crea(
                        partecipazione,
                        "Repository del progetto"
                );

        repository.salva(
                sottomissione
        );

        assertSame(
                sottomissione,
                repository.recuperaSottomissione(
                        partecipazione
                )
        );
    }

    @Test
    void rifiutaPartecipazioneNullaONonAssociata() {
        SottomissioneRepositoryImpl repository =
                new SottomissioneRepositoryImpl();

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository
                                .recuperaSottomissione(null)
                ),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> repository
                                .recuperaSottomissione(
                                        creaPartecipazione()
                                )
                )
        );
    }

    @Test
    void risalvaSottomissioneAggiornata() {
        SottomissioneRepositoryImpl repository =
                new SottomissioneRepositoryImpl();

        Partecipazione partecipazione =
                creaPartecipazione();

        Sottomissione sottomissione =
                Sottomissione.crea(
                        partecipazione,
                        "Prima versione"
                );

        repository.salva(
                sottomissione
        );

        sottomissione.aggiornaContenuto(
                "Versione aggiornata"
        );

        repository.salva(
                sottomissione
        );

        Sottomissione recuperata =
                repository.recuperaSottomissione(
                        partecipazione
                );

        assertAll(
                () -> assertSame(
                        sottomissione,
                        recuperata
                ),
                () -> assertEquals(
                        "Versione aggiornata",
                        recuperata.ottieniContenuto()
                )
        );
    }

    private Partecipazione creaPartecipazione() {
        Utente responsabile =
                new Utente(10L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        return Partecipazione.crea(
                creaHackathon(),
                team
        );
    }

    private Hackathon creaHackathon() {
        LocalDate oggi =
                LocalDate.now();

        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub",
                        "Regolamento",
                        "Criteri di valutazione",
                        oggi.minusDays(3),
                        oggi.minusDays(2),
                        oggi.plusDays(3),
                        "Camerino",
                        BigDecimal.valueOf(1_000),
                        5
                );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(
                        new Utente(3L)
                )
        );
    }
}