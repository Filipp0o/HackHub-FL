package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.InMemoryTeamRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryUtenteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvitiConfigurationTest {

    @Test
    void collegaControlERepositoryERegistraInvitoDisponibileAlDestinatario() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(UtenteRepository.class,
                    () -> new InMemoryUtenteRepository(List.of()));
            context.registerBean(TeamRepository.class, InMemoryTeamRepository::new);
            context.register(InvitiConfiguration.class);
            context.refresh();

            var utenti = context.getBean(UtenteRepository.class);
            var teams = context.getBean(TeamRepository.class);
            var inviti = context.getBean(InvitoRepository.class);
            var control = context.getBean(InvitareUtentiTeamControl.class);
            Utente creatore = Utente.crea("creatore@example.com", "hash-creatore");
            Utente destinatario = Utente.crea("destinatario@example.com", "hash-destinatario");
            utenti.salva(creatore);
            utenti.salva(destinatario);
            Team team = Team.crea("ByteBuilders", creatore, creatore);
            teams.salva(team);

            assertEquals(List.of(destinatario), control.richiediUtentiInvitabili(creatore));
            control.richiediInvito(creatore, new Utente(destinatario.getId()));

            var ricevuti = inviti.recuperaInvitiRicevuti(destinatario);
            assertEquals(1, ricevuti.size());
            assertNotNull(ricevuti.getFirst().getId());
            assertSame(team, ricevuti.getFirst().ottieniTeam());
            assertFalse(ricevuti.getFirst().isAccettato());
            assertTrue(inviti.recuperaInvitiRicevuti(creatore).isEmpty());
            assertEquals(1, team.numeroMembri());

            assertThrows(IllegalArgumentException.class,
                    () -> control.richiediInvito(creatore, creatore));
            assertThrows(IllegalStateException.class,
                    () -> control.richiediInvito(destinatario, creatore));
            assertEquals(1, inviti.recuperaInvitiRicevuti(destinatario).size());
        }
    }
}
