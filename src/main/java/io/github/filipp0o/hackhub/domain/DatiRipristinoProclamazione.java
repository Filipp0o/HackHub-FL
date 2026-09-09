package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public final class DatiRipristinoProclamazione {

    private final Hackathon hackathon;
    private final StatoHackathon stato;
    private final Partecipazione vincitrice;
    private final RiscossionePremio riscossionePremio;

    DatiRipristinoProclamazione(
            Hackathon hackathon,
            StatoHackathon stato,
            Partecipazione vincitrice,
            RiscossionePremio riscossionePremio
    ) {
        this.hackathon = Objects.requireNonNull(
                hackathon, "L'hackathon è obbligatorio"
        );
        this.stato = Objects.requireNonNull(
                stato, "Lo stato è obbligatorio"
        );
        this.vincitrice = vincitrice;
        this.riscossionePremio = riscossionePremio;
    }

    Hackathon getHackathon() {
        return hackathon;
    }

    StatoHackathon getStato() {
        return stato;
    }

    Partecipazione getVincitrice() {
        return vincitrice;
    }

    RiscossionePremio getRiscossionePremio() {
        return riscossionePremio;
    }
}