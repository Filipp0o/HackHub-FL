package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IscrivereTeamHackathonDomainTest {

    @Test
    void numeroMembriRestituisceLaDimensioneDelTeam() {
        Utente responsabile = new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        assertEquals(
                1,
                team.numeroMembri()
        );
    }

    @Test
    void factoryCreaPartecipazioneAttiva() {
        Hackathon hackathon =
                creaHackathonAperto(5);

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

        assertAll(
                () -> assertNotNull(
                        partecipazione.getId()
                ),
                () -> assertSame(
                        hackathon,
                        partecipazione.getHackathon()
                ),
                () -> assertSame(
                        team,
                        partecipazione.getTeam()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertNull(
                        partecipazione.getSottomissione()
                )
        );
    }

    @Test
    void hackathonConIscrizioniAncoraAperteRisultaAperto() {
        Hackathon hackathon =
                creaHackathonAperto(5);

        assertTrue(
                hackathon.isApertoAlleIscrizioni()
        );
    }

    @Test
    void hackathonConScadenzaIscrizioniTrascorsaNonRisultaAperto() {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon =
                creaHackathon(
                        oggi.minusDays(1),
                        oggi.plusDays(1),
                        oggi.plusDays(5),
                        5
                );

        assertFalse(
                hackathon.isApertoAlleIscrizioni()
        );
    }

    @Test
    void hackathonNonInIscrizioneNonRisultaAperto() {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon =
                creaHackathon(
                        oggi.plusDays(1),
                        oggi.plusDays(2),
                        oggi.plusDays(5),
                        5
                );

        hackathon.aggiornaStato(
                oggi.plusDays(2)
        );

        assertAll(
                () -> assertEquals(
                        TipoStatoHackathon.IN_CORSO,
                        hackathon.getStato()
                ),
                () -> assertFalse(
                        hackathon.isApertoAlleIscrizioni()
                )
        );
    }

    @Test
    void accettaTeamEntroLaDimensioneMassima() {
        Hackathon hackathon =
                creaHackathonAperto(5);

        assertAll(
                () -> assertTrue(
                        hackathon
                                .rispettaDimensioneMassima(1)
                ),
                () -> assertTrue(
                        hackathon
                                .rispettaDimensioneMassima(5)
                )
        );
    }

    @Test
    void rifiutaTeamOltreLaDimensioneMassima() {
        Hackathon hackathon =
                creaHackathonAperto(5);

        assertFalse(
                hackathon
                        .rispettaDimensioneMassima(6)
        );
    }

    @Test
    void rifiutaNumeroMembriNullo() {
        Hackathon hackathon =
                creaHackathonAperto(5);

        assertThrows(
                NullPointerException.class,
                () -> hackathon
                        .rispettaDimensioneMassima(null)
        );
    }

    private Hackathon creaHackathonAperto(
            int dimensioneMassimaTeam
    ) {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                oggi.plusDays(1),
                oggi.plusDays(2),
                oggi.plusDays(5),
                dimensioneMassimaTeam
        );
    }

    private Hackathon creaHackathon(
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine,
            int dimensioneMassimaTeam
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        "HackHub",
                        "Regolamento",
                        "Criteri di valutazione",
                        scadenzaIscrizioni,
                        dataInizio,
                        dataFine,
                        "Camerino",
                        BigDecimal.valueOf(1_000),
                        dimensioneMassimaTeam
                );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );
    }
}