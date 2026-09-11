package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.ProclamareTeamVincitoreControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons/{hackathonId}/proclamazione")
public class ProclamareTeamVincitoreBoundary {

    private final ProclamareTeamVincitoreControl proclamareTeamVincitoreControl;

    private SessioneUtente sessioneUtente;
    private HackathonRepository hackathonRepository;
    private PartecipazioneRepository partecipazioneRepository;

    public ProclamareTeamVincitoreBoundary(
            ProclamareTeamVincitoreControl proclamareTeamVincitoreControl
    ) {
        this.proclamareTeamVincitoreControl = Objects.requireNonNull(
                proclamareTeamVincitoreControl,
                "Il control di proclamazione è obbligatorio"
        );
    }

    @Autowired
    public ProclamareTeamVincitoreBoundary(
            ProclamareTeamVincitoreControl control,
            SessioneUtente sessioneUtente,
            HackathonRepository hackathonRepository,
            PartecipazioneRepository partecipazioni
    ) {
        this(control);
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente);
        this.hackathonRepository = Objects.requireNonNull(hackathonRepository);
        this.partecipazioneRepository = Objects.requireNonNull(partecipazioni);
    }

    public List<TeamAmmissibile> selezionaProclamazioneTeamVincitore(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        return proclamareTeamVincitoreControl
                .avviaProclamazioneTeamVincitore(organizzatore, hackathon)
                .stream()
                .map(this::creaTeamAmmissibile)
                .toList();
    }

    public RiepilogoProclamazione selezionaTeamVincitore(
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        proclamareTeamVincitoreControl.preparaProclamazione(
                hackathon,
                partecipazioneSelezionata
        );

        return new RiepilogoProclamazione(
                partecipazioneSelezionata.getId(),
                partecipazioneSelezionata.getTeam().getNome(),
                partecipazioneSelezionata.getSottomissione()
                        .getValutazione().getPunteggio(),
                hackathon.getImportoPremio()
        );
    }

    public void confermaProclamazione(
            Utente organizzatore,
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        proclamareTeamVincitoreControl.confermaProclamazione(
                organizzatore,
                hackathon,
                partecipazioneSelezionata
        );
    }

    @GetMapping("/team-ammissibili")
    public ResponseEntity<List<TeamAmmissibile>> ottieniTeamAmmissibili(
            @PathVariable Long hackathonId
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        autorizza(hackathon, organizzatore);

        return ResponseEntity.ok(
                SupportoRest.esegui(() ->
                        selezionaProclamazioneTeamVincitore(
                                organizzatore,
                                hackathon
                        )
                )
        );
    }

    @GetMapping("/riepilogo")
    public ResponseEntity<RiepilogoProclamazione> ottieniRiepilogo(
            @PathVariable Long hackathonId,
            @RequestParam Long partecipazioneId
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        autorizza(hackathon, organizzatore);

        Partecipazione partecipazione =
                trovaPartecipazione(hackathon, partecipazioneId);

        return ResponseEntity.ok(
                SupportoRest.esegui(() ->
                        selezionaTeamVincitore(hackathon, partecipazione)
                )
        );
    }

    @PostMapping
    public ResponseEntity<Void> proclama(
            @PathVariable Long hackathonId,
            @RequestBody RichiestaProclamazione richiesta
    ) {
        Utente organizzatore = sessioneUtente.recupera();

        SupportoRest.richiesta(richiesta);
        SupportoRest.id(richiesta.partecipazioneId());

        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        autorizza(hackathon, organizzatore);

        Partecipazione partecipazione = trovaPartecipazione(
                hackathon,
                richiesta.partecipazioneId()
        );

        SupportoRest.esegui(() -> {
            confermaProclamazione(
                    organizzatore,
                    hackathon,
                    partecipazione
            );

            return null;
        });

        return ResponseEntity.noContent().build();
    }

    private Partecipazione trovaPartecipazione(
            Hackathon hackathon,
            Long id
    ) {
        SupportoRest.id(id);

        return SupportoRest.leggi(() ->
                        partecipazioneRepository.ottieniPartecipazioni(hackathon)
                )
                .stream()
                .filter(p -> Objects.equals(p.getId(), id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Partecipazione non trovata"
                ));
    }

    private void autorizza(Hackathon hackathon, Utente organizzatore) {
        SupportoRest.autorizza(
                hackathon.getOrganizzatore().getId(),
                organizzatore.getId()
        );
    }

    private TeamAmmissibile creaTeamAmmissibile(
            Partecipazione partecipazione
    ) {
        return new TeamAmmissibile(
                partecipazione.getId(),
                partecipazione.getTeam().getNome(),
                partecipazione.getSottomissione().getContenuto(),
                partecipazione.getSottomissione().getValutazione().getGiudizio(),
                partecipazione.getSottomissione().getValutazione().getPunteggio()
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

    public record RichiestaProclamazione(Long partecipazioneId) {
    }
}