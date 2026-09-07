package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamAggiuntaMembroTest {

    private final Utente responsabile = new Utente(1L);
    private final Team team = Team.crea("ByteBuilders", responsabile, responsabile);

    @Test
    void aggiungeMembroAggiornandoDimensioneEConservandoResponsabile() {
        Utente nuovoMembro = new Utente(2L);
        team.assegnaId(10L);

        team.aggiungiMembro(nuovoMembro);

        assertEquals(List.of(responsabile, nuovoMembro), team.getMembri());
        assertEquals(2, team.numeroMembri());
        assertSame(responsabile, team.getResponsabile());
        assertEquals("ByteBuilders", team.getNome());
        assertEquals(10L, team.getId());
    }

    @Test
    void rifiutaMembroNulloSenzaModificareTeam() {
        assertThrows(NullPointerException.class, () -> team.aggiungiMembro(null));

        assertEquals(List.of(responsabile), team.getMembri());
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void rifiutaLaStessaIstanzaGiaPresente() {
        Utente nuovoMembro = new Utente(2L);
        team.aggiungiMembro(nuovoMembro);

        assertThrows(IllegalArgumentException.class,
                () -> team.aggiungiMembro(nuovoMembro));
        assertEquals(List.of(responsabile, nuovoMembro), team.getMembri());
    }

    @Test
    void rifiutaUnAltraIstanzaConLoStessoId() {
        assertThrows(IllegalArgumentException.class,
                () -> team.aggiungiMembro(new Utente(1L)));

        assertEquals(List.of(responsabile), team.getMembri());
        assertSame(responsabile, team.getResponsabile());
    }

    @Test
    void distingueUtentiSenzaIdUsandoIdentitaDiOggetto() {
        Utente primo = Utente.crea("primo@example.com", "hash-primo");
        Utente secondo = Utente.crea("secondo@example.com", "hash-secondo");

        team.aggiungiMembro(primo);
        team.aggiungiMembro(secondo);

        assertEquals(List.of(responsabile, primo, secondo), team.getMembri());
        assertThrows(IllegalArgumentException.class, () -> team.aggiungiMembro(primo));
        assertEquals(3, team.numeroMembri());
    }

    @Test
    void aggiuntaNonModificaSnapshotPrecedenteENonEsponeListaInterna() {
        List<Utente> prima = team.getMembri();
        Utente nuovoMembro = new Utente(2L);

        team.aggiungiMembro(nuovoMembro);

        assertEquals(List.of(responsabile), prima);
        assertEquals(List.of(responsabile, nuovoMembro), team.getMembri());
        assertThrows(UnsupportedOperationException.class,
                () -> team.getMembri().clear());
        assertEquals(2, team.numeroMembri());
    }

    @Test
    void aggiungeMembroAncheATeamRicostruito() {
        Team ricostruito = Team.ricostruisci(
                10L, "ByteBuilders", List.of(responsabile), responsabile
        );
        Utente nuovoMembro = new Utente(2L);

        ricostruito.aggiungiMembro(nuovoMembro);

        assertEquals(List.of(responsabile, nuovoMembro), ricostruito.getMembri());
        assertEquals(2, ricostruito.numeroMembri());
        assertEquals(10L, ricostruito.getId());
    }
}
