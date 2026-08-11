package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class RiscossionePremio {

    private Long id;

    private StatoRiscossionePremio stato;
    private String beneficiaryRef;
    private String paymentRef;

    private final Hackathon hackathon;

    private RiscossionePremio(Hackathon hackathon) {
        this.hackathon = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        this.stato = StatoRiscossionePremio.DA_CONFIGURARE;

        this.hackathon.registraRiscossionePremio(this);
    }

    public static RiscossionePremio crea(Hackathon hackathon) {
        return new RiscossionePremio(hackathon);
    }

    public void configura(String beneficiaryRef) {
        if (stato != StatoRiscossionePremio.DA_CONFIGURARE) {
            throw new IllegalStateException(
                    "La riscossione è già stata configurata"
            );
        }

        if (beneficiaryRef == null || beneficiaryRef.isBlank()) {
            throw new IllegalArgumentException(
                    "Il riferimento del beneficiario è obbligatorio"
            );
        }

        this.beneficiaryRef = beneficiaryRef;
        this.stato = StatoRiscossionePremio.PRONTA;
    }

    public void registraErogazione(String paymentRef) {
        if (stato != StatoRiscossionePremio.PRONTA) {
            throw new IllegalStateException(
                    "La riscossione non è pronta per l'erogazione"
            );
        }

        if (paymentRef == null || paymentRef.isBlank()) {
            throw new IllegalArgumentException(
                    "Il riferimento del pagamento è obbligatorio"
            );
        }

        this.paymentRef = paymentRef;
        this.stato = StatoRiscossionePremio.EROGATA;
    }

    public Long getId() {
        return id;
    }

    public StatoRiscossionePremio getStato() {
        return stato;
    }

    public String getBeneficiaryRef() {
        return beneficiaryRef;
    }

    public String getPaymentRef() {
        return paymentRef;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }
}