package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SottomissioneRepositoryIdentitaTest {

    private final SottomissioneRepositoryImpl repository =
            new SottomissioneRepositoryImpl();

    @Test
    void recuperaSottomissioneConPartecipazioneRicostruitaDelloStessoId() {
        Partecipazione originale = creaPartecipazione();
        originale.assegnaId(42L);

        Sottomissione s = Sottomissione.crea(
                originale, "Progetto"
        );
        repository.salva(s);

        Partecipazione riletta = Partecipazione.ricostruisci(
                42L,
                originale.getHackathon(),
                originale.getTeam(),
                StatoPartecipazione.ATTIVA
        );

        assertNotSame(originale, riletta);
        assertSame(s, repository.recuperaSottomissione(riletta));

        Partecipazione altra = Partecipazione.ricostruisci(
                43L,
                originale.getHackathon(),
                originale.getTeam(),
                StatoPartecipazione.ATTIVA
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaSottomissione(altra)
        );
    }

    @Test
    void partecipazioniNuoveSenzaIdNonVengonoConfuse() {
        Partecipazione prima = creaPartecipazione();
        Partecipazione seconda = creaPartecipazione();

        Sottomissione s = Sottomissione.crea(
                prima, "Progetto"
        );
        repository.salva(s);

        assertNull(prima.getId());
        assertNull(seconda.getId());

        assertSame(s, repository.recuperaSottomissione(prima));
        assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaSottomissione(seconda)
        );
    }

    private Partecipazione creaPartecipazione() {
        Utente responsabile = new Utente(4L);
        LocalDate data = LocalDate.of(2025, 4, 1);

        Hackathon h = Hackathon.crea(
                new DatiHackathon(
                        "Hackathon",
                        "Regolamento",
                        "Criteri",
                        data,
                        data.plusDays(1),
                        data.plusDays(2),
                        "Camerino",
                        BigDecimal.TEN,
                        5
                ),
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );

        return Partecipazione.crea(
                h, Team.crea("Team", responsabile, responsabile)
        );
    }
}