package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JdbcHackathonRepository implements HackathonRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate transazioni;

    public JdbcHackathonRepository(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "Il datasource è obbligatorio");
        jdbc = JdbcClient.create(dataSource);
        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);
        transazioni = new TransactionTemplate(gestore);
        transazioni.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public Hackathon recuperaHackathon(Long hackathonId) {
        Objects.requireNonNull(hackathonId, "L'id dell'hackathon è obbligatorio");
        return esegui(() -> carica(hackathonId, LocalDate.now()));
    }

    @Override
    public List<Hackathon> ottieniTuttiHackathon() {
        return elenco(hackathon -> true);
    }

    @Override
    public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
        return elenco(Hackathon::isApertoAlleIscrizioni);
    }

    @Override
    public List<Hackathon> ottieniHackathonValutabili(Utente giudice) {
        Objects.requireNonNull(giudice, "Il giudice è obbligatorio");
        if (giudice.getId() == null) return List.of();
        return elenco(h -> h.consenteValutazioni()
                && giudice.getId().equals(h.getGiudice().getId())
                && jdbc.sql("""
                        SELECT COUNT(*) FROM partecipazione p
                        JOIN sottomissione s ON s.partecipazione_id = p.id
                        WHERE p.hackathon_id = :id
                          AND NOT EXISTS (
                              SELECT 1 FROM valutazione v WHERE v.sottomissione_id = s.id
                          )
                        """).param("id", h.getId()).query(Long.class).single() > 0);
    }

    @Override
    public List<Hackathon> ottieniHackathonSegnalabili(Utente mentore) {
        Objects.requireNonNull(mentore, "Il mentore è obbligatorio");
        if (mentore.getId() == null) return List.of();
        return elenco(h -> h.consenteSegnalazioni()
                && h.getMentori().stream().anyMatch(u -> mentore.getId().equals(u.getId()))
                && jdbc.sql("SELECT COUNT(*) FROM partecipazione WHERE hackathon_id = :id")
                .param("id", h.getId()).query(Long.class).single() > 0);
    }

    private List<Hackathon> elenco(Predicate<Hackathon> filtro) {
        return esegui(() -> {
            LocalDate oggi = LocalDate.now();
            return jdbc.sql("SELECT id FROM hackathon ORDER BY id")
                    .query(Long.class).list().stream()
                    .map(id -> carica(id, oggi)).filter(filtro).toList();
        });
    }

    private Hackathon carica(Long id, LocalDate oggi) {
        Hackathon h = jdbc.sql("SELECT * FROM hackathon WHERE id = :id")
                .param("id", id)
                .query((rs, numero) -> Hackathon.ricostruisci(new DatiRipristinoHackathon(
                        id,
                        new DatiHackathon(
                                rs.getString("nome"), rs.getString("regolamento"),
                                rs.getString("criteri_valutazione"),
                                rs.getDate("scadenza_iscrizioni").toLocalDate(),
                                rs.getDate("data_inizio").toLocalDate(),
                                rs.getDate("data_fine").toLocalDate(), rs.getString("luogo"),
                                rs.getBigDecimal("importo_premio"),
                                rs.getInt("dimensione_massima_team")
                        ),
                        caricaUtente(rs.getLong("organizzatore_id")),
                        caricaUtente(rs.getLong("giudice_id")),
                        jdbc.sql("SELECT mentore_id FROM hackathon_mentore WHERE hackathon_id = :id ORDER BY mentore_id")
                                .param("id", id).query(Long.class).list().stream()
                                .map(this::caricaUtente).toList(),
                        TipoStatoHackathon.valueOf(rs.getString("tipo_stato")),
                        caricaVincitrice(id, rs.getObject("vincitrice_id", Long.class)),
                        caricaRiscossione(id)
                )))
                .optional().orElseThrow(() -> new IllegalStateException("Hackathon non trovato"));

        TipoStatoHackathon precedente = h.getStato();
        h.aggiornaStato(oggi);
        if (precedente != h.getStato()) {
            verificaRiga(jdbc.sql("""
                            UPDATE hackathon SET tipo_stato = :nuovo
                            WHERE id = :id AND tipo_stato = :precedente
                            """)
                    .param("nuovo", h.getStato().name()).param("id", id)
                    .param("precedente", precedente.name()).update());
        }
        return h;
    }

    private Utente caricaUtente(Long id) {
        return jdbc.sql("SELECT id, email, password_hash FROM utente WHERE id = :id")
                .param("id", id).query((rs, numero) -> Utente.ricostruisci(
                        rs.getLong("id"), rs.getString("email"), rs.getString("password_hash")
                )).single();
    }

    private Team caricaTeam(Long id) {
        return jdbc.sql("SELECT nome, responsabile_id FROM team WHERE id = :id")
                .param("id", id).query((rs, numero) -> {
                    List<Utente> membri = jdbc.sql("""
                                    SELECT u.id, u.email, u.password_hash
                                    FROM team_membro m JOIN utente u ON u.id = m.utente_id
                                    WHERE m.team_id = :id ORDER BY u.id
                                    """)
                            .param("id", id).query((r, n) -> Utente.ricostruisci(
                                    r.getLong("id"), r.getString("email"), r.getString("password_hash")
                            )).list();
                    long responsabileId = rs.getLong("responsabile_id");
                    Utente responsabile = membri.stream()
                            .filter(u -> u.getId() == responsabileId).findFirst()
                            .orElseThrow(() -> new IllegalStateException("Responsabile assente dai membri"));
                    return Team.ricostruisci(id, rs.getString("nome"), membri, responsabile);
                }).single();
    }

    private DatiRipristinoHackathon.Vincitrice caricaVincitrice(Long hackathonId, Long id) {
        if (id == null) return null;
        return jdbc.sql("SELECT team_id, stato FROM partecipazione WHERE id = :id AND hackathon_id = :hackathon")
                .param("id", id).param("hackathon", hackathonId)
                .query((rs, numero) -> new DatiRipristinoHackathon.Vincitrice(
                        id, caricaTeam(rs.getLong("team_id")),
                        StatoPartecipazione.valueOf(rs.getString("stato")), caricaSottomissione(id)
                )).single();
    }

    private DatiRipristinoHackathon.SottomissioneSalvata caricaSottomissione(Long partecipazioneId) {
        return jdbc.sql("SELECT id, contenuto FROM sottomissione WHERE partecipazione_id = :id")
                .param("id", partecipazioneId)
                .query((rs, numero) -> new DatiRipristinoHackathon.SottomissioneSalvata(
                        rs.getLong("id"), rs.getString("contenuto"), caricaValutazione(rs.getLong("id"))
                )).optional().orElse(null);
    }

    private DatiRipristinoHackathon.ValutazioneSalvata caricaValutazione(Long sottomissioneId) {
        return jdbc.sql("SELECT * FROM valutazione WHERE sottomissione_id = :id")
                .param("id", sottomissioneId)
                .query((rs, numero) -> new DatiRipristinoHackathon.ValutazioneSalvata(
                        rs.getLong("id"), caricaUtente(rs.getLong("giudice_id")),
                        new DatiValutazione(rs.getString("giudizio"), rs.getBigDecimal("punteggio")),
                        rs.getTimestamp("data_ora").toLocalDateTime()
                )).optional().orElse(null);
    }

    private DatiRipristinoHackathon.Riscossione caricaRiscossione(Long hackathonId) {
        return jdbc.sql("SELECT * FROM riscossione_premio WHERE hackathon_id = :id")
                .param("id", hackathonId)
                .query((rs, numero) -> new DatiRipristinoHackathon.Riscossione(
                        rs.getLong("id"), StatoRiscossionePremio.valueOf(rs.getString("stato")),
                        rs.getString("beneficiary_ref"), rs.getString("payment_ref")
                )).optional().orElse(null);
    }

    @Override
    public void salva(Hackathon h) {
        Objects.requireNonNull(h, "L'hackathon è obbligatorio");
        verificaIdNonInAttesa(h);
        BigDecimal premio = premioEsatto(h.getImportoPremio());
        boolean nuovo = h.getId() == null;
        RiscossionePremio riscossione = h.getRiscossionePremio();
        boolean nuovaRiscossione = riscossione != null && riscossione.getId() == null;

        long[] id = esegui(() -> {
            if (!nuovo) verificaAssociazioniConservate(h);
            verificaVincitrice(h);
            long hackathonId = scriviHackathon(h, premio);
            jdbc.sql("DELETE FROM hackathon_mentore WHERE hackathon_id = :id")
                    .param("id", hackathonId).update();
            for (Long mentoreId : h.getMentori().stream().map(Utente::getId).distinct().toList()) {
                jdbc.sql("INSERT INTO hackathon_mentore (hackathon_id, mentore_id) VALUES (:id, :mentore)")
                        .param("id", hackathonId).param("mentore", mentoreId).update();
            }
            long riscossioneId = riscossione == null ? 0 : scriviRiscossione(hackathonId, riscossione);
            return new long[]{hackathonId, riscossioneId};
        });

        if (nuovo || nuovaRiscossione) dopoCommit(h, () -> {
            if (nuovo) h.assegnaId(id[0]);
            if (nuovaRiscossione) riscossione.assegnaId(id[1]);
        });
    }

    private void verificaAssociazioniConservate(Hackathon h) {
        Long precedente = jdbc.sql("SELECT vincitrice_id FROM hackathon WHERE id = :id FOR UPDATE")
                .param("id", h.getId())
                .query((rs, n) -> java.util.Optional.ofNullable(rs.getObject(1, Long.class)))
                .single().orElse(null);
        Long attuale = h.getVincitrice() == null ? null : h.getVincitrice().getId();
        if (precedente != null && !precedente.equals(attuale)) {
            throw new IllegalStateException("La vincitrice già registrata deve essere conservata");
        }
        if (h.getRiscossionePremio() == null
                && jdbc.sql("SELECT COUNT(*) FROM riscossione_premio WHERE hackathon_id = :id")
                .param("id", h.getId()).query(Long.class).single() != 0) {
            throw new IllegalStateException("La riscossione già registrata deve essere conservata");
        }
    }

    private void verificaVincitrice(Hackathon h) {
        Partecipazione p = h.getVincitrice();
        if (p == null) return;
        if (p.getId() == null || !h.haStessaIdentita(p.getHackathon())
                || p.getStato() != StatoPartecipazione.ATTIVA) {
            throw new IllegalStateException("La vincitrice deve essere una partecipazione attiva già salvata");
        }
        long presenti = jdbc.sql("""
                        SELECT COUNT(*) FROM partecipazione
                        WHERE id = :id AND hackathon_id = :hackathon AND team_id = :team AND stato = 'ATTIVA'
                        """)
                .param("id", p.getId()).param("hackathon", h.getId())
                .param("team", p.getTeam().getId()).query(Long.class).single();
        if (presenti != 1) throw new IllegalStateException("Vincitrice assente o non più attiva");
    }

    private long scriviHackathon(Hackathon h, BigDecimal premio) {
        boolean nuovo = h.getId() == null;
        String sql = nuovo ? """
                INSERT INTO hackathon (nome, regolamento, criteri_valutazione, scadenza_iscrizioni,
                    data_inizio, data_fine, luogo, importo_premio, dimensione_massima_team,
                    tipo_stato, organizzatore_id, giudice_id, vincitrice_id)
                VALUES (:nome, :regolamento, :criteri, :scadenza, :inizio, :fine, :luogo,
                    :premio, :dimensione, :stato, :organizzatore, :giudice, :vincitrice)
                """ : """
                UPDATE hackathon SET nome = :nome, regolamento = :regolamento,
                    criteri_valutazione = :criteri, scadenza_iscrizioni = :scadenza,
                    data_inizio = :inizio, data_fine = :fine, luogo = :luogo,
                    importo_premio = :premio, dimensione_massima_team = :dimensione,
                    tipo_stato = :stato, organizzatore_id = :organizzatore,
                    giudice_id = :giudice, vincitrice_id = :vincitrice WHERE id = :id
                """;
        var richiesta = jdbc.sql(sql)
                .param("nome", h.getNome()).param("regolamento", h.getRegolamento())
                .param("criteri", h.getCriteriValutazione()).param("scadenza", h.getScadenzaIscrizioni())
                .param("inizio", h.getDataInizio()).param("fine", h.getDataFine()).param("luogo", h.getLuogo())
                .param("premio", premio).param("dimensione", h.getDimensioneMassimaTeam())
                .param("stato", h.getStato().name()).param("organizzatore", h.getOrganizzatore().getId())
                .param("giudice", h.getGiudice().getId())
                .param("vincitrice", h.getVincitrice() == null ? null : h.getVincitrice().getId());
        if (!nuovo) {
            verificaRiga(richiesta.param("id", h.getId()).update());
            return h.getId();
        }
        GeneratedKeyHolder chiave = new GeneratedKeyHolder();
        verificaRiga(richiesta.update(chiave, "ID"));
        return chiaveValida(chiave);
    }

    private long scriviRiscossione(long hackathonId, RiscossionePremio r) {
        boolean nuova = r.getId() == null;
        var richiesta = jdbc.sql(nuova ? """
                        INSERT INTO riscossione_premio (hackathon_id, stato, beneficiary_ref, payment_ref)
                        VALUES (:hackathon, :stato, :beneficiario, :pagamento)
                        """ : """
                        UPDATE riscossione_premio SET stato = :stato, beneficiary_ref = :beneficiario,
                            payment_ref = :pagamento WHERE id = :id AND hackathon_id = :hackathon
                        """)
                .param("hackathon", hackathonId).param("stato", r.getStato().name())
                .param("beneficiario", r.getBeneficiaryRef()).param("pagamento", r.getPaymentRef());
        if (!nuova) {
            verificaRiga(richiesta.param("id", r.getId()).update());
            return r.getId();
        }
        GeneratedKeyHolder chiave = new GeneratedKeyHolder();
        verificaRiga(richiesta.update(chiave, "ID"));
        return chiaveValida(chiave);
    }

    private BigDecimal premioEsatto(BigDecimal premio) {
        try {
            BigDecimal esatto = premio.setScale(2, RoundingMode.UNNECESSARY);
            if (esatto.signum() <= 0 || esatto.precision() > 19) {
                throw new IllegalArgumentException("Premio non rappresentabile come DECIMAL(19,2)");
            }
            return esatto;
        } catch (ArithmeticException errore) {
            throw new IllegalArgumentException("Il premio deve essere esprimibile in centesimi", errore);
        }
    }

    private <T> T esegui(Supplier<T> operazione) {
        try {
            return transazioni.execute(stato -> operazione.get());
        } catch (RuntimeException errore) {
            throw new IllegalStateException("Operazione sul repository hackathon fallita", errore);
        }
    }

    private void verificaIdNonInAttesa(Hackathon h) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.getSynchronizations().stream()
                .anyMatch(s -> s instanceof AssegnazioneId a && a.entita() == h)) {
            throw new IllegalStateException("L'identificativo sarà disponibile dopo il commit della transazione");
        }
    }

    private void dopoCommit(Hackathon h, Runnable azione) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new AssegnazioneId(h, azione));
        } else {
            azione.run();
        }
    }

    private record AssegnazioneId(Hackathon entita, Runnable azione) implements TransactionSynchronization {
        @Override
        public void afterCommit() {
            azione.run();
        }
    }

    private void verificaRiga(int righe) {
        if (righe != 1) throw new IllegalStateException("Record assente o modifica concorrente");
    }

    private long chiaveValida(GeneratedKeyHolder chiave) {
        Number id = chiave.getKey();
        if (id == null || id.longValue() <= 0) {
            throw new IllegalStateException("Inserimento senza identificativo valido");
        }
        return id.longValue();
    }
}