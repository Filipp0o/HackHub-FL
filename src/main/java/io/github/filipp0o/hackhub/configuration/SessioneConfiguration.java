package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.presentation.SessioneUtente;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class SessioneConfiguration {

    @Bean
    @SessionScope
    public SessioneUtente sessioneUtente() {
        return new SessioneUtente();
    }
}
