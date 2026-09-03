package io.github.filipp0o.hackhub.domain;

import java.time.LocalDate;

public final class StatoHackathonConcluso
        implements StatoHackathon {

    @Override
    public TipoStatoHackathon tipo() {
        return TipoStatoHackathon.CONCLUSO;
    }

    @Override
    public StatoHackathon aggiorna(
            LocalDate dataCorrente,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        return this;
    }

    @Override
    public boolean consenteIscrizioni() {
        return false;
    }

    @Override
    public boolean consenteSegnalazioni() {
        return false;
    }

    @Override
    public boolean consenteValutazioni() {
        return false;
    }

    @Override
    public boolean consenteRiscossionePremio() {
        return true;
    }

    @Override
    public StatoHackathon concludi() {
        throw new IllegalStateException(
                "Può essere concluso solo un hackathon in valutazione"
        );
    }
}