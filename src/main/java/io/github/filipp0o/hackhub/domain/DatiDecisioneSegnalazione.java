package io.github.filipp0o.hackhub.domain;

public record DatiDecisioneSegnalazione(
        EsitoSegnalazione esito,
        String motivazione
) {
}