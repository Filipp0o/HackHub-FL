package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;

public interface SottomissioneRepository {

    Sottomissione recuperaSottomissione(
            Partecipazione partecipazione
    );

    void salva(Sottomissione sottomissione);
}