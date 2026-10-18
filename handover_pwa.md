# Handover dalla PWA: cose che riguardano l'app Android

La PWA per iPhone, iPad e Mac si sviluppa in `D:\Users\Gabriele\GenteStranaWeb` e usa lo stesso progetto Firebase (`gente-strana`). Lavorandoci emergono problemi e modifiche del backend che toccano anche questa app. Qui sono raccolti in ordine di data, con cosa fare lato Android.

Il documento cresce man mano: ogni voce nuova va aggiunta in fondo, con la data.

Dove sta il backend:
- regole Firestore e Storage, configurazione delle estensioni, Cloud Functions: `D:\Users\Gabriele\GenteStranaFirebase` (senza repo git);
- copia documentata delle regole Firestore: [firestore_rules.md](firestore_rules.md).

---

## 1. Miniature delle foto profilo (2026-10-17)

### Il problema
L'estensione `storage-resize-images` non aveva mai creato una miniatura: su 25 foto originali, 0 miniature.

Nella configurazione i percorsi erano scritti con un segnaposto letterale:

```
INCLUDE_PATH_LIST=/users/{UID}/gallery
EXCLUDE_PATH_LIST=/users/{UID}/gallery_resized
RESIZED_IMAGES_PATH=/users/{UID}/gallery_resized
```

L'estensione non interpreta `{UID}`: lo confronta come testo. Ogni foto veniva scartata con questo log:
`Image path '/users/<uid>/gallery' is not supported, these are the supported absolute paths: /users/{UID}/gallery`.

### La correzione (già fatta)
In `GenteStranaFirebase/extensions/storage-resize-images.env`:

```
INCLUDE_PATH_LIST=/users/*/gallery
EXCLUDE_PATH_LIST=/users/*/gallery_resized
RESIZED_IMAGES_PATH=../gallery_resized
DO_BACKFILL=true
```

- `*` vale per qualsiasi uid.
- `RESIZED_IMAGES_PATH` è relativo alla cartella della foto, quindi la miniatura finisce in `users/{uid}/gallery_resized/`.
- `DO_BACKFILL=true` ha creato le miniature delle 25 foto già presenti. Il recupero parte solo quando si installa o si riconfigura l'estensione, non a ogni caricamento.

Per pubblicare la configurazione, da `GenteStranaFirebase`:
`npx -y -p node@22 -c "firebase deploy --only extensions --project gente-strana"`.
La configurazione è stata esportata con `firebase ext:export`, che ha aggiunto la sezione `extensions` a `firebase.json`.

Verificato il 2026-10-17:
- 25 miniature su 25;
- una foto caricata dopo la correzione riceve la miniatura in pochi secondi;
- il nome è `<nome>_200x200.webp`;
- la miniatura ha lo **stesso token di download** dell'originale (`REGENERATE_TOKEN=false`).

### Cosa fare lato Android
**`getThumbnailUrl()` in `utils/ImageUtils.kt` costruisce URL che non funzionano.** Al passo 6 rimette le `/` non codificate nel percorso dell'oggetto (`.replace("%2F", "/")`). Firebase Storage vuole il percorso codificato per intero:

| URL della miniatura | Risposta |
|---|---|
| `…/o/users%2F<uid>%2Fgallery_resized%2F<hash>_200x200.webp?alt=media&token=…` | 200 |
| `…/o/users/<uid>/gallery_resized/<hash>_200x200.webp?alt=media&token=…` (come oggi in `getThumbnailUrl`) | 400 |

Oggi non fa danni: `getThumbnailUrl` è importata in `components/ChatListItem.kt` ma non viene mai chiamata, e l'app mostra sempre le foto originali. Se si vuole usarla, ad esempio negli elenchi utenti e chat per risparmiare traffico:
1. costruire l'URL codificando il percorso completo dell'oggetto, per esempio con `Uri.encode("users/$uid/gallery_resized/${hash}_200x200.webp")`, e tenere il `token` dell'originale;
2. ripiegare sull'originale se la miniatura risponde 404: subito dopo un caricamento la miniatura non esiste ancora per qualche secondo.

