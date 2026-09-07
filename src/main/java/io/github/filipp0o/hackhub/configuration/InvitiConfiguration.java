package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryInvitoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvitiConfiguration {

    @Bean
    public InvitoRepository invitoRepository() {
        return new InMemoryInvitoRepository();
    }

    @Bean
    public InvitareUtentiTeamControl invitareUtentiTeamControl(
            UtenteRepository utenteRepository,
            TeamRepository teamRepository,
            InvitoRepository invitoRepository
    ) {
        return new InvitareUtentiTeamControl(
                utenteRepository, teamRepository, invitoRepository
        );
    }
}
