package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiEcomAssistant {

    suspend fun generateProductDescription(productName: String, category: String, keywords: String): String = withContext(Dispatchers.IO) {
        return@withContext """
            **$productName** - Eccellenza e qualità per il tuo stile.
            
            Scopri $productName, progettato appositamente per chi cerca affidabilità, design contemporaneo e prestazioni senza compromessi nella categoria $category.
            
            **Punti di Forza:**
            • Materiali selezionati di prima qualità per una lunga durata nel tempo ($keywords)
            • Design ergonomico ed elegante, ideale per l'uso quotidiano
            • Massima efficienza e cura meticolosa delle finiture
            • Garanzia ufficiale OpenCart Store e spedizione rapida con tracciamento
            
            **Meta Description Consigliata per OpenCart:**
            Acquista online $productName al miglior prezzo. Qualità garantita, spedizione veloce in 24/48h e pagamenti sicuri nel nostro store ufficiale.
        """.trimIndent()
    }

    suspend fun generatePromoSocialPost(productName: String, price: Double, specialPrice: Double?): String = withContext(Dispatchers.IO) {
        val priceText = if (specialPrice != null && specialPrice < price) {
            "In super offerta a €%.2f (invece di €%.2f)".format(specialPrice, price)
        } else {
            "Disponibile a soli €%.2f".format(price)
        }

        return@withContext """
            🔥 NOVITÀ SULLO STORE! 🔥
            
            Non lasciarti sfuggire il nuovo **$productName**!
            $priceText 🎉
            
            ✨ Qualità garantita, disponibilità limitata in magazzino.
            📦 Spedizione express e reso facile in 14 giorni.
            
            👉 Clicca sul link per ordinare subito prima che finiscano le scorte!
            
            #OpenCart #ShoppingOnline #Offerte #$productName #ECommerce #Promozione
        """.trimIndent()
    }

    suspend fun analyzeStorePerformance(
        storeName: String,
        todaySales: Double,
        monthSales: Double,
        pendingOrders: Int,
        lowStockItems: Int
    ): String = withContext(Dispatchers.IO) {
        return@withContext """
            📊 **Audit Strategico E-Commerce per '$storeName'**:
            
            1. 📦 **Priorità Evasione Ordini**: Hai $pendingOrders ordini in lavorazione o attesa. L'evasione tempestiva entro 24 ore incrementa del 35% il tasso di riacquisto e le recensioni positive a 5 stelle.
            2. ⚠️ **Rifornimento Magazzino**: Rilevati $lowStockItems prodotti sottoscorta o a rischio esaurimento. Consigliato riordinare subito dai fornitori per evitare vendite mancate su articoli ad alta rotazione.
            3. 💰 **Andamento Vendite**: Fatturato mensile a €%.2f. Ottimo momento per lanciare un coupon sconto con scadenza a 48 ore per recuperare i carrelli abbandonati.
            4. 🚀 **Ottimizzazione OpenCart**: Esegui periodicamente la pulizia della cache Twig e OCMod dalla sezione Strumenti per mantenere il tempo di caricamento del catalogo sotto i 2 secondi.
        """.trimIndent().format(monthSales)
    }
}
