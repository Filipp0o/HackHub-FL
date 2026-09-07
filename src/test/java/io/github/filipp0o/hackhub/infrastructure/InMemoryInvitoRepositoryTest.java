package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryInvitoRepositoryTest {

    private final InvitoRepository repository = new InMemoryInvitoRepository();
    private final Utente responsabile = new Utente(1L);
    private final Utente destinatario = new Utente(2L);
    private final Team team = Team.crea("ByteBuilders", responsabile, responsabile);

    @Test
    void nessunInvitoRestituisceListaVuota() {
        assertTrue(repository.recuperaInvitiRicevuti(destinatario).isEmpty());
    }

    @Test
    void salvaNuoviInvitiConIdDistintiEOrdineDiInserimento() {
        Invito primo = Invito.crea(team, destinatario);
        Invito secondo = Invito.crea(team, destinatario);

        repository.salva(primo);
        repository.salva(secondo);

        assertTrue(primo.getId() > 0);
        assertTrue(secondo.getId() > 0);
        assertNotEquals(primo.getId(), secondo.getId());
        assertEquals(List.of(primo, secondo), repository.recuperaInvitiRicevuti(destinatario));
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void recuperaSoltantoInvitiDelDestinatarioAncheConIstanzaDiversa() {
        Invito proprio = Invito.crea(team, destinatario);
        repository.salva(proprio);
        repository.salva(Invito.crea(team, new Utente(3L)));

        assertEquals(List.of(proprio), repository.recuperaInvitiRicevuti(new Utente(2L)));
        assertTrue(repository.recuperaInvitiRicevuti(new Utente(4L)).isEmpty());
    }

    @Test
    void distingueDestinatariSenzaIdPerIdentitaDiOggetto() {
        Utente primo = Utente.crea("primo@example.com", "hash-primo");
        Utente secondo = Utente.crea("secondo@example.com", "hash-secondo");
        Invito invito = Invito.crea(team, primo);
        repository.salva(invito);

        assertEquals(List.of(invito), repository.recuperaInvitiRicevuti(primo));
        assertTrue(repository.recuperaInvitiRicevuti(secondo).isEmpty());
    }

    @Test
    void salvaAccettazioneEdEscludeInvitoDagliInvitiDisponibili() {
        Invito accettato = Invito.crea(team, destinatario);
        Invito pendente = Invito.crea(team, destinatario);
        repository.salva(accettato);
        repository.salva(pendente);
        Long id = accettato.getId();

        accettato.registraAccettazione();
        repository.salva(accettato);

        assertEquals(id, accettato.getId());
        assertEquals(List.of(pendente), repository.recuperaInvitiRicevuti(destinatario));
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void salvataggiRipetutiNonDuplicanoInvito() {
        Invito invito = Invito.crea(team, destinatario);
        repository.salva(invito);
        Long id = invito.getId();

        repository.salva(invito);

        assertEquals(id, invito.getId());
        assertEquals(List.of(invito), repository.recuperaInvitiRicevuti(destinatario));
    }

    @Test
    void sostituiscePerIdAncheConIstanzaRicostruita() {
        Invito originale = Invito.crea(team, destinatario);
        repository.salva(originale);
        Invito aggiornato = Invito.ricostruisci(
                originale.getId(), team, destinatario, true
        );

        repository.salva(aggiornato);

        assertFalse(originale.isAccettato());
        assertTrue(repository.recuperaInvitiRicevuti(destinatario).isEmpty());
    }

    @Test
    void idRicostruitiNonCollidonoConNuoviInviti() {
        Invito ricostruito = Invito.ricostruisci(10L, team, destinatario, false);
        repository.salva(ricostruito);
        repository.salva(Invito.ricostruisci(3L, team, destinatario, true));
        Invito nuovo = Invito.crea(team, destinatario);

        repository.salva(nuovo);

        assertTrue(nuovo.getId() > 10L);
        assertEquals(List.of(ricostruito, nuovo), repository.recuperaInvitiRicevuti(destinatario));
    }

    @Test
    void listaRestituitaNonPermetteDiModificareRepository() {
        Invito primo = Invito.crea(team, destinatario);
        repository.salva(primo);
        List<Invito> snapshot = repository.recuperaInvitiRicevuti(destinatario);

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        Invito secondo = Invito.crea(team, destinatario);
        repository.salva(secondo);

        assertEquals(List.of(primo), snapshot);
        assertEquals(List.of(primo, secondo), repository.recuperaInvitiRicevuti(destinatario));
    }

    @Test
    void rifiutaArgomentiNulliSenzaModificareDati() {
        Invito invito = Invito.crea(team, destinatario);
        repository.salva(invito);

        assertThrows(NullPointerException.class, () -> repository.salva(null));
        assertThrows(NullPointerException.class, () -> repository.recuperaInvitiRicevuti(null));
        assertEquals(List.of(invito), repository.recuperaInvitiRicevuti(destinatario));
    }
}
