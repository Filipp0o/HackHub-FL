package io.github.filipp0o.hackhub.domain;

import java.time.LocalDate;

public interface StatoHackathon {

    TipoStatoHackathon tipo();

    StatoHackathon aggiorna(
            LocalDate dataCorrente,
            LocalDate dataInizio,
            LocalDate dataFine
    );

    boolean consenteIscrizioni();

    boolean consenteSegnalazioni();

    boolean consenteValutazioni();

    boolean consenteRiscossionePremio();

    StatoHackathon concludi();
}