# HackHub

HackHub è un progetto universitario per la gestione degli hackathon. Permette di creare un evento, iscrivere i team, raccogliere e valutare le sottomissioni, proclamare il vincitore e gestire il premio.

L'applicazione è sviluppata in Java con Spring Boot ed espone API REST, utilizzabili con un client HTTP come curl o Postman.

## Funzionalità

- Registrazione e accesso degli utenti.
- Consultazione e creazione degli hackathon, con assegnazione di giudice e mentori.
- Creazione dei team, invito degli utenti e accettazione degli inviti.
- Iscrizione dei team agli hackathon.
- Invio, aggiornamento e valutazione delle sottomissioni.
- Segnalazione delle violazioni, notifiche all'organizzatore ed esame delle segnalazioni.
- Proclamazione del team vincitore, configurazione della riscossione ed erogazione simulata del premio.

I passaggi di verifica, riepilogo e conferma sono disponibili per i casi d'uso che li prevedono. Le operazioni riservate agli utenti autenticati utilizzano una sessione HTTP: il client deve conservare e reinviare il cookie ricevuto all'accesso.

## Avvio

È necessario JDK 21. Il progetto include Maven Wrapper, che scarica Maven e le dipendenze al primo utilizzo.

Dal terminale, nella cartella del progetto:

```bash
bash ./mvnw spring-boot:run
```

L'applicazione viene avviata su `http://localhost:8080`. Il profilo predefinito è `in-memory`: i dati rimangono disponibili fino alla chiusura dell'applicazione.

Per conservare i dati tra un avvio e l'altro, utilizzare il profilo `persistent`:

```bash
bash ./mvnw spring-boot:run -Dspring-boot.run.profiles=persistent
```

Questo profilo utilizza Spring JDBC e un database H2 salvato in `data/hackhub.mv.db`. I percorsi sono relativi alla cartella da cui viene avviata l'applicazione. Dopo un riavvio è necessario effettuare nuovamente l'accesso.


## Pagamento simulato

La configurazione del beneficiario e l'erogazione del premio utilizzano un simulatore. Non vengono effettuati trasferimenti di denaro né raccolti dati personali e finanziari per il pagamento.

Nel profilo `persistent`, il simulatore salva i riferimenti dei beneficiari e dei pagamenti in `data/pagamenti-simulati.properties`. Un nuovo tentativo di registrazione dello stesso premio recupera il pagamento già effettuato dal simulatore, evitando di duplicarlo. Il database e questo archivio devono essere conservati insieme.

## Struttura del progetto

- `domain`: entità e regole del dominio.
- `application`: controlli dei casi d'uso e interfacce dei repository e dei servizi esterni.
- `infrastructure`: repository in memoria e JDBC, codifica delle password e simulatore di pagamento.
- `presentation`: API REST e gestione della sessione.
- `configuration`: configurazione di Spring e dei profili.

Il modello Visual Paradigm si trova in `models/HackHub.vpp` e contiene i diagrammi delle tre iterazioni. Le rotte REST e i dati delle richieste sono definiti nelle classi del package `presentation`.



## Sviluppi futuri

La gestione delle richieste di supporto, la proposta di call e la prenotazione degli slot, compresa l'integrazione con Calendar, rimangono funzionalità future. I relativi casi d'uso UC06, UC07, UC19 e UC20 sono presenti nel modello generale, ma non sono dettagliati né implementati in questa versione.

È inoltre prevista come possibile estensione l'integrazione con un sistema di pagamento reale.

## Autori

- Mattia Livieri
- Filippo Ferretti

## Licenza

Il progetto è distribuito con licenza MIT. Consultare il file `LICENSE`.

