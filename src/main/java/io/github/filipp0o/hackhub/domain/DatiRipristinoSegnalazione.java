package io.github.filipp0o.hackhub.domain;

import java.time.LocalDateTime;

public record DatiRipristinoSegnalazione(
        Long id,
        Utente mentoreSegnalante,
        Partecipazione partecipazione,
        String descrizione,
        LocalDateTime dataOraCreazione,
        StatoSegnalazione stato,
        EsitoSegnalazione esito,
        String motivazione,
        LocalDateTime dataOraEsame,
        Utente esaminatore
) {}