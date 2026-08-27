# CartAdmin Bridge Plugin per OpenCart

Bridge HTTPS tra CartAdmin Android e OpenCart, sviluppato da SOLO SOLUZIONI per OpenCart ITALIA.

## Requisiti di sicurezza

- Il negozio deve essere raggiungibile esclusivamente tramite HTTPS con certificato valido.
- Per una nuova installazione, crea dal pannello OpenCart un utente API dedicato, attivo e con una chiave casuale univoca.
- Non inserire mai chiavi API in URL, screenshot, ticket o log.
- L'endpoint non espone e non genera chiavi tramite richieste pubbliche.

Le chiavi bridge già configurate da versioni precedenti continuano a funzionare, ma è consigliata la loro rotazione dopo l'aggiornamento.

## Installazione OCMOD

1. Scarica `cartadmin-opencart-bridge-<versione>.ocmod.zip` dalla sezione Releases del repository.
2. Nel pannello OpenCart apri **Estensioni > Programma di installazione** e carica il pacchetto.
3. Apri **Estensioni > Modifiche** e aggiorna la cache delle modifiche.
4. Verifica che nella root OpenCart siano presenti `cartadmin_api.php` e `cartadmin_auth.php`.

## Installazione manuale

1. Copia `upload/cartadmin_api.php` e `upload/cartadmin_auth.php` nella root di OpenCart, accanto a `config.php`.
2. Imposta permessi restrittivi compatibili con il web server; normalmente `0644` per entrambi i file.
3. Crea o seleziona un utente API dedicato dal pannello amministrativo OpenCart.

## Configurazione CartAdmin

1. Apri **Impostazioni > Negozio** nell'app.
2. Inserisci l'URL HTTPS dello store.
3. Inserisci lo username e la chiave dell'utente API OpenCart dedicato.
4. Tocca **Test API** e salva soltanto dopo una risposta valida.

Le credenziali vengono inviate negli header `X-CartAdmin-User` e `X-CartAdmin-Key`; query string e form body non vengono accettati per l'autenticazione.

## Aggiornamento di sicurezza

Le versioni fino alla 1.2.5 esponevano un endpoint di provisioning pubblico. Dopo l'aggiornamento:

1. sostituisci entrambi i file PHP;
2. ruota la chiave API usata precedentemente;
3. controlla i log web per richieste a `action=get_key_setup`;
4. verifica gli utenti API OpenCart e disabilita quelli non riconosciuti.