Altre due cose, a bassa priorità:
- `uploadMainProfileImage()`, la foto scelta in registrazione, carica il file originale a piena risoluzione con nome `.webp` anche se è un JPEG. Comprimerlo come fa `uploadNewProfileImage()` (WebP q80), magari riducendo anche le dimensioni, risparmierebbe spazio e traffico.
- La PWA riduce le foto a 1600 px sul lato lungo prima di caricarle, e usa lo stesso schema di nomi (`<hash 32 caratteri>.webp`), quindi `getThumbnailUrl` funziona anche per le foto caricate dal web.

---

## 2. Regole di Storage: solo immagini, massimo 20 MB (2026-10-17)

### Prima
Su `users/{uid}/gallery/**` il proprietario poteva scrivere qualsiasi file di qualsiasi dimensione. Con la PWA basta la console del browser per riempire la propria cartella, e lo spazio oltre la quota gratuita si paga.

### Adesso
Sorgente: `GenteStranaFirebase/storage.rules`, pubblicata con `firebase deploy --only storage --project gente-strana`.

```
match /users/{userId}/gallery/{allPaths=**} {
  allow read: if request.auth != null;
  allow create, update: if request.auth != null && request.auth.uid == userId
                        && request.resource.size < 20 * 1024 * 1024
                        && request.resource.contentType.matches('image/.*');
  allow delete: if request.auth != null && request.auth.uid == userId;
}
match /users/{userId}/gallery_resized/{allPaths=**} {
  allow read: if request.auth != null;
  allow write: if false;   // le scrive solo l'estensione, con privilegi di admin
}
```

Le regole sono state provate prima del deploy con l'API Firebase Rules (`projects.test`) su 12 casi:
- passano: WebP piccolo, JPEG da 15 MB, sovrascrittura, eliminazione e lettura di una foto altrui;
- sono bloccati: 25 MB, PDF, `application/octet-stream`, la cartella di un altro, l'eliminazione altrui, la scrittura nelle miniature e la lettura senza login.

### Cosa verificare lato Android
Un caricamento ora passa solo se il file ha un **tipo immagine** e pesa **meno di 20 MB**. L'SDK di Storage ricava il tipo dal `ContentResolver` quando non gli si passano metadati, quindi le foto della galleria (`image/jpeg`) e i WebP temporanei del `FileProvider` (`image/webp`) dovrebbero passare. **Da provare sul telefono:**
1. registrazione con foto;
2. "aggiungi foto" dal profilo.

Se uno dei due fallisce, la causa più probabile è un `contentType` mancante: va passato esplicitamente con `storageMetadata { contentType = "image/webp" }` in `putFile`.

La sovrascrittura di un file esistente resta permessa, con gli stessi limiti, come prima: lo stesso hash finisce sullo stesso nome.

---

## 3. Modifiche precedenti al backend che toccano Android (riassunto, 2026-10-16/17)

- **Regole Firestore, `isAdmin`**: un utente non può più cambiare il proprio `isAdmin` dal client. Alla creazione deve essere `false` o mancare. Dettagli in [firestore_rules.md](firestore_rules.md). Android scrive solo `isAdmin: false` alla creazione, quindi non è toccato.
- **Link delle email di Firebase Auth** (verifica e reset password): la Cloud Function `emailVerificationRedirect` (us-central1) ora reindirizza a `https://gente-strana.web.app/auth/action` con tutti i parametri, invece che a `gentestrana://verifyemail?oobCode=…`. Prima toglieva il parametro `mode`, e `MainActivity.handleIntent` trattava ogni link come reset password: la verifica email da Android non poteva funzionare. Ora la pagina web verifica o reimposta la password su qualsiasi dispositivo e, su Android, offre "Apri l'app" (`intent://verifyemail#Intent;scheme=gentestrana;package=com.gentestrana;…`). `VerifyEmailScreen`, che controlla ogni 2,4 secondi, se ne accorge da sola.
- **Remote Config `registration_email_validation_regex`**: dalla versione 5 ammette anche `hotmail.com`. Il valore di Remote Config è più stretto della riserva `DEFAULT_EMAIL_REGEX` in `RegistrationScreen.kt`: libero.it e altri domini restano esclusi finché non li si aggiunge lì.
- **`rawBirthTimestamp` ha due formati**: la registrazione lo salva come intero in millisecondi (`Long`), mentre `ProfileViewModel.updateProfile()` e `setBirthTimestamp()` lo salvano come `Timestamp` Firestore. `User.birthTimestamp` li gestisce entrambi, e anche la PWA, che scrive sempre l'intero. Conviene uniformare quando capita.
- **SendGrid**: `sendDiagnosisReminderEmail` fallisce con "Maximum credits exceeded". SendGrid non è mai stato attivo: la funzione si può eliminare.
- **Ambiente delle Cloud Functions**: in locale usare `npm ci`, perché `node_modules` era disallineato dal lock. Pubblicare con Node 22 (`npx -y -p node@22 -c "firebase deploy --only functions:<nome> --project gente-strana"`), perché con Node 25 l'analisi pre-deploy fallisce. `src/index.ts` ora ha gli a capo in formato LF, come chiede eslint.

