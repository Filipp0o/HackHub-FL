package io.github.filipp0o.hackhub.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DatiHackathon(
        String nome,
        String regolamento,
        String criteriValutazione,
        LocalDate scadenzaIscrizioni,
        LocalDate dataInizio,
        LocalDate dataFine,
        String luogo,
        BigDecimal importoPremio,
        Integer dimensioneMassimaTeam
) {
}