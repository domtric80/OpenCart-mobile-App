<?php
/**
 * CartAdmin Mobile Bridge Plugin for OpenCart 4.1.x
 * Developed by SOLO SOLUZIONI - Official OpenCart ITALIA Partner (https://www.solosoluzioni.it)
 * 
 * Enterprise-grade secure API endpoint and webhook receiver for CartAdmin Android App.
 *
 * Security measures:
 * - Non-reversible Argon2id/bcrypt token hashing in the OpenCart database
 * - SQL Injection prevention via MySQLi prepared statements (all parameters including LIMIT bound)
 * - Strict regex allowlist validation on DB_PREFIX
 * - Centralized sendJson() function with secure encoding and error masking
 * - Zero reflection of arbitrary unvalidated input (Semgrep compliant)
 * - Header-only credentials to prevent URL and form-data leakage
 * - No unauthenticated provisioning or first-request key adoption
 * - Brute-force throttling (timed delay on unauthorized requests)
 * - Safe OpenCart root config.php loader
 * - Remote audit trail logging in 'cartadmin_audit' table
 */

// 1. Configurazione Intestazioni di Sicurezza
header('Content-Type: application/json; charset=UTF-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');
header('Cache-Control: no-store');

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    sendJson(['success' => false, 'error' => 'Metodo non consentito.'], 405);
}

require_once __DIR__ . '/cartadmin_auth.php';

/**
 * Centralized secure JSON output handler
 */