---

## 4. Profilo personale dal web: differenze da conoscere (2026-10-17)

La PWA ora modifica gli stessi campi di `PersonalProfileScreen`: nome, bio, argomenti, data di nascita, lingue parlate e autopilot. La posizione GPS arriverà con la Fase 3. Cosa scrive su `users/{uid}`, e dove si comporta diversamente da Android:

- **Nome (`username`): DECISO il 2026-10-17, i nomi doppi vanno permessi** ("Anna Maria"). Regola valida per entrambe le app: togliere gli spazi **solo all'inizio e alla fine**, ridurre gli spazi doppi interni a uno solo, lunghezza da 2 a 13 caratteri contata sul nome già ripulito.
  - **PWA**: già fatto, sia in registrazione sia nel profilo (`normalizeUsername` in `app/src/users.ts`).
  - **Android, da correggere**: `ProfileTextField(removeSpaces = true)` chiama `removeSpaces()` (`trim()` + spazi doppi ridotti) **a ogni tasto**. Così lo spazio appena digitato sparisce e "Anna Maria" diventa "AnnaMaria". Correzione: in `PersonalProfileTextFields.kt` non applicare il `trim()` durante la digitazione (al massimo ridurre gli spazi doppi), e fare `removeSpaces(username)` solo nel salvataggio (`ProfileViewModel.updateProfile`). Il `maxLength = 13` va controllato sul nome ripulito, non sul testo grezzo, altrimenti gli spazi iniziali fanno perdere lettere.
  - **Registrazione Android**: `RegistrationScreen` fa solo `username.trim()` e non ha limiti di lunghezza, quindi chi si registra con un nome di 20 caratteri poi non riesce a salvare il profilo. Allinearla: stessa pulizia e stesso limite 2-13.
- **`rawBirthTimestamp`**: la PWA lo salva sempre come **intero in millisecondi**, a mezzogiorno locale del giorno scelto. Android lo salva come intero in registrazione e come `Timestamp` da `ProfileViewModel`. Vedi la voce 3: `User.birthTimestamp` li legge entrambi.
- **`spokenLanguages`**: lista di codici (`["it","en"]`), con gli stessi 40 codici di `R.array.supported_language_codes`. La PWA salva `[]` quando non ci sono lingue. `ProfileViewModel.setSpokenLanguages("")` salva invece `[""]`, cioè una lingua vuota: la PWA la ignora in lettura, ma conviene filtrare le stringhe vuote anche su Android.
- **`topics`**: la PWA salva la lista senza voci vuote. `ProfileViewModel.updateProfile()` fa `split(",").map { trim }` senza filtrare, quindi una virgola finale produce un argomento vuoto.
- **`autopilotEnabled`**: booleano, come su Android.
- **`profilePicUrl`**: il salvataggio del profilo dal web non lo tocca. Le foto si aggiornano subito, una per volta, con una transazione come in `uploadNewProfileImage()`.

---

## 5. Testi condivisi, termini, linee guida e onboarding (2026-10-17)

