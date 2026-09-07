package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import io.github.filipp0o.hackhub.application.RegistrarsiControl;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RegistrazioneConfiguration {

    @Bean
    public RegistrarsiControl registrarsiControl(
            UtenteRepository utenteRepository,
            CodificatorePassword codificatorePassword
    ) {
        return new RegistrarsiControl(
                utenteRepository,
                codificatorePassword
        );
    }
}