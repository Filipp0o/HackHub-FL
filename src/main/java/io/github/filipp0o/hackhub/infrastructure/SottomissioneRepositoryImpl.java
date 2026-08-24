package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SottomissioneRepository;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SottomissioneRepositoryImpl
        implements SottomissioneRepository {

    private final List<Sottomissione> sottomissioniSalvate =
            new ArrayList<>();

    @Override
    public Sottomissione recuperaSottomissione(
            Partecipazione partecipazione
    ) {
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                );

        return sottomissioniSalvate.stream()
                .filter(sottomissione ->
                        sottomissione.getPartecipazione()
                                == partecipazioneValida
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Sottomissione non trovata"
                        )
                );
    }

    @Override
    public void salva(
            Sottomissione sottomissione
    ) {
        Sottomissione sottomissioneValida =
                Objects.requireNonNull(
                        sottomissione,
                        "La sottomissione è obbligatoria"
                );

        sottomissioniSalvate.removeIf(
                sottomissioneSalvata ->
                        Objects.equals(
                                sottomissioneSalvata.getId(),
                                sottomissioneValida.getId()
                        )
        );

        sottomissioniSalvate.add(
                sottomissioneValida
        );
    }
}