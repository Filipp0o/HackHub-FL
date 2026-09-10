package io.github.filipp0o.hackhub.configuration;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.infrastructure.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@Profile("persistent")
@EnableTransactionManagement(proxyTargetClass = true)
public class RepositoryJdbcConfiguration {

    @Bean
    public JdbcTransactionManager transactionManager(DataSource dataSource) {
        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);
        return gestore;
    }

    @Bean
    public UtenteRepository jdbcUtenteRepository(DataSource dataSource) {
        return new JdbcUtenteRepository(JdbcClient.create(dataSource));
    }

    @Bean
    public TeamRepository jdbcTeamRepository(DataSource dataSource) {
        return new JdbcTeamRepository(dataSource);
    }

    @Bean
    public InvitoRepository jdbcInvitoRepository(DataSource dataSource) {
        return new JdbcInvitoRepository(JdbcClient.create(dataSource));
    }

    @Bean
    public HackathonRepository jdbcHackathonRepository(DataSource dataSource) {
        return new JdbcHackathonRepository(dataSource);
    }

    @Bean
    public PartecipazioneRepository jdbcPartecipazioneRepository(
            DataSource dataSource,
            HackathonRepository hackathonRepository
    ) {
        return new JdbcPartecipazioneRepository(dataSource, hackathonRepository);
    }

    @Bean
    public SottomissioneRepository jdbcSottomissioneRepository(
            DataSource dataSource,
            PartecipazioneRepository partecipazioneRepository
    ) {
        return new JdbcSottomissioneRepository(dataSource, partecipazioneRepository);
    }

    @Bean
    public ValutazioneRepository jdbcValutazioneRepository(DataSource dataSource) {
        return new JdbcValutazioneRepository(dataSource);
    }
}