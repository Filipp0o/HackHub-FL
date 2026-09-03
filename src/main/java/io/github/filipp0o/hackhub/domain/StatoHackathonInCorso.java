package io.github.filipp0o.hackhub.domain;

import java.time.LocalDate;

public final class StatoHackathonInCorso
        implements StatoHackathon {

    @Override
    public TipoStatoHackathon tipo() {
        return TipoStatoHackathon.IN_CORSO;
    }

    @Override
    public StatoHackathon aggiorna(
            LocalDate dataCorrente,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        if (dataCorrente.isAfter(dataFine)) {
            return new StatoHackathonInValutazione();
        }

        return this;
    }

    @Override
    public boolean consenteIscrizioni() {
        return false;
    }

    @Override
    public boolean consenteSegnalazioni() {
        return true;
    }

    @Override
    public boolean consenteValutazioni() {
        return false;
    }

    @Override
    public boolean consenteRiscossionePremio() {
        return false;
    }

    @Override
    public StatoHackathon concludi() {
        throw new IllegalStateException(
                "Può essere concluso solo un hackathon in valutazione"
        );
    }
}