package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ConsultareHackathonControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons")
public class ConsultareHackathonBoundary {

    private final ConsultareHackathonControl
            consultareHackathonControl;

    public ConsultareHackathonBoundary(
            ConsultareHackathonControl consultareHackathonControl
    ) {
        this.consultareHackathonControl =
                Objects.requireNonNull(
                        consultareHackathonControl,
                        "Il control di consultazione è obbligatorio"
                );
    }

    @GetMapping
    public List<RiepilogoHackathon> consultaHackathon() {
        return consultareHackathonControl
                .consultaHackathon()
                .stream()
                .map(this::creaRiepilogoHackathon)
                .toList();
    }

    @GetMapping("/{hackathonId}")
    public InformazioniHackathon selezionaHackathon(
            @PathVariable Long hackathonId
    ) {
        Long hackathonIdValido =
                Objects.requireNonNull(
                        hackathonId,
                        "L'id dell'hackathon è obbligatorio"
                );

        try {
            Hackathon hackathon =
                    consultareHackathonControl
                            .selezionaHackathon(
                                    hackathonIdValido
                            );

            return creaInformazioniHackathon(
                    hackathon
            );
        } catch (IllegalStateException eccezione) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    eccezione.getMessage(),
                    eccezione
            );
        }
    }

    private RiepilogoHackathon creaRiepilogoHackathon(
            Hackathon hackathon
    ) {
        return new RiepilogoHackathon(
                hackathon.getId(),
                hackathon.getNome(),
                hackathon.getStato(),
                hackathon.getScadenzaIscrizioni(),
                hackathon.getDataInizio(),
                hackathon.getDataFine(),
                hackathon.getLuogo()
        );
    }

    private InformazioniHackathon creaInformazioniHackathon(
            Hackathon hackathon
    ) {
        return new InformazioniHackathon(
                hackathon.getId(),
                hackathon.getNome(),
                hackathon.getRegolamento(),
                hackathon.getCriteriValutazione(),
                hackathon.getScadenzaIscrizioni(),
                hackathon.getDataInizio(),
                hackathon.getDataFine(),
                hackathon.getLuogo(),
                hackathon.getImportoPremio(),
                hackathon.getDimensioneMassimaTeam(),
                hackathon.getStato()
        );
    }

    public record RiepilogoHackathon(
            Long id,
            String nome,
            StatoHackathon stato,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine,
            String luogo
    ) {
    }

    public record InformazioniHackathon(
            Long id,
            String nome,
            String regolamento,
            String criteriValutazione,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine,
            String luogo,
            BigDecimal importoPremio,
            Integer dimensioneMassimaTeam,
            StatoHackathon stato
    ) {
    }
}