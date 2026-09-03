package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryHackathonRepository
        implements HackathonRepository {

    private final List<Hackathon> hackathonSalvati =
            new ArrayList<>();

    private final PartecipazioneRepository
            partecipazioneRepository;

    public InMemoryHackathonRepository(
            PartecipazioneRepository partecipazioneRepository
    ) {
        this.partecipazioneRepository =
                Objects.requireNonNull(
                        partecipazioneRepository,
                        "Il repository delle partecipazioni è obbligatorio"
                );
    }

    @Override
    public List<Hackathon> ottieniHackathonValutabili(
            Utente giudice
    ) {
        Utente giudiceValido = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        return hackathonSalvati.stream()
                .filter(Hackathon::consenteValutazioni)
                .filter(hackathon ->
                        Objects.equals(
                                hackathon.getGiudice().getId(),
                                giudiceValido.getId()
                        )
                )
                .filter(this::haSottomissioneNonValutata)
                .toList();
    }

    @Override
    public List<Hackathon> ottieniHackathonSegnalabili(
            Utente mentore
    ) {
        Utente mentoreValido = Objects.requireNonNull(
                mentore,
                "Il mentore è obbligatorio"
        );

        return hackathonSalvati.stream()
                .filter(Hackathon::consenteSegnalazioni)
                .filter(hackathon ->
                        hackathon.getMentori().stream()
                                .anyMatch(mentoreAssegnato ->
                                        Objects.equals(
                                                mentoreAssegnato.getId(),
                                                mentoreValido.getId()
                                        )
                                )
                )
                .filter(this::haAlmenoUnTeamIscritto)
                .toList();
    }

    @Override
    public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
        return hackathonSalvati.stream()
                .filter(Hackathon::isApertoAlleIscrizioni)
                .toList();
    }

    @Override
    public List<Hackathon> ottieniTuttiHackathon() {
        return List.copyOf(
                hackathonSalvati
        );
    }

    @Override
    public Hackathon recuperaHackathon(
            Long hackathonId
    ) {
        Long hackathonIdValido =
                Objects.requireNonNull(
                        hackathonId,
                        "L'id dell'hackathon è obbligatorio"
                );

        return hackathonSalvati.stream()
                .filter(hackathon ->
                        Objects.equals(
                                hackathon.getId(),
                                hackathonIdValido
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Hackathon non trovato"
                        )
                );
    }

    @Override
    public void salva(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido =
                Objects.requireNonNull(
                        hackathon,
                        "L'hackathon è obbligatorio"
                );

        for (int indice = 0;
             indice < hackathonSalvati.size();
             indice++) {
            if (Objects.equals(
                    hackathonSalvati.get(indice).getId(),
                    hackathonValido.getId()
            )) {
                hackathonSalvati.set(
                        indice,
                        hackathonValido
                );
                return;
            }
        }

        hackathonSalvati.add(hackathonValido);
    }

    private boolean haSottomissioneNonValutata(
            Hackathon hackathon
    ) {
        return partecipazioneRepository
                .ottieniPartecipazioni(hackathon)
                .stream()
                .map(Partecipazione::getSottomissione)
                .filter(Objects::nonNull)
                .anyMatch(sottomissione ->
                        sottomissione.getValutazione() == null
                );
    }

    private boolean haAlmenoUnTeamIscritto(
            Hackathon hackathon
    ) {
        return !partecipazioneRepository
                .ottieniPartecipazioni(hackathon)
                .isEmpty();
    }
}