package io.github.filipp0o.hackhub.domain;

import java.time.LocalDate;

public final class StatoHackathonInIscrizione
        implements StatoHackathon {

    @Override
    public TipoStatoHackathon tipo() {
        return TipoStatoHackathon.IN_ISCRIZIONE;
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

        if (!dataCorrente.isBefore(dataInizio)) {
            return new StatoHackathonInCorso();
        }

        return this;
    }

    @Override
    public boolean consenteIscrizioni() {
        return true;
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
        return false;
    }

    @Override
    public StatoHackathon concludi() {
        throw new IllegalStateException(
                "Può essere concluso solo un hackathon in valutazione"
        );
    }
}