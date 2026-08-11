package io.github.filipp0o.hackhub.domain;

import java.math.BigDecimal;

public record DatiValutazione(
        String giudizio,
        BigDecimal punteggio
) {
}