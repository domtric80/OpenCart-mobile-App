# CartAdmin Bridge per OpenCart 4.1

Estensione HTTPS tra CartAdmin Android e OpenCart 4.1.x, sviluppata da OpenCart ITALIA by SOLOSOLUZIONI.

## Installazione

1. Scarica `cartadmin.ocmod.zip` dalla release stabile GitHub.
2. Nel pannello OpenCart apri **Estensioni > Installer**, carica lo ZIP e completa l'installazione.
3. Apri **Estensioni > Estensioni**, seleziona **Moduli** e installa **CartAdmin Bridge**.
4. Apri il modulo e premi **Genera token**.
5. Copia subito il token nell'app CartAdmin: il valore completo non sarà più visualizzabile.

Non creare né modificare manualmente file PHP. Il nome `cartadmin.ocmod.zip` è intenzionalmente stabile perché OpenCart usa il nome dello ZIP come codice e cartella dell'estensione.

## Protezione del token

- Il token è generato con 256 bit casuali usando `random_bytes()`.
- Nel database viene memorizzato soltanto un hash non reversibile Argon2id, con fallback all'algoritmo sicuro predefinito di PHP.
- Il token in chiaro è restituito soltanto nella risposta amministrativa che lo genera.
- La rotazione invalida immediatamente il token precedente.
- Un eventuale `api_key` in chiaro creato da una versione precedente viene convertito automaticamente in hash e poi eliminato.
- Le credenziali sono accettate esclusivamente negli header HTTPS; URL e form body vengono ignorati.

L'app Android conserva la propria copia del token tramite AES-256-GCM e Android Keystore hardware-backed.

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
