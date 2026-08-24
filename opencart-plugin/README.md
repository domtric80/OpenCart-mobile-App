# CartAdmin Bridge Plugin per OpenCart 2.x, 3.x & 4.x
**OpenCart ITALIA by SOLO SOLUZIONI** (https://www.solosoluzioni.it - https://www.opencartitalia.it)

Questo plugin abilita il collegamento bidirezionale e sicuro tra il tuo negozio OpenCart e l'applicazione mobile **CartAdmin Android**.

---

## 🚀 Metodo 1: Installazione Rapida tramite Pannello OpenCart (OCMOD)

1. Scarica il file **`cartadmin-opencart-bridge.ocmod.zip`** dalla sezione [Releases](https://github.com) di questo repository.
2. Accedi al pannello amministratore del tuo OpenCart (*Admin Panel*).
3. Vai in **Estensioni > Programma di Installazione** (*Extensions > Installer*).
4. Clicca su **Carica** (*Upload*) e seleziona il file `cartadmin-opencart-bridge.ocmod.zip`.
5. Vai in **Estensioni > Modifiche** (*Extensions > Modifications*) e clicca sul pulsante arancione/blu **Aggiorna** (*Refresh*).

---

## 🛠️ Metodo 2: Installazione Manuale via FTP / File Manager

1. Estrai il contenuto del pacchetto.
2. Carica il file `upload/cartadmin_api.php` nella directory principale (root) della tua installazione OpenCart (dove si trovano `config.php`, `index.php`, `admin/`, `catalog/`).
3. Verifica i permessi del file (consigliato: `644`).

---

## 🔑 Configurazione nell'App Android CartAdmin

1. Apri l'app **CartAdmin** sul tuo smartphone o tablet Android.
2. Vai nella schermata **Impostazioni** (icona ingranaggio o tocca la tua immagine profilo).
3. Inserisci l'URL del tuo negozio (es. `https://tuonegozio.it`).
4. Inserisci la **Chiave Segreta API**:
   - Per visualizzare la chiave generata dal plugin, apri nel browser `https://tuonegozio.it/cartadmin_api.php?action=get_key_setup` (oppure definiscine una personalizzata).
5. Tocca **Test API** e poi **Salva**. I tuoi ordini, prodotti e log di audit saranno sincronizzati istantaneamente in tempo reale con crittografia hardware!
