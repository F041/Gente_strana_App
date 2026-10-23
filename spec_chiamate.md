# Chiamate audio (e video facoltativo) — specifica comune Android + web

Stato (2026-10-21): **server e web fatti e online**, provati da cima a fondo con due browser (vedi §9). **Android da fare**, seguendo
questo documento: stessi dati, stesse regole, stesse notifiche, così una chiamata partita da Android squilla sul web e viceversa.
Le modifiche a questo documento vanno segnalate anche in `handover_pwa.md`.

Riferimenti nel codice:
- server: `GenteStranaFirebase/functions/src/index.ts`, sezione "Chiamate" (`startCall`, `getCallToken`, `sendCallNotification`);
- regole: `GenteStranaFirebase/firestore.rules`, blocco `calls` (copia in `firestore_rules.md`);
- web: `GenteStranaWeb/app/src/calls.ts` (dati e funzioni), `callContext.tsx` (stato della chiamata, squillo, rinnovo),
  `agoraCall.ts` (audio e video), `callTones.ts` (suoni), `screens/CallScreen.tsx`, `public/push-sw.js` (notifiche).

## 1. Cosa fa (prima versione)
- Chiamata **tra due persone**, da una chat in cui **ognuno ha scritto almeno 5 messaggi** (Remote Config `call_unlock_messages`,
  predefinito 5). Prima il pulsante si vede grigio, dopo verde (§7). Niente chiamate a sconosciuti, e chi insiste da solo non arriva mai a poter chiamare.
- La chiamata la fa partire **il server** (`startCall`), che conta i messaggi veri: il documento della chat è scrivibile dai partecipanti,
  quindi un contatore salvato lì si potrebbe falsificare.
- Parte **solo audio**. Durante la chiamata ognuno può accendere e spegnere la propria fotocamera.
- Ognuno può decidere di **non ricevere chiamate** (impostazione "Accetto chiamate", attiva di partenza).
- Audio e video passano da **Agora RTC** (stesso servizio usato da Tandem). Il nostro database vede solo lo squillo e lo stato della chiamata,
  mai l'audio o il video.
- **Tetto di costo garantito dal server**: i permessi per entrare in una chiamata li dà solo una nostra Cloud Function, che conta i minuti
  e smette di darli prima di uscire dalla quota gratuita di Agora (10.000 minuti per partecipante al mese).

Fuori dalla prima versione: chiamate di gruppo, chiamata dal profilo di qualcuno, registro delle chiamate dentro la chat
("📞 Chiamata, 12 min"), schermata di chiamata di sistema (CallKit / ConnectionService).

## 2. Costi
| Voce | Uso stimato | Costo |
|---|---|---|
| Agora RTC | 10.000 minuti/partecipante al mese gratuiti; una chiamata a due di 30 min = 60 min | 0 € fino al tetto, che è sotto la quota |
| Cloud Functions `startCall`, `getCallToken` | 1 per chiamata + 1 ogni 10 minuti per partecipante | dentro i 2 milioni gratuiti |
| Firestore | ~5 scritture per chiamata (documento creato, risposta, fine, cancellazione) + 3 per ogni blocco di 10 minuti; 2 conteggi (`count()`, 1 lettura ciascuno) all'apertura di una chat e a ogni `startCall` | trascurabile |
| Secret Manager (certificato Agora) | 1 segreto, letto all'avvio della funzione | dentro la quota gratuita (c'è già `SENDGRID_API_KEY`) |
| Notifiche (FCM, web push) | 1-2 per chiamata | gratis |

Da verificare nella console Agora dopo le prime chiamate vere: come vengono scalati i minuti **video** dalla quota gratuita
(Agora li conta in base alla risoluzione ricevuta). Il tetto conta tutti i minuti allo stesso modo; se il video pesa di più, si abbassa il tetto.

## 3. Configurazione (fatta)
1. Progetto Agora con **App Certificate** attivo: i token sono obbligatori (senza, chiunque abbia l'App ID potrebbe usare i nostri minuti).
2. **App ID** `e9fc67e94e8641749866bde425a988d6`: non è segreto. I client **non** lo tengono nel codice: arriva da `getCallToken`
   insieme al token (campo `appId`), così si cambia in un posto solo.
3. **App Certificate**: segreto `AGORA_APP_CERTIFICATE` in Secret Manager, letto solo da `getCallToken`. Mai nei client, mai in git.
4. Da fare (utente): informativa privacy (sito WordPress) e "Sicurezza dei dati" in Play Console: audio e video delle chiamate passano
   da Agora (non vengono registrati né salvati).

## 4. Dati

### `calls/{callId}` (collezione **temporanea**)
L'ID del documento (ID automatico Firestore, 20 caratteri) è anche il **nome del canale Agora**.

Serve solo mentre la chiamata è in corso: fa squillare l'app aperta anche a chi non ha le notifiche push (sull'iPhone le push ci sono
solo con la PWA installata) e fa sapere a chi chiama se l'altra persona ha risposto, rifiutato o chiuso. **Alla fine viene cancellato**
(§6): non resta uno storico di chi ha chiamato chi. Lo crea solo il server.

