package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurareRiscossionePremioControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ConfigurareRiscossionePremioControl(
                                null,
                                new HackathonRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ConfigurareRiscossionePremioControl(
                                new SistemaPagamentoGatewayFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void rifiutaParametriNulli() {
        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        new SistemaPagamentoGatewayFinto(),
                        new HackathonRepositoryFinto()
                );

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaConfigurazioneRiscossionePremio(
                                null,
                                new Utente(4L)
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaConfigurazioneRiscossionePremio(
                                creaHackathonValido(),
                                null
                        )
                )
        );
    }

    @Test
    void configuraRiscossioneESalvaHackathon() {
        Utente responsabileAssegnato = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabileAssegnato
                );

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        /*
         * Istanza diversa, ma stesso id:
         * rappresenta lo stesso Responsabile.
         */
        Utente responsabile = new Utente(4L);

        control.avviaConfigurazioneRiscossionePremio(
                hackathon,
                responsabile
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroConfigurazioni
                ),
                () -> assertSame(
                        responsabile,
                        gateway.responsabileRicevuto
                ),
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
                        hackathon,
                        repository.hackathonSalvato
                ),
                () -> assertEquals(
                        1,
                        repository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfiguraSenzaTeamVincitore() {
        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        Hackathon hackathon = creaHackathonValido();

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        new Utente(4L)
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void nonConfiguraSeUtenteNonEResponsabileDelTeamVincitore() {
        Utente responsabileVincitore = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabileVincitore
                );

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        Utente altroUtente = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        altroUtente
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfiguraSenzaRiscossioneInizializzata() {
        Utente responsabile = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabile
                );

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabile
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfiguraSeRiscossioneNonEDaConfigurare() {
        Utente responsabile = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabile
                );

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        riscossione.configura("beneficiary-esistente");

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabile
                )
        );

        assertAll(
                () -> assertEquals(
                        0,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.PRONTA,
                        riscossione.getStato()
                ),
                () -> assertEquals(
                        "beneficiary-esistente",
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                )
        );
    }

    @Test
    void mantieneRiscossioneDaConfigurareSeGatewayFallisce() {
        Utente responsabile = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabile
                );

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        gateway.fallisceConfigurazione = true;

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabile
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertNull(
                        riscossione.getPaymentRef()
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    @Test
    void nonSalvaSeGatewayRestituisceBeneficiaryRefNonValido() {
        Utente responsabile = new Utente(4L);

        Hackathon hackathon =
                creaHackathonConclusoConVincitore(
                        responsabile
                );

        RiscossionePremio riscossione =
                RiscossionePremio.crea(hackathon);

        SistemaPagamentoGatewayFinto gateway =
                new SistemaPagamentoGatewayFinto();

        gateway.beneficiaryRefDaRestituire = "   ";

        HackathonRepositoryFinto repository =
                new HackathonRepositoryFinto();

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        gateway,
                        repository
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabile
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        gateway.numeroConfigurazioni
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertNull(
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertEquals(
                        0,
                        repository.numeroSalvataggi
                ),
                () -> assertNull(
                        repository.hackathonSalvato
                )
        );
    }

    private Hackathon creaHackathonConclusoConVincitore(
            Utente responsabile
    ) {
        Hackathon hackathon = creaHackathonValido();

        hackathon.aggiornaStato(
                LocalDate.of(3000, 1, 1)
        );

        Team team = Team.crea(
                "Team vincitore",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        hackathon,
                        team
                );

        hackathon.registraPartecipazioneVincitrice(
                partecipazione
        );

        hackathon.concludi();

        assertEquals(
                StatoHackathon.CONCLUSO,
                hackathon.getStato()
        );

        return hackathon;
    }

    private Hackathon creaHackathonValido() {
        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Roma",
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

    private static class SistemaPagamentoGatewayFinto
            implements SistemaPagamentoGateway {

        private String beneficiaryRefDaRestituire =
                "beneficiary-123";

        private boolean fallisceConfigurazione;

        private int numeroConfigurazioni;

        private Utente responsabileRicevuto;

        @Override
        public String avviaConfigurazioneBeneficiario(
                Utente responsabileTeam
        ) {
            numeroConfigurazioni++;
            responsabileRicevuto = responsabileTeam;

            if (fallisceConfigurazione) {
                throw new IllegalStateException(
                        "Configurazione beneficiario non completata"
                );
            }

            return beneficiaryRefDaRestituire;
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

        private int numeroSalvataggi;

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
            numeroSalvataggi++;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}