package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamRepositoryImplTest {

    @Test
    void inizialmenteNessunUtenteAppartieneAUnTeam() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        assertFalse(
                repository.verificaAppartenenzaTeam(
                        new Utente(1L)
                )
        );
    }

    @Test
    void rifiutaUtenteNulloDuranteLaVerifica() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.verificaAppartenenzaTeam(null)
        );
    }

    @Test
    void rifiutaTeamNulloDuranteIlSalvataggio() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void riconosceIlMembroDiUnTeamSalvato() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        Utente utente = new Utente(1L);
        Team team = Team.crea(
                "ByteBuilders",
                utente,
                utente
        );

        repository.salva(team);

        assertTrue(
                repository.verificaAppartenenzaTeam(utente)
        );
    }

    @Test
    void riconosceUnUtenteConLoStessoIdentificativo() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        Utente membroSalvato = new Utente(1L);
        Team team = Team.crea(
                "ByteBuilders",
                membroSalvato,
                membroSalvato
        );

        repository.salva(team);

        Utente stessoUtente = new Utente(1L);

        assertTrue(
                repository.verificaAppartenenzaTeam(
                        stessoUtente
                )
        );
    }

    @Test
    void nonRiconosceUnUtenteEstraneoAiTeamSalvati() {
        InMemoryTeamRepository repository =
                new InMemoryTeamRepository();

        Utente membro = new Utente(1L);
        Team team = Team.crea(
                "ByteBuilders",
                membro,
                membro
        );

        repository.salva(team);

        assertFalse(
                repository.verificaAppartenenzaTeam(
                        new Utente(2L)
                )
        );
    }
}