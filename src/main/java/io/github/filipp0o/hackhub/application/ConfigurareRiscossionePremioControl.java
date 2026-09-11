package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

public class ConfigurareRiscossionePremioControl {

    private final SistemaPagamentoGateway sistemaPagamentoGateway;
    private final HackathonRepository hackathonRepository;

    public ConfigurareRiscossionePremioControl(
            SistemaPagamentoGateway sistemaPagamentoGateway,
            HackathonRepository hackathonRepository
    ) {
        this.sistemaPagamentoGateway = Objects.requireNonNull(
                sistemaPagamentoGateway,
                "Il gateway del sistema di pagamento è obbligatorio"
        );
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );
    }

    public void avviaConfigurazioneRiscossionePremio(
            Hackathon hackathon,
            Utente responsabileTeam
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );
        Utente responsabileValido = Objects.requireNonNull(
                responsabileTeam,
                "Il responsabile del team è obbligatorio"
        );

        verificaResponsabileTeamVincitore(
                hackathonValido,
                responsabileValido
        );

        RiscossionePremio riscossione =
                ottieniRiscossioneDaConfigurare(hackathonValido);

        String beneficiaryRef;

        try {
            beneficiaryRef =
                    sistemaPagamentoGateway.avviaConfigurazioneBeneficiario(
                            responsabileValido
                    );

            if (beneficiaryRef == null || beneficiaryRef.isBlank()) {
                throw new IllegalStateException(
                        "La configurazione non ha restituito un riferimento valido"
                );
            }
        } catch (SistemaPagamentoGateway.ConfigurazioneAnnullataException errore) {
            throw errore;
        } catch (RuntimeException errore) {
            throw new ConfigurazioneFallitaException(
                    "Configurazione del beneficiario non completata",
                    errore
            );
        }

        Runnable ripristino = () ->
                riscossione.annullaConfigurazioneNonRegistrata(beneficiaryRef);

        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int stato) {
                                if (stato == STATUS_ROLLED_BACK) {
                                    ripristino.run();
                                }
                            }
                        }
                );
            }

            riscossione.configura(beneficiaryRef);
            hackathonRepository.salva(hackathonValido);
        } catch (RuntimeException errore) {
            ripristino.run();

            throw new ConfigurazioneFallitaException(
                    "Configurazione non registrata. È possibile riprovare",
                    errore
            );
        }
    }

    private void verificaResponsabileTeamVincitore(
            Hackathon hackathon,
            Utente responsabileTeam
    ) {
        Partecipazione vincitrice = hackathon.getVincitrice();

        if (vincitrice == null) {
            throw new IllegalStateException(
                    "L'hackathon non possiede un team vincitore"
            );
        }

        Utente responsabileVincitore =
                vincitrice.getTeam().getResponsabile();

        if (!Objects.equals(
                responsabileVincitore.getId(),
                responsabileTeam.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'utente non è il responsabile del team vincitore"
            );
        }
    }

    private RiscossionePremio ottieniRiscossioneDaConfigurare(
            Hackathon hackathon
    ) {
        RiscossionePremio riscossione = hackathon.getRiscossionePremio();

        if (riscossione == null) {
            throw new IllegalStateException(
                    "La riscossione del premio non è stata inizializzata"
            );
        }

        if (riscossione.getStato()
                != StatoRiscossionePremio.DA_CONFIGURARE) {
            throw new IllegalStateException(
                    "La riscossione del premio non è da configurare"
            );
        }

        return riscossione;
    }

    public static class ConfigurazioneFallitaException
            extends IllegalStateException {

        public ConfigurazioneFallitaException(
                String messaggio,
                Throwable causa
        ) {
            super(messaggio, causa);
        }
    }
}