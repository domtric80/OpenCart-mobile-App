# 🛒 CartAdmin — E-Commerce Store Admin & Analytics for Android

<p align="center">
  <a href="https://github.com"><img src="https://img.shields.io/badge/Platform-Android%207.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GNU%20GPL%20v3-blue.svg?style=for-the-badge" alt="License: GNU GPL v3" /></a>
  <a href="https://github.com"><img src="https://img.shields.io/badge/Version-v1.2.1-brightgreen?style=for-the-badge" alt="Version" /></a>
</p>

<p align="center">
  <img src="app/src/main/res/drawable/img_cartadmin_preview_1786997536667.jpg" alt="CartAdmin App Banner" width="85%" />
</p>

---

## 📱 About CartAdmin

**CartAdmin** è l'**App Ufficiale di OpenCart ITALIA** ([www.opencartitalia.it](https://www.opencartitalia.it)) sviluppata da **SOLO SOLUZIONI** ([www.solosoluzioni.it](https://www.solosoluzioni.it)).

Un'applicazione Android moderna e ad alte prestazioni creata per i gestori e gli amministratori di negozi e-commerce OpenCart. Sviluppata interamente con **Jetpack Compose** e **Material Design 3**, CartAdmin offre metriche di vendita in tempo reale, gestione ordini, catalogo prodotti e notifiche push direttamente sul tuo smartphone.

---

## 📥 Download Release & Compatibilità Hardware

Tutti i rilasci compilati dell'**App Android** e del **Plugin per OpenCart** sono scaricabili direttamente dalla sezione [GitHub Releases](../../releases).

### 📦 File Disponibili per il Download:

| Componente | File Scaricabile | Compatibilità | Note di Installazione |
| :--- | :--- | :--- | :--- |
| 📱 **App Android** (Latest) | [`CartAdmin-v1.2.1.apk`](../../releases/tag/v1.2.1) | Android 7.0 (API 24) o superiore | Scarica sul telefono/tablet e tocca Installa |
| 🔌 **Plugin OpenCart** (Latest) | [`cartadmin-opencart-bridge-v1.2.1.ocmod.zip`](../../releases/tag/v1.2.1) | OpenCart 2.x, 3.x, 4.x | Carica in *Estensioni > Programma di Installazione* (OCMOD) |

---

### 🕒 Cronologia Release

| Versione Release | App Android (`.apk`) | Plugin OpenCart (`.ocmod.zip`) | Stato |
| :--- | :--- | :--- | :--- |
| **v1.2.1** (Latest) | [`CartAdmin-v1.2.1.apk`](../../releases/tag/v1.2.1) | [`cartadmin-opencart-bridge-v1.2.1.ocmod.zip`](../../releases/tag/v1.2.1) | 🟢 Stabile (Risoluzione Token FCM Push, Sicurezza Bancaria, Timeout 5m, Sblocco Biometrico, TEE AES-GCM) |
| **v1.2.0** | [`CartAdmin-v1.2.0.apk`](../../releases/tag/v1.2.0) | — | ⚪ Precedente |
| **v1.1.3** | [`CartAdmin-v1.1.3.apk`](../../releases/tag/v1.1.3) | — | ⚪ Precedente |
| **v1.1.2** | [`CartAdmin-v1.1.2.apk`](../../releases/tag/v1.1.2) | — | ⚪ Precedente |
| **v1.1.1** | [`CartAdmin-v1.1.1.apk`](../../releases/tag/v1.1.1) | — | ⚪ Precedente |
| **v1.1.0** | [`CartAdmin-v1.1.0.apk`](../../releases/tag/v1.1.0) | — | ⚪ Precedente |

### 📋 Requisiti di Sistema & Dispositivi
- **Sistema Operativo**: Android 7.0 Nougat (API 24) fino ad Android 15 (API 35/36)
- **Fattori di forma supportati**: Smartphone, Foldables, Tablet e ambienti ChromeOS (layout responsive con supporto Window Size Classes)
- **OpenCart Supportati**: OpenCart 3.0.x, OpenCart 4.0.x (tramite modulo API `cartadmin_api.php`)

---

## ✨ Funzionalità Principali

