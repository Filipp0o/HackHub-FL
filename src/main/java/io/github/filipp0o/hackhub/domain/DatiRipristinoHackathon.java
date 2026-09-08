package io.github.filipp0o.hackhub.domain;

import java.time.LocalDateTime;
import java.util.List;

public record DatiRipristinoHackathon(
        Long id,
        DatiHackathon dati,
        Utente organizzatore,
        Utente giudice,
        List<Utente> mentori,
        TipoStatoHackathon stato,
        Vincitrice vincitrice,
        Riscossione riscossione
) {
    public DatiRipristinoHackathon {
        mentori = List.copyOf(mentori);
    }

    public record Vincitrice(
            Long id,
            Team team,
            StatoPartecipazione stato,
            SottomissioneSalvata sottomissione
    ) { }

    public record SottomissioneSalvata(
            Long id,
            String contenuto,
            ValutazioneSalvata valutazione
    ) { }

    public record ValutazioneSalvata(
            Long id,
            Utente giudice,
            DatiValutazione dati,
            LocalDateTime dataOra
    ) { }

    public record Riscossione(
            Long id,
            StatoRiscossionePremio stato,
            String beneficiaryRef,
            String paymentRef
    ) { }
}