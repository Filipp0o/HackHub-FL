package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.CodificatorePassword;
import io.github.filipp0o.hackhub.application.EffettuareAccessoControl;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.BCryptPasswordEncoderAdapter;
import io.github.filipp0o.hackhub.infrastructure.InMemoryUtenteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccessoConfigurationTest {

    @Test
    void configuraIlControlConRepositoryECodificatoreReali() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.registerBean(
                    UtenteRepository.class,
                    () -> new InMemoryUtenteRepository(List.of())
            );
            context.registerBean(
                    CodificatorePassword.class,
                    BCryptPasswordEncoderAdapter::new
            );
            context.register(AccessoConfiguration.class);
            context.refresh();

            UtenteRepository repository = context.getBean(UtenteRepository.class);
            CodificatorePassword codificatore = context.getBean(CodificatorePassword.class);
            EffettuareAccessoControl control = context.getBean(EffettuareAccessoControl.class);
            String email = "utente@example.com";
            String password = "Password-di-prova!42";
            Utente utente = Utente.crea(email, codificatore.codifica(password));
            repository.salva(utente);

            assertSame(utente, control.richiediAccesso(email, password));
            assertThrows(
                    EffettuareAccessoControl.CredenzialiNonValideException.class,
                    () -> control.richiediAccesso(email, "password-errata")
            );
        }
    }
}