function sendJson(array $data, int $statusCode = 200): void {
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
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

// 3. Connessione al Database OpenCart & Validazione Prefisso
$db_host = defined('DB_HOSTNAME') ? DB_HOSTNAME : 'localhost';
$db_user = defined('DB_USERNAME') ? DB_USERNAME : 'root';
$db_pass = defined('DB_PASSWORD') ? DB_PASSWORD : '';
$db_name = defined('DB_DATABASE') ? DB_DATABASE : '';
$db_port = defined('DB_PORT') ? (int)DB_PORT : 3306;

$rawPrefix = defined('DB_PREFIX') ? (string)DB_PREFIX : 'oc_';
$db_prefix = preg_match('/^[a-zA-Z0-9_]{1,32}$/', $rawPrefix) ? $rawPrefix : 'oc_';

$mysqli = @new mysqli($db_host, $db_user, $db_pass, $db_name, $db_port);
if ($mysqli->connect_error) {
    sendJson(['success' => false, 'error' => 'Impossibile stabilire la connessione al database OpenCart.'], 500);
}
$mysqli->set_charset('utf8mb4');

// 4. Inizializzazione Tabelle CartAdmin se non esistono
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

// 5. Recupero dell'hash del token configurato dal pannello amministrativo.
$resKey = $mysqli->query("SELECT `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'token_hash' LIMIT 1");
$configuredHash = '';
if ($resKey && $row = $resKey->fetch_assoc()) {
    $configuredHash = trim($row['value']);
}

// Migrazione una tantum: trasforma l'eventuale vecchio token in chiaro in un hash
// non reversibile e rimuove immediatamente il valore precedente dal database.
if ($configuredHash === '') {
    $resLegacy = $mysqli->query("SELECT `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'api_key' LIMIT 1");
    $legacyToken = ($resLegacy && $row = $resLegacy->fetch_assoc()) ? trim($row['value']) : '';

    if ($legacyToken !== '') {
        $configuredHash = cartadminHashToken($legacyToken);
        $stmtMigrate = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('token_hash', ?, NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");

        if ($stmtMigrate) {
            $stmtMigrate->bind_param('s', $configuredHash);
            $stmtMigrate->execute();
            $stmtMigrate->close();
            $mysqli->query("DELETE FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'api_key'");
        }
    }
}

// 6. Verifica autenticazione. Le credenziali in URL o nel form body non sono accettate.
[$receivedKey, $receivedUsername] = cartadminExtractCredentials($_SERVER);

$isAuthenticated = cartadminTokenMatches($configuredHash, $receivedKey);

if (!$isAuthenticated) {
    usleep(200000); // 200ms anti-bruteforce delay
    sendJson([
        'success' => false,
        'error' => 'Non autorizzato. Token CartAdmin non valido, mancante o non ancora configurato dal pannello OpenCart.',
        'code' => 401
    ], 401);
}

// 7. Router delle API CartAdmin
$rawAction = isset($_GET['action']) ? $_GET['action'] : (isset($_POST['action']) ? $_POST['action'] : 'status');
$action = is_string($rawAction) ? strtolower(trim($rawAction)) : 'status';

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

            sendJson([
                'success' => true,
                'status' => 'online',
                'bridge_version' => '1.2.6-dev.1',
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
            $hasStatusFilter = isset($_GET['status_id']) && is_numeric($_GET['status_id']);
            $statusFilter = $hasStatusFilter ? (int)$_GET['status_id'] : -1;

            if ($hasStatusFilter && $statusFilter >= 0) {
                $stmt = $mysqli->prepare("SELECT o.order_id, o.invoice_no, o.invoice_prefix, o.firstname, o.lastname, 
                                                 o.email, o.telephone, o.total, o.currency_code, o.currency_value,
                                                 o.order_status_id, os.name AS status_name, o.date_added, o.date_modified,
                                                 o.payment_method, o.shipping_method
                                          FROM `{$db_prefix}order` o
                                          LEFT JOIN `{$db_prefix}order_status` os ON (o.order_status_id = os.order_status_id AND os.language_id = 1)
                                          WHERE o.order_status_id = ?
                                          ORDER BY o.order_id DESC LIMIT ?");
                $stmt->bind_param('ii', $statusFilter, $limit);
            } else {
                $stmt = $mysqli->prepare("SELECT o.order_id, o.invoice_no, o.invoice_prefix, o.firstname, o.lastname, 
                                                 o.email, o.telephone, o.total, o.currency_code, o.currency_value,
                                                 o.order_status_id, os.name AS status_name, o.date_added, o.date_modified,
                                                 o.payment_method, o.shipping_method
                                          FROM `{$db_prefix}order` o
                                          LEFT JOIN `{$db_prefix}order_status` os ON (o.order_status_id = os.order_status_id AND os.language_id = 1)
                                          ORDER BY o.order_id DESC LIMIT ?");
                $stmt->bind_param('i', $limit);
            }

            $orders = [];
            if ($stmt) {
                $stmt->execute();
                $res = $stmt->get_result();
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
                $stmt->close();
            }

            sendJson([
                'success' => true,
                'count' => count($orders),
                'orders' => $orders
            ]);
            break;

        case 'products':
            $limit = isset($_GET['limit']) ? max(1, min(200, (int)$_GET['limit'])) : 100;
            $stmt = $mysqli->prepare("SELECT p.product_id, p.model, p.sku, p.quantity, p.price, p.status, p.image,
                                             pd.name, pd.description
                                      FROM `{$db_prefix}product` p
                                      LEFT JOIN `{$db_prefix}product_description` pd ON (p.product_id = pd.product_id AND pd.language_id = 1)
                                      ORDER BY p.product_id DESC LIMIT ?");
            $products = [];
            if ($stmt) {
                $stmt->bind_param('i', $limit);
                $stmt->execute();
                $res = $stmt->get_result();
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
                $stmt->close();
            }

            sendJson([
                'success' => true,
                'count' => count($products),
                'products' => $products
            ]);
            break;

        case 'categories':
            $limit = isset($_GET['limit']) ? max(1, min(200, (int)$_GET['limit'])) : 100;
            $stmt = $mysqli->prepare("SELECT c.category_id, cd.name, cd.description, c.status, c.sort_order,
                                             (SELECT COUNT(p2c.product_id) FROM `{$db_prefix}product_to_category` p2c WHERE p2c.category_id = c.category_id) AS products_count
                                      FROM `{$db_prefix}category` c
                                      LEFT JOIN `{$db_prefix}category_description` cd ON (c.category_id = cd.category_id AND cd.language_id = 1)
                                      GROUP BY c.category_id
                                      ORDER BY c.sort_order ASC, cd.name ASC
                                      LIMIT ?");
            $categories = [];
            if ($stmt) {
                $stmt->bind_param('i', $limit);
                $stmt->execute();
                $res = $stmt->get_result();
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
                $stmt->close();
            }

            sendJson([
                'success' => true,
                'count' => count($categories),
                'categories' => $categories
            ]);
            break;

        case 'update_stock':
            $productId = isset($_POST['product_id']) ? (int)$_POST['product_id'] : 0;
            $quantity = isset($_POST['quantity']) ? max(0, (int)$_POST['quantity']) : 0;

            if ($productId <= 0) {
                sendJson(['success' => false, 'error' => 'ID Prodotto non valido.'], 400);
            }

            $stmt = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `quantity` = ?, `date_modified` = NOW() WHERE `product_id` = ?");
            $affected = 0;
            if ($stmt) {
                $stmt->bind_param('ii', $quantity, $productId);
                $stmt->execute();
                $affected = $stmt->affected_rows;
                $stmt->close();
            }

            sendJson([
                'success' => true,
                'product_id' => $productId,
                'quantity' => $quantity,
                'updated' => $affected > 0
            ]);
            break;

        case 'update_order_status':
            $orderId = isset($_POST['order_id']) ? (int)$_POST['order_id'] : 0;
            $statusId = isset($_POST['status_id']) ? (int)$_POST['status_id'] : 0;
            $rawComment = isset($_POST['comment']) && is_string($_POST['comment']) ? trim($_POST['comment']) : 'Aggiornato da CartAdmin App';
            $comment = mb_substr(strip_tags($rawComment), 0, 255);

            if ($orderId <= 0 || $statusId <= 0) {
                sendJson(['success' => false, 'error' => 'Parametri ordine non validi.'], 400);
            }

            $stmt = $mysqli->prepare("UPDATE `{$db_prefix}order` SET `order_status_id` = ?, `date_modified` = NOW() WHERE `order_id` = ?");
            if ($stmt) {
                $stmt->bind_param('ii', $statusId, $orderId);
                $stmt->execute();
                $stmt->close();
            }

            $stmtHist = $mysqli->prepare("INSERT INTO `{$db_prefix}order_history` (`order_id`, `order_status_id`, `notify`, `comment`, `date_added`) VALUES (?, ?, 0, ?, NOW())");
            if ($stmtHist) {
                $stmtHist->bind_param('iis', $orderId, $statusId, $comment);
                $stmtHist->execute();
                $stmtHist->close();
            }

            sendJson([
                'success' => true,
                'order_id' => $orderId,
                'status_id' => $statusId
            ]);
            break;

        case 'subscriptions':
            $limit = isset($_GET['limit']) ? max(1, min(100, (int)$_GET['limit'])) : 50;
            $subscriptions = [];

            $checkSubTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}subscription'");
            $checkRecurringTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}order_recurring'");

            if ($checkSubTable && $checkSubTable->num_rows > 0) {
                $stmtSub = $mysqli->prepare("SELECT s.subscription_id, s.customer_id, s.order_id, s.subscription_plan_id,
                                                    CONCAT(c.firstname, ' ', c.lastname) AS customer_name, c.email,
                                                    s.status, s.date_added, s.trial_price, s.price
                                             FROM `{$db_prefix}subscription` s
                                             LEFT JOIN `{$db_prefix}customer` c ON (s.customer_id = c.customer_id)
                                             ORDER BY s.subscription_id DESC LIMIT ?");
                if ($stmtSub) {
                    $stmtSub->bind_param('i', $limit);
                    $stmtSub->execute();
                    $res = $stmtSub->get_result();
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
                    $stmtSub->close();
                }
            } elseif ($checkRecurringTable && $checkRecurringTable->num_rows > 0) {
                $stmtRec = $mysqli->prepare("SELECT orr.order_recurring_id, orr.order_id, orr.recurring_name, orr.status, 
                                                    orr.recurring_price, orr.recurring_frequency, orr.recurring_cycle,
                                                    CONCAT(o.firstname, ' ', o.lastname) AS customer_name, o.email, orr.date_added
                                             FROM `{$db_prefix}order_recurring` orr
                                             LEFT JOIN `{$db_prefix}order` o ON (orr.order_id = o.order_id)
                                             ORDER BY orr.order_recurring_id DESC LIMIT ?");
                if ($stmtRec) {
                    $stmtRec->bind_param('i', $limit);
                    $stmtRec->execute();
                    $res = $stmtRec->get_result();
                    if ($res) {
                        $statusMap = [1 => 'PENDING', 2 => 'ACTIVE', 3 => 'SUSPENDED', 4 => 'CANCELED', 5 => 'EXPIRED'];
                        while ($row = $res->fetch_assoc()) {
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
                    $stmtRec->close();
                }
            }

            sendJson([
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
                $stmtRet = $mysqli->prepare("SELECT r.return_id, r.order_id, r.firstname, r.lastname, r.email, r.telephone,
                                                    r.product, r.model, r.quantity, r.opened, r.comment, r.date_added,
                                                    rr.name AS reason_name, rs.name AS status_name, ra.name AS action_name
                                             FROM `{$db_prefix}return` r
                                             LEFT JOIN `{$db_prefix}return_reason` rr ON (r.return_reason_id = rr.return_reason_id AND rr.language_id = 1)
                                             LEFT JOIN `{$db_prefix}return_status` rs ON (r.return_status_id = rs.return_status_id AND rs.language_id = 1)
                                             LEFT JOIN `{$db_prefix}return_action` ra ON (r.return_action_id = ra.return_action_id AND ra.language_id = 1)
                                             ORDER BY r.return_id DESC LIMIT ?");
                if ($stmtRet) {
                    $stmtRet->bind_param('i', $limit);
                    $stmtRet->execute();
                    $res = $stmtRet->get_result();
                    if ($res) {
                        while ($row = $res->fetch_assoc()) {
                            $statusStr = 'PENDING';
                            if (stripos($row['status_name'] ?? '', 'attesa') !== false) $statusStr = 'AWAITING_PRODUCTS';
                            elseif (stripos($row['status_name'] ?? '', 'complet') !== false || stripos($row['status_name'] ?? '', 'rimbors') !== false) $statusStr = 'COMPLETE_REFUNDED';
                            elseif (stripos($row['status_name'] ?? '', 'sostitu') !== false) $statusStr = 'COMPLETE_REPLACED';
                            elseif (stripos($row['status_name'] ?? '', 'rifiut') !== false) $statusStr = 'DENIED';

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
                    $stmtRet->close();
                }
            }

            sendJson([
                'success' => true,
                'count' => count($returns),
                'returns' => $returns
            ]);
            break;

        case 'update_subscription_status':
            $subId = isset($_POST['subscription_id']) ? (int)$_POST['subscription_id'] : 0;
            $allowedStatuses = ['ACTIVE' => 1, 'SUSPENDED' => 0, 'PENDING' => 2, 'CANCELED' => 4, 'EXPIRED' => 5];
            $rawStatus = isset($_POST['status']) && is_string($_POST['status']) ? strtoupper(trim($_POST['status'])) : 'ACTIVE';
            $canonicalStatus = array_key_exists($rawStatus, $allowedStatuses) ? $rawStatus : 'ACTIVE';
            $statusCode = $allowedStatuses[$canonicalStatus];

            $stmtSub = $mysqli->prepare("UPDATE `{$db_prefix}subscription` SET `status` = ?, `date_modified` = NOW() WHERE `subscription_id` = ?");
            if ($stmtSub) {
                $stmtSub->bind_param('ii', $statusCode, $subId);
                $stmtSub->execute();
                $stmtSub->close();
            }
            sendJson(['success' => true, 'subscription_id' => $subId, 'status' => $canonicalStatus]);
            break;

        case 'update_return_status':
            $retId = isset($_POST['return_id']) ? (int)$_POST['return_id'] : 0;
            $statusId = isset($_POST['status_id']) ? max(1, min(10, (int)$_POST['status_id'])) : 1;

            $stmtRet = $mysqli->prepare("UPDATE `{$db_prefix}return` SET `return_status_id` = ?, `date_modified` = NOW() WHERE `return_id` = ?");
            if ($stmtRet) {
                $stmtRet->bind_param('ii', $statusId, $retId);
                $stmtRet->execute();
                $stmtRet->close();
            }
            sendJson(['success' => true, 'return_id' => $retId, 'status_id' => $statusId]);
            break;

        case 'audit_log':
            $input = json_decode(file_get_contents('php://input'), true);
            if (!empty($input) && isset($input['action_type']) && is_string($input['action_type'])) {
                $stmtAudit = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_audit` 
                    (`log_id`, `action_type`, `description`, `operator_username`, `timestamp_iso`, `device_model`, `android_version`, `app_version`, `created_at`) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())");
                if ($stmtAudit) {
                    $logId = mb_substr(strip_tags((string)($input['log_id'] ?? ('log_' . time()))), 0, 64);
                    $actType = mb_substr(strip_tags((string)($input['action_type'] ?? 'UNKNOWN')), 0, 64);
                    $desc = mb_substr(strip_tags((string)($input['description'] ?? '')), 0, 255);
                    $operator = mb_substr(strip_tags((string)($input['operator_username'] ?? 'admin')), 0, 64);
                    $tsIso = mb_substr(strip_tags((string)($input['timestamp_iso'] ?? date('c'))), 0, 64);
                    $devModel = mb_substr(strip_tags((string)($input['device_model'] ?? 'Android')), 0, 128);
                    $androidVer = mb_substr(strip_tags((string)($input['android_version'] ?? '')), 0, 64);
                    $appVer = mb_substr(strip_tags((string)($input['app_version'] ?? '1.2.4')), 0, 32);

                    $stmtAudit->bind_param('ssssssss', $logId, $actType, $desc, $operator, $tsIso, $devModel, $androidVer, $appVer);
                    $stmtAudit->execute();
                    $stmtAudit->close();
                }
            }

            sendJson(['success' => true, 'message' => 'Audit log registrato con successo.']);
            break;

        default:
            sendJson(['success' => false, 'error' => 'Azione non supportata.'], 400);
            break;
    }
} catch (Throwable $e) {
    sendJson(['success' => false, 'error' => 'Errore interno del server durante l\'elaborazione.'], 500);
}
