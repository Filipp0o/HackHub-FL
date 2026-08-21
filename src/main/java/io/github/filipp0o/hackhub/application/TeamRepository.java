package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

public interface TeamRepository {

    boolean verificaAppartenenzaTeam(Utente utente);

    Team recuperaTeam(Utente utente);

    void salva(Team team);
}