package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public final class StatoHackathonFactory {

    private StatoHackathonFactory() {
    }

    public static StatoHackathon ricostruisci(
            TipoStatoHackathon tipo
    ) {
        TipoStatoHackathon tipoValido =
                Objects.requireNonNull(
                        tipo,
                        "Il tipo dello stato dell'hackathon è obbligatorio"
                );

        return switch (tipoValido) {
            case IN_ISCRIZIONE ->
                    new StatoHackathonInIscrizione();
            case IN_CORSO ->
                    new StatoHackathonInCorso();
            case IN_VALUTAZIONE ->
                    new StatoHackathonInValutazione();
            case CONCLUSO ->
                    new StatoHackathonConcluso();
        };
    }
}