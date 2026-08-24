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
import io.github.filipp0o.hackhub.infrastructure.HackathonRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.PartecipazioneRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.SegnalazioneRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.SistemaPagamentoAdapter;
import io.github.filipp0o.hackhub.infrastructure.SottomissioneRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.TeamRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.UtenteRepositoryImpl;
import io.github.filipp0o.hackhub.infrastructure.ValutazioneRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import io.github.filipp0o.hackhub.application.AggiornareSottomissioneControl;

@Configuration
public class HackHubConfiguration {

    @Bean
    public UtenteRepository utenteRepository() {
        return new UtenteRepositoryImpl(List.of());
    }

    @Bean
    public TeamRepository teamRepository() {
        return new TeamRepositoryImpl();
    }

    @Bean
    public PartecipazioneRepository partecipazioneRepository() {
        return new PartecipazioneRepositoryImpl();
    }

    @Bean
    public HackathonRepository hackathonRepository(
            PartecipazioneRepository partecipazioneRepository
    ) {
        return new HackathonRepositoryImpl(
                partecipazioneRepository
        );
    }

    @Bean
    public SegnalazioneRepository segnalazioneRepository() {
        return new SegnalazioneRepositoryImpl();
    }

    @Bean
    public ValutazioneRepository valutazioneRepository() {
        return new ValutazioneRepositoryImpl();
    }

    @Bean
    public SottomissioneRepository sottomissioneRepository() {
        return new SottomissioneRepositoryImpl();
    }

    @Bean
    public SistemaPagamentoGateway sistemaPagamentoGateway() {
        return new SistemaPagamentoAdapter();
    }

    @Bean
    public CreareTeamControl creareTeamControl(
            TeamRepository teamRepository
    ) {
        return new CreareTeamControl(
                teamRepository
        );
    }

    @Bean
    public CreareHackathonControl creareHackathonControl(
            UtenteRepository utenteRepository,
            HackathonRepository hackathonRepository
    ) {
        return new CreareHackathonControl(
                utenteRepository,
                hackathonRepository
        );
    }

    @Bean
    public ValutareSottomissioneControl
    valutareSottomissioneControl(
            HackathonRepository hackathonRepository,
            PartecipazioneRepository partecipazioneRepository,
            ValutazioneRepository valutazioneRepository
    ) {
        return new ValutareSottomissioneControl(
                hackathonRepository,
                partecipazioneRepository,
                valutazioneRepository
        );
    }

    @Bean
    public SegnalareViolazioneControl
    segnalareViolazioneControl(
            HackathonRepository hackathonRepository,
            PartecipazioneRepository partecipazioneRepository,
            SegnalazioneRepository segnalazioneRepository
    ) {
        return new SegnalareViolazioneControl(
                hackathonRepository,
                partecipazioneRepository,
                segnalazioneRepository
        );
    }

    @Bean
    public EsaminareSegnalazioneControl
    esaminareSegnalazioneControl(
            SegnalazioneRepository segnalazioneRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
        return new EsaminareSegnalazioneControl(
                segnalazioneRepository,
                partecipazioneRepository
        );
    }

    @Bean
    public ProclamareTeamVincitoreControl
    proclamareTeamVincitoreControl(
            PartecipazioneRepository partecipazioneRepository,
            HackathonRepository hackathonRepository,
            SegnalazioneRepository segnalazioneRepository
    ) {
        return new ProclamareTeamVincitoreControl(
                partecipazioneRepository,
                hackathonRepository,
                segnalazioneRepository
        );
    }

    @Bean
    public ConfigurareRiscossionePremioControl
    configurareRiscossionePremioControl(
            SistemaPagamentoGateway sistemaPagamentoGateway,
            HackathonRepository hackathonRepository
    ) {
        return new ConfigurareRiscossionePremioControl(
                sistemaPagamentoGateway,
                hackathonRepository
        );
    }

    @Bean
    public ErogarePremioControl erogarePremioControl(
            SistemaPagamentoGateway sistemaPagamentoGateway,
            HackathonRepository hackathonRepository
    ) {
        return new ErogarePremioControl(
                sistemaPagamentoGateway,
                hackathonRepository
        );
    }

    @Bean
    public IscrivereTeamHackathonControl
    iscrivereTeamHackathonControl(
            HackathonRepository hackathonRepository,
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
        return new IscrivereTeamHackathonControl(
                hackathonRepository,
                teamRepository,
                partecipazioneRepository
        );
    }

    @Bean
    public InviareSottomissioneControl
    inviareSottomissioneControl(
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository,
            SottomissioneRepository sottomissioneRepository
    ) {
        return new InviareSottomissioneControl(
                teamRepository,
                partecipazioneRepository,
                sottomissioneRepository
        );
    }

    @Bean
    public AggiornareSottomissioneControl
    aggiornareSottomissioneControl(
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository,
            SottomissioneRepository sottomissioneRepository
    ) {
        return new AggiornareSottomissioneControl(
                teamRepository,
                partecipazioneRepository,
                sottomissioneRepository
        );
    }
}