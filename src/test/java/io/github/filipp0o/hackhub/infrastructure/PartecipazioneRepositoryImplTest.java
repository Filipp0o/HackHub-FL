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

class PartecipazioneRepositoryImplTest {

    @Test
    void rifiutaHackathonNulloDuranteIlRecupero() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.ottieniPartecipazioni(null)
        );
    }

    @Test
    void rifiutaHackathonNulloDuranteIlRecuperoDelleNonEscluse() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository
                        .recuperaPartecipazioniNonEscluse(null)
        );
    }

    @Test
    void rifiutaPartecipazioneNullaDuranteIlSalvataggio() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void restituisceListaVuotaSeNonEsistonoPartecipazioni() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon(
                        "Hackathon senza partecipazioni"
                );

        assertTrue(
                repository
                        .ottieniPartecipazioni(hackathon)
                        .isEmpty()
        );
    }

    @Test
    void restituisceSoloPartecipazioniDellHackathonSelezionato() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon primoHackathon =
                creaHackathon("Primo Hackathon");

        Hackathon secondoHackathon =
                creaHackathon("Secondo Hackathon");

        Partecipazione primaPartecipazione =
                creaPartecipazione(
                        primoHackathon,
                        10L
                );

        Partecipazione secondaPartecipazione =
                creaPartecipazione(
                        primoHackathon,
                        20L
                );

        Partecipazione partecipazioneAltroHackathon =
                creaPartecipazione(
                        secondoHackathon,
                        30L
                );

        secondaPartecipazione.escludi();

        repository.salva(primaPartecipazione);
        repository.salva(secondaPartecipazione);
        repository.salva(partecipazioneAltroHackathon);

        assertEquals(
                List.of(
                        primaPartecipazione,
                        secondaPartecipazione
                ),
                repository.ottieniPartecipazioni(
                        primoHackathon
                )
        );
    }

    @Test
    void recuperaSoloPartecipazioniNonEscluse() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon("Hackathon");

        Partecipazione partecipazioneAttiva =
                creaPartecipazione(
                        hackathon,
                        10L
                );

        Partecipazione partecipazioneEsclusa =
                creaPartecipazione(
                        hackathon,
                        20L
                );

        partecipazioneEsclusa.escludi();

        repository.salva(partecipazioneAttiva);
        repository.salva(partecipazioneEsclusa);

        assertEquals(
                List.of(partecipazioneAttiva),
                repository
                        .recuperaPartecipazioniNonEscluse(
                                hackathon
                        )
        );
    }

    @Test
    void restituisceListeNonModificabili() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon("Hackathon");

        Partecipazione partecipazione =
                creaPartecipazione(
                        hackathon,
                        10L
                );

        repository.salva(partecipazione);

        List<Partecipazione> tutte =
                repository
                        .ottieniPartecipazioni(
                                hackathon
                        );

        List<Partecipazione> nonEscluse =
                repository
                        .recuperaPartecipazioniNonEscluse(
                                hackathon
                        );

        assertAll(
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> tutte.add(
                                partecipazione
                        )
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> nonEscluse.add(
                                partecipazione
                        )
                )
        );
    }

    @Test
    void recuperaPartecipazioneDelTeamNellHackathon() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon("Hackathon");

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

        assertSame(
                partecipazione,
                repository.recuperaPartecipazione(
                        team,
                        hackathon
                )
        );
    }

    @Test
    void recuperaPartecipazioneDistingueTeamEHackathon() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon primoHackathon =
                creaHackathon("Primo Hackathon");

        Hackathon secondoHackathon =
                creaHackathon("Secondo Hackathon");

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

        Partecipazione attesa =
                Partecipazione.crea(
                        primoHackathon,
                        primoTeam
                );

        repository.salva(
                Partecipazione.crea(
                        primoHackathon,
                        secondoTeam
                )
        );

        repository.salva(
                Partecipazione.crea(
                        secondoHackathon,
                        primoTeam
                )
        );

        repository.salva(
                attesa
        );

        assertSame(
                attesa,
                repository.recuperaPartecipazione(
                        primoTeam,
                        primoHackathon
                )
        );
    }

    @Test
    void recuperaPartecipazioneFallisceSeNonEsiste() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon("Hackathon");

        Utente responsabile =
                new Utente(10L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaPartecipazione(
                        team,
                        hackathon
                )
        );
    }

    @Test
    void recuperaPartecipazioneRifiutaParametriNulli() {
        PartecipazioneRepositoryImpl repository =
                new PartecipazioneRepositoryImpl();

        Hackathon hackathon =
                creaHackathon("Hackathon");

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
                                .recuperaPartecipazione(
                                        null,
                                        hackathon
                                )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository
                                .recuperaPartecipazione(
                                        team,
                                        null
                                )
                )
        );
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon,
            Long idUtente
    ) {
        Utente responsabile =
                new Utente(idUtente);

        Team team = Team.crea(
                "Team " + idUtente,
                responsabile,
                responsabile
        );

        return new Partecipazione(
                hackathon,
                team
        );
    }

    private Hackathon creaHackathon(
            String nome
    ) {
        LocalDate oggi =
                LocalDate.now();

        DatiHackathon dati =
                new DatiHackathon(
                        nome,
                        "Regolamento",
                        "Criteri di valutazione",
                        oggi.plusDays(1),
                        oggi.plusDays(2),
                        oggi.plusDays(5),
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