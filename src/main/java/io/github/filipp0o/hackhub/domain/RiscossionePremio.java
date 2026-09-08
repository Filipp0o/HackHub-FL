package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class RiscossionePremio {

    private Long id;
    private StatoRiscossionePremio stato;
    private String beneficiaryRef;
    private String paymentRef;

    private final Hackathon hackathon;

    private RiscossionePremio(
            Long id,
            Hackathon hackathon,
            StatoRiscossionePremio stato,
            String beneficiaryRef,
            String paymentRef
    ) {
        this.hackathon = Objects.requireNonNull(
                hackathon, "L'hackathon è obbligatorio"
        );
        this.stato = Objects.requireNonNull(
                stato, "Lo stato della riscossione è obbligatorio"
        );

        boolean coerente = switch (stato) {
            case DA_CONFIGURARE ->
                    beneficiaryRef == null && paymentRef == null;
            case PRONTA ->
                    testoPresente(beneficiaryRef) && paymentRef == null;
            case EROGATA ->
                    testoPresente(beneficiaryRef) && testoPresente(paymentRef);
        };

        if (!coerente) {
            throw new IllegalArgumentException(
                    "Riferimenti incompatibili con lo stato della riscossione"
            );
        }

        this.beneficiaryRef = beneficiaryRef;
        this.paymentRef = paymentRef;

        if (id != null) {
            assegnaId(id);
        }

        this.hackathon.registraRiscossionePremio(this);
    }

    public static RiscossionePremio crea(Hackathon hackathon) {
        return new RiscossionePremio(
                null,
                hackathon,
                StatoRiscossionePremio.DA_CONFIGURARE,
                null,
                null
        );
    }

    public static RiscossionePremio ricostruisci(
            Long id,
            Hackathon hackathon,
            StatoRiscossionePremio stato,
            String beneficiaryRef,
            String paymentRef
    ) {
        Objects.requireNonNull(
                id, "L'id della riscossione è obbligatorio"
        );

        return new RiscossionePremio(
                id, hackathon, stato, beneficiaryRef, paymentRef
        );
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(
                id, "L'id della riscossione è obbligatorio"
        );

        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id della riscossione deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id della riscossione è già stato assegnato"
            );
        }

        this.id = idValido;
    }

    private static boolean testoPresente(String testo) {
        return testo != null && !testo.isBlank();
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