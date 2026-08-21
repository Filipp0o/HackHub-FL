package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreareTeamControlTest {

    @Test
    void rifiutaRepositoryNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new CreareTeamControl(null)
        );
    }

    @Test
    void verificaCheUtenteNonAppartengaGiaAUnTeam() {
        TeamRepositoryFinto repository = new TeamRepositoryFinto();
        CreareTeamControl control = new CreareTeamControl(repository);
        Utente utente = new Utente(1L);

        control.avviaCreazioneTeam(utente);

        assertSame(utente, repository.utenteVerificato);
    }

    @Test
    void impedisceCreazioneSeUtenteAppartieneGiaAUnTeam() {
        TeamRepositoryFinto repository = new TeamRepositoryFinto();
        repository.appartieneGiaAUnTeam = true;
        CreareTeamControl control = new CreareTeamControl(repository);

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaCreazioneTeam(new Utente(1L))
        );
    }

    @Test
    void rifiutaUtenteNulloAllAvvio() {
        CreareTeamControl control = new CreareTeamControl(
                new TeamRepositoryFinto()
        );

        assertThrows(
                NullPointerException.class,
                () -> control.avviaCreazioneTeam(null)
        );
    }

    @Test
    void accettaNomeValido() {
        CreareTeamControl control = new CreareTeamControl(
                new TeamRepositoryFinto()
        );

        assertDoesNotThrow(
                () -> control.verificaNomeTeam("ByteBuilders")
        );
    }

    @Test
    void rifiutaNomeNulloOVuoto() {
        CreareTeamControl control = new CreareTeamControl(
                new TeamRepositoryFinto()
        );

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaNomeTeam(null)
                ),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.verificaNomeTeam("   ")
                )
        );
    }

    @Test
    void creaESalvaTeamConUtenteComeMembroEResponsabile() {
        TeamRepositoryFinto repository = new TeamRepositoryFinto();
        CreareTeamControl control = new CreareTeamControl(repository);
        Utente utente = new Utente(1L);

        control.creaTeam("ByteBuilders", utente);

        Team teamSalvato = repository.teamSalvato;

        assertAll(
                () -> assertNotNull(teamSalvato),
                () -> assertEquals(
                        "ByteBuilders",
                        teamSalvato.getNome()
                ),
                () -> assertSame(
                        utente,
                        teamSalvato.getResponsabile()
                ),
                () -> assertEquals(
                        List.of(utente),
                        teamSalvato.getMembri()
                )
        );
    }

    private static class TeamRepositoryFinto
            implements TeamRepository {

        private boolean appartieneGiaAUnTeam;
        private Utente utenteVerificato;
        private Team teamSalvato;

        @Override
        public boolean verificaAppartenenzaTeam(Utente utente) {
            utenteVerificato = utente;
            return appartieneGiaAUnTeam;
        }

        @Override
        public void salva(Team team) {
            teamSalvato = team;
        }

        @Override
        public Team recuperaTeam(Utente utente) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}