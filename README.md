# CartAdmin 2.0.0-dev.1

CartAdmin è un'app Android per consultare e amministrare un negozio OpenCart da smartphone. Il progetto è sviluppato in Kotlin con Jetpack Compose e include il bridge CartAdmin per OpenCart 4.1.x. Il ramo corrente contiene lo sviluppo della nuova navigazione 2.0; la release stabile pubblicata resta la v1.2.8.

## Download

La release stabile è disponibile in [GitHub Releases](https://github.com/domtric80/OpenCart-mobile-App/releases/latest).

| Componente | File | Compatibilità confermata |
| --- | --- | --- |
| App Android | `CartAdmin-v1.2.8.apk` | Android 7.0 o successivo, API 24–36 |
| Bridge OpenCart | `cartadmin.ocmod.zip` | OpenCart 4.1.x |

Il bridge incluso non dichiara compatibilità con OpenCart 3.x. Un eventuale pacchetto per quel ramo dovrà essere pubblicato e verificato separatamente.

### Sviluppo 2.0.0-dev.1

- barra inferiore ridotta a cinque voci: Home, Vendite, Catalogo, Clienti e Altro;
- sottomenu Catalogo con Prodotti, Categorie, Piani di abbonamento, Pagine e Recensioni;
- nuovo menu CMS con Articoli, Argomenti, Commenti e Antispam;
- nuovo menu Clienti con Clienti, Approvazione clienti e GDPR;
- Traffic, CMS e Configurazione raggruppati sotto Altro;
- Audit e Licenza spostati nella schermata Configurazione;
- elenchi amministrativi letti in tempo reale dalle tabelle native OpenCart, senza record dimostrativi;
- attivazione e disattivazione remota verificata per i moduli che espongono uno stato semplice;
- compatibilità esplicita: se una tabella non esiste nella versione installata, l'app mostra “funzione non disponibile” invece di dati fittizi.

Approvazioni clienti e richieste GDPR sono inizialmente in sola lettura: approvazione, rifiuto, esportazione o cancellazione richiedono il flusso completo OpenCart con notifiche ed eventi e non vengono simulati con una semplice modifica al database.

### Novità della 1.2.8

- telemetria visitatori reale letta dalla tabella nativa OpenCart `customer_online`, con aggiornamento automatico ogni 30 secondi;
- diagnostica esplicita quando il tracciamento **Clienti online** è disabilitato o il bridge non è aggiornato;
- modifica remota verificata di nome, descrizione, modello, SKU, prezzo, quantità, stato e categoria esistente dei prodotti;
- aggiornamenti locali applicati soltanto dopo la conferma positiva del bridge;
- eliminati i messaggi di successo per operazioni che non hanno ancora un endpoint remoto sicuro;
- versione e build installate visibili nella schermata Config;
- mantenute le correzioni 1.2.7 per abbonamenti e resi senza dati dimostrativi.

La creazione e l'eliminazione dei prodotti e il CRUD delle categorie restano temporaneamente non disponibili dall'app: CartAdmin non modifica più soltanto la cache fingendo un aggiornamento dello store. Le offerte e le promozioni programmate continuano a essere gestite dal pannello OpenCart.

## Prima configurazione

### 1. Installa il bridge in OpenCart

1. Scarica `cartadmin.ocmod.zip` dalla release.
2. Nel pannello OpenCart apri **Estensioni > Installer** e carica lo ZIP.
3. Apri **Estensioni > Estensioni > Moduli** e installa **CartAdmin Bridge**.
4. Apri il modulo e premi **Genera token** oppure **Ruota token**.
5. Copia subito il token: OpenCart mostra il valore completo una sola volta e conserva nel database soltanto il suo hash non reversibile.

### 2. Aggiungi il primo negozio nell'app

1. Apri **Config**. Se non esistono profili, la schermata mostra l'avviso **Nessun negozio configurato**.
2. Inserisci il nome del negozio e l'URL HTTPS, senza il percorso del file API. Esempio: `https://negozio.example`.
3. Inserisci un nome operatore per l'audit e incolla il token generato da OpenCart.
4. Seleziona la versione OpenCart e premi **Aggiungi**. Questo crea e salva il primo profilo; non è necessario passare prima dal selettore dei negozi.
5. Premi **Test API**, quindi **Sincronizza dati adesso**.

Dopo il salvataggio il campo token torna vuoto e il valore non viene più mostrato. Per aggiornare gli altri dati lascia il campo vuoto: CartAdmin mantiene il token protetto. Inserisci un nuovo valore solo dopo aver ruotato il token dal pannello OpenCart.

### 3. Abilita la telemetria visitatori

1. Nel pannello OpenCart apri **Sistema > Impostazioni** e modifica il negozio.
2. Nella scheda **Opzioni** abilita **Clienti online** e imposta il tempo di inattività desiderato.
3. Installa il bridge della stessa release dell'app e riapri **Traffic**.

OpenCart registra soltanto visitatori attivi, URL, provenienza e ultimo aggiornamento. Non registra nella tabella `customer_online` user agent, geolocalizzazione, durata completa della sessione o bounce rate; CartAdmin lascia queste metriche non disponibili invece di generare valori dimostrativi.

## Come viene protetto il token

- OpenCart genera un token casuale a 256 bit e salva soltanto un hash Argon2id, con fallback all'algoritmo sicuro disponibile in PHP.
- Android salva la propria copia cifrata con AES-256-GCM e una chiave non esportabile di Android Keystore.
- StrongBox viene preferito quando presente; in alternativa è obbligatoria una chiave hardware-backed nel TEE. Se il dispositivo offre soltanto protezione software, il salvataggio fallisce in modo sicuro.
- Il token salvato non viene associato al campo dell'interfaccia. Dopo lo sblocco dell'app la credenziale può essere decifrata in memoria soltanto per effettuare richieste HTTPS autenticate.
- Il bridge accetta il token negli header HTTP e ignora credenziali inviate in URL o form body.
- La rotazione dal pannello OpenCart invalida immediatamente il token precedente.

Non inserire token, password, keystore o chiavi di firma in issue, screenshot, commit o file di configurazione versionati.

## Funzionalità presenti

- dashboard con indicatori di vendita e attività;
- consultazione e aggiornamento dello stato degli ordini;
- catalogo, categorie, quantità e prezzi;
- abbonamenti e resi esposti dal bridge;
- cache locale Room e sincronizzazione manuale;
- selezione di più profili negozio;
- notifiche Firebase Cloud Messaging quando configurate;
- audit delle operazioni inviate al bridge;
- blocco dell'app con password locale PBKDF2 e sblocco biometrico forte opzionale.

Le schermate possono mostrare dati memorizzati localmente quando il negozio non è raggiungibile. Una sincronizzazione riuscita che restituisce zero abbonamenti o zero resi svuota le rispettive cache: l'app non inserisce dati dimostrativi. Verificare sempre l'esito della sincronizzazione prima di considerare aggiornati i dati.

## Screenshot

<p align="center">
  <img src="app/src/main/res/drawable/img_cartadmin_screen_dash_1786997549527.jpg" width="45%" alt="Dashboard CartAdmin" />
  &nbsp;
  <img src="app/src/main/res/drawable/img_cartadmin_screen_orders_1786997562634.jpg" width="45%" alt="Elenco ordini CartAdmin" />
</p>

Gli screenshot non devono contenere token, password, dati cliente reali o altri segreti. La schermata Config nasconde sempre il token digitato e non lo ripropone dopo il salvataggio.

## Architettura

- UI: Jetpack Compose e Material 3;
- stato applicativo: MVVM, `MainViewModel`, Coroutines e `StateFlow`;
- persistenza: Room;
- rete: Retrofit/OkHttp e JSON Moshi;
- sicurezza locale: Android Keystore, AES-256-GCM, PBKDF2 e BiometricPrompt;
- integrazione server: estensione PHP CartAdmin Bridge per OpenCart 4.1.x.

## Build e test locali

Il progetto usa il Gradle Wrapper e richiede Android Studio con Android SDK 36. È preferibile il JDK integrato in Android Studio.

In PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
./gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
```

Con un emulatore o dispositivo collegato:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" devices
./gradlew.bat :app:connectedDebugAndroidTest
```

I workflow GitHub Actions compilano l'APK, eseguono test e Lint, validano il pacchetto OpenCart ed effettuano controlli CodeQL, Semgrep e PHP. La pubblicazione stabile richiede un tag identico al `versionName` e usa i segreti di firma configurati nel repository GitHub.

## Repository

- `app/`: applicazione Android e test;
- `opencart-plugin/`: manifest, modulo amministrativo, bridge PHP e test di autenticazione;
- `.github/workflows/`: build, pubblicazione e controlli di sicurezza;
- `gradle/` e `gradlew*`: Gradle Wrapper.

## Licenza e progetto

CartAdmin è distribuito con licenza [GNU GPL v3](LICENSE).

- OpenCart ITALIA: [www.opencartitalia.it](https://www.opencartitalia.it)
- SOLO SOLUZIONI: [www.solosoluzioni.it](https://www.solosoluzioni.it)
- Sviluppatore: [www.domenicotricarico.it](https://www.domenicotricarico.it)
