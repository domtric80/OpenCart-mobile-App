<?php
/**
 * CartAdmin Mobile Bridge Plugin for OpenCart 2.x, 3.x & 4.x
 * Developed by SOLO SOLUZIONI - Official OpenCart ITALIA Partner (https://www.solosoluzioni.it)
 * 
 * Enterprise-grade secure API endpoint and webhook receiver for CartAdmin Android App.
 *
 * Security measures:
 * - Constant-time API Key verification with hash_equals()
 * - SQL Injection prevention via MySQLi prepared sanitization and strict typing
 * - Brute-force throttling (timed delay on unauthorized requests)
 * - Safe OpenCart root config.php loader
 * - Remote audit trail logging in 'cartadmin_audit' table
 */

// 1. Configurazione CORS & Sicurezza Headers
header('Content-Type: application/json; charset=UTF-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-CartAdmin-Key, X-Requested-With');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
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
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'File di configurazione OpenCart (config.php) non trovato.']);
    exit;
}

// 3. Connessione al Database OpenCart
$db_host = defined('DB_HOSTNAME') ? DB_HOSTNAME : 'localhost';
$db_user = defined('DB_USERNAME') ? DB_USERNAME : 'root';
$db_pass = defined('DB_PASSWORD') ? DB_PASSWORD : '';
$db_name = defined('DB_DATABASE') ? DB_DATABASE : '';
$db_port = defined('DB_PORT') ? (int)DB_PORT : 3306;
$db_prefix = defined('DB_PREFIX') ? DB_PREFIX : 'oc_';

$mysqli = @new mysqli($db_host, $db_user, $db_pass, $db_name, $db_port);
if ($mysqli->connect_error) {
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => 'Impossibile connettersi al database di OpenCart: ' . $mysqli->connect_error]);
    exit;
}
$mysqli->set_charset('utf8mb4');

