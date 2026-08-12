package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface UtenteRepository {

    List<Utente> recuperaUtentiAssegnabili();
}