# CartAdmin Bridge per OpenCart 4.1

Estensione HTTPS tra CartAdmin Android e OpenCart 4.1.x, sviluppata da OpenCart ITALIA by SOLOSOLUZIONI. Questo sorgente corrisponde alla release stabile `2.1.4` e deve essere usato con l'app della stessa versione.

## Installazione

1. Scarica `cartadmin.ocmod.zip` dalla release stabile GitHub.
2. Nel pannello OpenCart apri **Estensioni > Installer**, carica lo ZIP e completa l'installazione.
3. Apri **Estensioni > Estensioni**, seleziona **Moduli** e installa **CartAdmin Bridge**.
4. Apri il modulo, assegna al token un'etichetta, seleziona un utente amministrativo OpenCart attivo e concedi i soli permessi necessari, quindi premi **Genera token**.
5. Copia subito il token nell'app CartAdmin: il valore completo non sarà più visualizzabile.

Non creare né modificare manualmente file PHP. Il nome `cartadmin.ocmod.zip` è intenzionalmente stabile perché OpenCart usa il nome dello ZIP come codice e cartella dell'estensione.

## Protezione del token

- Il token è generato con 256 bit casuali usando `random_bytes()`.
- Nel database viene memorizzato soltanto un hash non reversibile Argon2id, con fallback all'algoritmo sicuro predefinito di PHP.
- Il token in chiaro è restituito soltanto nella risposta amministrativa che lo genera.
- Ogni token è revocabile individualmente dal pannello e può avere scope di sola lettura o di scrittura separati per ordini, catalogo, contenuti, clienti e audit.
- Al primo utilizzo il token viene associato atomicamente alla chiave pubblica ECDSA di una sola installazione Android. La chiave privata non è esportabile dal TEE/StrongBox del dispositivo.
- Ogni richiesta include firma, timestamp e nonce: il bridge rifiuta firme errate, richieste scadute e replay.
- I permessi di lettura sono separati tra stato, ordini, catalogo, contenuti, clienti/GDPR e telemetria. Il vecchio scope globale `read` non è accettato.
- Un eventuale `api_key` in chiaro creato da una versione precedente viene convertito automaticamente in hash e poi eliminato.
- Le credenziali sono accettate esclusivamente negli header HTTPS; URL e form body vengono ignorati.
- L'identità autorevole dell'operatore (`user_id` e username) proviene da un utente OpenCart attivo selezionato nel pannello e viene salvata nel token. L'app 2.1 non invia un nome operatore; un eventuale client precedente può produrre soltanto un digest HMAC e un indicatore di incongruenza, senza attribuire l'operazione a un altro utente.

L'app Android conserva la propria copia del token tramite AES-256-GCM e Android Keystore hardware-backed.

### Aggiornamento dalla 2.0

La 2.1.1 richiede la rotazione dei token precedenti. Dopo l'aggiornamento del bridge genera un token nominativo con i soli scope necessari, aggiorna l'app, verifica la connessione e revoca ogni token legacy. Il bridge rifiuta client privi di prova hardware e token che conservano soltanto il vecchio scope globale `read`.

## Contenuti e moderazione

Il bridge 2.1 consente all'app di modificare soltanto campi esplicitamente autorizzati:

- titolo e ordinamento delle pagine informative;
- titolo e autore degli articoli;
- titolo e ordinamento degli argomenti;
- autore, testo e valutazione delle recensioni.

Consente inoltre di creare articoli e categorie CMS. La creazione richiede titolo e contenuto; un articolo deve indicare autore e categoria CMS esistente. Il bridge inizializza le descrizioni per tutte le lingue attive, associa il record allo store principale e inserisce l'audit di successo nella stessa transazione. Se uno dei passaggi fallisce, l'intera creazione viene annullata.

## Immagini prodotto e telemetria guest

L'app può inviare una nuova immagine prodotto dalla fotocamera o dalla galleria. Il file viaggia in multipart mentre il token resta nell'header HTTPS. Il bridge accetta soltanto JPEG, PNG e WebP fino a 5 MB, verifica il MIME dal contenuto e le dimensioni, ignora il nome originale e salva con nome casuale sotto `image/catalog/cartadmin/`.

La telemetria legge sia i guest (`customer_id = 0`) sia i clienti registrati (`customer_id > 0`) dalla tabella nativa `customer_online`. Poiché il modello OpenCart elimina già le sessioni scadute, il bridge usa le righe rimaste senza confrontarle con l'orologio MySQL; restituisce inoltre numero di record e data dell'ultima visita per la diagnosi. L'API espone conteggi separati e percorsi aggregati, ma non IP, nome, email o ID cliente.

Per Pagine, Articoli e Argomenti viene aggiornata esclusivamente la lingua principale attiva. Il contenuto HTML, i metadati SEO e le altre traduzioni non vengono riscritti dall'app. Recensioni e commenti possono inoltre essere attivati o disattivati; la valutazione aggregata del prodotto viene ricalcolata dopo ogni modifica a una recensione.

Ogni modulo e tabella è definito in un'allowlist lato server. Le modifiche usano query preparate, transazioni e invalidazione mirata della cache; se lo schema richiesto non è disponibile, l'operazione fallisce senza aggiornamenti parziali. Per le modifiche editoriali, aggiornamento e audit di successo sono atomici. Il registro salva soltanto metadati e digest HMAC dello stato precedente e successivo; dopo un rollback registra un evento di fallimento separato senza contenuti in chiaro.

## Approvazioni clienti e richieste GDPR

Queste operazioni non vengono applicate direttamente dal bridge pubblico. Dall'app è possibile inviare una richiesta **Approva** o **Rifiuta**, che compare nella sezione **Richieste sensibili dall'app** del modulo CartAdmin Bridge.

Un amministratore OpenCart con permesso di modifica deve quindi:

1. aprire **Estensioni > Estensioni > Moduli > CartAdmin Bridge**;
2. verificare modulo, ID, operazione e operatore che ha inviato la richiesta;
3. scegliere **Conferma ed esegui** oppure **Rifiuta richiesta**.

Solo la conferma dal pannello richiama i modelli amministrativi nativi OpenCart. In questo modo vengono rispettati eventi, notifiche email e stati GDPR ufficiali. Una richiesta mobile pendente non modifica il cliente e non elabora dati personali.

## Endpoint

L'estensione espone:

`https://negozio.example/extension/cartadmin/cartadmin_api.php`

Il negozio deve utilizzare HTTPS con certificato valido.

## Compatibilità

Questo pacchetto è destinato a OpenCart 4.1.x. Non dichiara compatibilità con OpenCart 3.x: quel ramo usa un formato di installazione differente e dovrà essere distribuito come artefatto separato.
