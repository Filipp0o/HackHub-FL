package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartecipazioneRicostruzioneTest {

    private final Utente responsabile = new Utente(4L);
    private final Utente giudice = new Utente(2L);

    private final Team team = Team.crea(
            "Team", responsabile, responsabile
    );
    private final Hackathon hackathon = creaHackathon();

    @Test
    void ricostruiscePartecipazioneAttivaConIdentitaEAssociazioni() {
        Partecipazione p = Partecipazione.ricostruisci(
                42L, hackathon, team, StatoPartecipazione.ATTIVA
        );

        assertEquals(42L, p.getId());
        assertEquals(StatoPartecipazione.ATTIVA, p.getStato());
        assertSame(hackathon, p.getHackathon());
        assertSame(hackathon, p.ottieniHackathon());
        assertSame(team, p.getTeam());
        assertNull(p.getSottomissione());

        assertThrows(
                IllegalStateException.class,
                () -> p.assegnaId(43L)
        );
        assertEquals(42L, p.getId());
    }

    @Test
    void ricostruisceEsclusaConSottomissioneEValutazioneStoriche() {
        Partecipazione p = Partecipazione.ricostruisci(
                42L, hackathon, team, StatoPartecipazione.ESCLUSA
        );

        Sottomissione s = Sottomissione.ricostruisci(
                50L, p, "Progetto salvato"
        );

        LocalDateTime data = LocalDateTime.of(
                2025, 4, 10, 12, 30
        );

        Valutazione v = Valutazione.ricostruisci(
                60L,
                s,
                giudice,
                new DatiValutazione(
                        "Giudizio salvato", new BigDecimal("8.5")
                ),
                data
        );

        assertEquals(StatoPartecipazione.ESCLUSA, p.getStato());
        assertEquals(42L, p.getId());

        assertSame(s, p.getSottomissione());
        assertSame(p, s.getPartecipazione());
        assertEquals(50L, s.getId());
        assertEquals("Progetto salvato", s.getContenuto());

        assertSame(v, s.getValutazione());
        assertSame(s, v.getSottomissione());
        assertEquals(60L, v.getId());
        assertEquals(data, v.getDataOra());
        assertEquals(new BigDecimal("8.5"), v.getPunteggio());
    }

    @Test
    void rifiutaIdAssenteONonPositivo() {
        assertThrows(
                NullPointerException.class,
                () -> Partecipazione.ricostruisci(
                        null, hackathon, team, StatoPartecipazione.ATTIVA
                )
        );

        for (long id : new long[]{0L, -1L}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Partecipazione.ricostruisci(
                            id, hackathon, team, StatoPartecipazione.ATTIVA
                    )
            );
        }
    }

    @Test
    void rifiutaAssociazioniOStatoAssenti() {
        assertThrows(
                NullPointerException.class,
                () -> Partecipazione.ricostruisci(
                        1L, null, team, StatoPartecipazione.ATTIVA
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> Partecipazione.ricostruisci(
                        1L, hackathon, null, StatoPartecipazione.ATTIVA
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> Partecipazione.ricostruisci(
                        1L, hackathon, team, null
                )
        );
    }

    @Test
    void creazioneOrdinariaRestaAttivaESenzaId() {
        for (Partecipazione p : List.of(
                Partecipazione.crea(hackathon, team),
                new Partecipazione(hackathon, team)
        )) {
            assertNull(p.getId());
            assertEquals(StatoPartecipazione.ATTIVA, p.getStato());
            assertNull(p.getSottomissione());
        }
    }

    private Hackathon creaHackathon() {
        LocalDate data = LocalDate.of(2025, 4, 1);

        return Hackathon.crea(
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
                giudice,
                List.of(new Utente(3L))
        );
    }
}