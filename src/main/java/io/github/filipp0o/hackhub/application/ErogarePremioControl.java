package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Utente;

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

        verificaOrganizzatore(
                organizzatoreValido,
                hackathonValido
        );

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

        verificaOrganizzatore(
                organizzatoreValido,
                hackathonValido
        );

        RiscossionePremio riscossione =
                ottieniRiscossionePronta(hackathonValido);

        String paymentRef =
                sistemaPagamentoGateway.richiediErogazionePremio(
                        hackathonValido.getImportoPremio(),
                        riscossione.getBeneficiaryRef()
                );

        riscossione.registraErogazione(paymentRef);
        hackathonRepository.salva(hackathonValido);
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
        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        if (riscossione == null) {
            throw new IllegalStateException(
                    "La riscossione del premio non è configurata"
            );
        }

        if (riscossione.getStato()
                != StatoRiscossionePremio.PRONTA) {
            throw new IllegalStateException(
                    "La riscossione non è pronta per l'erogazione"
            );
        }

        return riscossione;
    }
}