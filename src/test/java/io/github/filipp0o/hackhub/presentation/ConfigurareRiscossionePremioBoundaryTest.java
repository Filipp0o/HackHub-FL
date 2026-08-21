package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ConfigurareRiscossionePremioControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigurareRiscossionePremioBoundaryTest {

    private Utente responsabile;
    private Hackathon hackathon;
    private RiscossionePremio riscossione;
    private SistemaPagamentoGatewayFinto gateway;
    private HackathonRepositoryFinto repository;

    @BeforeEach
    void configuraDati() {
        responsabile = new Utente(4L);
        hackathon = creaHackathonConclusoConVincitore();
        riscossione = RiscossionePremio.crea(hackathon);
        gateway = new SistemaPagamentoGatewayFinto();
        repository = new HackathonRepositoryFinto();
    }

    @Test
    void configuraRiscossioneDelPremio() {
        ConfigurareRiscossionePremioBoundary boundary =
                creaBoundary();

        boundary.selezionaConfigurazioneRiscossionePremio(
                hackathon,
                responsabile
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.PRONTA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "beneficiary-123",
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertNull(
                        riscossione.getPaymentRef()
                ),
                () -> assertSame(
                        responsabile,
                        gateway.responsabileRicevuto
                ),
                () -> assertSame(
                        hackathon,
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void nonModificaRiscossioneSeConfigurazioneFallisce() {
        gateway.fallisceConfigurazione = true;

        ConfigurareRiscossionePremioBoundary boundary =
                creaBoundary();

        assertThrows(
                IllegalStateException.class,
                () -> boundary
                        .selezionaConfigurazioneRiscossionePremio(
                                hackathon,
                                responsabile
                        )
        );

        assertAll(
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void rifiutaControlNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new ConfigurareRiscossionePremioBoundary(
                        null
                )
        );
    }

    private ConfigurareRiscossionePremioBoundary
    creaBoundary() {
        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        return new ConfigurareRiscossionePremioBoundary(
                control
        );
    }

    private Hackathon creaHackathonConclusoConVincitore() {
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
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );

        risultato.aggiornaStato(oggi);

        Team team = Team.crea(
                "Team vincitore",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        risultato,
                        team
                );

        risultato.registraPartecipazioneVincitrice(
                partecipazione
        );

        risultato.concludi();

        assertEquals(
                StatoHackathon.CONCLUSO,
                risultato.getStato()
        );

        return risultato;
    }

    private static class SistemaPagamentoGatewayFinto
            implements SistemaPagamentoGateway {

        private boolean fallisceConfigurazione;
        private Utente responsabileRicevuto;

        @Override
        public String avviaConfigurazioneBeneficiario(
                Utente responsabileTeam
        ) {
            responsabileRicevuto = responsabileTeam;

            if (fallisceConfigurazione) {
                throw new IllegalStateException(
                        "Configurazione non completata"
                );
            }

            return "beneficiary-123";
        }

        @Override
        public String richiediErogazionePremio(
                BigDecimal importoPremio,
                String beneficiaryRef
        ) {
            return "payment-123";
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
        public void salva(
                Hackathon hackathon
        ) {
            hackathonSalvato = hackathon;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}