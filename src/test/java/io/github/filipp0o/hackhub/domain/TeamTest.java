package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TeamTest {

    @Test
    void creaTeamValidoConResponsabileTraIMembri() {
        Utente responsabile = new Utente(1L);

        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        assertEquals("ByteBuilders", team.getNome());
        assertEquals(responsabile, team.getResponsabile());
        assertEquals(List.of(responsabile), team.getMembri());
    }

    @Test
    void rifiutaResponsabileCheNonAppartieneAiMembri() {
        Utente membroIniziale = new Utente(1L);
        Utente responsabile = new Utente(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> Team.crea(
                        "ByteBuilders",
                        membroIniziale,
                        responsabile
                )
        );
    }

    @Test
    void rifiutaNomeNullo() {
        Utente responsabile = new Utente(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> Team.crea(null, responsabile, responsabile)
        );
    }

    @Test
    void rifiutaNomeVuoto() {
        Utente responsabile = new Utente(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> Team.crea("   ", responsabile, responsabile)
        );
    }

    @Test
    void rifiutaMembroInizialeNullo() {
        Utente responsabile = new Utente(1L);

        assertThrows(
                NullPointerException.class,
                () -> Team.crea(
                        "ByteBuilders",
                        null,
                        responsabile
                )
        );
    }

    @Test
    void rifiutaResponsabileNullo() {
        Utente membroIniziale = new Utente(1L);

        assertThrows(
                NullPointerException.class,
                () -> Team.crea(
                        "ByteBuilders",
                        membroIniziale,
                        null
                )
        );
    }

    @Test
    void nonEsponeLaListaInternaDeiMembri() {
        Utente responsabile = new Utente(1L);
        Team team = Team.crea(
                "ByteBuilders",
                responsabile,
                responsabile
        );

        List<Utente> membri = team.getMembri();

        assertThrows(
                UnsupportedOperationException.class,
                () -> membri.add(new Utente(2L))
        );
    }
}