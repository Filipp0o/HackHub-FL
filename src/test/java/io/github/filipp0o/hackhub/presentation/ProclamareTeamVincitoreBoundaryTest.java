package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.ProclamareTeamVincitoreControl;
import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProclamareTeamVincitoreBoundaryTest {

    private Utente organizzatore;
    private Hackathon hackathon;
    private Partecipazione primaPartecipazione;
    private Partecipazione secondaPartecipazione;
    private HackathonRepositoryFinto hackathonRepository;
    private ProclamareTeamVincitoreBoundary boundary;

    @BeforeEach
    void configuraBoundary() {
        organizzatore = new Utente(1L);
        hackathon = creaHackathonInValutazione();

        primaPartecipazione = creaPartecipazioneValutata(
                "Team Alpha",
                4L,
                BigDecimal.valueOf(8)
        );

        secondaPartecipazione = creaPartecipazioneValutata(
                "Team Beta",
                5L,
                BigDecimal.valueOf(9)
        );

        PartecipazioneRepositoryFinto
                partecipazioneRepository =
                new PartecipazioneRepositoryFinto(
                        List.of(
                                primaPartecipazione,
                                secondaPartecipazione
                        )
                );

        hackathonRepository =
                new HackathonRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        hackathonRepository,
                        new SegnalazioneRepositoryFinto()
                );

        boundary = new ProclamareTeamVincitoreBoundary(
                control
        );
    }

    @Test
    void mostraTeamAmmissibili() {
        List<ProclamareTeamVincitoreBoundary.TeamAmmissibile>
                risultato = boundary
                .selezionaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                );

        assertAll(
                () -> assertEquals(2, risultato.size()),
                () -> assertEquals(
                        primaPartecipazione.getId(),
                        risultato.getFirst().partecipazioneId()
                ),
                () -> assertEquals(
                        "Team Alpha",
                        risultato.getFirst().nomeTeam()
                ),
                () -> assertEquals(
                        "Sottomissione Team Alpha",
                        risultato.getFirst().sottomissione()
                ),
                () -> assertEquals(
                        "Buona sottomissione",
                        risultato.getFirst().giudizio()
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(8),
                        risultato.getFirst().punteggio()
                )
        );
    }

    @Test
    void mostraRiepilogoPrimaDellaConferma() {
        ProclamareTeamVincitoreBoundary
                .RiepilogoProclamazione risultato =
                boundary.selezionaTeamVincitore(
                        hackathon,
                        secondaPartecipazione
                );

        assertAll(
                () -> assertEquals(
                        secondaPartecipazione.getId(),
                        risultato.partecipazioneId()
                ),
                () -> assertEquals(
                        "Team Beta",
                        risultato.nomeTeam()
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(9),
                        risultato.punteggio()
                ),
                () -> assertEquals(
                        BigDecimal.valueOf(5000),
                        risultato.importoPremio()
                ),
                () -> assertNull(
                        hackathon.getVincitrice()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
                )
        );
    }

    @Test
    void confermaProclamazione() {
        boundary.confermaProclamazione(
                organizzatore,
                hackathon,
                secondaPartecipazione
        );

        assertAll(
                () -> assertSame(
                        secondaPartecipazione,
                        hackathon.getVincitrice()
                ),
                () -> assertEquals(
                        StatoHackathon.CONCLUSO,
                        hackathon.getStato()
                ),
                () -> assertNotNull(
                        hackathon.getRiscossionePremio()
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        hackathon
                                .getRiscossionePremio()
                                .getStato()
                ),
                () -> assertSame(
                        hackathon,
                        hackathonRepository.hackathonSalvato
                )
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ProclamareTeamVincitoreBoundary(
                        null
                )
        );
    }

    private Hackathon creaHackathonInValutazione() {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                oggi.minusDays(10),
                oggi.minusDays(5),
                oggi.minusDays(1),
                "Camerino",
                BigDecimal.valueOf(5000),
                5
        );

        Hackathon risultato = Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        risultato.aggiornaStato(oggi);
        return risultato;
    }

    private Partecipazione creaPartecipazioneValutata(
            String nomeTeam,
            Long responsabileId,
            BigDecimal punteggio
    ) {
        Utente responsabile = new Utente(
                responsabileId
        );

        Team team = Team.crea(
                nomeTeam,
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
                        "Sottomissione " + nomeTeam
                );

        Valutazione.crea(
                sottomissione,
                hackathon.getGiudice(),
                new DatiValutazione(
                        "Buona sottomissione",
                        punteggio
                )
        );

        return partecipazione;
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private final List<Partecipazione> partecipazioni;

        private PartecipazioneRepositoryFinto(
                List<Partecipazione> partecipazioni
        ) {
            this.partecipazioni = partecipazioni;
        }

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            return partecipazioni;
        }

        @Override
        public List<Partecipazione>
        recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            return partecipazioni.stream()
                    .filter(partecipazione ->
                            partecipazione.getHackathon()
                                    == hackathon
                    )
                    .toList();
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
        }

        @Override
        public boolean esistePartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }

        @Override
        public Partecipazione recuperaPartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private Hackathon hackathonSalvato;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            return List.of();
        }

        @Override
        public void salva(Hackathon hackathon) {
            hackathonSalvato = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        @Override
        public List<Segnalazione>
        ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            return List.of();
        }

        @Override
        public void salva(
                Segnalazione segnalazione
        ) {
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
        }

        @Override
        public void salvaNotifica(
                NotificaSegnalazione notifica
        ) {
        }
    }
}