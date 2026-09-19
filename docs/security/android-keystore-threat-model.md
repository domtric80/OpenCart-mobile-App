# Modello di sicurezza Android Keystore

## Scopo

CartAdmin separa autenticazione dell'operatore, cifratura locale e identità crittografica del dispositivo. Le tre chiavi Android Keystore non sono intercambiabili e non devono essere interpretate come tre fattori biometrici.

| Chiave | Dato o operazione protetta | Autenticazione richiesta | Durata |
| --- | --- | --- | --- |
| `CartAdmin_BiometricUnlockSigningKey_v1` | apertura della sessione amministrativa | biometria forte tramite `BiometricPrompt.CryptoObject` | una singola operazione |
| `CartAdmin_StoreCredentials_UserAuthKey_v4` | profili negozio e token CartAdmin cifrati con AES-256-GCM | biometria forte o credenziale sicura del dispositivo | massimo 300 secondi |
| `CartAdmin_BridgeDeviceSigningKey_v1` | prova di possesso del dispositivo per ogni richiesta HTTPS | nessuna autenticazione aggiuntiva della chiave | firma automatica durante la sessione |

Tutte le chiavi private o segrete sono non esportabili. La build stabile rifiuta le chiavi software e accetta esclusivamente TEE o StrongBox.

## Sblocco biometrico

Lo sblocco usa una chiave ECDSA dedicata, autorizzata per singola operazione e soltanto con biometria forte. Il callback del prompt non apre direttamente la sessione: deve prima completare e verificare la firma di una challenge casuale. La chiave viene invalidata quando cambia l'insieme delle biometrie e viene rigenerata soltanto dopo un nuovo prompt valido.

Annullamento, errore o assenza del `CryptoObject` lasciano la sessione bloccata. La password CartAdmin rimane un fallback applicativo distinto e non trasforma la chiave biometrica in una chiave riutilizzabile.

## Cifratura delle credenziali

Nome negozio, URL, versione, operatore e token sono cifrati con AES-256-GCM. Store, campo e formato sono autenticati come AAD, quindi un valore cifrato non può essere spostato su un altro profilo o campo senza invalidare il tag GCM.

Su Android 11/API 30 e versioni successive la chiave dichiara esplicitamente `AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL` e una finestra di 300 secondi. Su Android 7–10 l'API Keystore disponibile espone soltanto la durata della validità: una credenziale sicura dello schermo può quindi autorizzare la chiave per la stessa finestra. Questo comportamento è intenzionale per mantenere il fallback e la compatibilità API 24–29; non viene dichiarata una garanzia “solo biometria” per tali versioni.

La sessione in memoria viene comunque distrutta quando l'app viene bloccata, passa in background fuori dai flussi di sistema autorizzati o raggiunge il timeout. I dati amministrativi remoti non vengono persistiti offline.

## Identità del dispositivo e firma bridge

La chiave ECDSA del bridge non autentica l'operatore e non cifra il token. È un'identità di installazione equivalente a una chiave client non esportabile: il server associa al token la chiave pubblica e richiede una firma su metodo, destinazione, timestamp e nonce per ogni richiesta.

L'uso della chiave non apre da solo alcun accesso perché il bridge richiede contemporaneamente:

1. token CartAdmin valido e non revocato;
2. firma prodotta dalla chiave del dispositivo associato;
3. timestamp entro la finestra ammessa;
4. nonce mai utilizzato in precedenza;
5. scope necessario all'azione richiesta.

La chiave è intenzionalmente configurata senza autenticazione utente per singola firma: una sincronizzazione genera molte richieste e richiedere un prompt per ciascuna renderebbe il client inutilizzabile. L'autenticazione dell'operatore avviene prima, mediante la sessione CartAdmin; il token necessario alla richiesta rimane cifrato quando la sessione è chiusa.

## Modello di attacco

Sono considerate minacce in ambito:

- furto del database o del backup dell'app;
- lettura diretta dei file applicativi;
- estrazione delle chiavi dal filesystem;
- riutilizzo di richieste firmate;
- copia del token su un secondo dispositivo;
- modifica dei valori cifrati;
- accesso dopo blocco o timeout della sessione.

Non è possibile garantire la segretezza dei dati già visualizzati quando un aggressore controlla integralmente il processo dell'app durante una sessione autenticata, il sistema operativo è compromesso o il dispositivo è sbloccato con credenziali note all'aggressore. In tali condizioni l'utente deve revocare il token dal pannello OpenCart e generare una nuova identità su un dispositivo attendibile.

## Gestione degli errori

- chiave biometrica invalidata: eliminazione e rigenerazione, seguita da una nuova autenticazione;
- chiave credenziali assente o invalidata: fallimento chiuso, senza ripiego su cifratura software o testo in chiaro;
- dispositivo senza TEE/StrongBox: salvataggio persistente rifiutato;
- autenticazione annullata: sessione non aperta;
- token o identità bridge revocati: nuova associazione esplicita dal pannello OpenCart.

## Alert CodeQL riesaminati il 19 settembre 2026

- `java/android/insecure-local-key-gen` su `BridgeDeviceIdentity`: la chiave è una prova di possesso del dispositivo, non una chiave di autenticazione biometrica. La mancata autenticazione per singola firma è intenzionale e compensata dal token cifrato, dalla sessione autenticata e dalla verifica server-side anti-replay.
- `java/android/insecure-local-key-gen` sul ramo API 24–29 di `AndroidKeystoreCredentialProtector`: la regola presume una politica esclusivamente biometrica, mentre CartAdmin consente esplicitamente anche la credenziale sicura del dispositivo. La chiave resta autenticata, hardware-backed e limitata a 300 secondi.

Qualsiasi modifica a queste decisioni richiede test su un dispositivo fisico con TEE/StrongBox e una nuova revisione del modello di minaccia.
