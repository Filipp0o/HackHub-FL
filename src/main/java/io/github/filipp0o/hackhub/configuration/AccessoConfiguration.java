package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import io.github.filipp0o.hackhub.application.EffettuareAccessoControl;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessoConfiguration {

    @Bean
    public EffettuareAccessoControl effettuareAccessoControl(
            UtenteRepository utenteRepository,
            CodificatorePassword codificatorePassword
    ) {
        return new EffettuareAccessoControl(
                utenteRepository,
                codificatorePassword
        );
    }
}
