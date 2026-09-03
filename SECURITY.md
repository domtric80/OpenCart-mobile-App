# Sicurezza di CartAdmin

## Versioni supportate

Ricevono correzioni di sicurezza la release stabile più recente e il branch di sviluppo attivo. App Android e bridge OpenCart devono sempre provenire dalla stessa release.

## Segnalazione privata

Non aprire issue pubbliche contenenti vulnerabilità, token, dati cliente, URL amministrativi, log completi o screenshot sensibili. Usa la funzione **Security > Report a vulnerability** del repository GitHub. Se il canale non è disponibile, contatta il maintainer tramite i riferimenti pubblicati nel profilo del repository chiedendo un canale cifrato, senza includere il dettaglio tecnico nel primo messaggio.

Indica versione di app e bridge, versione Android/OpenCart, impatto, prerequisiti e passaggi minimi di riproduzione. Usa esclusivamente dati di prova.

## Risposta a un'esposizione

1. Revoca immediatamente dal pannello OpenCart il token coinvolto.
2. Rimuovi l'accesso del dispositivo se perso o compromesso.
3. Ruota eventuali credenziali API native che possano essere state riutilizzate.
4. Conserva log server e audit senza pubblicarli.
5. Installa app e bridge corretti insieme; genera un nuovo token per operatore/dispositivo con scope minimi.

## Proprietà di sicurezza

- token OpenCart memorizzato sul server soltanto come hash non reversibile;
- copia Android cifrata AES-256-GCM con chiave autenticata e hardware-backed;
- firma ECDSA hardware di ogni richiesta, protezione anti-replay, HTTPS/HSTS e pinning TLS per host;
- scope granulari, revoca selettiva, identità operatore risolta lato server e audit transazionale;
- nessuna persistenza offline di ordini, clienti, resi, abbonamenti, audit, catalogo o telemetria;
- cifratura hardware-bound dell'intero profilo negozio (nome, URL, versione, operatore e token);
- nessuna copia applicativa persistente del token FCM;
- backup Android, screenshot, overlay e notifiche con dettagli personali disabilitati;
- release firmate con checksum SHA-256 e attestazione di provenienza GitHub.

Queste misure riducono il rischio ma non garantiscono rischio zero su dispositivi rooted, sistemi operativi compromessi, server OpenCart compromessi o account amministrativi sottratti.
