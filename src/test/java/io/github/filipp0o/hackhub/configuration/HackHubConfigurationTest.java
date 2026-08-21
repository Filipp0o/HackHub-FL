package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.ConfigurareRiscossionePremioControl;
import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.application.ErogarePremioControl;
import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.InviareSottomissioneControl;
import io.github.filipp0o.hackhub.application.IscrivereTeamHackathonControl;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.ProclamareTeamVincitoreControl;
import io.github.filipp0o.hackhub.application.SegnalareViolazioneControl;
import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.application.SottomissioneRepository;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import io.github.filipp0o.hackhub.application.ValutazioneRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class HackHubConfigurationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void configuraComponentiApplicativi() {
        assertAll(
                () -> assertNotNull(
                        context.getBean(UtenteRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(TeamRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(PartecipazioneRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(HackathonRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(SegnalazioneRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(ValutazioneRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(SottomissioneRepository.class)
                ),
                () -> assertNotNull(
                        context.getBean(SistemaPagamentoGateway.class)
                ),
                () -> assertNotNull(
                        context.getBean(CreareTeamControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(CreareHackathonControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(ValutareSottomissioneControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(SegnalareViolazioneControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(EsaminareSegnalazioneControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(ProclamareTeamVincitoreControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(
                                ConfigurareRiscossionePremioControl.class
                        )
                ),
                () -> assertNotNull(
                        context.getBean(ErogarePremioControl.class)
                ),
                () -> assertNotNull(
                        context.getBean(
                                IscrivereTeamHackathonControl.class
                        )
                ),
                () -> assertNotNull(
                        context.getBean(
                                InviareSottomissioneControl.class
                        )
                )
        );
    }
}