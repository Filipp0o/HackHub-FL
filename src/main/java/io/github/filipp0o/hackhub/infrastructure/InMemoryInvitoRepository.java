package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InMemoryInvitoRepository implements InvitoRepository {

    private final Map<Long, Invito> invitiSalvati = new LinkedHashMap<>();
    private long prossimoId = 1L;

    @Override
    public void salva(Invito invito) {
        Invito invitoValido = Objects.requireNonNull(invito, "L'invito è obbligatorio");

        if (invitoValido.getId() == null) {
            invitoValido.assegnaId(prossimoId++);
        } else {
            prossimoId = Math.max(prossimoId, invitoValido.getId() + 1);
        }

        invitiSalvati.put(invitoValido.getId(), invitoValido);
    }

    @Override
    public List<Invito> recuperaInvitiRicevuti(Utente utente) {
        Utente destinatario = Objects.requireNonNull(utente, "L'utente è obbligatorio");

        return invitiSalvati.values().stream()
                .filter(invito -> !invito.isAccettato())
                .filter(invito -> stessaIdentita(invito.getDestinatario(), destinatario))
                .toList();
    }

    private static boolean stessaIdentita(Utente primo, Utente secondo) {
        return primo == secondo
                || (primo.getId() != null && Objects.equals(primo.getId(), secondo.getId()));
    }
}
