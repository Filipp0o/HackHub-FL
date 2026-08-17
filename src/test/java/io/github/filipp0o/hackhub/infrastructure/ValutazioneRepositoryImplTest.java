package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValutazioneRepositoryImplTest {

    @Test
    void rifiutaValutazioneNullaDuranteIlSalvataggio() {
        ValutazioneRepositoryImpl repository =
                new ValutazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void salvaUnaValutazioneValida() {
        ValutazioneRepositoryImpl repository =
                new ValutazioneRepositoryImpl();

        Valutazione valutazione =
                creaValutazione();

        assertDoesNotThrow(
                () -> repository.salva(valutazione)
        );
    }

    private Valutazione creaValutazione() {
        LocalDate oggi = LocalDate.now();

        Utente organizzatore = new Utente(1L);
        Utente giudice = new Utente(2L);
        Utente mentore = new Utente(3L);
        Utente responsabile = new Utente(4L);

        DatiHackathon dati = new DatiHackathon(
                "Hackathon",
                "Regolamento",
                "Criteri di valutazione",
                oggi.plusDays(1),
                oggi.plusDays(2),
                oggi.plusDays(5),
                "Camerino",
                BigDecimal.valueOf(1_000),
                5
        );

        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                giudice,
                List.of(mentore)
        );

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        hackathon,
                        team
                );

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Contenuto della sottomissione"
                );

        return Valutazione.crea(
                sottomissione,
                giudice,
                new DatiValutazione(
                        "Buona sottomissione",
                        BigDecimal.valueOf(8)
                )
        );
    }
}