- 📊 **Metriche & Statistiche di Vendita in Tempo Reale**: Monitora fatturato giornaliero, ordini recenti, nuovi clienti e grafici di trend.
- 📦 **Gestione Ciclo di Vita Ordini**: Filtra per stato (In attesa, In lavorazione, Spedito, Completato, Rimborsato), visualizza il dettaglio dei prodotti acquistati e aggiorna gli stati in tempo reale.
- 🏷️ **Catalogo Prodotti & Allarmi Stock**: Gestione rapida dell'inventario, prezzi, quantità in magazzino e avvisi automatici sottoscorta.
- 📶 **Resilienza Offline (Room Database)**: I dati del negozio vengono salvati localmente per una consultazione istantanea anche in assenza di rete.
- 🔔 **Notifiche Push Istantanee**: Supporto Firebase Cloud Messaging per la ricezione immediata dei nuovi ordini.
- 🎨 **Interfaccia Material Design 3**: Dynamic theming, tipografia ad alto contrasto e transizioni fluide.

---

## 💖 Supporto & Sponsorizzazione

Se utilizzi CartAdmin per gestire il tuo business e desideri supportare lo sviluppo continuo del progetto o richiedere personalizzazioni dedicate:

<p align="center">
  <a href="https://github.com/sponsors/domtric80"><img src="https://img.shields.io/badge/Sponsor-%E2%9D%A4-ea4aaa?style=for-the-badge&logo=github&logoColor=white" alt="Sponsor domtric80 on GitHub" /></a>
  &nbsp;&nbsp;
  <a href="https://www.domenicotricarico.it"><img src="https://img.shields.io/badge/Domenico%20Tricarico-Developer-10b981?style=for-the-badge&logo=googlechrome&logoColor=white" alt="Domenico Tricarico" /></a>
  &nbsp;&nbsp;
  <a href="https://www.solosoluzioni.it"><img src="https://img.shields.io/badge/SOLO%20SOLUZIONI-Azienda%20del%20Progetto-6366f1?style=for-the-badge" alt="Solo Soluzioni" /></a>
  &nbsp;&nbsp;
  <a href="https://www.opencartitalia.it"><img src="https://img.shields.io/badge/OpenCart%20ITALIA-Community-0284c7?style=for-the-badge&logo=opencart&logoColor=white" alt="OpenCart Italia" /></a>
</p>

- 👨‍💻 **Sito dello Sviluppatore**: [www.domenicotricarico.it](https://www.domenicotricarico.it)
- 🏢 **Azienda del Progetto**: **SOLO SOLUZIONI** — [www.solosoluzioni.it](https://www.solosoluzioni.it)
- 🌐 **Portale Ufficiale Community**: **OpenCart ITALIA** — [www.opencartitalia.it](https://www.opencartitalia.it)
- ☕ **Sostieni lo sviluppo su GitHub**: [github.com/sponsors/domtric80](https://github.com/sponsors/domtric80)

---

## 📸 Screenshot

<p align="center">
  <img src="app/src/main/res/drawable/img_cartadmin_screen_dash_1786997549527.jpg" width="45%" alt="Dashboard Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="app/src/main/res/drawable/img_cartadmin_screen_orders_1786997562634.jpg" width="45%" alt="Orders Screen" />
</p>

---

## 🛠️ Architettura & Stack Tecnologico

- **UI Framework**: Jetpack Compose con componenti Material 3
- **Architettura**: Clean MVVM con Kotlin Coroutines & StateFlow
- **Persistenza Locale**: Android Room Database
- **Networking**: Retrofit 2 + OkHttp3 + Kotlinx Serialization
- **Cloud Messaging**: Firebase Cloud Messaging (FCM)
- **Target SDK**: minSdk 24, targetSdk 36

---

## 📄 Licenza (License)

Questo progetto è rilasciato sotto licenza libera e open-source **GNU General Public License v3.0 (GNU GPL v3)**.  
Consulta il file [`LICENSE`](LICENSE) per il testo completo dei termini e delle condizioni di licenza.

```text
CartAdmin - App Ufficiale OpenCart ITALIA by SOLO SOLUZIONI
Copyright (C) 2026 OpenCart ITALIA & SOLO SOLUZIONI

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```
