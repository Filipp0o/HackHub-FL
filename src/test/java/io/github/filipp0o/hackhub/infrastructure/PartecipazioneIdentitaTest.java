package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartecipazioneIdentitaTest {

    private final InMemoryPartecipazioneRepository repository =
            new InMemoryPartecipazioneRepository();

    @Test
    void assegnaIdAlPrimoSalvataggioSenzaConfondereEntitaNuove() {
        Partecipazione prima = crea();
        Partecipazione seconda = crea();

        assertNull(prima.getId());
        assertNull(seconda.getId());

        repository.salva(prima);
        repository.salva(seconda);

        assertTrue(prima.getId() > 0);
        assertTrue(seconda.getId() > 0);
        assertNotEquals(prima.getId(), seconda.getId());

        assertSame(
                prima,
                repository.recuperaPartecipazione(
                        prima.getTeam(), prima.getHackathon()
                )
        );
        assertSame(
                seconda,
                repository.recuperaPartecipazione(
                        seconda.getTeam(), seconda.getHackathon()
                )
        );
    }

    @Test
    void risalvareConservaIdStatoESottomissioneSenzaDuplicare() {
        Partecipazione partecipazione = crea();
        repository.salva(partecipazione);
        Long id = partecipazione.getId();

        Sottomissione sottomissione = Sottomissione.crea(
                partecipazione, "Progetto"
        );
        partecipazione.escludi();
        repository.salva(partecipazione);

        assertEquals(id, partecipazione.getId());
        assertEquals(
                List.of(partecipazione),
                repository.ottieniPartecipazioni(
                        partecipazione.getHackathon()
                )
        );
        assertSame(sottomissione, partecipazione.getSottomissione());
        assertTrue(
                repository.recuperaPartecipazioniNonEscluse(
                        partecipazione.getHackathon()
                ).isEmpty()
        );
    }

    @Test
    void idEsplicitoRiallineaContatoreEAggiornamentoSostituiscePerId() {
        Partecipazione esistente = crea();
        esistente.assegnaId(50L);
        repository.salva(esistente);

        Partecipazione nuova = crea();
        repository.salva(nuova);
        assertTrue(nuova.getId() > 50L);

        Partecipazione aggiornata = Partecipazione.crea(
                esistente.getHackathon(), esistente.getTeam()
        );
        aggiornata.assegnaId(50L);
        aggiornata.escludi();
        repository.salva(aggiornata);

        assertEquals(
                List.of(aggiornata),
                repository.ottieniPartecipazioni(
                        esistente.getHackathon()
                )
        );
    }

    @Test
    void idDeveEsserePositivoEAssegnabileUnaSolaVolta() {
        Partecipazione partecipazione = crea();

        assertThrows(
                NullPointerException.class,
                () -> partecipazione.assegnaId(null)
        );

        for (long id : new long[]{0L, -1L}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> partecipazione.assegnaId(id)
            );
        }

        assertNull(partecipazione.getId());

        partecipazione.assegnaId(7L);

        assertThrows(
                IllegalStateException.class,
                () -> partecipazione.assegnaId(8L)
        );
        assertEquals(7L, partecipazione.getId());
    }

    private Partecipazione crea() {
        Utente responsabile = new Utente(1L);
        Team team = Team.crea("Team", responsabile, responsabile);
        LocalDate data = LocalDate.of(2026, 10, 1);

        Hackathon hackathon = Hackathon.crea(
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
                new Utente(2L),
                new Utente(3L),
                List.of(new Utente(4L))
        );

        return Partecipazione.crea(hackathon, team);
    }
}