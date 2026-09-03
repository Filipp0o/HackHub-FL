package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IscrivereTeamHackathonRepositoryTest {

    @Test
    void recuperaTeamDellUtente() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        Utente membroSalvato =
                new Utente(10L);

        Team team = Team.crea(
                "ByteBuilders",
                membroSalvato,
                membroSalvato
        );

        repository.salva(team);

        /*
         * Usiamo volutamente un'altra istanza
         * con lo stesso identificativo.
         */
        Utente stessoUtente =
                new Utente(10L);

        Team risultato =
                repository.recuperaTeam(
                        stessoUtente
                );

        assertSame(
                team,
                risultato
        );
    }

    @Test
    void recuperaTeamRifiutaUtenteNullo() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.recuperaTeam(null)
        );
    }

    @Test
    void recuperaTeamFallisceSeUtenteNonAppartieneAdAlcunTeam() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaTeam(
                        new Utente(99L)
                )
        );
    }

    @Test
    void recuperaSoloHackathonApertiAlleIscrizioni() {
        InMemoryPartecipazioneRepository
                partecipazioneRepository =
                new InMemoryPartecipazioneRepository();

        InMemoryHackathonRepository repository =
                new InMemoryHackathonRepository(
                        partecipazioneRepository
                );

        Hackathon aperto =
                creaHackathonAperto(
                        "Hackathon aperto"
                );

        Hackathon scaduto =
                creaHackathonScaduto(
                        "Hackathon scaduto"
                );

        repository.salva(aperto);
        repository.salva(scaduto);

        assertEquals(
                List.of(aperto),
                repository
                        .ottieniHackathonApertiAlleIscrizioni()
        );
    }

    @Test
    void listaHackathonApertiNonEModificabile() {
        InMemoryPartecipazioneRepository
                partecipazioneRepository =
                new InMemoryPartecipazioneRepository();

        InMemoryHackathonRepository repository =
                new InMemoryHackathonRepository(
                        partecipazioneRepository
                );

        Hackathon hackathon =
                creaHackathonAperto(
                        "Hackathon"
                );

        repository.salva(hackathon);

        List<Hackathon> risultato =
                repository
                        .ottieniHackathonApertiAlleIscrizioni();

        assertThrows(
                UnsupportedOperationException.class,
                () -> risultato.add(
                        creaHackathonAperto("Altro")
                )
        );
    }

    @Test
    void riconoscePartecipazioneEsistente() {
        InMemoryPartecipazioneRepository repository =
                new InMemoryPartecipazioneRepository();

        Hackathon hackathon =
                creaHackathonAperto(
                        "Hackathon"
                );

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

        repository.salva(
                partecipazione
        );

        assertTrue(
                repository.esistePartecipazione(
                        team,
                        hackathon
                )
        );
    }

    @Test
    void nonRiconoscePartecipazioneDiAltroTeamOHackathon() {
        InMemoryPartecipazioneRepository repository =
                new InMemoryPartecipazioneRepository();

        Hackathon primoHackathon =
                creaHackathonAperto(
                        "Primo"
                );

        Hackathon secondoHackathon =
                creaHackathonAperto(
                        "Secondo"
                );

        Utente primoResponsabile =
                new Utente(10L);

        Team primoTeam = Team.crea(
                "Primo Team",
                primoResponsabile,
                primoResponsabile
        );

        Utente secondoResponsabile =
                new Utente(20L);

        Team secondoTeam = Team.crea(
                "Secondo Team",
                secondoResponsabile,
                secondoResponsabile
        );

        repository.salva(
                Partecipazione.crea(
                        primoHackathon,
                        primoTeam
                )
        );

        assertAll(
                () -> assertFalse(
                        repository.esistePartecipazione(
                                secondoTeam,
                                primoHackathon
                        )
                ),
                () -> assertFalse(
                        repository.esistePartecipazione(
                                primoTeam,
                                secondoHackathon
                        )
                )
        );
    }

    @Test
    void esistePartecipazioneRifiutaParametriNulli() {
        InMemoryPartecipazioneRepository repository =
                new InMemoryPartecipazioneRepository();

        Hackathon hackathon =
                creaHackathonAperto(
                        "Hackathon"
                );

        Utente responsabile =
                new Utente(10L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository
                                .esistePartecipazione(
                                        null,
                                        hackathon
                                )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository
                                .esistePartecipazione(
                                        team,
                                        null
                                )
                )
        );
    }

    private Hackathon creaHackathonAperto(
            String nome
    ) {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                nome,
                oggi.plusDays(1),
                oggi.plusDays(2),
                oggi.plusDays(5)
        );
    }

    private Hackathon creaHackathonScaduto(
            String nome
    ) {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                nome,
                oggi.minusDays(1),
                oggi.plusDays(1),
                oggi.plusDays(5)
        );
    }

    private Hackathon creaHackathon(
            String nome,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        DatiHackathon dati =
                new DatiHackathon(
                        nome,
                        "Regolamento",
                        "Criteri di valutazione",
                        scadenzaIscrizioni,
                        dataInizio,
                        dataFine,
                        "Camerino",
                        BigDecimal.valueOf(1_000),
                        5
                );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );
    }
}