- **I testi lunghi della PWA vengono da qui.** Termini di servizio (`tos_*`), linee guida (`guidelines_*`, `community_guidelines`) e onboarding (`onboarding_*` più `next`, `verify_score_button`, `adhd_score_error`) sono importati da `app/src/main/res/values*/strings.xml` con `npm run strings:import`, lanciato nella cartella `app/` di GenteStranaWeb. **Dopo ogni modifica di questi testi su Android va rilanciato l'import e ripubblicata la PWA**, altrimenti il web resta alla versione vecchia. Il markup supportato è lo stesso di Android: `<b>`, `<a href>`, URL ed email nudi, `\n`.
- **Refuso nell'email di contatto dei termini**: `tos_contact_text`, in italiano e in inglese, dice `info.gestestrana@gmail.com`, mentre altrove, compreso il mittente della Cloud Function, l'indirizzo è `info.gentestrana@gmail.com`. In tutto `gestestrana` compare 4 volte nelle risorse: cercarlo e correggerlo in `strings.xml`, poi rilanciare l'import sul web. Finché non si corregge, il link email nei termini della PWA porta all'indirizzo sbagliato.
- **I termini citano solo l'app**: "applicazione mobile", "accessibili dalla schermata Impostazioni dell'App". Ora valgono anche per la PWA, dove le linee guida sono a `https://gente-strana.web.app/guidelines` e i termini a `/terms`. Quando si aggiorna il testo conviene renderlo generico (app e sito) e aggiornare `tos_last_updated`.
- **Onboarding sul web**: stesse pagine e stessa logica di `OnboardingAssessmentPage` (consenso; diagnosi oppure autismo ≥ 64 o ADHD ≥ 4, con ADHD da 1 a 6; un punteggio insufficiente blocca la verifica sul dispositivo). Come su Android non si salva nulla su Firestore: tutto resta in `localStorage` (`gs.onboardingCompleted`, `gs.onboardingScoreError`). Sul web l'onboarding compare solo a chi non è collegato, mentre su Android compare al primo avvio anche agli utenti già collegati. I link ai test vengono dagli stessi parametri di Remote Config (`onboarding_autism_test_url`, `onboarding_adhd_test_url`). Il video (`res/raw/onboarding_video1.mp4`, 3,9 MB, 1080x1920) e l'immagine (`onboarding_screen1.png`) sono copiati in `app/public/onboarding/`: se cambiano su Android vanno ricopiati.

---

## 6. Community e chat: cose notate leggendo il codice Android (2026-10-17)

La PWA ora ha la community: elenco utenti come `UserListViewModel` (pagine da 10 per `lastActive`, senza sé stessi né i bloccati, ricerca da 2 caratteri su nome e argomenti, filtro per lingua) e il profilo di un utente in sola lettura. Le chat arrivano nei prossimi passi. Leggendo il codice sono emersi questi punti:

- **Età per i nati prima del 1970**: `computeAgeFromTimestamp()` in `utils/UserUtils.kt` restituisce 0 se il timestamp è ≤ 0, quindi a chi è nato prima del 1970 non viene mostrata l'età. Eppure `ProfileValidation` considera valide le date negative. La PWA calcola l'età anche per loro.
- **Eliminare l'ultimo messaggio non elimina la chat**: `ChatRepository.deleteMessage()`, quando non restano messaggi, prova a cancellare il documento della chat, ma le regole Firestore hanno `allow delete: if false` su `chats`. La cancellazione fallisce e l'eccezione esce da `deleteMessage` anche se il messaggio era stato cancellato. Conviene togliere quel passaggio, oppure impostare `hasMessages = false`.
- **Una chat "eliminata per me" non ricompare più**: `deleteChatForMe()` aggiunge l'uid a `deletedFor`, e `processChatDocument()` la nasconde per sempre, anche se l'altra persona scrive di nuovo. I messaggi nuovi finiscono in una chat invisibile. Se non è voluto, alla ricezione di un messaggio nuovo andrebbe tolto l'uid da `deletedFor`.
- **I limiti sui messaggi sono solo sul telefono**: `MessageRateLimiter` e `MessageDailyLimitManager` stanno in memoria e in `SharedPreferences`. Dal web chiunque li aggira con la console del browser. Prima di aprire le chat dal web servono limiti lato server (regole Firestore o Cloud Function): è già nel piano della PWA, nella sezione "Sicurezza".
- **Foto di riserva da un sito esterno**: `DEFAULT_PROFILE_IMAGE_URL` punta a `icons.veryicon.com`. Se quel sito cambia o è lento, le schede senza foto restano vuote, e ogni visualizzazione fa una richiesta a un terzo. La PWA usa un'immagine sua (`/avatar.svg`).

---

## 7. Chat dal web e limiti sui messaggi lato server (2026-10-17)

La PWA ora ha elenco chat e chat, compatibili con quelle Android: stessi documenti e stessi campi di `ChatRepository`. Un utente web e uno Android si scrivono normalmente. Cambi nel backend che toccano anche Android:

