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

class SemanticaRepositoryInMemoryTest {

    @Test
    void assegnaIdentificativoAlTeamAlPrimoSalvataggio() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        Utente responsabile = new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        assertNull(team.getId());

        repository.salva(team);

        assertAll(
                () -> assertNotNull(team.getId()),
                () -> assertTrue(team.getId() > 0)
        );
    }

    @Test
    void ricostruisceTeamConIdentificativoEsistente() {
        Utente responsabile = new Utente(1L);

        Team team = Team.ricostruisci(
                42L,
                "ByteBuilders",
                List.of(responsabile),
                responsabile
        );

        assertAll(
                () -> assertEquals(42L, team.getId()),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> team.assegnaId(43L)
                )
        );
    }

    @Test
    void secondoSalvataggioNonDuplicaHackathon() {
        InMemoryPartecipazioneRepository
                partecipazioneRepository =
                new InMemoryPartecipazioneRepository();

        InMemoryHackathonRepository repository =
                new InMemoryHackathonRepository(
                        partecipazioneRepository
                );

        Hackathon hackathon = creaHackathon();

        repository.salva(hackathon);
        repository.salva(hackathon);

        assertEquals(
                1,
                repository.ottieniTuttiHackathon().size()
        );
    }

    @Test
    void secondoSalvataggioNonDuplicaPartecipazione() {
        InMemoryPartecipazioneRepository repository =
                new InMemoryPartecipazioneRepository();

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        creaHackathon(),
                        team
                );

        repository.salva(partecipazione);
        repository.salva(partecipazione);

        assertEquals(
                1,
                repository
                        .ottieniPartecipazioni(
                                partecipazione.getHackathon()
                        )
                        .size()
        );
    }

    private Hackathon creaHackathon() {
        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Camerino",
                BigDecimal.valueOf(5000),
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