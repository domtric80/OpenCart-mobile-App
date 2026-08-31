package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.Product
import com.example.model.ProductImageUpload
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.CardSurfaceLight
import com.example.ui.theme.CardSurfacePure
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusAlertRedBg
import com.example.ui.theme.StatusConfirmedBlue
import com.example.ui.theme.StatusConfirmedBlueBg
import com.example.ui.theme.StatusPendingGold
import com.example.ui.theme.StatusPendingGoldBg
import com.example.ui.theme.StatusShippedGreen
import com.example.ui.theme.StatusShippedGreenBg
import com.example.ui.theme.ThemeOnPrimaryContainer
import com.example.ui.theme.ThemeOutlineVariant
import com.example.ui.theme.ThemePrimary
import com.example.ui.theme.ThemePrimaryContainer
import com.example.ui.theme.ThemeSecondary
import com.example.ui.theme.ThemeSecondaryContainer
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CatalogTab(val label: String) {
    PRODUCTS("Prodotti"),
    CATEGORIES("Categorie")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    products: List<Product>,
    categories: List<Category>,
    onUpdateStock: (productId: String, delta: Int) -> Unit,
    onSetDirectStock: (productId: String, newQty: Int) -> Unit,
    onAddNewProduct: (
        name: String,
        model: String,
        sku: String,
        price: Double,
        specialPrice: Double?,
        quantity: Int,
        minAlert: Int,
        category: String,
        description: String,
        status: Boolean,
        image: ProductImageUpload?
    ) -> Unit,
    onUpdateProduct: (product: Product, image: ProductImageUpload?) -> Unit,
    onDeleteProduct: (productId: String) -> Unit,
    onToggleProductStatus: (productId: String) -> Unit,
    onAddNewCategory: (name: String, description: String, sortOrder: Int, status: Boolean) -> Unit,
    onUpdateCategory: (categoryId: String, name: String, description: String, sortOrder: Int, status: Boolean) -> Unit,
    onDeleteCategory: (categoryId: String) -> Unit,
    onToggleCategoryStatus: (categoryId: String) -> Unit,
    requestedTab: CatalogTab = CatalogTab.PRODUCTS,
    operationMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(requestedTab) }

    // Search and filter state
    var searchQuery by remember { mutableStateOf("") }
    var filterLowStockOnly by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Modals state
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(operationMessage) {
        operationMessage?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    LaunchedEffect(requestedTab) {
        selectedTab = requestedTab
    }

    val filteredProducts = products.filter { prod ->
        val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.model.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
        val matchesLowStock = !filterLowStockOnly || prod.quantity <= prod.minQuantityAlert
        val matchesCategory = selectedCategoryFilter == null || selectedCategoryFilter == "Tutte" || prod.category == selectedCategoryFilter
        matchesSearch && matchesLowStock && matchesCategory
    }

    val filteredCategories = categories.filter { cat ->
        searchQuery.isBlank() ||
                cat.name.contains(searchQuery, ignoreCase = true) ||
                cat.description.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Title & Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Catalogo OpenCart",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedTab == CatalogTab.PRODUCTS)
                            "${products.size} prodotti registrati"
                        else
                            "${categories.size} categorie attive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        if (selectedTab == CatalogTab.PRODUCTS) {
                            showAddProductDialog = true
                        } else {
                            showAddCategoryDialog = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("add_item_top_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedTab == CatalogTab.PRODUCTS) "Nuovo Prodotto" else "Nuova Categoria",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Segmented Tab Switcher (Prodotti / Categorie)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CatalogTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val count = if (tab == CatalogTab.PRODUCTS) products.size else categories.size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (tab == CatalogTab.PRODUCTS) Icons.Default.ShoppingBag else Icons.Default.Category,
                                contentDescription = null,
                                tint = if (isSelected) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${tab.label} ($count)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) ThemePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (selectedTab == CatalogTab.PRODUCTS)
                            "Cerca per nome, modello o SKU..."
                        else
                            "Cerca categoria..."
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancella", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Content Area depending on Tab
            when (selectedTab) {
                CatalogTab.PRODUCTS -> {
                    // Category & Low Stock Filter Pills
                    val categoryNames = listOf("Tutte") + categories.map { it.name }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Low Stock Alert Filter Chip
                        item {
                            FilterChip(
                                selected = filterLowStockOnly,
                                onClick = { filterLowStockOnly = !filterLowStockOnly },
                                label = { Text("⚠️ Scorte Basse") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertRedContainer,
                                    selectedLabelColor = AlertRed
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Category Chips
                        items(categoryNames) { cat ->
                            val isSelected = (selectedCategoryFilter == null && cat == "Tutte") || selectedCategoryFilter == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (cat == "Tutte") null else cat
                                },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Products List
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Nessun prodotto trovato",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        filterLowStockOnly = false
                                        selectedCategoryFilter = null
                                    }
                                ) {
                                    Text("Reimposta filtri")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                ProductManagementCard(
                                    product = product,
                                    onUpdateStock = { delta -> onUpdateStock(product.id, delta) },
                                    onEdit = { editingProduct = product },
                                    onDelete = { productToDelete = product },
                                    onToggleStatus = { onToggleProductStatus(product.id) }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                CatalogTab.CATEGORIES -> {
                    // Categories List
                    if (filteredCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Nessuna categoria trovata",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(filteredCategories, key = { it.id }) { category ->
                                CategoryManagementCard(
                                    category = category,
                                    onEdit = { editingCategory = category },
                                    onDelete = { categoryToDelete = category },
                                    onToggleStatus = { onToggleCategoryStatus(category.id) },
                                    onFilterProducts = {
                                        selectedCategoryFilter = category.name
                                        selectedTab = CatalogTab.PRODUCTS
                                    }
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- MODAL DIALOGS ---

    // 1. Add Product Dialog
    if (showAddProductDialog) {
        ProductFormDialog(
            title = "Aggiungi Prodotto OpenCart",
            categories = categories,
            initialProduct = null,
            onDismiss = { showAddProductDialog = false },
            onConfirm = { name, model, sku, price, specialPrice, qty, minAlert, category, desc, status, image ->
                onAddNewProduct(name, model, sku, price, specialPrice, qty, minAlert, category, desc, status, image)
                showAddProductDialog = false
            }
        )
    }

    // 2. Edit Product Dialog
    editingProduct?.let { prod ->
        ProductFormDialog(
            title = "Modifica Prodotto",
            categories = categories,
            initialProduct = prod,
            onDismiss = { editingProduct = null },
            onConfirm = { name, model, sku, price, specialPrice, qty, minAlert, category, desc, status, image ->
                val updated = prod.copy(
                    name = name,
                    model = model,
                    sku = sku,
                    price = price,
                    specialPrice = prod.specialPrice,
                    quantity = qty,
                    minQuantityAlert = minAlert,
                    category = category,
                    description = desc,
                    status = status
                )
                onUpdateProduct(updated, image)
                editingProduct = null
            }
        )
    }

    // 3. Delete Product Confirmation
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed) },
            title = { Text("Eliminare Prodotto?") },
            text = { Text("Sei sicuro di voler eliminare '${prod.name}' dal catalogo OpenCart? L'operazione rimuoverà il prodotto anche dalla cache locale.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProduct(prod.id)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    // 4. Add Category Dialog
    if (showAddCategoryDialog) {
        CategoryFormDialog(
            title = "Nuova Categoria OpenCart",
            initialCategory = null,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, desc, sortOrder, status ->
                onAddNewCategory(name, desc, sortOrder, status)
                showAddCategoryDialog = false
            }
        )
    }

    // 5. Edit Category Dialog
    editingCategory?.let { cat ->
        CategoryFormDialog(
            title = "Modifica Categoria",
            initialCategory = cat,
            onDismiss = { editingCategory = null },
            onConfirm = { name, desc, sortOrder, status ->
                onUpdateCategory(cat.id, name, desc, sortOrder, status)
                editingCategory = null
            }
        )
    }

    // 6. Delete Category Confirmation
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AlertRed) },
            title = { Text("Eliminare Categoria?") },
            text = { Text("Sei sicuro di voler eliminare la categoria '${cat.name}'? L'associazione verrà rimossa, ma i prodotti (${cat.productsCount}) resteranno nello store. Le categorie con sottocategorie devono essere gestite dal pannello OpenCart.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

/**
 * Product Item Card with stock adjuster, category badge, promotion tag, and quick actions.
 */
@Composable
private fun ProductManagementCard(
    product: Product,
    onUpdateStock: (delta: Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isLowStock = product.quantity <= product.minQuantityAlert
    val isOutOfStock = product.quantity <= 0

    val (stockBg, stockFg, stockLabel) = when {
        isOutOfStock -> Triple(StatusAlertRedBg, StatusAlertRed, "Esaurito (0 pz)")
        isLowStock -> Triple(StatusPendingGoldBg, StatusPendingGold, "Scorte Basse (${product.quantity} pz)")
        else -> Triple(StatusShippedGreenBg, StatusShippedGreen, "Disponibile (${product.quantity} pz)")
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category pill & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ThemeSecondary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = ThemeSecondary
                    )
                }

                // Stock status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stockBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stockLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = stockFg
                    )
                }
            }

            // Middle Row: Product Name, SKU, and Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (product.status) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Modello: ${product.model}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Price Column
                Column(horizontalAlignment = Alignment.End) {
                    if (product.specialPrice != null && product.specialPrice < product.price) {
                        Text(
                            text = "€%.2f".format(product.price),
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "€%.2f".format(product.specialPrice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusConfirmedBlue,
                                fontSize = 16.sp
                            )
                        )
                    } else {
                        Text(
                            text = "€%.2f".format(product.price),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bottom Actions Row: Stock Stepper & Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock adjustment stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateStock(-1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Diminuisci scorta", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "${product.quantity} pz",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { onUpdateStock(1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Aumenta scorta", modifier = Modifier.size(16.dp))
                    }
                }

                // Edit & Delete Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifica",
                            tint = ThemePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = AlertRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category Card with products count badge, status, and edit/delete actions.
 */
@Composable
private fun CategoryManagementCard(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onFilterProducts: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurfacePure),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFilterProducts() }
            .testTag("category_card_${category.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ThemePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = ThemePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = if (category.status) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                        if (category.description.isNotBlank()) {
                            Text(
                                text = category.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Products count pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ThemePrimaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${category.productsCount} prodotti",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeOnPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Bottom Actions: Status, View Products, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ordinamento: ${category.sortOrder} • ${if (category.status) "Attiva" else "Disattivata"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onFilterProducts,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Vedi Prodotti", fontSize = 12.sp, color = ThemePrimary)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = ThemePrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = AlertRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding or editing a product with OpenCart fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormDialog(
    title: String,
    categories: List<Category>,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        model: String,
        sku: String,
        price: Double,
        specialPrice: Double?,
        quantity: Int,
        minAlert: Int,
        category: String,
        description: String,
        status: Boolean,
        image: ProductImageUpload?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var model by remember { mutableStateOf(initialProduct?.model ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var priceText by remember { mutableStateOf(initialProduct?.price?.toString() ?: "") }
    var specialPriceText by remember { mutableStateOf(initialProduct?.specialPrice?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(initialProduct?.quantity?.toString() ?: "10") }
    var minAlertText by remember { mutableStateOf(initialProduct?.minQuantityAlert?.toString() ?: "5") }
    var selectedCategory by remember {
        mutableStateOf(initialProduct?.category ?: categories.firstOrNull()?.name ?: "Generale")
    }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var status by remember { mutableStateOf(initialProduct?.status ?: true) }
    var selectedImage by remember { mutableStateOf<ProductImageUpload?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun importImage(uri: Uri, fallbackName: String) {
        scope.launch {
            runCatching { readProductImage(context, uri, fallbackName) }
                .onSuccess { selectedImage = it; imageError = null }
                .onFailure { imageError = it.message ?: "Immagine non valida" }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importImage(it, "gallery-image") }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) importImage(uri, "camera-photo.jpg")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Prodotto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Immagine prodotto", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val file = File.createTempFile("product-camera-", ".jpg", context.cacheDir)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f).testTag("product_camera")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Text(" Fotocamera")
                    }
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).testTag("product_gallery")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text(" Galleria")
                    }
                }
                selectedImage?.let {
                    Text("Selezionata: ${it.fileName} (${it.bytes.size / 1024} KB)", color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { selectedImage = null }) { Text("Rimuovi immagine") }
                }
                imageError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                // Model & SKU in Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modello *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Codice SKU") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria Assegnata *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Prices: Regular & Special Discount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prezzo Listino (€) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = specialPriceText,
                        onValueChange = {},
                        label = { Text("Prezzo offerta (pannello OpenCart)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quantity & Minimum Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantità Scorta *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minAlertText,
                        onValueChange = { minAlertText = it },
                        label = { Text("Soglia Allerta (pz)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione breve") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stato Prodotto: ${if (status) "Attivo" else "Disabilitato"}", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = status, onCheckedChange = { status = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val special = specialPriceText.toDoubleOrNull()
                    val qty = quantityText.toIntOrNull() ?: 0
                    val minAlert = minAlertText.toIntOrNull() ?: 5
                    if (name.isNotBlank() && model.isNotBlank() && price > 0) {
                        onConfirm(name, model, sku, price, special, qty, minAlert, selectedCategory, description, status, selectedImage)
                    }
                },
                enabled = name.isNotBlank() && model.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Salva Prodotto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private suspend fun readProductImage(context: Context, uri: Uri, fallbackName: String): ProductImageUpload =
    withContext(Dispatchers.IO) {
        val maxBytes = 5 * 1024 * 1024
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "L'immagine supera il limite di 5 MB" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: error("Impossibile leggere l'immagine selezionata")
        val mime = when {
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
            bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "image/png"
            bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            else -> error("Formato non supportato: usa JPEG, PNG o WebP")
        }
        val extension = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        ProductImageUpload(bytes, mime, "${fallbackName.substringBeforeLast('.')}.$extension")
    }

/**
 * Dialog for adding or editing a category with OpenCart parameters.
 */
@Composable
private fun CategoryFormDialog(
    title: String,
    initialCategory: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, sortOrder: Int, status: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var description by remember { mutableStateOf(initialCategory?.description ?: "") }
    var sortOrderText by remember { mutableStateOf(initialCategory?.sortOrder?.toString() ?: "1") }
    var status by remember { mutableStateOf(initialCategory?.status ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Categoria *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione Categoria") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it },
                    label = { Text("Ordinamento (Sort Order)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stato Categoria: ${if (status) "Attiva" else "Disattivata"}", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = status, onCheckedChange = { status = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val order = sortOrderText.toIntOrNull() ?: 1
                    if (name.isNotBlank()) {
                        onConfirm(name, description, order, status)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salva Categoria")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