- **`sendChatNotification` applica i limiti sui messaggi per tutti.** Usa gli stessi parametri di Remote Config di Android (`daily_message_limit` 300, `message_rate_limit_max_messages` 4 ogni `message_rate_limit_window_seconds` 50). Un messaggio oltre il limite viene **cancellato** e non genera notifica. I conteggi stanno in `rateLimits/{uid}`, aggiornati in transazione e basati sull'ora di creazione del documento, non sul campo `timestamp` del client. Le regole Firestore non hanno una `match` per `rateLimits`, quindi i client non possono né leggerlo né scriverlo (provato: 403). Chi usa l'app Android normalmente non se ne accorge, perché l'app si ferma prima con gli stessi valori. Se un messaggio "sparisce", la causa è questa: nei log della funzione compare `limite rate/daily superato`.
- **Orario dei messaggi**: se il `timestamp` scritto dal client si discosta di più di 5 minuti dall'ora di creazione, la funzione lo sostituisce con quella vera. Così non si può falsare l'ordine dei messaggi.
- **Niente notifica da chi è bloccato**: se il destinatario ha il mittente in `blockedUsers`, la notifica non parte. Prima partiva.
- **Riaprire una chat eliminata dal web**: "chatta con" sulla PWA riusa la chat esistente e, se l'avevo eliminata per me, mi toglie da `deletedFor`, così torna visibile a me. Su Android `createNewChat()` restituisce la chat ma la lascia nascosta (vedi voce 6).
- **Eliminare l'ultimo messaggio dal web** imposta `hasMessages = false`, invece di provare a cancellare la chat come fa Android, che fallisce per le regole.
- **Messaggi su una riga**: come `sanitizeInput(removeSpaces())` su Android, la PWA toglie i tag HTML e riduce spazi e a capo a uno spazio. In più limita i messaggi a 2000 caratteri, mentre Android non ha limiti.
- **Lo stato "online" non funziona**: vedi la voce 8, dove c'è la soluzione adottata (correzione del 2026-10-18: `ChatListItem` legge `Chat.isOnline`, un campo del documento della chat che nessuno scrive; `OnlineStatusRepository` non è usato da nessuna parte).

---

## 8. Impostazioni, blocco, segnalazione, eliminazione account e stato online (2026-10-18)

