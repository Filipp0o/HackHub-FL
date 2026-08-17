package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.ValutazioneRepository;
import io.github.filipp0o.hackhub.domain.Valutazione;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValutazioneRepositoryImpl
        implements ValutazioneRepository {

    private final List<Valutazione> valutazioniSalvate =
            new ArrayList<>();

    @Override
    public void salva(Valutazione valutazione) {
        valutazioniSalvate.add(
                Objects.requireNonNull(
                        valutazione,
                        "La valutazione è obbligatoria"
                )
        );
    }
}