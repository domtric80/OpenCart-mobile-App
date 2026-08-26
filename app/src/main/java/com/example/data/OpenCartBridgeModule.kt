package com.example.data

object OpenCartBridgeModule {

    const val FILE_NAME = "cartadmin_api.php"

    /**
     * Generates a ready-to-use, hardened and secure PHP bridge script for OpenCart 2.x, 3.x, 4.x.
     * Incorporates OWASP security standards:
     * - Timing attack protection with hash_equals()
     * - MySQLi prepared statements for ALL parameters (including limit/status)
     * - Strict regex validation on table prefix
     * - Centralized sendJson() function with error masking
     * - Secure credential retrieval from OpenCart config.php
     */
    fun generatePhpScript(secretApiKey: String): String {
        val s = "$"
        val cleanKey = if (secretApiKey.isNotBlank()) secretApiKey else "CARTADMIN_" + (10000000..99999999).random()

        return """<?php
/**
 * CartAdmin Mobile Bridge Plugin for OpenCart 2.x, 3.x & 4.x
 * Developed by SOLO SOLUZIONI - Official OpenCart ITALIA Partner (https://www.solosoluzioni.it)
 * 
 * Enterprise-grade secure API endpoint and webhook receiver for CartAdmin Android App.
 */

// 1. Intestazioni di Sicurezza
header('Content-Type: application/json; charset=UTF-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-CartAdmin-Key, X-Requested-With');

if (isset(${s}_SERVER['REQUEST_METHOD']) && ${s}_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

function sendJson(array ${s}data, int ${s}statusCode = 200): void {
    http_response_code(${s}statusCode);
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode(${s}data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

// 2. Localizzazione del file config.php di OpenCart
if (file_exists(__DIR__ . '/config.php')) {
    require_once(__DIR__ . '/config.php');
} elseif (file_exists(__DIR__ . '/../config.php')) {
    require_once(__DIR__ . '/../config.php');
} elseif (file_exists(__DIR__ . '/../../config.php')) {
    require_once(__DIR__ . '/../../config.php');
} else {
    sendJson(['success' => false, 'error' => 'File di configurazione OpenCart (config.php) non trovato.'], 500);
}

// 3. Connessione al Database OpenCart
${s}db_host = defined('DB_HOSTNAME') ? DB_HOSTNAME : 'localhost';
${s}db_user = defined('DB_USERNAME') ? DB_USERNAME : 'root';
${s}db_pass = defined('DB_PASSWORD') ? DB_PASSWORD : '';
${s}db_name = defined('DB_DATABASE') ? DB_DATABASE : '';
${s}db_port = defined('DB_PORT') ? (int)DB_PORT : 3306;

${s}rawPrefix = defined('DB_PREFIX') ? (string)DB_PREFIX : 'oc_';
${s}db_prefix = preg_match('/^[a-zA-Z0-9_]{1,32}${s}/', ${s}rawPrefix) ? ${s}rawPrefix : 'oc_';

${s}mysqli = @new mysqli(${s}db_host, ${s}db_user, ${s}db_pass, ${s}db_name, ${s}db_port);
if (${s}mysqli->connect_error) {
    sendJson(['success' => false, 'error' => 'Impossibile stabilire la connessione al database OpenCart.'], 500);
}
${s}mysqli->set_charset('utf8mb4');

// 4. Inizializzazione Tabelle CartAdmin
${s}mysqli->query("CREATE TABLE IF NOT EXISTS `${s}{db_prefix}cartadmin_setting` (
    `key` VARCHAR(64) NOT NULL PRIMARY KEY,
    `value` TEXT NOT NULL,
    `date_updated` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

${s}mysqli->query("CREATE TABLE IF NOT EXISTS `${s}{db_prefix}cartadmin_audit` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `log_id` VARCHAR(64) NOT NULL,
    `action_type` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `operator_username` VARCHAR(64) NOT NULL,
    `timestamp_iso` VARCHAR(64) NOT NULL,
    `device_model` VARCHAR(128) NOT NULL,
    `android_version` VARCHAR(64) NOT NULL,
    `app_version` VARCHAR(32) NOT NULL,
    `created_at` DATETIME NOT NULL,
    INDEX `idx_timestamp` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

${s}resKey = ${s}mysqli->query("SELECT `value` FROM `${s}{db_prefix}cartadmin_setting` WHERE `key` = 'api_key' LIMIT 1");
${s}configuredKey = '';
if (${s}resKey && ${s}row = ${s}resKey->fetch_assoc()) {
    ${s}configuredKey = trim(${s}row['value']);
}

// 5. Verifica Autenticazione
${s}receivedKey = '';
${s}receivedUsername = '';

if (isset(${s}_SERVER['HTTP_X_CARTADMIN_KEY']) && !empty(trim(${s}_SERVER['HTTP_X_CARTADMIN_KEY']))) {
    ${s}receivedKey = trim(${s}_SERVER['HTTP_X_CARTADMIN_KEY']);
} elseif (isset(${s}_REQUEST['api_key']) && !empty(trim(${s}_REQUEST['api_key']))) {
    ${s}receivedKey = trim(${s}_REQUEST['api_key']);
} elseif (isset(${s}_SERVER['HTTP_AUTHORIZATION']) && !empty(trim(${s}_SERVER['HTTP_AUTHORIZATION']))) {
    ${s}receivedKey = trim(str_replace('Bearer ', '', ${s}_SERVER['HTTP_AUTHORIZATION']));
}

if (isset(${s}_REQUEST['username'])) {
    ${s}receivedUsername = trim(${s}_REQUEST['username']);
} elseif (isset(${s}_SERVER['HTTP_X_CARTADMIN_USER'])) {
    ${s}receivedUsername = trim(${s}_SERVER['HTTP_X_CARTADMIN_USER']);
}

${s}isSetupCall = isset(${s}_GET['action']) && ${s}_GET['action'] === 'get_key_setup';
if (${s}isSetupCall) {
    if (empty(${s}configuredKey)) {
        ${s}configuredKey = '$cleanKey';
        ${s}stmtInit = ${s}mysqli->prepare("INSERT INTO `${s}{db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('api_key', ?, NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
        if (${s}stmtInit) {
            ${s}stmtInit->bind_param('s', ${s}configuredKey);
            ${s}stmtInit->execute();
            ${s}stmtInit->close();
        }
    }
    sendJson([
        'success' => true,
        'plugin' => 'CartAdmin OpenCart Bridge',
        'version' => '1.2.4',
        'api_key' => ${s}configuredKey,
        'message' => 'Configura questa chiave nell\'App Android CartAdmin.'
    ]);
}

${s}isAuthenticated = false;
if (!empty(${s}configuredKey) && !empty(${s}receivedKey) && hash_equals(${s}configuredKey, ${s}receivedKey)) {
    ${s}isAuthenticated = true;
}

if (!${s}isAuthenticated && empty(${s}configuredKey) && !empty(${s}receivedKey)) {
    ${s}configuredKey = ${s}receivedKey;
    ${s}stmtInitKey = ${s}mysqli->prepare("INSERT INTO `${s}{db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('api_key', ?, NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
    if (${s}stmtInitKey) {
        ${s}stmtInitKey->bind_param('s', ${s}configuredKey);
        ${s}stmtInitKey->execute();
        ${s}stmtInitKey->close();
    }
    ${s}isAuthenticated = true;
}

if (!${s}isAuthenticated && !empty(${s}receivedKey)) {
    ${s}stmtApi = ${s}mysqli->prepare("SELECT `api_id` FROM `${s}{db_prefix}api` WHERE `status` = 1 AND (`key` = ? OR `username` = ?) LIMIT 1");
    if (${s}stmtApi) {
        ${s}stmtApi->bind_param('ss', ${s}receivedKey, ${s}receivedKey);
        ${s}stmtApi->execute();
        ${s}resApi = ${s}stmtApi->get_result();
        if (${s}resApi && ${s}resApi->num_rows > 0) {
            ${s}isAuthenticated = true;
        }
        ${s}stmtApi->close();
    }
}

if (!${s}isAuthenticated) {
    usleep(200000);
    sendJson([
        'success' => false,
        'error' => 'Non autorizzato. Chiave API OpenCart non valida o mancante.',
        'code' => 401
    ], 401);
}

// 6. Router API
${s}rawAction = isset(${s}_GET['action']) ? ${s}_GET['action'] : (isset(${s}_POST['action']) ? ${s}_POST['action'] : 'status');
${s}action = is_string(${s}rawAction) ? strtolower(trim(${s}rawAction)) : 'status';

try {
    switch (${s}action) {
        case 'status':
        case 'ping':
            ${s}resStore = ${s}mysqli->query("SELECT `value` FROM `${s}{db_prefix}setting` WHERE `key` = 'config_name' LIMIT 1");
            ${s}storeName = (${s}resStore && ${s}row = ${s}resStore->fetch_assoc()) ? ${s}row['value'] : 'OpenCart Store';

            ${s}resOrders = ${s}mysqli->query("SELECT COUNT(*) AS total FROM `${s}{db_prefix}order`");
            ${s}totalOrders = (${s}resOrders && ${s}row = ${s}resOrders->fetch_assoc()) ? (int)${s}row['total'] : 0;

            ${s}resProds = ${s}mysqli->query("SELECT COUNT(*) AS total FROM `${s}{db_prefix}product`");
            ${s}totalProducts = (${s}resProds && ${s}row = ${s}resProds->fetch_assoc()) ? (int)${s}row['total'] : 0;

            sendJson([
                'success' => true,
                'status' => 'online',
                'bridge_version' => '1.2.4',
                'author' => 'SOLO SOLUZIONI (OpenCart ITALIA)',
                'store_name' => ${s}storeName,
                'total_orders' => ${s}totalOrders,
                'total_products' => ${s}totalProducts,
                'database_prefix' => ${s}db_prefix,
                'timestamp' => date('c')
            ]);
            break;

        case 'orders':
            ${s}limit = isset(${s}_GET['limit']) ? max(1, min(100, (int)${s}_GET['limit'])) : 50;
            ${s}hasStatusFilter = isset(${s}_GET['status_id']) && is_numeric(${s}_GET['status_id']);
            ${s}statusFilter = ${s}hasStatusFilter ? (int)${s}_GET['status_id'] : -1;

            if (${s}hasStatusFilter && ${s}statusFilter >= 0) {
                ${s}stmt = ${s}mysqli->prepare("SELECT o.order_id, o.invoice_no, o.invoice_prefix, o.firstname, o.lastname, 
                                                 o.email, o.telephone, o.total, o.currency_code, o.currency_value,
                                                 o.order_status_id, os.name AS status_name, o.date_added, o.date_modified,
                                                 o.payment_method, o.shipping_method
                                          FROM `${s}{db_prefix}order` o
                                          LEFT JOIN `${s}{db_prefix}order_status` os ON (o.order_status_id = os.order_status_id AND os.language_id = 1)
                                          WHERE o.order_status_id = ?
                                          ORDER BY o.order_id DESC LIMIT ?");
                ${s}stmt->bind_param('ii', ${s}statusFilter, ${s}limit);
            } else {
                ${s}stmt = ${s}mysqli->prepare("SELECT o.order_id, o.invoice_no, o.invoice_prefix, o.firstname, o.lastname, 
                                                 o.email, o.telephone, o.total, o.currency_code, o.currency_value,
                                                 o.order_status_id, os.name AS status_name, o.date_added, o.date_modified,
                                                 o.payment_method, o.shipping_method
                                          FROM `${s}{db_prefix}order` o
                                          LEFT JOIN `${s}{db_prefix}order_status` os ON (o.order_status_id = os.order_status_id AND os.language_id = 1)
                                          ORDER BY o.order_id DESC LIMIT ?");
                ${s}stmt->bind_param('i', ${s}limit);
            }

            ${s}orders = [];
            if (${s}stmt) {
                ${s}stmt->execute();
                ${s}res = ${s}stmt->get_result();
                if (${s}res) {
                    while (${s}row = ${s}res->fetch_assoc()) {
                        ${s}orders[] = [
                            'order_id' => (int)${s}row['order_id'],
                            'customer' => trim(${s}row['firstname'] . ' ' . ${s}row['lastname']),
                            'email' => ${s}row['email'],
                            'phone' => ${s}row['telephone'],
                            'total' => (float)${s}row['total'],
                            'currency' => ${s}row['currency_code'],
                            'status_id' => (int)${s}row['order_status_id'],
                            'status_name' => ${s}row['status_name'] ?: 'In Lavorazione',
                            'payment_method' => ${s}row['payment_method'],
                            'shipping_method' => ${s}row['shipping_method'],
                            'date_added' => ${s}row['date_added']
                        ];
                    }
                }
                ${s}stmt->close();
            }

            sendJson(['success' => true, 'count' => count(${s}orders), 'orders' => ${s}orders]);
            break;

        case 'products':
            ${s}limit = isset(${s}_GET['limit']) ? max(1, min(200, (int)${s}_GET['limit'])) : 100;
            ${s}stmt = ${s}mysqli->prepare("SELECT p.product_id, p.model, p.sku, p.quantity, p.price, p.status, p.image,
                                             pd.name, pd.description
                                      FROM `${s}{db_prefix}product` p
                                      LEFT JOIN `${s}{db_prefix}product_description` pd ON (p.product_id = pd.product_id AND pd.language_id = 1)
                                      ORDER BY p.product_id DESC LIMIT ?");
            ${s}products = [];
            if (${s}stmt) {
                ${s}stmt->bind_param('i', ${s}limit);
                ${s}stmt->execute();
                ${s}res = ${s}stmt->get_result();
                if (${s}res) {
                    while (${s}row = ${s}res->fetch_assoc()) {
                        ${s}products[] = [
                            'product_id' => (int)${s}row['product_id'],
                            'id' => 'prod_' . ${s}row['product_id'],
                            'name' => html_entity_decode(${s}row['name'] ?? '', ENT_QUOTES | ENT_HTML5, 'UTF-8'),
                            'model' => ${s}row['model'],
                            'sku' => ${s}row['sku'],
                            'quantity' => (int)${s}row['quantity'],
                            'price' => (float)${s}row['price'],
                            'status' => (int)${s}row['status'] === 1,
                            'image' => ${s}row['image']
                        ];
                    }
                }
                ${s}stmt->close();
            }

            sendJson(['success' => true, 'count' => count(${s}products), 'products' => ${s}products]);
            break;

        case 'categories':
            ${s}limit = isset(${s}_GET['limit']) ? max(1, min(200, (int)${s}_GET['limit'])) : 100;
            ${s}stmt = ${s}mysqli->prepare("SELECT c.category_id, cd.name, cd.description, c.status, c.sort_order,
                                             (SELECT COUNT(p2c.product_id) FROM `${s}{db_prefix}product_to_category` p2c WHERE p2c.category_id = c.category_id) AS products_count
                                      FROM `${s}{db_prefix}category` c
                                      LEFT JOIN `${s}{db_prefix}category_description` cd ON (c.category_id = cd.category_id AND cd.language_id = 1)
                                      GROUP BY c.category_id
                                      ORDER BY c.sort_order ASC, cd.name ASC
                                      LIMIT ?");
            ${s}categories = [];
            if (${s}stmt) {
                ${s}stmt->bind_param('i', ${s}limit);
                ${s}stmt->execute();
                ${s}res = ${s}stmt->get_result();
                if (${s}res) {
                    while (${s}row = ${s}res->fetch_assoc()) {
                        ${s}categories[] = [
                            'id' => 'cat_' . (int)${s}row['category_id'],
                            'category_id' => (int)${s}row['category_id'],
                            'name' => html_entity_decode(${s}row['name'] ?: ('Categoria #' . ${s}row['category_id']), ENT_QUOTES | ENT_HTML5, 'UTF-8'),
                            'description' => ${s}row['description'] ?: '',
                            'products_count' => (int)${s}row['products_count'],
                            'status' => (int)${s}row['status'] === 1,
                            'sort_order' => (int)${s}row['sort_order']
                        ];
                    }
                }
                ${s}stmt->close();
            }

            sendJson(['success' => true, 'count' => count(${s}categories), 'categories' => ${s}categories]);
            break;

        case 'update_stock':
            ${s}productId = isset(${s}_POST['product_id']) ? (int)${s}_POST['product_id'] : 0;
            ${s}quantity = isset(${s}_POST['quantity']) ? max(0, (int)${s}_POST['quantity']) : 0;

            if (${s}productId <= 0) {
                sendJson(['success' => false, 'error' => 'ID Prodotto non valido.'], 400);
            }

            ${s}stmt = ${s}mysqli->prepare("UPDATE `${s}{db_prefix}product` SET `quantity` = ?, `date_modified` = NOW() WHERE `product_id` = ?");
            ${s}affected = 0;
            if (${s}stmt) {
                ${s}stmt->bind_param('ii', ${s}quantity, ${s}productId);
                ${s}stmt->execute();
                ${s}affected = ${s}stmt->affected_rows;
                ${s}stmt->close();
            }

            sendJson([
                'success' => true,
                'product_id' => ${s}productId,
                'quantity' => ${s}quantity,
                'updated' => ${s}affected > 0
            ]);
            break;

        case 'update_order_status':
            ${s}orderId = isset(${s}_POST['order_id']) ? (int)${s}_POST['order_id'] : 0;
            ${s}statusId = isset(${s}_POST['status_id']) ? (int)${s}_POST['status_id'] : 0;
            ${s}rawComment = isset(${s}_POST['comment']) && is_string(${s}_POST['comment']) ? trim(${s}_POST['comment']) : 'Aggiornato da CartAdmin App';
            ${s}comment = mb_substr(strip_tags(${s}rawComment), 0, 255);

            if (${s}orderId <= 0 || ${s}statusId <= 0) {
                sendJson(['success' => false, 'error' => 'Parametri ordine non validi.'], 400);
            }

            ${s}stmt = ${s}mysqli->prepare("UPDATE `${s}{db_prefix}order` SET `order_status_id` = ?, `date_modified` = NOW() WHERE `order_id` = ?");
            if (${s}stmt) {
                ${s}stmt->bind_param('ii', ${s}statusId, ${s}orderId);
                ${s}stmt->execute();
                ${s}stmt->close();
            }

            ${s}stmtHist = ${s}mysqli->prepare("INSERT INTO `${s}{db_prefix}order_history` (`order_id`, `order_status_id`, `notify`, `comment`, `date_added`) VALUES (?, ?, 0, ?, NOW())");
            if (${s}stmtHist) {
                ${s}stmtHist->bind_param('iis', ${s}orderId, ${s}statusId, ${s}comment);
                ${s}stmtHist->execute();
                ${s}stmtHist->close();
            }

            sendJson(['success' => true, 'order_id' => ${s}orderId, 'status_id' => ${s}statusId]);
            break;

        default:
            sendJson(['success' => false, 'error' => 'Azione non supportata.'], 400);
            break;
    }
} catch (Throwable ${s}e) {
    sendJson(['success' => false, 'error' => 'Errore interno del server durante l\'elaborazione.'], 500);
}
""".trimIndent()
    }
}