La PWA ora ha la quarta scheda **Impostazioni** come `SettingsScreen`: cambio password, utenti bloccati, eliminazione account, tema (sistema / chiaro / scuro / dislessia con OpenDyslexic), privacy, termini, linee guida e versione. Ha anche segnala e blocca/sblocca nel profilo utente, come `UserProfileScreen`. Documenti e campi sono quelli di Android: `users/{uid}.blockedUsers` con arrayUnion/arrayRemove, e `reports` con `reportedUserId`, `reporterUserId`, `reason` (nome dell'enum `ReportReason`), `timestamp` e `additionalComments`. Cose che toccano Android:

### Eliminazione account: su Android oggi si rompe a metà
`FirestoreDeletionUtils.deleteUserDataFromFirestore()` cancella foto e documento utente, poi prova a cancellare le chat. Le regole hanno `allow delete: if false` su `chats`, quindi la cancellazione fallisce con un'eccezione, e `deleteUserAccount()` **non arriva mai a `user.delete()`**. Risultato: profilo e foto spariti, account Auth ancora vivo. Al login successivo `createMissingUserDocument()` ricrea un profilo vuoto.

**Soluzione nel backend (nuova Cloud Function `cleanupDeletedUser`)**: trigger "utente Auth eliminato" (prima generazione, `europe-west1`). Quando un account sparisce, da qualunque parte (PWA, Android o console), la funzione cancella:
- le foto in Storage `users/{uid}/`, miniature comprese;
- le chat con i loro messaggi;
- i ricordi scritti o ricevuti;
- le notifiche programmate e `rateLimits/{uid}`;
- il documento utente con le sue sottoraccolte, togliendo 1 a `stats/community.totalUsers`.

In più toglie l'uid dai `blockedUsers` degli altri. Le segnalazioni restano, perché servono alla moderazione.

**Cosa fare su Android**: semplificare `deleteUserAccount()` a una nuova autenticazione, se serve, e poi `user.delete()`. Va tolta la chiamata a `FirestoreDeletionUtils`, che è proprio il passaggio che fallisce. La PWA chiede la password nella finestra di conferma per rifare l'autenticazione. Firebase rifiuta `delete()` se l'accesso non è recente (`requires-recent-login`): oggi Android in quel caso mostra solo un Toast di errore.

### Stato online: soluzione adottata
- **Online = `lastActive` più recente di 6 minuti e mezzo.** Il client che è aperto e in primo piano aggiorna `lastActive` ogni 5 minuti, e quando si torna sull'app (al massimo una volta al minuto). Niente Realtime Database e niente campo `isOnline`. Costa 12 scritture l'ora per persona attiva, dentro la quota gratuita (20.000 al giorno).
- La PWA fa già così: il pallino verde o grigio sulla foto nell'elenco chat è come `ChatListItem`, e ascolta il documento dell'altra persona solo per le righe visibili sullo schermo e con l'app in primo piano, per non pagare letture inutili; le altre righe usano il `lastActive` letto all'apertura dell'elenco.
- **Su Android, per allinearsi**:
  1. Chiamare `updateUserLastActive()`, oggi definita ma mai usata, in `onResume` e ogni 5 minuti mentre l'app è in primo piano.
  2. In `ChatListItem`/`ChatListViewModel` calcolare `isOnline = now - participant.lastActive < 6,5 min` dal documento dell'altra persona, invece di `Chat.isOnline`.
  3. Togliere `OnlineStatusRepository`: non è usato e, se lo fosse, ascolterebbe l'intera raccolta `users`, cioè letture a ogni modifica di chiunque.
- Effetto collaterale voluto: `lastActive` diventa preciso, quindi l'ordine della community e il badge "nottambulo" (lastActive tra mezzanotte e le 5) riflettono l'uso reale. Oggi Android aggiorna `lastActive` solo al login.

### Altre differenze
- **Sblocco**: il dialogo di Android ha testi fissi in italiano ("Sblocca Utente", "Sei sicuro di voler sbloccare…", "Sblocca", "Utente sbloccato."). Vanno messi in `strings.xml`; la PWA per ora usa i suoi.
- **Segnalazione**: Android chiude il dialogo senza dire niente all'utente (c'è solo un commento "Eventuale feedback"). La PWA mostra "Segnalazione inviata, grazie". Il limite di 3 segnalazioni al minuto è lo stesso di `ReportRateLimiter`, sempre solo lato client.
- **Cambio password**: la PWA lo mostra solo a chi ha un account email/password. Su Android la voce c'è anche per chi usa Google, e fallisce.
- **Privacy policy** nelle Impostazioni Android non ha un'azione al tocco: la PWA apre `https://appgentestrana.wordpress.com/trattamento-dati-personali/`, la pagina citata nei termini.
- **Notifiche push**: l'interruttore di Android salva solo una preferenza in `SharedPreferences` (`push_notifications_enabled`) che nessun altro punto del codice legge: spegnerlo non ferma le notifiche. Sul web le notifiche push arrivano con la Fase 3.
- **Chi è bloccato può ancora scrivere**: i suoi messaggi arrivano, senza notifica, e la chat resta nascosta a chi ha bloccato. Riappaiono se si sblocca. È così anche su Android; la PWA si comporta allo stesso modo.

## 9. Ricordi e icebreaker sul web (2026-10-18)

La PWA ora ha icebreaker e ricordi con gli stessi dati di Android: collezione `memories` con gli stessi campi, card `MEMORY_CARD|autore|testo` in chat, sblocco a 50 messaggi ricevuti. Differenze trovate e cose da sistemare su Android:

- **Notifica della card**: prima la notifica push mostrava il testo grezzo "MEMORY_CARD|Nome|testo". Ora `sendChatNotification` (già pubblicata) manda "💬 " + testo. Su Android non serve cambiare niente.
- **Anteprima nell'elenco chat**: Android mostra "MEMORY_CARD|…" come ultimo messaggio. La PWA mostra "💬 testo". Da fare in `ChatListItem` controllando il prefisso (anche il vecchio `RECOMMENDATION_CARD|`).
- **Testo con "|"**: `RecommendationSystemMessageBubble` usa `parts[2]`, quindi un ricordo che contiene "|" viene troncato nella card. La PWA unisce tutto quello che segue il secondo "|". Su Android: `parts.drop(2).joinToString("|")`.
- **Testi fissi in italiano**: le card ("Hai lasciato una memoria a…", "… ti ha lasciato una memoria"), il dialogo delle metriche e la sezione Memorie sono scritti nel codice, non in `strings.xml`. La PWA ha anche l'inglese.
- **Sblocco dal profilo**: `UserProfileScreen` chiama `chatRepository.createNewChat()` per contare i messaggi, quindi aprire un profilo può creare una chat vuota. La PWA cerca solo una chat esistente e non ne crea.
- **Conteggio nella community**: Android fa una query `memories` per ogni utente della pagina. La PWA ne fa una sola per pagina con `whereIn("targetUserId", uids)` (al massimo 30 valori). Conviene fare lo stesso: meno letture.
- **Cancellazione account**: i ricordi scritti e ricevuti li cancella `cleanupDeletedUser` (voce 8).

