package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.domain.Utente;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simulatore: nessun pagamento reale e nessun dato finanziario personale.
 */
public class SistemaPagamentoAdapter implements SistemaPagamentoGateway {

    private static final Map<Path, Object> LOCK = new ConcurrentHashMap<>();

    private final Properties memoria = new Properties();
    private final Path archivio;

    public SistemaPagamentoAdapter() {
        this.archivio = null;
    }

    public SistemaPagamentoAdapter(Path archivio) {
        this.archivio = Objects.requireNonNull(archivio)
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String avviaConfigurazioneBeneficiario(
            Utente responsabileTeam
    ) {
        Objects.requireNonNull(
                responsabileTeam,
                "Il responsabile del team è obbligatorio"
        );

        Long id = responsabileTeam.getId();

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Responsabile senza identificativo valido"
            );
        }

        return esegui(dati -> {
            String chiave = "beneficiario." + id;
            String riferimento = dati.getProperty(chiave);

            if (riferimento == null) {
                riferimento = "beneficiary-" + UUID.randomUUID();
                dati.setProperty(chiave, riferimento);
            }

            return riferimento;
        });
    }

    @Override
    public String richiediErogazionePremio(
            BigDecimal importoPremio,
            String beneficiaryRef,
            String chiaveErogazione
    ) {
        Objects.requireNonNull(
                importoPremio,
                "L'importo del premio è obbligatorio"
        );

        if (importoPremio.signum() <= 0) {
            throw new IllegalArgumentException(
                    "L'importo del premio deve essere maggiore di zero"
            );
        }

        if (beneficiaryRef == null || beneficiaryRef.isBlank()) {
            throw new IllegalArgumentException(
                    "Il riferimento del beneficiario è obbligatorio"
            );
        }

        if (chiaveErogazione == null || chiaveErogazione.isBlank()) {
            throw new IllegalArgumentException(
                    "La chiave di erogazione è obbligatoria"
            );
        }

        String prefisso = "pagamento."
                + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        chiaveErogazione.getBytes(StandardCharsets.UTF_8)
                )
                + ".";

        return esegui(dati -> {
            String riferimento =
                    dati.getProperty(prefisso + "riferimento");

            if (riferimento != null) {
                boolean stessoBeneficiario = beneficiaryRef.equals(
                        dati.getProperty(prefisso + "beneficiario")
                );

                BigDecimal importoRegistrato = new BigDecimal(
                        dati.getProperty(prefisso + "importo")
                );

                if (!stessoBeneficiario
                        || importoPremio.compareTo(importoRegistrato) != 0) {
                    throw new IllegalArgumentException(
                            "Chiave di erogazione già utilizzata con dati diversi"
                    );
                }

                return riferimento;
            }

            riferimento = "payment-" + UUID.randomUUID();

            dati.setProperty(prefisso + "riferimento", riferimento);
            dati.setProperty(prefisso + "beneficiario", beneficiaryRef);
            dati.setProperty(
                    prefisso + "importo",
                    importoPremio.toPlainString()
            );

            return riferimento;
        });
    }

    private String esegui(Function<Properties, String> operazione) {
        if (archivio == null) {
            synchronized (memoria) {
                return operazione.apply(memoria);
            }
        }

        synchronized (LOCK.computeIfAbsent(archivio, p -> new Object())) {
            try {
                Files.createDirectories(archivio.getParent());

                Path lock = archivio.resolveSibling(
                        archivio.getFileName() + ".lock"
                );

                try (
                        FileChannel canale = FileChannel.open(
                                lock,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.WRITE
                        );
                        var blocco = canale.lock()
                ) {
                    Properties dati = new Properties();

                    if (Files.exists(archivio)) {
                        try (InputStream in = Files.newInputStream(archivio)) {
                            dati.load(in);
                        }
                    }

                    String risultato = operazione.apply(dati);

                    Path temporaneo = Files.createTempFile(
                            archivio.getParent(),
                            "pagamenti-",
                            ".tmp"
                    );

                    try {
                        try (
                                OutputStream out =
                                        Files.newOutputStream(temporaneo)
                        ) {
                            dati.store(
                                    out,
                                    "Sistema di pagamento simulato HackHub"
                            );
                        }

                        Files.move(
                                temporaneo,
                                archivio,
                                StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } finally {
                        Files.deleteIfExists(temporaneo);
                    }

                    return risultato;
                }
            } catch (IOException errore) {
                throw new IllegalStateException(
                        "Archivio del pagamento simulato non disponibile",
                        errore
                );
            }
        }
    }
}