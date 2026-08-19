package com.example.model

enum class AuditActionType(val label: String, val category: String) {
    ORDER_STATUS_UPDATE("Cambio Stato Ordine", "Ordini"),
    ORDER_NOTES_UPDATE("Modifica Note Ordine", "Ordini"),
    PRODUCT_STOCK_UPDATE("Aggiornamento Giacenza", "Magazzino"),
    PRODUCT_CREATE("Creazione Nuovo Prodotto", "Catalogo"),
    PRODUCT_UPDATE("Modifica Scheda Prodotto", "Catalogo"),
    PRODUCT_DELETE("Eliminazione Prodotto", "Catalogo"),
    PRODUCT_STATUS_TOGGLE("Attivazione/Disattivazione Prodotto", "Catalogo"),
    CATEGORY_CREATE("Creazione Categoria", "Categorie"),
    CATEGORY_UPDATE("Modifica Categoria", "Categorie"),
    CATEGORY_DELETE("Eliminazione Categoria", "Categorie"),
    STORE_CONFIG_UPDATE("Aggiornamento Connessione OpenCart", "Configurazione"),
    DUMMY_DATA_CLEARED("Eliminazione Dati Dimostrativi", "Database"),
    SYSTEM_LOGIN("Accesso Operatore", "Sicurezza")
}

data class AuditLog(
    val id: String,
    val timestamp: String,
    val timestampMillis: Long,
    val operatorUsername: String,
    val actionType: AuditActionType,
    val description: String,
    val details: String? = null,
    val deviceModel: String,
    val androidVersion: String,
    val appVersion: String = "1.1.2",
    val storeName: String = "TechGadgets Italy",
    val apiProfileUsed: String = "OpenCart Admin API (Direct Session)"
)