## 10. Foto (2026-10-18)
- **Registrazione**: la stringa `registration_select_profile_picture_button` è in inglese anche in `values-it` ("Select Profile Picture"). La PWA usa "Scegli la foto profilo".
- **Foto a schermo intero**: la PWA apre il carosello toccando la foto del profilo, come `ProfilePicturesScreen`, ma mostra la foto intera invece che ritagliata a quadrato. In `UserProfileScreen.kt` `GalleryOverlay`/`showGallery` non si apre mai (`onProfileImageClick` non è usato da `ProfileContent`): codice morto da togliere.

## 11. Filtri della community (2026-10-18)
La PWA ha ora gli stessi filtri di `FilterDialog` (tutti, lingua, posizione, somiglianza ≥ 0,3) con la stessa logica. Cose notate su Android:
- **Somiglianza e accenti**: `preprocessText` tiene solo `[a-z0-9s]`, quindi le lettere accentate spariscono ("città" → "citt", "perché" → "perch"). Due parole diverse possono diventare uguali, e le parole con lettere non latine (cirillico, greco…) vengono ignorate del tutto. La PWA fa lo stesso, per avere gli stessi risultati: se si corregge, conviene farlo su entrambe (es. `p{L}` invece di `a-z`, dopo aver tolto gli accenti con `Normalizer`).
- **`strings.xml`**: le righe di `apply` ed `enter_location` (in `values` e `values-it`) finiscono con `">` dopo `</string>`. È testo in più fuori dal tag, da togliere.
- **Paese della posizione**: si riconosce solo se la parte dopo l'ultima virgola è il nome del paese in inglese o in italiano. Una `location` scritta dal Geocoder in un'altra lingua del telefono (es. "Deutschland") non viene trovata dal filtro. Questo vale sia su Android sia sulla PWA.

## 12. Notifiche push del web (2026-10-18)
- Nuovo campo `users/{uid}.webPushTokens` (array di stringhe): i token FCM dei browser con la PWA, uno per browser. `fcmToken` resta del telefono Android e la PWA non lo tocca.
- **Da Android non va mai cancellato**: se un punto del codice riscrive il documento utente intero con `set()` senza `merge` (es. `createMissingUserDocument` o una migrazione), deve conservare `webPushTokens`. Gli `update()` sui singoli campi vanno bene.
- `sendChatNotification` e `sendWelcomeNotification` (già pubblicate) mandano la stessa notifica anche ai token web e tolgono da soli quelli scaduti. Per Android non cambia niente: stesso payload di prima.
- Corretto anche un errore di stamattina in `sendChatNotification` (notifiche "💬 " senza testo tra le 07:20 e le 08:16 UTC del 2026-10-18): secondo i log non è partita nessuna notifica in quell'intervallo.
- L'interruttore "notifiche push" di Android salva solo `push_notifications_enabled` in locale (voce 8): sul web invece spegne davvero le notifiche, togliendo il token. Su Android l'equivalente sarebbe `FirebaseMessaging.getInstance().deleteToken()` più `fcmToken = ""`.

## 13. Traduzione nella PWA (2026-10-18)
- La PWA non usa servizi a pagamento (niente Cloud Translation). Dove il browser ha le API `Translator` e `LanguageDetector` (Chrome/Edge desktop recenti) traduce sul dispositivo, come ML Kit. Altrove (Safari, iPhone, Firefox) "traduci" apre Google Traduttore in una nuova scheda con il testo già inserito. Lingua di arrivo = lingua del dispositivo, come `TranslationHelper`.
- Stessi punti di Android: bio e argomenti (icona Translate), "traduci" sul ricordo, menu del messaggio in chat.
- **Menu del messaggio**: su Android le voci del dialogo "Opzioni messaggio" ("Copia", "Traduci", "Originale", "Elimina") e il segno "🌐 Tradotto" sono scritti nel codice, non in `strings.xml`: in inglese restano in italiano. La PWA usa le stringhe `translate` / `translation_failed` di `strings.xml` più le sue in it/en. Conviene spostarle in `strings.xml`.

