package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.presentation.SessioneUtente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class SessioneConfigurationTest {

    private AnnotationConfigWebApplicationContext context;
    private MockServletContext servletContext;
    private SessioneUtente sessione;

    @BeforeEach
    void preparaContestoWeb() {
        servletContext = new MockServletContext();
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(servletContext);
        context.register(SessioneConfiguration.class);
        context.refresh();
        sessione = context.getBean(SessioneUtente.class);
    }

    @AfterEach
    void chiudiContesto() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void conservaLUtenteTraRichiesteEIsolaLeSessioniHttp() {
        MockHttpSession prima = new MockHttpSession(servletContext);
        MockHttpSession seconda = new MockHttpSession(servletContext);
        Utente primoUtente = Utente.ricostruisci(1L, "primo@example.com", "hash-primo");
        Utente secondoUtente = Utente.ricostruisci(2L, "secondo@example.com", "hash-secondo");

        nellaSessione(prima, () -> sessione.registra(primoUtente));
        nellaSessione(seconda, () -> {
            assertThrows(
                    SessioneUtente.UtenteNonAutenticatoException.class,
                    sessione::recupera
            );
            sessione.registra(secondoUtente);
        });
        nellaSessione(prima, () -> assertSame(primoUtente, sessione.recupera()));
        nellaSessione(seconda, () -> assertSame(secondoUtente, sessione.recupera()));

        nellaSessione(prima, sessione::svuota);
        nellaSessione(prima, () -> assertThrows(
                SessioneUtente.UtenteNonAutenticatoException.class,
                sessione::recupera
        ));
        nellaSessione(seconda, () -> assertSame(secondoUtente, sessione.recupera()));
    }

    @Test
    void unaNuovaSessioneHttpNonEreditaLUtenteDiQuellaInvalidata() {
        MockHttpSession precedente = new MockHttpSession(servletContext);
        Utente utente = Utente.ricostruisci(1L, "utente@example.com", "hash");
        nellaSessione(precedente, () -> sessione.registra(utente));
        precedente.invalidate();

        MockHttpSession nuova = new MockHttpSession(servletContext);
        nellaSessione(nuova, () -> assertThrows(
                SessioneUtente.UtenteNonAutenticatoException.class,
                sessione::recupera
        ));
    }

    private void nellaSessione(MockHttpSession httpSession, Runnable operazione) {
        MockHttpServletRequest richiesta = new MockHttpServletRequest(servletContext);
        richiesta.setSession(httpSession);
        ServletRequestAttributes attributi = new ServletRequestAttributes(richiesta);
        RequestAttributes precedenti = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(attributi);
        try {
            operazione.run();
        } finally {
            try {
                attributi.requestCompleted();
            } finally {
                if (precedenti == null) {
                    RequestContextHolder.resetRequestAttributes();
                } else {
                    RequestContextHolder.setRequestAttributes(precedenti);
                }
            }
        }
    }
}