| Campo | Tipo | Chi lo scrive | Note |
|---|---|---|---|
| `chatId` | string | `startCall` | chat da cui parte |
| `callerId` | string | `startCall` | chi chiama |
| `calleeId` | string | `startCall` | chi riceve |
| `callerName` | string | `startCall` | `username` di chi chiama, per la finestra dello squillo (niente lettura del profilo) |
| `callerPhoto` | string | `startCall` | prima foto di chi chiama (`profilePicUrl`), `""` se non ce l'ha |
| `participants` | array [callerId, calleeId] | `startCall` | per le query e le regole |
| `status` | string | server e client | `ringing` → `active` → `ended`; oppure `ringing` → `declined` / `cancelled` / `missed` |
| `createdAt` | Timestamp | `startCall` | `serverTimestamp()` |
| `expireAt` | Timestamp | `startCall` | creazione + 1 giorno: criterio TTL di Firestore (attivo), cancella i documenti rimasti appesi |
| `answeredAt` | Timestamp | chi riceve | `serverTimestamp()` quando passa ad `active`; da qui si conta la durata |
| `activeUntil` | Timestamp | `getCallToken` | scadenza dell'ultimo permesso dato; serve a `startCall` per capire se una chiamata `active` è ancora viva |
| `endedAt` | Timestamp | client | `serverTimestamp()` quando finisce in qualsiasi modo |
| `endedBy` | string | client | uid di chi ha chiuso (assente per `missed`) |

Stati:
- `ringing`: creato da `startCall`. Squilla per **10 secondi** (Remote Config `call_ring_seconds`, predefinito 10).
- `active`: chi riceve ha risposto. Solo adesso **tutti e due** chiedono il permesso ed entrano nel canale Agora (chi chiama non entra prima:
  i minuti da soli costano).
- `declined`: chi riceve ha rifiutato.
- `cancelled`: chi chiama ha chiuso prima della risposta.
- `missed`: nessuna risposta entro lo squillo. Lo scrive il client di chi chiama, allo scadere dei 10 s.
- `ended`: chiamata finita dopo la risposta (uno dei due ha chiuso, la linea è caduta, oppure i permessi sono finiti).

Chiamate appese (telefono spento, app chiusa a forza): i client trattano come finita ogni chiamata `ringing` con `createdAt` più vecchio
dello squillo + 10 secondi (20 s con i valori predefiniti) e non la mostrano come in arrivo. `startCall` considera ancora viva:
- una `ringing` fino a `createdAt` + squillo + 10 s;
- una `active` fino ad `activeUntil` + 60 s (senza `activeUntil`: `answeredAt` + 120 s + 60 s);
e cancella le altre che trova dei due partecipanti prima di crearne una nuova.

### `users/{uid}.acceptCalls`
`false` = non accetto chiamate. Assente o `true` = accetto. Lo scrive il client con `update` (un `set()` senza `merge` sul documento utente lo cancellerebbe).

### `users/{uid}.callClients`
Array con le app di quella persona che **sanno ricevere chiamate**: `"web"` (scritto dalla PWA con `arrayUnion` al primo avvio dopo
l'aggiornamento), `"android"` (da scrivere con `arrayUnion` dall'app Android quando avrà le chiamate, all'avvio con l'utente loggato).
- `startCall` rifiuta con `not-supported` se l'array dell'altra persona è vuoto o assente.
- `sendCallNotification` manda le notifiche di chiamata solo alle app elencate.

Perché: le versioni Android di adesso ricevono i messaggi FCM con solo `data` e mostrerebbero una notifica generica, senza poter rispondere.
Togliere `"android"` non serve: chi disinstalla l'app smette di ricevere i push da solo.