## 14. Località nella PWA (2026-10-18)
- La PWA scrive `users/{uid}.location` nello stesso formato di `LocationUtils` ("Città, Paese", oppure solo il paese), subito al tocco di "ottieni località", come `ProfileViewModel.setLocation`.
- Il nome del paese viene dal codice ISO, nella lingua dell'app web (italiano o inglese): per esempio "Regno Unito" / "United Kingdom". Android invece usa il Geocoder con `Locale.getDefault()`, quindi su un telefono in un'altra lingua (es. tedesco, "Deutschland") il filtro "posizione" non riconosce il paese (vedi voce 11). Conviene che anche Android scriva il paese in inglese o in italiano, ad esempio con `Locale("", address.countryCode).getDisplayCountry(Locale.ENGLISH)`.
- Il browser dà solo le coordinate: la PWA cerca la città più vicina in un elenco GeoNames che scarica al tocco. Nessun servizio esterno, le coordinate non escono dal dispositivo. Oltre 100 km dalla città più vicina salva solo il paese.
- Su Android il messaggio di errore è un Toast in italiano scritto nel codice ("Errore localizzazione: …", "Permesso di localizzazione negato"): da spostare in `strings.xml`.

## 15. Accesso solo per i tester sul web (2026-10-18)
- Nuova collezione Firestore `testers/{email}` (email minuscola, Gmail senza punti), con regole che permettono a ognuno di leggere solo la propria voce: vedi `firestore_rules.md`. Le regole delle altre collezioni non cambiano.
- La usa solo la PWA. **Android non cambia**: lì il controllo lo fa Google Play (test chiuso).
- Chi si iscrive al Google Form va aggiunto in due posti: lista email di Play Console (app Android) e un documento in `testers` dalla console Firebase (web).
- Il titolo del form non dice più "ANDROID ONLY" (cambiato dall'utente il 2026-10-18).

## 16. Profilo utente, onboarding e `strings.xml` (2026-10-18)
- ⚠️ **`strings.xml` non valido**: in `values/strings.xml` e `values-it/strings.xml` le righe di `apply` ed `enter_location` hanno barre in più. Nel commit attuale è `<string name=\"apply\">`, nelle modifiche non ancora salvate `<string\ name="apply">` e `insert\ country` / `inserisci\ paese`. Non è XML valido e il build di Android probabilmente si ferma. Vanno scritte così: `<string name="apply">apply</string>`, `<string name="enter_location">insert country</string>` (e in italiano `applica`, `inserisci paese`), senza barre e senza il vecchio `">` in fondo. Lo script della PWA che importa le stringhe ora segnala righe come queste.
- **Profilo utente**: la PWA ora segue `UserProfileContent.kt`: stesso ordine, riquadri "i miei argomenti" (un argomento alla volta, frecce, traduce quello mostrato) e "parla di te" (traduzione sotto l'originale), "lingue parlate" in elenco, "chatta con" in fondo. Due cose che sul web restano diverse, decise prima: la località sotto il nome e il tocco sulla foto che apre tutte le foto. Nota: su un profilo altrui i titoli "i miei argomenti" e "parla di te" sono in prima persona (stesse stringhe del proprio profilo); se si vuole, servono stringhe a parte.
- **`incrementProfileView`** (`UserActivityUtils.kt`) scrive in `users/{uid}/profileViews`, ma le regole Firestore non hanno una regola per quella sottocollezione: la scrittura viene rifiutata in silenzio (l'unico documento è di aprile 2025) e nessuno la legge. La PWA non la replica. O si toglie da Android, o si aggiunge una regola se serve davvero.
- **Onboarding sul web**: su richiesta dell'utente non viene più prima del login ma dopo registrazione e verifica dell'email, solo per gli account creati dal web. Nuovo campo `users/{uid}.onboardingCompleted` (false alla registrazione web o al primo login Google dal web, true alla fine). Android non lo legge e non deve toglierlo (`set()` senza `merge` lo cancellerebbe: l'onboarding sul web verrebbe saltato). Se si vuole lo stesso ordine anche su Android, va deciso con l'utente.
