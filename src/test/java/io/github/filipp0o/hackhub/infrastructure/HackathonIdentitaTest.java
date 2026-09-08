package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackathonIdentitaTest {

    private final InMemoryHackathonRepository repository =
            new InMemoryHackathonRepository(
                    new InMemoryPartecipazioneRepository()
            );

    @Test
    void assegnaIdDistintiAlSalvataggioSenzaSostituireHackathonNuovi() {
        Hackathon primo = crea();
        Hackathon secondo = crea();

        assertNull(primo.getId());
        assertNull(secondo.getId());

        repository.salva(primo);
        repository.salva(secondo);

        assertTrue(primo.getId() > 0);
        assertTrue(secondo.getId() > 0);
        assertNotEquals(primo.getId(), secondo.getId());
        assertSame(primo, repository.recuperaHackathon(primo.getId()));
        assertSame(secondo, repository.recuperaHackathon(secondo.getId()));
        assertEquals(2, repository.ottieniTuttiHackathon().size());
    }

    @Test
    void risalvareConservaIdStatoEVincitriceSenzaDuplicare() {
        Hackathon hackathon = crea();
        repository.salva(hackathon);
        Long id = hackathon.getId();

        hackathon.aggiornaStato(
                hackathon.getDataFine().plusDays(1)
        );

        Utente responsabile = new Utente(5L);
        Partecipazione vincitrice = Partecipazione.crea(
                hackathon,
                Team.crea("Vincitore", responsabile, responsabile)
        );

        hackathon.registraPartecipazioneVincitrice(vincitrice);
        hackathon.concludi();
        repository.salva(hackathon);

        assertEquals(id, hackathon.getId());
        assertEquals(
                List.of(hackathon),
                repository.ottieniTuttiHackathon()
        );
        assertEquals(
                TipoStatoHackathon.CONCLUSO,
                repository.recuperaHackathon(id).getStato()
        );
        assertSame(
                vincitrice,
                repository.recuperaHackathon(id).getVincitrice()
        );
    }

    @Test
    void idEsplicitoRiallineaContatoreEAggiornamentoSostituiscePerId() {
        Hackathon primo = crea();
        primo.assegnaId(50L);
        repository.salva(primo);

        Hackathon nuovo = crea();
        repository.salva(nuovo);

        assertTrue(nuovo.getId() > 50L);

        Hackathon aggiornato = crea();
        aggiornato.assegnaId(50L);
        repository.salva(aggiornato);

        assertSame(aggiornato, repository.recuperaHackathon(50L));
        assertEquals(2, repository.ottieniTuttiHackathon().size());
    }

    @Test
    void idDeveEsserePositivoEAssegnabileUnaSolaVolta() {
        Hackathon hackathon = crea();

        assertThrows(
                NullPointerException.class,
                () -> hackathon.assegnaId(null)
        );

        for (long id : new long[]{0L, -1L}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> hackathon.assegnaId(id)
            );
        }

        assertNull(hackathon.getId());

        hackathon.assegnaId(7L);

        assertThrows(
                IllegalStateException.class,
                () -> hackathon.assegnaId(8L)
        );
        assertEquals(7L, hackathon.getId());
    }

    private Hackathon crea() {
        LocalDate data = LocalDate.of(2026, 10, 1);

        return Hackathon.crea(
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
    }
}