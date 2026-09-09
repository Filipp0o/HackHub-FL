package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons")
public class CreareHackathonBoundary {

    private final CreareHackathonControl creareHackathonControl;
    private final SessioneUtente sessioneUtente;

    public CreareHackathonBoundary(
            CreareHackathonControl creareHackathonControl,
            SessioneUtente sessioneUtente
    ) {
        this.creareHackathonControl = Objects.requireNonNull(
                creareHackathonControl,
                "Il control di creazione dell'hackathon è obbligatorio"
        );
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @PostMapping("/verifica")
    public RiepilogoCreazioneHackathon verificaCreazioneHackathon(
            @RequestBody RichiestaCreazioneHackathon richiesta
    ) {
        sessioneUtente.recupera();
        CreazioneVerificata verifica = verificaRichiesta(richiesta);

        return new RiepilogoCreazioneHackathon(
                verifica.dati(),
                new UtenteAssegnabile(
                        verifica.giudice().getId(),
                        verifica.giudice().recuperaEmail()
                ),
                verifica.mentori().stream()
                        .map(mentore -> new UtenteAssegnabile(
                                mentore.getId(),
                                mentore.recuperaEmail()
                        ))
                        .toList()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void creaHackathon(
            @RequestBody RichiestaCreazioneHackathon richiesta
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        CreazioneVerificata verifica = verificaRichiesta(richiesta);

        creareHackathonControl.crea(
                verifica.dati(),
                organizzatore,
                verifica.giudice(),
                verifica.mentori()
        );
    }

    private CreazioneVerificata verificaRichiesta(
            RichiestaCreazioneHackathon richiesta
    ) {
        RichiestaCreazioneHackathon richiestaValida =
                Objects.requireNonNull(
                        richiesta,
                        "La richiesta di creazione è obbligatoria"
                );

        DatiHackathon dati = new DatiHackathon(
                richiestaValida.nome(),
                richiestaValida.regolamento(),
                richiestaValida.criteriValutazione(),
                richiestaValida.scadenzaIscrizioni(),
                richiestaValida.dataInizio(),
                richiestaValida.dataFine(),
                richiestaValida.luogo(),
                richiestaValida.importoPremio(),
                richiestaValida.dimensioneMassimaTeam()
        );

        Utente giudice = richiestaValida.giudiceId() == null
                ? null
                : creareHackathonControl.recuperaUtenteAssegnabile(
                richiestaValida.giudiceId()
        );

        List<Utente> mentori = richiestaValida.mentoriIds() == null
                ? null
                : richiestaValida.mentoriIds().stream()
                .map(creareHackathonControl::recuperaUtenteAssegnabile)
                .toList();

        List<String> errori =
                creareHackathonControl.verificaInformazioniEStaff(
                        dati, giudice, mentori
                );

        if (!errori.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errori));
        }

        return new CreazioneVerificata(dati, giudice, mentori);
    }

    private record CreazioneVerificata(
            DatiHackathon dati,
            Utente giudice,
            List<Utente> mentori
    ) {
    }

    public record RiepilogoCreazioneHackathon(
            DatiHackathon dati,
            UtenteAssegnabile giudice,
            List<UtenteAssegnabile> mentori
    ) {
    }

    public record RichiestaCreazioneHackathon(
            String nome,
            String regolamento,
            String criteriValutazione,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine,
            String luogo,
            BigDecimal importoPremio,
            Integer dimensioneMassimaTeam,
            Long giudiceId,
            List<Long> mentoriIds
    ) {
    }

    @GetMapping("/utenti-assegnabili")
    public List<UtenteAssegnabile> ottieniUtentiAssegnabili() {
        sessioneUtente.recupera();

        return creareHackathonControl.recuperaUtentiAssegnabili().stream()
                .map(utente -> new UtenteAssegnabile(
                        utente.getId(),
                        utente.recuperaEmail()
                ))
                .toList();
    }

    public record UtenteAssegnabile(
            Long id,
            String email
    ) {
    }
}