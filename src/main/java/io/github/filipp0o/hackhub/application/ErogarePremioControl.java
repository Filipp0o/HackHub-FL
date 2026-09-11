package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

public class ErogarePremioControl {

    private final SistemaPagamentoGateway sistemaPagamentoGateway;
    private final HackathonRepository hackathonRepository;

    public ErogarePremioControl(
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

    public void avviaErogazionePremio(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        verificaOrganizzatore(organizzatoreValido, hackathonValido);
        ottieniRiscossionePronta(hackathonValido);
    }

    public void confermaErogazionePremio(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        verificaOrganizzatore(organizzatoreValido, hackathonValido);

        RiscossionePremio riscossione =
                ottieniRiscossionePronta(hackathonValido);

        if (hackathonValido.getId() == null
                || hackathonValido.getId() <= 0) {
            throw new IllegalStateException(
                    "L'hackathon deve essere salvato prima dell'erogazione"
            );
        }

        String chiaveErogazione =
                "premio-hackathon-" + hackathonValido.getId();

        String paymentRef;

        try {
            paymentRef = sistemaPagamentoGateway.richiediErogazionePremio(
                    hackathonValido.getImportoPremio(),
                    riscossione.getBeneficiaryRef(),
                    chiaveErogazione
            );

            if (paymentRef == null || paymentRef.isBlank()) {
                throw new IllegalStateException(
                        "Il pagamento non ha restituito un riferimento valido"
                );
            }
        } catch (RuntimeException errore) {
            throw new ErogazioneFallitaException(
                    "Erogazione non completata",
                    errore
            );
        }

        Runnable ripristino = () ->
                riscossione.annullaErogazioneNonRegistrata(paymentRef);

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

            riscossione.registraErogazione(paymentRef);
            hackathonRepository.salva(hackathonValido);
        } catch (RuntimeException errore) {
            ripristino.run();

            throw new ErogazioneFallitaException(
                    "Pagamento eseguito, ma registrazione non completata. "
                            + "È possibile riprovare senza duplicare il pagamento",
                    errore
            );
        }
    }

    private void verificaOrganizzatore(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        if (!Objects.equals(
                hackathon.getOrganizzatore().getId(),
                organizzatore.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'organizzatore non è assegnato a questo hackathon"
            );
        }
    }

    private RiscossionePremio ottieniRiscossionePronta(
            Hackathon hackathon
    ) {
        RiscossionePremio riscossione = hackathon.getRiscossionePremio();

        if (riscossione == null) {
            throw new IllegalStateException(
                    "La riscossione del premio non è configurata"
            );
        }

        if (riscossione.getStato() != StatoRiscossionePremio.PRONTA) {
            throw new IllegalStateException(
                    "La riscossione non è pronta per l'erogazione"
            );
        }

        return riscossione;
    }

    public static class ErogazioneFallitaException
            extends IllegalStateException {

        public ErogazioneFallitaException(
                String messaggio,
                Throwable causa
        ) {
            super(messaggio, causa);
        }
    }
}