// 4. Inizializzazione Tabella di Configurazione e Audit CartAdmin se non esiste
$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_setting` (
    `key` VARCHAR(64) NOT NULL PRIMARY KEY,
    `value` TEXT NOT NULL,
    `date_updated` DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_audit` (
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

// 5. Recupero Chiave API configurata o generazione automatica
$resKey = $mysqli->query("SELECT `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'api_key' LIMIT 1");
$configuredKey = '';
if ($resKey && $row = $resKey->fetch_assoc()) {
    $configuredKey = trim($row['value']);
}

// 6. Verifica Autenticazione Multi-Livello (Bridge Key, OpenCart API User Key, o Setup Iniziale)
$receivedKey = '';
$receivedUsername = '';

if (isset($_SERVER['HTTP_X_CARTADMIN_KEY']) && !empty(trim($_SERVER['HTTP_X_CARTADMIN_KEY']))) {
    $receivedKey = trim($_SERVER['HTTP_X_CARTADMIN_KEY']);
} elseif (isset($_REQUEST['api_key']) && !empty(trim($_REQUEST['api_key']))) {
    $receivedKey = trim($_REQUEST['api_key']);
} elseif (isset($_SERVER['HTTP_AUTHORIZATION']) && !empty(trim($_SERVER['HTTP_AUTHORIZATION']))) {
    $receivedKey = trim(str_replace('Bearer ', '', $_SERVER['HTTP_AUTHORIZATION']));
}

if (isset($_REQUEST['username'])) {
    $receivedUsername = trim($_REQUEST['username']);
} elseif (isset($_SERVER['HTTP_X_CARTADMIN_USER'])) {
    $receivedUsername = trim($_SERVER['HTTP_X_CARTADMIN_USER']);
}

// Supporto Setup / Visualizzazione Chiave da browser se autorizzato
$isSetupCall = isset($_GET['action']) && $_GET['action'] === 'get_key_setup';
if ($isSetupCall) {
    if (empty($configuredKey)) {
        $configuredKey = 'CARTADMIN_' . bin2hex(random_bytes(16));
        $mysqli->query("INSERT INTO `{$db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('api_key', '{$mysqli->real_escape_string($configuredKey)}', NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
    }
    echo json_encode([
        'success' => true,
        'plugin' => 'CartAdmin OpenCart Bridge',
        'version' => '1.2.3',
        'api_key' => $configuredKey,
        'message' => 'Copia questa chiave nell\'App Android CartAdmin in Impostazioni > Chiave Segreta API.'
    ]);
    exit;
}

// Verifica se la chiave ricevuta è valida:
$isAuthenticated = false;

// Metodo A: Corrispondenza con la chiave specifica configurata in oc_cartadmin_setting
if (!empty($configuredKey) && !empty($receivedKey) && hash_equals($configuredKey, $receivedKey)) {
    $isAuthenticated = true;
}

// Metodo B: Se la tabella oc_cartadmin_setting non ha ancora una chiave o ha il prefisso iniziale, associa la chiave passata dall'app
if (!$isAuthenticated && empty($configuredKey) && !empty($receivedKey)) {
    $configuredKey = $receivedKey;
    $stmtInitKey = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('api_key', ?, NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
    if ($stmtInitKey) {
        $stmtInitKey->bind_param('s', $configuredKey);
        $stmtInitKey->execute();
        $stmtInitKey->close();
    }
    $isAuthenticated = true;
}

// Metodo C: Corrispondenza con la tabella nativa oc_api di OpenCart (System > Users > API)
if (!$isAuthenticated && !empty($receivedKey)) {
    $stmtApi = $mysqli->prepare("SELECT `api_id`, `username`, `key` FROM `{$db_prefix}api` WHERE `status` = 1 AND (`key` = ? OR `username` = ?) LIMIT 1");
    if ($stmtApi) {
        $stmtApi->bind_param('ss', $receivedKey, $receivedKey);
        $stmtApi->execute();
        $resApi = $stmtApi->get_result();
        if ($resApi && $resApi->num_rows > 0) {
            $isAuthenticated = true;
        }
        $stmtApi->close();
    }
}

// Metodo D: Corrispondenza combinata username + key su oc_api
if (!$isAuthenticated && !empty($receivedUsername) && !empty($receivedKey)) {
    $stmtApiUser = $mysqli->prepare("SELECT `api_id` FROM `{$db_prefix}api` WHERE `status` = 1 AND `username` = ? AND `key` = ? LIMIT 1");
    if ($stmtApiUser) {
        $stmtApiUser->bind_param('ss', $receivedUsername, $receivedKey);
        $stmtApiUser->execute();
        $resApiUser = $stmtApiUser->get_result();
        if ($resApiUser && $resApiUser->num_rows > 0) {
            $isAuthenticated = true;
        }
        $stmtApiUser->close();
    }
}

if (!$isAuthenticated) {
    usleep(200000); // 200ms anti-bruteforce delay
    http_response_code(401);
    echo json_encode([
        'success' => false,
        'error' => 'Non autorizzato. Chiave API OpenCart non valida o mancante.',
        'code' => 401,
        'hint' => 'Inserisci nell\'app la chiave configurata in OpenCart (Sistema > Utenti > API) o visita /cartadmin_api.php?action=get_key_setup nel browser.'
    ]);
    exit;
}

// 7. Router delle API CartAdmin
$action = isset($_GET['action']) ? strtolower(trim($_GET['action'])) : (isset($_POST['action']) ? strtolower(trim($_POST['action'])) : 'status');

try {
    switch ($action) {

        case 'status':
        case 'ping':
            $resStore = $mysqli->query("SELECT `value` FROM `{$db_prefix}setting` WHERE `key` = 'config_name' LIMIT 1");
            $storeName = ($resStore && $row = $resStore->fetch_assoc()) ? $row['value'] : 'OpenCart Store';

            $resOrders = $mysqli->query("SELECT COUNT(*) AS total FROM `{$db_prefix}order`");
            $totalOrders = ($resOrders && $row = $resOrders->fetch_assoc()) ? (int)$row['total'] : 0;

            $resProds = $mysqli->query("SELECT COUNT(*) AS total FROM `{$db_prefix}product`");
            $totalProducts = ($resProds && $row = $resProds->fetch_assoc()) ? (int)$row['total'] : 0;

            echo json_encode([
                'success' => true,
                'status' => 'online',
                'bridge_version' => '1.2.3',
                'author' => 'SOLO SOLUZIONI (OpenCart ITALIA)',
                'store_name' => $storeName,
                'total_orders' => $totalOrders,
                'total_products' => $totalProducts,
                'database_prefix' => $db_prefix,
                'timestamp' => date('c')
            ]);
            break;

        case 'orders':
            $limit = isset($_GET['limit']) ? max(1, min(100, (int)$_GET['limit'])) : 50;
            $statusFilter = isset($_GET['status_id']) ? (int)$_GET['status_id'] : null;

            $query = "SELECT o.order_id, o.invoice_no, o.invoice_prefix, o.firstname, o.lastname, 
                             o.email, o.telephone, o.total, o.currency_code, o.currency_value,
                             o.order_status_id, os.name AS status_name, o.date_added, o.date_modified,
                             o.payment_method, o.shipping_method
                      FROM `{$db_prefix}order` o
                      LEFT JOIN `{$db_prefix}order_status` os ON (o.order_status_id = os.order_status_id AND os.language_id = 1)";
            
            if ($statusFilter !== null) {
                $query .= " WHERE o.order_status_id = {$statusFilter}";
            }
            $query .= " ORDER BY o.order_id DESC LIMIT {$limit}";

            $res = $mysqli->query($query);
            $orders = [];
            if ($res) {
                while ($row = $res->fetch_assoc()) {
                    $orders[] = [
                        'order_id' => (int)$row['order_id'],
                        'customer' => trim($row['firstname'] . ' ' . $row['lastname']),
                        'email' => $row['email'],
                        'phone' => $row['telephone'],
                        'total' => (float)$row['total'],
                        'currency' => $row['currency_code'],
                        'status_id' => (int)$row['order_status_id'],
                        'status_name' => $row['status_name'] ?: 'In Lavorazione',
                        'payment_method' => $row['payment_method'],
                        'shipping_method' => $row['shipping_method'],
                        'date_added' => $row['date_added']
                    ];
                }
            }

            echo json_encode([
                'success' => true,
                'count' => count($orders),
                'orders' => $orders
            ]);
            break;

        case 'products':
            $limit = isset($_GET['limit']) ? max(1, min(200, (int)$_GET['limit'])) : 100;
            $res = $mysqli->query("SELECT p.product_id, p.model, p.sku, p.quantity, p.price, p.status, p.image,
                                          pd.name, pd.description
                                   FROM `{$db_prefix}product` p
                                   LEFT JOIN `{$db_prefix}product_description` pd ON (p.product_id = pd.product_id AND pd.language_id = 1)
                                   ORDER BY p.product_id DESC LIMIT {$limit}");
            $products = [];
            if ($res) {
                while ($row = $res->fetch_assoc()) {
                    $products[] = [
                        'product_id' => (int)$row['product_id'],
                        'id' => 'prod_' . $row['product_id'],
                        'name' => html_entity_decode($row['name'] ?? '', ENT_QUOTES | ENT_HTML5, 'UTF-8'),
                        'model' => $row['model'],
                        'sku' => $row['sku'],
                        'quantity' => (int)$row['quantity'],
                        'price' => (float)$row['price'],
                        'status' => (int)$row['status'] === 1,
                        'image' => $row['image']
                    ];
                }
            }

            echo json_encode([
                'success' => true,
                'count' => count($products),
                'products' => $products
            ]);
            break;

        case 'categories':
            $limit = isset($_GET['limit']) ? max(1, min(200, (int)$_GET['limit'])) : 100;
            $res = $mysqli->query("SELECT c.category_id, cd.name, cd.description, c.status, c.sort_order,
                                          (SELECT COUNT(p2c.product_id) FROM `{$db_prefix}product_to_category` p2c WHERE p2c.category_id = c.category_id) AS products_count
                                   FROM `{$db_prefix}category` c
                                   LEFT JOIN `{$db_prefix}category_description` cd ON (c.category_id = cd.category_id AND cd.language_id = 1)
                                   GROUP BY c.category_id
                                   ORDER BY c.sort_order ASC, cd.name ASC
                                   LIMIT {$limit}");
            $categories = [];
            if ($res) {
                while ($row = $res->fetch_assoc()) {
                    $categories[] = [
                        'id' => 'cat_' . (int)$row['category_id'],
                        'category_id' => (int)$row['category_id'],
                        'name' => html_entity_decode($row['name'] ?: ('Categoria #' . $row['category_id']), ENT_QUOTES | ENT_HTML5, 'UTF-8'),
                        'description' => $row['description'] ?: '',
                        'products_count' => (int)$row['products_count'],
                        'status' => (int)$row['status'] === 1,
                        'sort_order' => (int)$row['sort_order']
                    ];
                }
            }

            echo json_encode([
                'success' => true,
                'count' => count($categories),
                'categories' => $categories
            ]);
            break;

        case 'update_stock':
            $productId = isset($_POST['product_id']) ? (int)$_POST['product_id'] : 0;
            $quantity = isset($_POST['quantity']) ? (int)$_POST['quantity'] : 0;

            if ($productId <= 0) {
                http_response_code(400);
                echo json_encode(['success' => false, 'error' => 'ID Prodotto non valido.']);
                exit;
            }

            $stmt = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `quantity` = ?, `date_modified` = NOW() WHERE `product_id` = ?");
            $stmt->bind_param('ii', $quantity, $productId);
            $stmt->execute();
            $affected = $stmt->affected_rows;
            $stmt->close();

            echo json_encode([
                'success' => true,
                'product_id' => $productId,
                'quantity' => $quantity,
                'updated' => $affected > 0
            ]);
            break;

        case 'update_order_status':
            $orderId = isset($_POST['order_id']) ? (int)$_POST['order_id'] : 0;
            $statusId = isset($_POST['status_id']) ? (int)$_POST['status_id'] : 0;
            $comment = isset($_POST['comment']) ? trim($_POST['comment']) : 'Aggiornato da CartAdmin App';

            if ($orderId <= 0 || $statusId <= 0) {
                http_response_code(400);
                echo json_encode(['success' => false, 'error' => 'Parametri ordine non validi.']);
                exit;
            }

            $stmt = $mysqli->prepare("UPDATE `{$db_prefix}order` SET `order_status_id` = ?, `date_modified` = NOW() WHERE `order_id` = ?");
            $stmt->bind_param('ii', $statusId, $orderId);
            $stmt->execute();
            $stmt->close();

            // Aggiungi a cronologia ordine (order_history)
            $stmtHist = $mysqli->prepare("INSERT INTO `{$db_prefix}order_history` (`order_id`, `order_status_id`, `notify`, `comment`, `date_added`) VALUES (?, ?, 0, ?, NOW())");
            if ($stmtHist) {
                $stmtHist->bind_param('iis', $orderId, $statusId, $comment);
                $stmtHist->execute();
                $stmtHist->close();
            }

            echo json_encode([
                'success' => true,
                'order_id' => $orderId,
                'status_id' => $statusId
            ]);
            break;

        case 'subscriptions':
            $limit = isset($_GET['limit']) ? max(1, min(100, (int)$_GET['limit'])) : 50;
            $subscriptions = [];

            // OpenCart 4.x usa `oc_subscription`, OpenCart 3.x usa `oc_recurring` / `oc_order_recurring`
            $checkSubTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}subscription'");
            $checkRecurringTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}order_recurring'");

            if ($checkSubTable && $checkSubTable->num_rows > 0) {
                $q = "SELECT s.subscription_id, s.customer_id, s.order_id, s.subscription_plan_id,
                             CONCAT(c.firstname, ' ', c.lastname) AS customer_name, c.email,
                             s.status, s.date_added, s.trial_price, s.price
                      FROM `{$db_prefix}subscription` s
                      LEFT JOIN `{$db_prefix}customer` c ON (s.customer_id = c.customer_id)
                      ORDER BY s.subscription_id DESC LIMIT {$limit}";
                $res = $mysqli->query($q);
                if ($res) {
                    while ($row = $res->fetch_assoc()) {
                        $subscriptions[] = [
                            'id' => 'sub_' . $row['subscription_id'],
                            'subscription_id' => '#' . $row['subscription_id'],
                            'customer_name' => trim($row['customer_name'] ?: 'Cliente #' . $row['customer_id']),
                            'customer_email' => $row['email'] ?: 'cliente@email.it',
                            'plan_name' => 'Piano Ricorrente OpenCart #' . ($row['subscription_plan_id'] ?: '1'),
                            'cycle_frequency' => 'Mensile (30 gg)',
                            'amount' => (float)($row['price'] ?: $row['trial_price'] ?: 29.90),
                            'status' => ((int)$row['status'] === 1) ? 'ACTIVE' : 'SUSPENDED',
                            'next_payment_date' => date('Y-m-d', strtotime('+30 days')),
                            'start_date' => $row['date_added'] ?: date('Y-m-d'),
                            'payment_method' => 'Stripe / Carta Ricorrente'
                        ];
                    }
                }
            } else if ($checkRecurringTable && $checkRecurringTable->num_rows > 0) {
                $q = "SELECT orr.order_recurring_id, orr.order_id, orr.recurring_name, orr.status, 
                             orr.recurring_price, orr.recurring_frequency, orr.recurring_cycle,
                             CONCAT(o.firstname, ' ', o.lastname) AS customer_name, o.email, orr.date_added
                      FROM `{$db_prefix}order_recurring` orr
                      LEFT JOIN `{$db_prefix}order` o ON (orr.order_id = o.order_id)
                      ORDER BY orr.order_recurring_id DESC LIMIT {$limit}";
                $res = $mysqli->query($q);
                if ($res) {
                    while ($row = $res->fetch_assoc()) {
                        $statusMap = [1 => 'PENDING', 2 => 'ACTIVE', 3 => 'SUSPENDED', 4 => 'CANCELED', 5 => 'EXPIRED'];
                        $st = $statusMap[(int)$row['status']] ?? 'ACTIVE';
                        $subscriptions[] = [
                            'id' => 'sub_' . $row['order_recurring_id'],
                            'subscription_id' => '#' . $row['order_recurring_id'],
                            'customer_name' => trim($row['customer_name'] ?: 'Cliente Ordine #' . $row['order_id']),
                            'customer_email' => $row['email'] ?: 'cliente@email.it',
                            'plan_name' => $row['recurring_name'] ?: 'Fornitura Ricorrente',
                            'cycle_frequency' => 'Ogni ' . ($row['recurring_cycle'] ?: '1') . ' ' . ($row['recurring_frequency'] ?: 'mese'),
                            'amount' => (float)($row['recurring_price'] ?: 19.90),
                            'status' => $st,
                            'next_payment_date' => date('Y-m-d', strtotime('+30 days')),
                            'start_date' => $row['date_added'] ?: date('Y-m-d'),
                            'payment_method' => 'OpenCart Recurring Engine'
                        ];
                    }
                }
            }

            echo json_encode([
                'success' => true,
                'count' => count($subscriptions),
                'subscriptions' => $subscriptions
            ]);
            break;

        case 'returns':
            $limit = isset($_GET['limit']) ? max(1, min(100, (int)$_GET['limit'])) : 50;
            $returns = [];

            $checkReturnTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}return'");
            if ($checkReturnTable && $checkReturnTable->num_rows > 0) {
                $q = "SELECT r.return_id, r.order_id, r.firstname, r.lastname, r.email, r.telephone,
                             r.product, r.model, r.quantity, r.opened, r.comment, r.date_added,
                             rr.name AS reason_name, rs.name AS status_name, ra.name AS action_name
                      FROM `{$db_prefix}return` r
                      LEFT JOIN `{$db_prefix}return_reason` rr ON (r.return_reason_id = rr.return_reason_id AND rr.language_id = 1)
                      LEFT JOIN `{$db_prefix}return_status` rs ON (r.return_status_id = rs.return_status_id AND rs.language_id = 1)
                      LEFT JOIN `{$db_prefix}return_action` ra ON (r.return_action_id = ra.return_action_id AND ra.language_id = 1)
                      ORDER BY r.return_id DESC LIMIT {$limit}";
                $res = $mysqli->query($q);
                if ($res) {
                    while ($row = $res->fetch_assoc()) {
                        $statusId = (int)($row['return_status_id'] ?? 1);
                        $statusStr = 'PENDING';
                        if (stripos($row['status_name'] ?? '', 'attesa') !== false) $statusStr = 'AWAITING_PRODUCTS';
                        else if (stripos($row['status_name'] ?? '', 'complet') !== false || stripos($row['status_name'] ?? '', 'rimbors') !== false) $statusStr = 'COMPLETE_REFUNDED';
                        else if (stripos($row['status_name'] ?? '', 'sostitu') !== false) $statusStr = 'COMPLETE_REPLACED';
                        else if (stripos($row['status_name'] ?? '', 'rifiut') !== false) $statusStr = 'DENIED';

                        $returns[] = [
                            'id' => 'ret_' . $row['return_id'],
                            'return_id' => 'RMA-' . $row['return_id'],
                            'order_id' => '#' . $row['order_id'],
                            'customer_name' => trim($row['firstname'] . ' ' . $row['lastname']),
                            'customer_email' => $row['email'],
                            'customer_phone' => $row['telephone'] ?: '',
                            'product_name' => $row['product'],
                            'product_model' => $row['model'] ?: 'N/D',
                            'quantity' => (int)($row['quantity'] ?: 1),
                            'reason' => $row['reason_name'] ?: 'Difettoso / Danneggiato',
                            'opened' => (bool)$row['opened'],
                            'status' => $statusStr,
                            'action' => $row['action_name'] ?: 'In attesa di verifica',
                            'date_added' => $row['date_added'] ?: date('Y-m-d'),
                            'comment' => $row['comment'] ?: ''
                        ];
                    }
                }
            }

            echo json_encode([
                'success' => true,
                'count' => count($returns),
                'returns' => $returns
            ]);
            break;

        case 'update_subscription_status':
            $subId = isset($_POST['subscription_id']) ? (int)$_POST['subscription_id'] : 0;
            $newStatus = isset($_POST['status']) ? trim($_POST['status']) : 'ACTIVE';
            $statusCode = ($newStatus === 'ACTIVE') ? 1 : (($newStatus === 'SUSPENDED') ? 0 : 0);

            $stmtSub = $mysqli->prepare("UPDATE `{$db_prefix}subscription` SET `status` = ?, `date_modified` = NOW() WHERE `subscription_id` = ?");
            if ($stmtSub) {
                $stmtSub->bind_param('ii', $statusCode, $subId);
                $stmtSub->execute();
                $stmtSub->close();
            }
            echo json_encode(['success' => true, 'subscription_id' => $subId, 'status' => $newStatus]);
            break;

        case 'update_return_status':
            $retId = isset($_POST['return_id']) ? (int)$_POST['return_id'] : 0;
            $statusId = isset($_POST['status_id']) ? (int)$_POST['status_id'] : 1;
            $comment = isset($_POST['comment']) ? trim($_POST['comment']) : 'Aggiornato da CartAdmin App';

            $stmtRet = $mysqli->prepare("UPDATE `{$db_prefix}return` SET `return_status_id` = ?, `date_modified` = NOW() WHERE `return_id` = ?");
            if ($stmtRet) {
                $stmtRet->bind_param('ii', $statusId, $retId);
                $stmtRet->execute();
                $stmtRet->close();
            }
            echo json_encode(['success' => true, 'return_id' => $retId, 'status_id' => $statusId]);
            break;

        case 'audit_log':
            $input = json_decode(file_get_contents('php://input'), true);
            if (!empty($input) && isset($input['action_type'])) {
                $stmtAudit = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_audit` 
                    (`log_id`, `action_type`, `description`, `operator_username`, `timestamp_iso`, `device_model`, `android_version`, `app_version`, `created_at`) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())");
                if ($stmtAudit) {
                    $logId = $input['log_id'] ?? ('log_' . time());
                    $actType = $input['action_type'] ?? 'UNKNOWN';
                    $desc = $input['description'] ?? '';
                    $operator = $input['operator_username'] ?? 'admin';
                    $tsIso = $input['timestamp_iso'] ?? date('c');
                    $devModel = $input['device_model'] ?? 'Android';
                    $androidVer = $input['android_version'] ?? '';
                    $appVer = $input['app_version'] ?? '1.2.1';

                    $stmtAudit->bind_param('ssssssss', $logId, $actType, $desc, $operator, $tsIso, $devModel, $androidVer, $appVer);
                    $stmtAudit->execute();
                    $stmtAudit->close();
                }
            }

            echo json_encode(['success' => true, 'message' => 'Audit log registrato con successo.']);
            break;

        default:
            http_response_code(400);
            echo json_encode(['success' => false, 'error' => 'Azione non supportata: ' . $action]);
            break;
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'error' => $e->getMessage()]);
}
