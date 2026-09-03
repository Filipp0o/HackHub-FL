package io.github.filipp0o.hackhub.domain;

import java.time.LocalDate;

public final class StatoHackathonInValutazione
        implements StatoHackathon {

    @Override
    public TipoStatoHackathon tipo() {
        return TipoStatoHackathon.IN_VALUTAZIONE;
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
        return true;
    }

    @Override
    public boolean consenteValutazioni() {
        return true;
    }

    @Override
    public boolean consenteRiscossionePremio() {
        return false;
    }

    @Override
    public StatoHackathon concludi() {
        return new StatoHackathonConcluso();
    }
}