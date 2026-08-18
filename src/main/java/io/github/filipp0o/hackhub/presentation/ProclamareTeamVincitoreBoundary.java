package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ProclamareTeamVincitoreControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class ProclamareTeamVincitoreBoundary {

    private final ProclamareTeamVincitoreControl
            proclamareTeamVincitoreControl;

    public ProclamareTeamVincitoreBoundary(
            ProclamareTeamVincitoreControl
                    proclamareTeamVincitoreControl
    ) {
        this.proclamareTeamVincitoreControl =
                Objects.requireNonNull(
                        proclamareTeamVincitoreControl,
                        "Il control di proclamazione è obbligatorio"
                );
    }

    public List<TeamAmmissibile>
    selezionaProclamazioneTeamVincitore(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        return proclamareTeamVincitoreControl
                .avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
                .stream()
                .map(this::creaTeamAmmissibile)
                .toList();
    }

    public RiepilogoProclamazione selezionaTeamVincitore(
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        proclamareTeamVincitoreControl
                .preparaProclamazione(
                        hackathon,
                        partecipazioneSelezionata
                );

        return new RiepilogoProclamazione(
                partecipazioneSelezionata.getId(),
                partecipazioneSelezionata
                        .getTeam()
                        .getNome(),
                partecipazioneSelezionata
                        .getSottomissione()
                        .getValutazione()
                        .getPunteggio(),
                hackathon.getImportoPremio()
        );
    }

    public void confermaProclamazione(
            Utente organizzatore,
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        proclamareTeamVincitoreControl
                .confermaProclamazione(
                        organizzatore,
                        hackathon,
                        partecipazioneSelezionata
                );
    }

    private TeamAmmissibile creaTeamAmmissibile(
            Partecipazione partecipazione
    ) {
        return new TeamAmmissibile(
                partecipazione.getId(),
                partecipazione.getTeam().getNome(),
                partecipazione
                        .getSottomissione()
                        .getContenuto(),
                partecipazione
                        .getSottomissione()
                        .getValutazione()
                        .getGiudizio(),
                partecipazione
                        .getSottomissione()
                        .getValutazione()
                        .getPunteggio()
        );
    }

    public record TeamAmmissibile(
            Long partecipazioneId,
            String nomeTeam,
            String sottomissione,
            String giudizio,
            BigDecimal punteggio
    ) {
    }

    public record RiepilogoProclamazione(
            Long partecipazioneId,
            String nomeTeam,
            BigDecimal punteggio,
            BigDecimal importoPremio
    ) {
    }
}