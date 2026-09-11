package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PremioRecoveryTest {

    @TempDir
    Path directory;

    private final Utente organizzatore = new Utente(1L);
    private final Utente responsabile = new Utente(4L);

    private Hackathon concluso() {
        LocalDate oggi = LocalDate.now();

        Hackathon h = Hackathon.crea(
                new DatiHackathon(
                        "H", "R", "C",
                        oggi.minusDays(10),
                        oggi.minusDays(5),
                        oggi.minusDays(1),
                        "Roma", BigDecimal.TEN, 5
                ),
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        h.assegnaId(10L);
        h.aggiornaStato(oggi);

        Partecipazione p = new Partecipazione(
                h,
                Team.crea("Team", responsabile, responsabile)
        );

        h.registraPartecipazioneVincitrice(p);
        h.concludi();
        RiscossionePremio.crea(h);

        return h;
    }

    @Test
    void conservaRicevutaERifiutaDatiDiversiAncheDopoRiavvio() {
        Path file = directory.resolve("pagamenti.properties");

        SistemaPagamentoAdapter primo = new SistemaPagamentoAdapter(file);

        String beneficiario =
                primo.avviaConfigurazioneBeneficiario(responsabile);

        String ricevuta = primo.richiediErogazionePremio(
                BigDecimal.TEN,
                beneficiario,
                "premio-hackathon-10"
        );

        SistemaPagamentoAdapter secondo = new SistemaPagamentoAdapter(file);

        assertEquals(
                beneficiario,
                secondo.avviaConfigurazioneBeneficiario(new Utente(4L))
        );

        assertEquals(
                ricevuta,
                secondo.richiediErogazionePremio(
                        new BigDecimal("10.00"),
                        beneficiario,
                        "premio-hackathon-10"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> secondo.richiediErogazionePremio(
                        BigDecimal.ONE,
                        beneficiario,
                        "premio-hackathon-10"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> secondo.richiediErogazionePremio(
                        BigDecimal.TEN,
                        "altro-beneficiario",
                        "premio-hackathon-10"
                )
        );

        assertNotEquals(
                ricevuta,
                secondo.richiediErogazionePremio(
                        BigDecimal.TEN,
                        beneficiario,
                        "premio-hackathon-11"
                )
        );
    }

    @Test
    void ripristinaConfigurazioneEPermetteNuovoTentativoDopoRiavvio() {
        Path file = directory.resolve("pagamenti.properties");
        Hackathon h = concluso();

        HackathonRepository repository = mock(HackathonRepository.class);

        RuntimeException causa =
                new IllegalArgumentException("Salvataggio fallito");

        doThrow(causa).when(repository).salva(h);

        ConfigurareRiscossionePremioControl control =
                new ConfigurareRiscossionePremioControl(
                        new SistemaPagamentoAdapter(file),
                        repository
                );

        var errore = assertThrows(
                ConfigurareRiscossionePremioControl.ConfigurazioneFallitaException.class,
                () -> control.avviaConfigurazioneRiscossionePremio(
                        h,
                        responsabile
                )
        );

        assertSame(causa, errore.getCause());
        assertNull(h.getRiscossionePremio().getBeneficiaryRef());
        assertEquals(
                StatoRiscossionePremio.DA_CONFIGURARE,
                h.getRiscossionePremio().getStato()
        );

        SistemaPagamentoAdapter nuovoGateway =
                new SistemaPagamentoAdapter(file);

        String beneficiario =
                nuovoGateway.avviaConfigurazioneBeneficiario(responsabile);

        doNothing().when(repository).salva(h);

        new ConfigurareRiscossionePremioControl(
                nuovoGateway,
                repository
        ).avviaConfigurazioneRiscossionePremio(h, responsabile);

        assertEquals(
                beneficiario,
                h.getRiscossionePremio().getBeneficiaryRef()
        );
        assertEquals(
                StatoRiscossionePremio.PRONTA,
                h.getRiscossionePremio().getStato()
        );
    }

    @Test
    void ripristinaPagamentoLocaleMaConservaRicevutaEsterna() {
        Path file = directory.resolve("pagamenti.properties");
        Hackathon h = concluso();

        h.getRiscossionePremio().configura("beneficiary-test");

        HackathonRepository repository = mock(HackathonRepository.class);

        doThrow(new IllegalStateException("Salvataggio fallito"))
                .when(repository).salva(h);

        ErogarePremioControl control = new ErogarePremioControl(
                new SistemaPagamentoAdapter(file),
                repository
        );

        assertThrows(
                ErogarePremioControl.ErogazioneFallitaException.class,
                () -> control.confermaErogazionePremio(organizzatore, h)
        );

        assertNull(h.getRiscossionePremio().getPaymentRef());
        assertEquals(
                StatoRiscossionePremio.PRONTA,
                h.getRiscossionePremio().getStato()
        );

        SistemaPagamentoAdapter nuovoGateway =
                new SistemaPagamentoAdapter(file);

        String ricevuta = nuovoGateway.richiediErogazionePremio(
                BigDecimal.TEN,
                "beneficiary-test",
                "premio-hackathon-10"
        );

        doNothing().when(repository).salva(h);

        new ErogarePremioControl(
                nuovoGateway,
                repository
        ).confermaErogazionePremio(organizzatore, h);

        assertEquals(
                ricevuta,
                h.getRiscossionePremio().getPaymentRef()
        );
        assertEquals(
                StatoRiscossionePremio.EROGATA,
                h.getRiscossionePremio().getStato()
        );
    }
}