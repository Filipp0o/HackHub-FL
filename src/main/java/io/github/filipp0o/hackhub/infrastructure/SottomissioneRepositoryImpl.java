package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SottomissioneRepository;
import io.github.filipp0o.hackhub.domain.Sottomissione;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SottomissioneRepositoryImpl
        implements SottomissioneRepository {

    private final List<Sottomissione> sottomissioniSalvate =
            new ArrayList<>();

    @Override
    public void salva(
            Sottomissione sottomissione
    ) {
        sottomissioniSalvate.add(
                Objects.requireNonNull(
                        sottomissione,
                        "La sottomissione è obbligatoria"
                )
        );
    }
}