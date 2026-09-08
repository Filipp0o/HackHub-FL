package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackathonRicostruzioneTest {

    private final Utente organizzatore = new Utente(1L);
    private final Utente giudice = new Utente(2L);
    private final Utente mentore = new Utente(3L);
    private final Utente responsabile = new Utente(4L);

    private final Team team = Team.crea(
            "Team", responsabile, responsabile
    );

    private final LocalDateTime timestamp =
            LocalDateTime.of(2025, 4, 10, 12, 30);

    private final DatiHackathon dati = new DatiHackathon(
            "Hackathon",
            "Regolamento",
            "Criteri",
            LocalDate.of(2025, 4, 1),
            LocalDate.of(2025, 4, 2),
            LocalDate.of(2025, 4, 3),
            "Camerino",
            BigDecimal.TEN,
            5
    );

    private DatiRipristinoHackathon salvato(
            TipoStatoHackathon stato,
            DatiRipristinoHackathon.Vincitrice vincitrice,
            DatiRipristinoHackathon.Riscossione riscossione
    ) {
        return new DatiRipristinoHackathon(
                10L, dati, organizzatore, giudice,
                List.of(mentore), stato, vincitrice, riscossione
        );
    }

    private DatiRipristinoHackathon.Vincitrice vincitrice() {
        return new DatiRipristinoHackathon.Vincitrice(
                20L,
                team,
                StatoPartecipazione.ATTIVA,
                new DatiRipristinoHackathon.SottomissioneSalvata(
                        30L,
                        "Progetto",
                        new DatiRipristinoHackathon.ValutazioneSalvata(
                                40L,
                                giudice,
                                new DatiValutazione("Ottimo", BigDecimal.TEN),
                                timestamp
                        )
                )
        );
    }

    @Test
    void ripristinaStatiNonConclusiSenzaRicalcolarliDalleDate() {
        for (TipoStatoHackathon tipo : List.of(
                TipoStatoHackathon.IN_ISCRIZIONE,
                TipoStatoHackathon.IN_CORSO,
                TipoStatoHackathon.IN_VALUTAZIONE
        )) {
            Hackathon h = Hackathon.ricostruisci(
                    salvato(tipo, null, null)
            );

            assertEquals(10L, h.getId());
            assertEquals(tipo, h.getStato());
            assertEquals(
                    tipo != TipoStatoHackathon.IN_ISCRIZIONE,
                    h.consenteSegnalazioni()
            );
            assertEquals(
                    tipo == TipoStatoHackathon.IN_VALUTAZIONE,
                    h.consenteValutazioni()
            );

            assertEquals(dati.nome(), h.getNome());
            assertEquals(dati.regolamento(), h.getRegolamento());
            assertEquals(
                    dati.criteriValutazione(), h.getCriteriValutazione()
            );
            assertEquals(
                    dati.scadenzaIscrizioni(), h.getScadenzaIscrizioni()
            );
            assertEquals(dati.dataInizio(), h.getDataInizio());
            assertEquals(dati.dataFine(), h.getDataFine());
            assertEquals(dati.luogo(), h.getLuogo());
            assertEquals(dati.importoPremio(), h.getImportoPremio());
            assertEquals(
                    dati.dimensioneMassimaTeam(),
                    h.getDimensioneMassimaTeam()
            );

            assertSame(organizzatore, h.getOrganizzatore());
            assertSame(giudice, h.getGiudice());
            assertEquals(List.of(mentore), h.getMentori());
            assertNull(h.getVincitrice());
            assertNull(h.getRiscossionePremio());
        }
    }

    @Test
    void ricostruisceConclusoConInteraCatenaEPagamento() {
        Hackathon h = Hackathon.ricostruisci(
                salvato(
                        TipoStatoHackathon.CONCLUSO,
                        vincitrice(),
                        new DatiRipristinoHackathon.Riscossione(
                                50L,
                                StatoRiscossionePremio.EROGATA,
                                "beneficiario",
                                "pagamento"
                        )
                )
        );

        assertEquals(TipoStatoHackathon.CONCLUSO, h.getStato());
        assertTrue(h.consenteRiscossionePremio());

        var p = h.getVincitrice();
        assertEquals(20L, p.getId());
        assertSame(h, p.getHackathon());
        assertSame(team, p.getTeam());

        var s = p.getSottomissione();
        assertEquals(30L, s.getId());
        assertSame(p, s.getPartecipazione());
        assertEquals("Progetto", s.getContenuto());

        var v = s.getValutazione();
        assertEquals(40L, v.getId());
        assertSame(s, v.getSottomissione());
        assertEquals(timestamp, v.getDataOra());

        var r = h.getRiscossionePremio();
        assertEquals(50L, r.getId());
        assertSame(h, r.getHackathon());
        assertEquals(StatoRiscossionePremio.EROGATA, r.getStato());
        assertEquals("beneficiario", r.getBeneficiaryRef());
        assertEquals("pagamento", r.getPaymentRef());
    }

    @Test
    void conservaVincitriceInValutazioneESupportaAssociazioniOpzionali() {
        var v = new DatiRipristinoHackathon.Vincitrice(
                20L, team, StatoPartecipazione.ATTIVA, null
        );

        for (TipoStatoHackathon tipo : List.of(
                TipoStatoHackathon.IN_VALUTAZIONE,
                TipoStatoHackathon.CONCLUSO
        )) {
            Hackathon h = Hackathon.ricostruisci(
                    salvato(tipo, v, null)
            );

            assertSame(h, h.getVincitrice().getHackathon());
            assertNull(h.getVincitrice().getSottomissione());
            assertNull(h.getRiscossionePremio());
        }
    }

    @Test
    void rifiutaCombinazioniIncoerentiDiStatoEVincitrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        salvato(TipoStatoHackathon.CONCLUSO, null, null)
                )
        );

        for (TipoStatoHackathon tipo : List.of(
                TipoStatoHackathon.IN_ISCRIZIONE,
                TipoStatoHackathon.IN_CORSO
        )) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Hackathon.ricostruisci(
                            salvato(tipo, vincitrice(), null)
                    )
            );
        }

        var esclusa = new DatiRipristinoHackathon.Vincitrice(
                20L, team, StatoPartecipazione.ESCLUSA, null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        salvato(TipoStatoHackathon.CONCLUSO, esclusa, null)
                )
        );
    }

    @Test
    void rifiutaRiscossionePrimaDellaConclusioneERiferimentiInvalidi() {
        var r = new DatiRipristinoHackathon.Riscossione(
                50L, StatoRiscossionePremio.EROGATA,
                "beneficiario", "pagamento"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        salvato(
                                TipoStatoHackathon.IN_VALUTAZIONE,
                                vincitrice(),
                                r
                        )
                )
        );

        var incoerente = new DatiRipristinoHackathon.Riscossione(
                50L, StatoRiscossionePremio.EROGATA,
                "beneficiario", null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        salvato(
                                TipoStatoHackathon.CONCLUSO,
                                vincitrice(),
                                incoerente
                        )
                )
        );
    }

    @Test
    void rifiutaIdOStatoAssentiESenzaModificareTeamEsterno() {
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.ricostruisci(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.ricostruisci(salvato(null, null, null))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        new DatiRipristinoHackathon(
                                0L, dati, organizzatore, giudice,
                                List.of(mentore),
                                TipoStatoHackathon.IN_CORSO,
                                null, null
                        )
                )
        );

        var errata = new DatiRipristinoHackathon.Vincitrice(
                0L, team, StatoPartecipazione.ATTIVA, null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.ricostruisci(
                        salvato(TipoStatoHackathon.CONCLUSO, errata, null)
                )
        );

        assertEquals(List.of(responsabile), team.getMembri());
    }
}