### `callUsage/{chiave}` (solo server)
- `callUsage/month-2026-10` → `{minutes, updatedAt}`: minuti dati in permessi nel mese (UTC, come la fatturazione Agora).
- `callUsage/day-2026-10-21-<uid>` → `{minutes, updatedAt}`: minuti di quella persona nel giorno (UTC).
Nessuna regola: i client non la leggono né la scrivono. `cleanupDeletedUser` non la tocca (contano i minuti usati, anche di account cancellati).

## 5. Regole Firestore (`calls`, online)
- **Lettura**: solo chi è in `participants` (anche la query `participants array-contains <me>` dello squillo in arrivo).
- **Creazione e cancellazione**: mai dal client.
- **Aggiornamento**: cambiano solo `status`, `answeredAt`, `endedAt`, `endedBy`, e solo così:
  - chi riceve: `ringing` → `active` con `answeredAt == request.time` (e nient'altro), oppure `ringing` → `declined`;
  - chi chiama: `ringing` → `cancelled` o `missed`;
  - tutti e due: `active` → `ended`;
  - per `declined`, `cancelled`, `missed`, `ended`: `endedAt == request.time`, `endedBy` (se c'è) uguale a chi scrive, `answeredAt` non si tocca;
  - da `declined`, `cancelled`, `missed`, `ended` non si torna indietro.
- In pratica, le scritture del client sono esattamente queste (web, `calls.ts`):
  - rispondi: `{status: "active", answeredAt: serverTimestamp()}`
  - rifiuta: `{status: "declined", endedAt: serverTimestamp(), endedBy: me}`
  - annulla: `{status: "cancelled", endedAt: serverTimestamp(), endedBy: me}`
  - persa: `{status: "missed", endedAt: serverTimestamp()}`
  - chiudi: `{status: "ended", endedAt: serverTimestamp(), endedBy: me}`

## 6. Cloud Functions (`europe-west1`, online)

Gli errori sono `HttpsError` con `details.reason` (stringa): i client scelgono il messaggio da `details.reason`, non dal codice.
Android: `FirebaseFunctionsException.details` è una mappa con la chiave `reason`.

### `startCall` (callable)
Input `{chatId}`. Restituisce `{callId, ringSeconds}`.
1. Utente autenticato e partecipante della chat (2 persone) → altrimenti `not-participant`.
2. Nessuno dei due ha bloccato l'altro → altrimenti `blocked`.
3. Chi riceve non ha `acceptCalls == false` → altrimenti `not-accepting`.
4. Chi riceve ha almeno un'app in `callClients` → altrimenti `not-supported`.
5. Messaggi: due `count()` su `chats/{chatId}/messages` con `sender == uid`; se uno dei due è sotto `call_unlock_messages` → `locked`,
   con `details.mine`, `details.theirs`, `details.needed` (per il messaggio).
6. Minuti (solo controllo, si contano in `getCallToken`): se manca un blocco libero nel mese → `monthly-limit`; nel giorno di chi chiama → `daily-limit`;
   nel giorno di chi riceve → `other-daily-limit`.
7. Occupato: se chi riceve ha una chiamata ancora viva (§4) → `busy`; se ce l'ha chi chiama → `self-busy`. Le chiamate non più vive si cancellano.
8. Crea `calls/{callId}` con `status = "ringing"`, `callerName`, `callerPhoto`, `expireAt`.

### `getCallToken` (callable)
Input `{callId}`. Restituisce `{token, appId, channel, account, expiresAt, encryption}` (`expiresAt` in millisecondi;
`encryption` = `{mode: "aes-128-gcm2", key, salt}`, vedi §8 "Cifratura").
1. La chiamata esiste (altrimenti `ended`), l'utente è in `participants` (altrimenti `not-participant`), `status == "active"` (altrimenti `not-active`).
2. Nessuno dei due ha bloccato l'altro → altrimenti `blocked` (ricontrollato a ogni rinnovo: un blocco a metà chiamata la chiude alla scadenza).
3. In una transazione su `callUsage`: se `mese + blocco > tetto mensile` → `monthly-limit`; se `giorno(uid) + blocco > tetto giornaliero` →
   `daily-limit`; altrimenti aggiunge il blocco ai due contatori (mese e giorno di chi chiede).
4. Token Agora (`agora-token`, `RtcTokenBuilder.buildTokenWithUserAccount`): canale = `callId`, **account = uid Firebase** (stringa),
   ruolo publisher, scadenza del token e dei permessi = **un blocco** (10 minuti).
5. Scrive `activeUntil` = scadenza del permesso.
6. Cifratura (dal 2026-10-22): `key` e `salt` sono 32 byte ciascuno in base64, ricavati con HMAC-SHA256 dal certificato e dal `callId`.
   Sono uguali per le due persone e per ogni rinnovo della stessa chiamata, diversi da una chiamata all'altra, e non stanno in Firestore.

Rinnovo (come fa il web): il client chiede un nuovo token **60 secondi prima di `expiresAt`** e lo passa a `renewToken`.
- Se la funzione rifiuta con un `reason` (minuti finiti, blocco, chiamata non più attiva): la chiamata continua fino alla scadenza con l'avviso
  "La chiamata finirà tra un minuto: minuti finiti."; alla scadenza Agora butta fuori (web: evento `token-privilege-did-expire`;
  Android: `onRequestToken` / `CONNECTION_CHANGED_TOKEN_EXPIRED`), il client scrive `ended` e mostra "Chiamata terminata: minuti finiti".
- Se fallisce per la rete: riprova ogni ~45 s finché mancano più di 20 s alla scadenza.
Così i minuti contati sono quelli veri arrotondati per eccesso a blocchi di 10, e un client modificato non può restare in chiamata oltre
quello che ha "pagato".

Limiti da Remote Config (in cache 10 minuti nel server; se un parametro non c'è valgono i predefiniti del codice; oggi nessuno è in Remote Config):
- `call_unlock_messages` = **5** (letto anche dai client, per il colore del pulsante);
- `call_monthly_minutes_cap` = **9000** (margine sotto i 10.000 gratuiti; il codice non accetta mai più di **9500**, anche se Remote Config dicesse di più);
- `call_daily_user_minutes` = **30** (3 blocchi da 10 minuti; così una persona sola non consuma il mese di tutti);
- `call_token_block_minutes` = **10**;
- `call_ring_seconds` = **10** (durata dello squillo, letta anche dai client).

### `sendCallNotification` (`onDocumentWritten` su `calls/{callId}`)
Lingua: italiano se `spokenLanguages` di chi riceve contiene `it`, altrimenti inglese. Niente notifiche se uno dei due ha bloccato l'altro.
Le notifiche vanno solo alle app in `callClients` di chi riceve (§4).

| Passaggio | `data.type` | Testo (`body`) | Link web |
|---|---|---|---|
| creazione `ringing` | `call` | "📞 ti sta chiamando" / "📞 is calling you" | `/call/<callId>` |
| `ringing` → `cancelled` / `missed` | `call_missed` | "📞 Chiamata persa" / "📞 Missed call" | `/chat/<chatId>` |
| `ringing` → `active` / `declined` | `call_answered` | "" | `/chat/<chatId>` |

- **Android** (se `callClients` contiene `"android"`): messaggio FCM con **solo `data`** (niente `notification`, così lo gestisce sempre
  `MyFirebaseMessagingService`, anche ad app chiusa), priorità alta, al `fcmToken`. Campi di `data` (tutti stringhe):
  `type`, `callId`, `chatId`, `callerId`, `title` (nome di chi chiama), `body`, e solo per `call` anche `ringUntil`
  (millisecondi: oltre quest'ora lo squillo è vecchio e va mostrato come chiamata persa). TTL: squillo + 10 s per `call`, 60 s per `call_answered`.
- **Web** (se `callClients` contiene `"web"`): push ai `webPushTokens`, stessi campi più il link. Il service worker usa il tag
  `call-<callId>`: la chiamata persa e la risposta altrove sostituiscono la notifica dello squillo.
- Android dovrebbe fare lo stesso: notifica con un id/tag legato a `callId`; `call_missed` la sostituisce, `call_answered` la chiude.
- **Stato finale** (`declined`, `cancelled`, `missed`, `ended`): **3 secondi** dopo (per far vedere ai client come è finita) cancella il documento.
  I client trattano la sparizione del documento come fine della chiamata.

### `cleanupDeletedUser` (aggiornata)
Cancella anche i documenti `calls` con l'uid in `participants`.

## 7. Schermate (uguali su Android e web)
Icone Material (filled): `call`, `call_end`, `mic`, `mic_off`, `videocam`, `videocam_off`.

1. **Chat**: icona `call` nella barra in alto, a destra dei chip delle metriche.
   - **Bloccata = grigia** (colore delle icone normali). Al tocco una finestra "Chiamate" spiega perché, senza chiamare:
     - meno di 5 messaggi a testa: "Le chiamate si sbloccano quando avete scritto almeno 5 messaggi ciascuno. A te ne mancano 2, a <nome> ne mancano 3."
       (con 1: "ne manca 1"; se manca solo una persona, solo quella parte);
     - l'altra persona non accetta chiamate: "<nome> non accetta chiamate.";
     - l'altra persona non ha app che ricevono chiamate: "<nome> non può ancora ricevere chiamate: serve la versione aggiornata dell'app.";
     - sono già in una chiamata: "Sei già in una chiamata.";
     - blocco (in un verso o nell'altro): l'icona non c'è.
   - **Sbloccata = verde** (`#2E7D32` nel tema chiaro e dislessia, `#66BB6A` nello scuro), e al tocco chiama.
   - Il passaggio da grigia a verde si vede subito: il client conta i messaggi di ognuno con due `count()` all'apertura della chat
     (1 lettura ciascuno), poi aggiunge i propri mandati e riconta quelli dell'altra persona quando ne arriva uno. È solo l'aspetto:
     il controllo vero lo rifà `startCall`, e i suoi rifiuti usano gli stessi testi (più quelli dei limiti, sotto).
2. **Chiamata in uscita** (`/call/<id>` sul web, a tutto schermo): foto e nome, "Chiamata in corso…", tono di attesa (425 Hz, 1 s ogni 5 s),
   microfono e fotocamera disattivati finché non risponde, pulsante rosso `call_end` (= annulla). Dopo 10 s senza risposta → `missed`, "Nessuna risposta".
3. **Chiamata in arrivo**:
   - app aperta: finestra sopra qualsiasi schermata con foto, nome, "ti sta chiamando", due pulsanti tondi (rosso `call_end` = Rifiuta,
     verde `call` = Rispondi), suoneria (880/660 Hz ogni 2 s) e vibrazione. Sparisce da sola se chi chiama annulla o allo scadere;
   - app chiusa: la notifica. Android: notifica ad alta priorità con le azioni "Rispondi" / "Rifiuta". Web: la notifica apre `/call/<id>`.
4. **In chiamata**: foto e nome, durata da `answeredAt` (`m:ss`, `h:mm:ss` oltre l'ora), pulsanti microfono, fotocamera, chiudi.
   - Il microfono si chiede quando si chiama o si risponde; la fotocamera **solo** al primo tocco su `videocam`.
   - Se l'altra persona accende la fotocamera: il suo video a tutto schermo (nome e durata in bianco sopra, pulsanti su fondo scuro),
     il proprio in piccolo in un angolo, a specchio.
   - Uscendo dalla schermata (indietro) la chiamata continua: in alto una barra verde "In chiamata con <nome>: tocca per tornare".
5. **Fine**: per 2,5 secondi "Chiamata terminata · 12:34" (o "Chiamata rifiutata", "Nessuna risposta", "Chiamata annullata", "Chiamata persa",
   "Chiamata terminata: minuti finiti"), poi si torna alla chat.
6. **Impostazioni**: sezione "chiamate" con l'interruttore "Accetto chiamate" (`acceptCalls`) e la spiegazione
   "Possono chiamarti solo le persone con cui avete scritto almeno 5 messaggi ciascuno. Audio e video passano da Agora e non vengono registrati."

Messaggi dei limiti e degli errori (`details.reason`):
- `busy`: "<nome> è già in un'altra chiamata. Riprova più tardi."
- `self-busy`: "Sei già in una chiamata."
- `monthly-limit`: "Per questo mese le chiamate gratuite sono finite: riprendono il 1° del mese."
- `daily-limit`: "Oggi hai usato tutti i minuti di chiamata: riprendono domani."
- `other-daily-limit`: "<nome> oggi ha già usato tutti i minuti di chiamata: riprova domani."
- `blocked`: "Non puoi chiamare questa persona."
- microfono negato: "Per chiamare serve il microfono. Dai il permesso dalle impostazioni del browser e riprova." (Android: "…dalle impostazioni del telefono…")
- fotocamera negata: "Per accendere la fotocamera serve il permesso. …"
- altro: "Non è stato possibile avviare la chiamata. Controlla la connessione e riprova."
- rinnovo rifiutato: avviso "La chiamata finirà tra un minuto: minuti finiti."
Testi inglesi: `GenteStranaWeb/app/src/i18n.ts`, blocco `calls`.

## 8. Note per piattaforma

### Android (da fare)
- SDK Agora **completo** (serve anche il video): `io.agora.rtc:full-sdk` 4.x. Aumenta la dimensione dell'app (decine di MB): valutare
  le librerie native solo per le architetture necessarie / App Bundle.
- Account Agora = uid Firebase (`joinChannelWithUserAccount(token, channel, account)` con i valori di `getCallToken`), **non** uid numerici:
  il web usa gli account stringa e i due tipi nello stesso canale non si vedono bene.
- Rinnovo: timer a `expiresAt - 60 s` → `getCallToken` → `renewToken`, come il web (§6).
- Permessi: `RECORD_AUDIO` (alla chiamata), `CAMERA` (solo al tocco sul video), `POST_NOTIFICATIONS` (già usato).
- Chiamata che continua in background: foreground service di tipo `microphone` (e `camera` col video accesa), con
  `FOREGROUND_SERVICE_MICROPHONE` / `FOREGROUND_SERVICE_CAMERA` da Android 14.
- `MyFirebaseMessagingService`: gestire `type` = `call`, `call_missed`, `call_answered` (§6); scrivere `"android"` in `callClients` solo
  quando tutto questo c'è.
- Squillo in arrivo con l'app aperta: ascolto `calls` con `participants array-contains <me>`, filtrando `calleeId == me`,
  `status == "ringing"` e `createdAt` recente (§4). Nome e foto da `callerName` / `callerPhoto`.

### Cifratura di audio e video (obbligatoria, dal 2026-10-22)
Cifratura integrata di Agora, **AES-128-GCM2**, impostata **prima** di entrare nel canale con i valori di `getCallToken.encryption`.
Se una delle due app non la attiva, o usa valori diversi, l'altra non sente e non vede niente.
- Web (fatto): `client.setEncryptionConfig("aes-128-gcm2", key, salt)`, dove `key` è la stringa base64 così com'è (44 caratteri;
  l'SDK web non accetta chiavi di più di 62 caratteri) e `salt` è il base64 decodificato in 32 byte. Evento `crypt-error` → avviso in console.
- Android (da fare), prima di `joinChannelWithUserAccount`:
  ```kotlin
  val enc = EncryptionConfig().apply {
      encryptionMode = EncryptionConfig.EncryptionMode.AES_128_GCM2
      encryptionKey = encryption.key                       // la stringa base64, non decodificata
      val salt = Base64.decode(encryption.salt, Base64.NO_WRAP)  // 32 byte
      System.arraycopy(salt, 0, encryptionKdfSalt, 0, 32)
  }
  rtcEngine.enableEncryption(true, enc)
  ```
  Callback `onEncryptionError` → log. Prova: chiamata Android ↔ web con il video acceso (il video si vede solo se le chiavi coincidono).

### Web (fatto)
- SDK `agora-rtc-sdk-ng` 4.24.8, in un file a parte caricato **solo** alla prima chiamata (non nel precache: 1,5 MB).
- iPhone: se si esce dall'app durante la chiamata, Safari ferma il microfono. Avviso nella schermata di chiamata:
  "Tieni l'app aperta durante la chiamata: se esci, il microfono si ferma."
- Niente schermata di chiamata di sistema: sull'iPhone una chiamata in arrivo è una notifica normale (con la PWA installata e le push attive).
- Con l'app aperta e visibile il service worker non mostra la notifica dello squillo (squilla l'app); Safari la mostra e la chiude subito,
  perché revoca il permesso se un push non mostra niente.

## 9. Prove fatte (2026-10-21)
- Regole: `projects.test` su 27 casi (`scratchpad/rules-test-calls.mjs`).
- Web, due account di prova in due contesti Chromium con microfono e fotocamera finti, Cloud Functions e Agora veri
  (`scratchpad/e2e-calls.mjs`, 35 verifiche, passata in locale e sul sito pubblicato, anche con schermo da iPhone): pulsante grigio → verde,
  spiegazione, chiamata creata dal client rifiutata, `callUsage` non leggibile, squillo con nome, notifica dello squillo inviata,
  occupato, risposta, audio e video Agora (B vede il video di A), microfono, barra "in chiamata", chiusura, rifiuto, chiamata persa dopo 10 s,
  cancellazione del documento, "Accetto chiamate" spento (`not-accepting`).
- Rinnovo e limiti con Remote Config di prova (blocco 2 minuti, 4 al giorno, poi tolti): rinnovo a 1 minuto, rinnovo rifiutato con l'avviso
  su tutti e due, fine alla scadenza "minuti finiti", nuova chiamata rifiutata con `daily-limit`.
- Da fare: chiamata Android ↔ web con telefoni veri; PWA su iPhone vero (microfono, chiamata con l'app installata, notifica dello squillo).
