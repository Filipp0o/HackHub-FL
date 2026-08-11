package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackathonTest {

    private final Utente organizzatore = new Utente(1L);
    private final Utente giudice = new Utente(2L);
    private final Utente mentore = new Utente(3L);

    @Test
    void creaHackathonValidoInStatoInIscrizione() {
        DatiHackathon dati = datiValidi();

        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                giudice,
                List.of(mentore)
        );

        assertAll(
                () -> assertEquals(dati.nome(), hackathon.getNome()),
                () -> assertEquals(
                        dati.regolamento(),
                        hackathon.getRegolamento()
                ),
                () -> assertEquals(
                        dati.criteriValutazione(),
                        hackathon.getCriteriValutazione()
                ),
                () -> assertEquals(
                        dati.scadenzaIscrizioni(),
                        hackathon.getScadenzaIscrizioni()
                ),
                () -> assertEquals(
                        dati.dataInizio(),
                        hackathon.getDataInizio()
                ),
                () -> assertEquals(
                        dati.dataFine(),
                        hackathon.getDataFine()
                ),
                () -> assertEquals(dati.luogo(), hackathon.getLuogo()),
                () -> assertEquals(
                        dati.importoPremio(),
                        hackathon.getImportoPremio()
                ),
                () -> assertEquals(
                        dati.dimensioneMassimaTeam(),
                        hackathon.getDimensioneMassimaTeam()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_ISCRIZIONE,
                        hackathon.getStato()
                ),
                () -> assertEquals(
                        organizzatore,
                        hackathon.getOrganizzatore()
                ),
                () -> assertEquals(giudice, hackathon.getGiudice()),
                () -> assertEquals(
                        List.of(mentore),
                        hackathon.getMentori()
                )
        );
    }

    @Test
    void rifiutaDatiNulli() {
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.crea(
                        null,
                        organizzatore,
                        giudice,
                        List.of(mentore)
                )
        );
    }

    @Test
    void rifiutaTestiObbligatoriVuoti() {
        List<DatiHackathon> datiNonValidi = List.of(
                datiConTesti(" ", "Regolamento", "Criteri", "Roma"),
                datiConTesti("HackHub", " ", "Criteri", "Roma"),
                datiConTesti("HackHub", "Regolamento", " ", "Roma"),
                datiConTesti("HackHub", "Regolamento", "Criteri", " ")
        );

        for (DatiHackathon dati : datiNonValidi) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Hackathon.crea(
                            dati,
                            organizzatore,
                            giudice,
                            List.of(mentore)
                    )
            );
        }
    }

    @Test
    void rifiutaDateObbligatorieNulle() {
        List<DatiHackathon> datiNonValidi = List.of(
                datiConDate(
                        null,
                        LocalDate.of(2026, 10, 10),
                        LocalDate.of(2026, 10, 12)
                ),
                datiConDate(
                        LocalDate.of(2026, 10, 1),
                        null,
                        LocalDate.of(2026, 10, 12)
                ),
                datiConDate(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 10),
                        null
                )
        );

        for (DatiHackathon dati : datiNonValidi) {
            assertThrows(
                    NullPointerException.class,
                    () -> Hackathon.crea(
                            dati,
                            organizzatore,
                            giudice,
                            List.of(mentore)
                    )
            );
        }
    }

    @Test
    void rifiutaScadenzaIscrizioniNonPrecedenteAllaDataInizio() {
        DatiHackathon dati = datiConDate(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.crea(
                        dati,
                        organizzatore,
                        giudice,
                        List.of(mentore)
                )
        );
    }

    @Test
    void rifiutaDataFineNonSuccessivaAllaDataInizio() {
        DatiHackathon dati = datiConDate(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.crea(
                        dati,
                        organizzatore,
                        giudice,
                        List.of(mentore)
                )
        );
    }

    @Test
    void rifiutaImportoPremioNonPositivo() {
        for (BigDecimal importo : List.of(
                BigDecimal.ZERO,
                BigDecimal.valueOf(-1)
        )) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Hackathon.crea(
                            datiConPremio(importo),
                            organizzatore,
                            giudice,
                            List.of(mentore)
                    )
            );
        }
    }

    @Test
    void rifiutaDimensioneMassimaTeamNonPositiva() {
        for (Integer dimensione : List.of(0, -1)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Hackathon.crea(
                            datiConDimensioneTeam(dimensione),
                            organizzatore,
                            giudice,
                            List.of(mentore)
                    )
            );
        }
    }

    @Test
    void rifiutaOrganizzatoreNullo() {
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.crea(
                        datiValidi(),
                        null,
                        giudice,
                        List.of(mentore)
                )
        );
    }

    @Test
    void rifiutaGiudiceNullo() {
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.crea(
                        datiValidi(),
                        organizzatore,
                        null,
                        List.of(mentore)
                )
        );
    }

    @Test
    void rifiutaListaMentoriNulla() {
        assertThrows(
                NullPointerException.class,
                () -> Hackathon.crea(
                        datiValidi(),
                        organizzatore,
                        giudice,
                        null
                )
        );
    }

    @Test
    void rifiutaListaMentoriVuota() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Hackathon.crea(
                        datiValidi(),
                        organizzatore,
                        giudice,
                        List.of()
                )
        );
    }

    @Test
    void proteggeLaListaDeiMentoriDaModificheEsterne() {
        List<Utente> mentori = new ArrayList<>();
        mentori.add(mentore);

        Hackathon hackathon = Hackathon.crea(
                datiValidi(),
                organizzatore,
                giudice,
                mentori
        );

        mentori.clear();

        assertEquals(List.of(mentore), hackathon.getMentori());
        assertThrows(
                UnsupportedOperationException.class,
                () -> hackathon.getMentori().add(new Utente(4L))
        );
    }

    @Test
    void registraPartecipazioneVincitriceValida() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon);

        hackathon.registraPartecipazioneVincitrice(partecipazione);

        assertSame(partecipazione, hackathon.getVincitrice());
    }

    @Test
    void impedisceRegistrazioneVincitoreFuoriDallaValutazione() {
        Hackathon hackathon = creaHackathonValido();
        Partecipazione partecipazione =
                creaPartecipazione(hackathon);

        assertThrows(
                IllegalStateException.class,
                () -> hackathon.registraPartecipazioneVincitrice(
                        partecipazione
                )
        );

        assertNull(hackathon.getVincitrice());
    }

    @Test
    void rifiutaPartecipazioneVincitriceNulla() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        assertThrows(
                NullPointerException.class,
                () -> hackathon.registraPartecipazioneVincitrice(null)
        );

        assertNull(hackathon.getVincitrice());
    }

    @Test
    void rifiutaPartecipazioneVincitriceEsclusa() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon);
        partecipazione.escludi();

        assertThrows(
                IllegalArgumentException.class,
                () -> hackathon.registraPartecipazioneVincitrice(
                        partecipazione
                )
        );

        assertNull(hackathon.getVincitrice());
    }

    @Test
    void impedisceRegistrazioneDiUnSecondoVincitore() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        Partecipazione prima = creaPartecipazione(hackathon);
        Partecipazione seconda = creaPartecipazione(hackathon);

        hackathon.registraPartecipazioneVincitrice(prima);

        assertThrows(
                IllegalStateException.class,
                () -> hackathon.registraPartecipazioneVincitrice(
                        seconda
                )
        );

        assertSame(prima, hackathon.getVincitrice());
    }

    @Test
    void rifiutaPartecipazioneVincitriceDiUnAltroHackathon() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        Hackathon altroHackathon = creaHackathonValido();
        Partecipazione partecipazione =
                creaPartecipazione(altroHackathon);

        assertThrows(
                IllegalArgumentException.class,
                () -> hackathon.registraPartecipazioneVincitrice(
                        partecipazione
                )
        );

        assertNull(hackathon.getVincitrice());
    }

    @Test
    void concludeHackathonConVincitoreRegistrato() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        Partecipazione partecipazione =
                creaPartecipazione(hackathon);
        hackathon.registraPartecipazioneVincitrice(partecipazione);

        hackathon.concludi();

        assertAll(
                () -> assertEquals(
                        StatoHackathon.CONCLUSO,
                        hackathon.getStato()
                ),
                () -> assertSame(
                        partecipazione,
                        hackathon.getVincitrice()
                )
        );
    }

    @Test
    void impedisceConclusioneFuoriDallaValutazione() {
        Hackathon hackathon = creaHackathonValido();

        assertThrows(
                IllegalStateException.class,
                hackathon::concludi
        );

        assertEquals(
                StatoHackathon.IN_ISCRIZIONE,
                hackathon.getStato()
        );
    }

    @Test
    void impedisceConclusioneSenzaVincitore() {
        Hackathon hackathon = creaHackathonValido();
        portaInValutazione(hackathon);

        assertThrows(
                IllegalStateException.class,
                hackathon::concludi
        );

        assertEquals(
                StatoHackathon.IN_VALUTAZIONE,
                hackathon.getStato()
        );
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon
    ) {
        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        return new Partecipazione(hackathon, team);
    }

    private Hackathon creaHackathonValido() {
        return Hackathon.crea(
                datiValidi(),
                organizzatore,
                giudice,
                List.of(mentore)
        );
    }

    private DatiHackathon datiValidi() {
        return new DatiHackathon(
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
    }

    private DatiHackathon datiConTesti(
            String nome,
            String regolamento,
            String criteri,
            String luogo
    ) {
        DatiHackathon dati = datiValidi();

        return new DatiHackathon(
                nome,
                regolamento,
                criteri,
                dati.scadenzaIscrizioni(),
                dati.dataInizio(),
                dati.dataFine(),
                luogo,
                dati.importoPremio(),
                dati.dimensioneMassimaTeam()
        );
    }

    private DatiHackathon datiConDate(
            LocalDate scadenza,
            LocalDate inizio,
            LocalDate fine
    ) {
        DatiHackathon dati = datiValidi();

        return new DatiHackathon(
                dati.nome(),
                dati.regolamento(),
                dati.criteriValutazione(),
                scadenza,
                inizio,
                fine,
                dati.luogo(),
                dati.importoPremio(),
                dati.dimensioneMassimaTeam()
        );
    }

    private DatiHackathon datiConPremio(BigDecimal importo) {
        DatiHackathon dati = datiValidi();

        return new DatiHackathon(
                dati.nome(),
                dati.regolamento(),
                dati.criteriValutazione(),
                dati.scadenzaIscrizioni(),
                dati.dataInizio(),
                dati.dataFine(),
                dati.luogo(),
                importo,
                dati.dimensioneMassimaTeam()
        );
    }

    private DatiHackathon datiConDimensioneTeam(Integer dimensione) {
        DatiHackathon dati = datiValidi();

        return new DatiHackathon(
                dati.nome(),
                dati.regolamento(),
                dati.criteriValutazione(),
                dati.scadenzaIscrizioni(),
                dati.dataInizio(),
                dati.dataFine(),
                dati.luogo(),
                dati.importoPremio(),
                dimensione
        );
    }

    private void portaInValutazione(Hackathon hackathon) {
        try {
            Field campoStato =
                    Hackathon.class.getDeclaredField("stato");

            campoStato.setAccessible(true);
            campoStato.set(
                    hackathon,
                    StatoHackathon.IN_VALUTAZIONE
            );
        } catch (ReflectiveOperationException eccezione) {
            throw new AssertionError(
                    "Impossibile predisporre l'hackathon in valutazione",
                    eccezione
            );
        }
    }
}