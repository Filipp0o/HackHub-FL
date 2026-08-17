package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;
import java.util.Objects;

public class UtenteRepositoryImpl implements UtenteRepository {

    private final List<Utente> utentiAssegnabili;

    public UtenteRepositoryImpl(
            List<Utente> utentiAssegnabili
    ) {
        Objects.requireNonNull(
                utentiAssegnabili,
                "La lista degli utenti è obbligatoria"
        );

        this.utentiAssegnabili =
                List.copyOf(utentiAssegnabili);
    }

    @Override
    public List<Utente> recuperaUtentiAssegnabili() {
        return utentiAssegnabili;
    }
}