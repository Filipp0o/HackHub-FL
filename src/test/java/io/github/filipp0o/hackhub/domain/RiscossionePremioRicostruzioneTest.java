package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiscossionePremioRicostruzioneTest {

    @Test
    void ricostruisceTuttiGliStatiConRiferimentiEAssociazione() {
        for (StatoRiscossionePremio stato : StatoRiscossionePremio.values()) {
            Hackathon h = creaHackathon(true);

            String beneficiario =
                    stato == StatoRiscossionePremio.DA_CONFIGURARE
                            ? null : "beneficiario-123";
            String pagamento =
                    stato == StatoRiscossionePremio.EROGATA
                            ? "pagamento-456" : null;

            RiscossionePremio r = RiscossionePremio.ricostruisci(
                    42L, h, stato, beneficiario, pagamento
            );

            assertEquals(42L, r.getId());
            assertEquals(stato, r.getStato());
            assertEquals(beneficiario, r.getBeneficiaryRef());
            assertEquals(pagamento, r.getPaymentRef());
            assertSame(h, r.getHackathon());
            assertSame(r, h.getRiscossionePremio());

            if (stato == StatoRiscossionePremio.EROGATA) {
                assertThrows(
                        IllegalStateException.class,
                        () -> r.registraErogazione("altro")
                );
                assertEquals(pagamento, r.getPaymentRef());
            }
        }
    }

    @Test
    void rifiutaCombinazioniIncoerentiSenzaCollegareOggettiParziali() {
        for (StatoRiscossionePremio stato : StatoRiscossionePremio.values()) {
            for (String beneficiario : new String[]{
                    null, "", " ", "beneficiario"
            }) {
                for (String pagamento : new String[]{
                        null, "", " ", "pagamento"
                }) {
                    boolean valido = switch (stato) {
                        case DA_CONFIGURARE ->
                                beneficiario == null && pagamento == null;
                        case PRONTA ->
                                "beneficiario".equals(beneficiario)
                                        && pagamento == null;
                        case EROGATA ->
                                "beneficiario".equals(beneficiario)
                                        && "pagamento".equals(pagamento);
                    };

                    if (valido) {
                        continue;
                    }

                    Hackathon h = creaHackathon(true);

                    assertThrows(
                            IllegalArgumentException.class,
                            () -> RiscossionePremio.ricostruisci(
                                    42L, h, stato, beneficiario, pagamento
                            )
                    );
                    assertNull(h.getRiscossionePremio());
                }
            }
        }
    }

    @Test
    void idInvalidoNonCollegaRiscossione() {
        Hackathon h = creaHackathon(true);

        assertThrows(
                NullPointerException.class,
                () -> RiscossionePremio.ricostruisci(
                        null, h, StatoRiscossionePremio.DA_CONFIGURARE,
                        null, null
                )
        );

        for (long id : new long[]{0L, -1L}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RiscossionePremio.ricostruisci(
                            id, h, StatoRiscossionePremio.DA_CONFIGURARE,
                            null, null
                    )
            );
        }

        assertNull(h.getRiscossionePremio());
    }

    @Test
    void rifiutaHackathonOStatoAssenti() {
        Hackathon h = creaHackathon(true);

        assertThrows(
                NullPointerException.class,
                () -> RiscossionePremio.ricostruisci(
                        42L, null, StatoRiscossionePremio.DA_CONFIGURARE,
                        null, null
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> RiscossionePremio.ricostruisci(
                        42L, h, null, null, null
                )
        );

        assertNull(h.getRiscossionePremio());
    }

    @Test
    void rifiutaHackathonNonConcluso() {
        Hackathon h = creaHackathon(false);

        assertThrows(
                IllegalStateException.class,
                () -> RiscossionePremio.ricostruisci(
                        42L, h, StatoRiscossionePremio.PRONTA,
                        "beneficiario", null
                )
        );

        assertNull(h.getRiscossionePremio());
    }

    @Test
    void secondaRicostruzioneNonSostituisceRiscossioneEsistente() {
        Hackathon h = creaHackathon(true);

        RiscossionePremio prima = RiscossionePremio.ricostruisci(
                42L, h, StatoRiscossionePremio.EROGATA,
                "beneficiario", "pagamento"
        );

        assertThrows(
                IllegalStateException.class,
                () -> RiscossionePremio.ricostruisci(
                        43L, h, StatoRiscossionePremio.DA_CONFIGURARE,
                        null, null
                )
        );

        assertSame(prima, h.getRiscossionePremio());
        assertEquals("pagamento", prima.getPaymentRef());
    }

    @Test
    void creazioneRestaSenzaIdEAssegnazioneAvvieneUnaSolaVolta() {
        RiscossionePremio r = RiscossionePremio.crea(
                creaHackathon(true)
        );

        assertNull(r.getId());
        assertEquals(StatoRiscossionePremio.DA_CONFIGURARE, r.getStato());

        assertThrows(
                NullPointerException.class,
                () -> r.assegnaId(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> r.assegnaId(0L)
        );
        assertNull(r.getId());

        r.assegnaId(7L);

        assertThrows(
                IllegalStateException.class,
                () -> r.assegnaId(8L)
        );
        assertEquals(7L, r.getId());
    }

    private Hackathon creaHackathon(boolean concluso) {
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

        if (concluso) {
            h.aggiornaStato(data.plusDays(3));

            Utente responsabile = new Utente(4L);
            h.registraPartecipazioneVincitrice(
                    Partecipazione.crea(
                            h,
                            Team.crea("Team", responsabile, responsabile)
                    )
            );

            h.concludi();
        }

        return h;
    }
}