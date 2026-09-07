package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface InvitoRepository {

    void salva(Invito invito);

    /** Restituisce gli inviti del destinatario ancora da accettare. */
    List<Invito> recuperaInvitiRicevuti(Utente utente);
}
