package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;

import java.util.List;
import java.util.Objects;

public class ConsultareHackathonControl {

    private final HackathonRepository
            hackathonRepository;

    public ConsultareHackathonControl(
            HackathonRepository hackathonRepository
    ) {
        this.hackathonRepository =
                Objects.requireNonNull(
                        hackathonRepository,
                        "Il repository degli hackathon è obbligatorio"
                );
    }

    public List<Hackathon> consultaHackathon() {
        return hackathonRepository
                .ottieniTuttiHackathon();
    }

    public Hackathon selezionaHackathon(
            Long hackathonId
    ) {
        Long hackathonIdValido =
                Objects.requireNonNull(
                        hackathonId,
                        "L'id dell'hackathon è obbligatorio"
                );

        return Objects.requireNonNull(
                hackathonRepository.recuperaHackathon(
                        hackathonIdValido
                ),
                "L'hackathon selezionato è obbligatorio"
        );
    }
}