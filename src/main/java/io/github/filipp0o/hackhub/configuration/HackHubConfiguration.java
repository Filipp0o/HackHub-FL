package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.infrastructure.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class HackHubConfiguration {

    @Bean
    public CodificatorePassword codificatorePassword() {
        return new BCryptPasswordEncoderAdapter();
    }

    @Bean
    @Profile("!persistent")
    public UtenteRepository utenteRepository() {
        return new InMemoryUtenteRepository(List.of());
    }

    @Bean
    @Profile("!persistent")
    public TeamRepository teamRepository() {
        return new InMemoryTeamRepository();
    }

    @Bean
    @Profile("!persistent")
    public PartecipazioneRepository partecipazioneRepository() {
        return new InMemoryPartecipazioneRepository();
    }

    @Bean
    @Profile("!persistent")
    public HackathonRepository hackathonRepository(
            PartecipazioneRepository partecipazioneRepository
    ) {
        return new InMemoryHackathonRepository(partecipazioneRepository);
    }

    @Bean
    @Profile("!persistent")
    public SegnalazioneRepository segnalazioneRepository() {
        return new SegnalazioneRepositoryImpl();
    }

    @Bean
    @Profile("!persistent")
    public ValutazioneRepository valutazioneRepository() {
        return new ValutazioneRepositoryImpl();
    }

    @Bean
    @Profile("!persistent")
    public SottomissioneRepository sottomissioneRepository() {
        return new SottomissioneRepositoryImpl();
    }

    @Bean
    public SistemaPagamentoGateway sistemaPagamentoGateway(
            Environment ambiente
    ) {
        if (ambiente.acceptsProfiles(Profiles.of("persistent"))) {
            Path archivio = Path.of(
                    ambiente.getProperty(
                            "hackhub.pagamenti.archivio",
                            "./data/pagamenti-simulati.properties"
                    )
            );

            return new SistemaPagamentoAdapter(archivio);
        }

        return new SistemaPagamentoAdapter();
    }

    @Bean
    public CreareTeamControl creareTeamControl(
            TeamRepository teamRepository
    ) {
        return new CreareTeamControl(teamRepository);
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
    public ConsultareHackathonControl consultareHackathonControl(
            HackathonRepository hackathonRepository
    ) {
        return new ConsultareHackathonControl(hackathonRepository);
    }

    @Bean
    public ValutareSottomissioneControl valutareSottomissioneControl(
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
    public SegnalareViolazioneControl segnalareViolazioneControl(
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
    public EsaminareSegnalazioneControl esaminareSegnalazioneControl(
            SegnalazioneRepository segnalazioneRepository,
            PartecipazioneRepository partecipazioneRepository,
            ObjectProvider<PlatformTransactionManager> gestori
    ) {
        PlatformTransactionManager gestore = gestori.getIfAvailable();

        TransactionOperations transazioni =
                TransactionOperations.withoutTransaction();

        if (gestore != null) {
            TransactionTemplate template = new TransactionTemplate(gestore);

            template.setIsolationLevel(
                    TransactionDefinition.ISOLATION_REPEATABLE_READ
            );

            transazioni = template;
        }

        return new EsaminareSegnalazioneControl(
                segnalazioneRepository,
                partecipazioneRepository,
                transazioni
        );
    }

    @Bean
    public ProclamareTeamVincitoreControl proclamareTeamVincitoreControl(
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
    public ConfigurareRiscossionePremioControl configurareRiscossionePremioControl(
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
    public IscrivereTeamHackathonControl iscrivereTeamHackathonControl(
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
    public InviareSottomissioneControl inviareSottomissioneControl(
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
    public AggiornareSottomissioneControl aggiornareSottomissioneControl(
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