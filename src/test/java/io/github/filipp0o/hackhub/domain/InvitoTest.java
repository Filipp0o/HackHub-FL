package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvitoTest {

    private final Utente responsabile = new Utente(1L);
    private final Utente destinatario = new Utente(2L);
    private final Team team = Team.crea("Team di prova", responsabile, responsabile);

    @Test
    void nuovoInvitoConservaAssociazioniENasceNonAccettatoSenzaId() {
        Invito invito = Invito.crea(team, destinatario);

        assertNull(invito.getId());
        assertSame(team, invito.ottieniTeam());
        assertSame(destinatario, invito.getDestinatario());
        assertFalse(invito.isAccettato());
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void creazioneRichiedeTeamEDestinatario() {
        assertThrows(NullPointerException.class, () -> Invito.crea(null, destinatario));
        assertThrows(NullPointerException.class, () -> Invito.crea(team, null));
    }

    @Test
    void accettazioneAggiornaInvitoEConservaComposizioneTeam() {
        Invito invito = Invito.crea(team, destinatario);

        invito.registraAccettazione();

        assertTrue(invito.isAccettato());
        assertEquals(1, team.numeroMembri());
        assertSame(responsabile, team.getMembri().getFirst());
        assertSame(destinatario, invito.getDestinatario());
    }

    @Test
    void secondaAccettazioneVieneRifiutata() {
        Invito invito = Invito.crea(team, destinatario);
        invito.registraAccettazione();

        assertThrows(IllegalStateException.class, invito::registraAccettazione);
        assertTrue(invito.isAccettato());
    }

    @Test
    void idPositivoVieneAssegnatoUnaSolaVolta() {
        Invito invito = Invito.crea(team, destinatario);
        invito.assegnaId(10L);

        assertEquals(10L, invito.getId());
        assertThrows(IllegalStateException.class, () -> invito.assegnaId(10L));
        assertThrows(IllegalStateException.class, () -> invito.assegnaId(11L));
        assertEquals(10L, invito.getId());
    }

    @Test
    void idNonValidoNonModificaInvito() {
        Invito invito = Invito.crea(team, destinatario);

        assertThrows(NullPointerException.class, () -> invito.assegnaId(null));
        assertThrows(IllegalArgumentException.class, () -> invito.assegnaId(0L));
        assertThrows(IllegalArgumentException.class, () -> invito.assegnaId(-1L));
        assertNull(invito.getId());
        invito.assegnaId(1L);
        assertEquals(1L, invito.getId());
    }

    @Test
    void ricostruzioneRipristinaEntrambiGliStatiELeAssociazioni() {
        for (boolean accettato : new boolean[]{false, true}) {
            Invito invito = Invito.ricostruisci(10L, team, destinatario, accettato);

            assertEquals(10L, invito.getId());
            assertSame(team, invito.ottieniTeam());
            assertSame(destinatario, invito.getDestinatario());
            assertEquals(accettato, invito.isAccettato());
            assertEquals(1, team.numeroMembri());
            if (accettato) {
                assertThrows(IllegalStateException.class, invito::registraAccettazione);
            } else {
                invito.registraAccettazione();
                assertTrue(invito.isAccettato());
            }
        }
    }

    @Test
    void ricostruzioneRifiutaDatiIncompletiOIdNonValido() {
        assertThrows(NullPointerException.class,
                () -> Invito.ricostruisci(null, team, destinatario, false));
        assertThrows(IllegalArgumentException.class,
                () -> Invito.ricostruisci(0L, team, destinatario, false));
        assertThrows(IllegalArgumentException.class,
                () -> Invito.ricostruisci(-1L, team, destinatario, false));
        assertThrows(NullPointerException.class,
                () -> Invito.ricostruisci(1L, null, destinatario, false));
        assertThrows(NullPointerException.class,
                () -> Invito.ricostruisci(1L, team, null, false));
        assertThrows(NullPointerException.class,
                () -> Invito.ricostruisci(1L, team, destinatario, null));
    }
}
