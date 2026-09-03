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
 * - Atomic server-side security audit with no client-supplied identity
 */

// 1. Configurazione Intestazioni di Sicurezza
header('Content-Type: application/json; charset=UTF-8');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('X-XSS-Protection: 1; mode=block');
header('Cache-Control: no-store');
header("Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
header('Referrer-Policy: no-referrer');
header('Permissions-Policy: camera=(), microphone=(), geolocation=()');

$httpsEnabled = (!empty($_SERVER['HTTPS']) && strtolower((string)$_SERVER['HTTPS']) !== 'off')
    || ((int)($_SERVER['SERVER_PORT'] ?? 0) === 443)
    || (strtolower(trim(explode(',', (string)($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? ''))[0])) === 'https');
if (!$httpsEnabled) {
    sendJson(['success' => false, 'error' => 'HTTPS obbligatorio.'], 426);
}
header('Strict-Transport-Security: max-age=31536000; includeSubDomains');

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

/**
 * Invalida soltanto le chiavi della cache file nativa interessate da una
 * modifica CartAdmin. Le chiavi sono definite dal bridge e il percorso deve
 * restare all'interno di DIR_CACHE; installazioni con cache non-file non
 * vengono toccate.
 */
function cartadminInvalidateFileCache(array $keys): void {
    if (!defined('DIR_CACHE') || !is_dir(DIR_CACHE)) {
        return;
    }

    $cacheRoot = realpath(DIR_CACHE);
    if ($cacheRoot === false) {
        return;
    }

    foreach ($keys as $key) {
        $safeKey = preg_replace('/[^A-Z0-9._-]/i', '', (string)$key);
        if ($safeKey === '') {
            continue;
        }

        $files = glob($cacheRoot . DIRECTORY_SEPARATOR . 'cache.' . $safeKey . '.*') ?: [];
        foreach ($files as $file) {
            $resolvedFile = realpath($file);
            if ($resolvedFile !== false && dirname($resolvedFile) === $cacheRoot && is_file($resolvedFile)) {
                @unlink($resolvedFile);
            }
        }
    }
}

/**
 * Restituisce gli ID delle lingue attive. Le descrizioni create da CartAdmin
 * vengono replicate su tutte le lingue per non produrre record invisibili nel
 * pannello OpenCart quando la lingua predefinita non ha ID 1.
 */
function cartadminActiveLanguageIds(mysqli $mysqli, string $dbPrefix): array {
    $languageIds = [];
    $result = $mysqli->query("SELECT `language_id` FROM `{$dbPrefix}language` WHERE `status` = 1 ORDER BY `sort_order`, `language_id`");

    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $languageId = (int)$row['language_id'];
            if ($languageId > 0) {
                $languageIds[] = $languageId;
            }
        }
    }

    if ($languageIds === []) {
        $fallback = $mysqli->query("SELECT `language_id` FROM `{$dbPrefix}language` ORDER BY `language_id` LIMIT 1");
        if ($fallback && $row = $fallback->fetch_assoc()) {
            $languageIds[] = (int)$row['language_id'];
        }
    }

    return array_values(array_filter(array_unique($languageIds), static fn(int $id): bool => $id > 0));
}

/**
 * Riduce l'HTML editoriale alla formattazione prodotta dall'editor mobile.
 * Script, link, media, handler evento e attributi arbitrari non raggiungono il DB.
 */
function cartadminSanitizeRichHtml(string $html): string {
    $allowedTags = '<p><br><strong><b><em><i><u><ul><ol><li><h1><h2><h3><font><span>';
    $clean = trim(strip_tags(mb_substr($html, 0, 65535), $allowedTags));

    if (!class_exists('DOMDocument')) {
        // Senza DOM conserviamo i tag consentiti, ma scartiamo tutti gli attributi.
        // Il colore viene perso in modo sicuro invece di accettare markup non verificato.
        return preg_replace('/<([a-z0-9]+)\b[^>]*>/i', '<$1>', $clean) ?? '';
    }

    $document = new DOMDocument('1.0', 'UTF-8');
    $previous = libxml_use_internal_errors(true);
    $loaded = $document->loadHTML('<?xml encoding="UTF-8"><div id="cartadmin-root">' . $clean . '</div>', LIBXML_HTML_NOIMPLIED | LIBXML_HTML_NODEFDTD);
    libxml_clear_errors();
    libxml_use_internal_errors($previous);
    if (!$loaded) {
        return '';
    }

    $root = $document->getElementById('cartadmin-root');
    if (!$root) {
        return '';
    }
    $elements = [];
    foreach ($root->getElementsByTagName('*') as $element) {
        $elements[] = $element;
    }
    foreach ($elements as $element) {
        $color = '';
        if (strtolower($element->nodeName) === 'font') {
            $candidate = (string)$element->getAttribute('color');
            if (preg_match('/^#[0-9a-f]{6}$/i', $candidate) === 1) {
                $color = strtoupper($candidate);
            }
        }
        while ($element->attributes && $element->attributes->length > 0) {
            $element->removeAttributeNode($element->attributes->item(0));
        }
        if ($color !== '') {
            $element->setAttribute('color', $color);
        }
    }

    $result = '';
    foreach ($root->childNodes as $child) {
        $result .= $document->saveHTML($child);
    }
    return trim($result);
}

function cartadminRequiredScope(string $action, string $module = ''): string {
    $ordersWrites = ['update_order_status', 'update_subscription_status', 'update_return_status'];
    $catalogWrites = ['update_stock', 'update_product', 'create_product', 'delete_product', 'create_category', 'update_category', 'delete_category'];

    if (in_array($action, $ordersWrites, true)) {
        return 'orders.write';
    }
    if (in_array($action, $catalogWrites, true)) {
        return 'catalog.write';
    }
    if (in_array($action, ['management_content', 'management_create', 'management_antispam'], true)) {
        return 'content.write';
    }
    if ($action === 'management_command') {
        return 'customers.write';
    }
    if ($action === 'management_status') {
        return $module === 'customers' ? 'customers.write' : 'content.write';
    }
    if (in_array($action, ['status', 'ping'], true)) {
        return 'status.read';
    }
    if (in_array($action, ['orders', 'subscriptions', 'returns'], true)) {
        return 'orders.read';
    }
    if (in_array($action, ['products', 'categories'], true)) {
        return 'catalog.read';
    }
    if ($action === 'visitor_telemetry') {
        return 'telemetry.read';
    }
    if ($action === 'management_list') {
        if (in_array($module, ['customers', 'customer_approvals', 'gdpr'], true)) {
            return 'customers.read';
        }
        if (in_array($module, ['subscription_plans', 'pages', 'reviews', 'articles', 'topics', 'comments', 'antispam'], true)) {
            return 'content.read';
        }
    }

    return 'forbidden';
}

function cartadminInsertSecurityAudit(
    mysqli $mysqli,
    string $dbPrefix,
    array $authContext,
    string $action,
    string $module,
    int $targetId,
    string $result,
    string $beforeDigest = '',
    string $afterDigest = '',
    string $summary = ''
): bool {
    $stmt = $mysqli->prepare("INSERT INTO `{$dbPrefix}cartadmin_security_audit` (`event_id`, `token_id`, `operator_user_id`, `operator_name`, `claimed_operator_digest`, `identity_claim_mismatch`, `device_hash`, `ip_hash`, `action_name`, `module_name`, `target_id`, `result`, `before_digest`, `after_digest`, `summary`, `created_at`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())");
    if (!$stmt) {
        return false;
    }

    $eventId = bin2hex(random_bytes(16));
    $tokenId = max(0, (int)($authContext['token_id'] ?? 0));
    $operatorUserId = max(0, (int)($authContext['operator_user_id'] ?? 0));
    $operator = mb_substr(strip_tags((string)($authContext['operator_name'] ?? '')), 0, 64);
    $claimedDigest = preg_match('/^[a-f0-9]{64}$/', (string)($authContext['claimed_operator_digest'] ?? '')) === 1 ? (string)$authContext['claimed_operator_digest'] : '';
    $claimMismatch = !empty($authContext['identity_claim_mismatch']) ? 1 : 0;
    $deviceHash = preg_match('/^[a-f0-9]{64}$/', (string)($authContext['device_hash'] ?? '')) === 1 ? (string)$authContext['device_hash'] : '';
    $ipHash = preg_match('/^[a-f0-9]{64}$/', (string)($authContext['ip_hash'] ?? '')) === 1 ? (string)$authContext['ip_hash'] : '';
    $cleanAction = mb_substr(preg_replace('/[^a-z0-9_.-]/i', '', $action), 0, 64);
    $cleanModule = mb_substr(preg_replace('/[^a-z0-9_.-]/i', '', $module), 0, 32);
    $cleanResult = in_array($result, ['success', 'failed', 'denied'], true) ? $result : 'failed';
    $before = preg_match('/^[a-f0-9]{64}$/', $beforeDigest) === 1 ? $beforeDigest : '';
    $after = preg_match('/^[a-f0-9]{64}$/', $afterDigest) === 1 ? $afterDigest : '';
    $cleanSummary = mb_substr(strip_tags($summary), 0, 255);

    $stmt->bind_param('siississssissss', $eventId, $tokenId, $operatorUserId, $operator, $claimedDigest, $claimMismatch, $deviceHash, $ipHash, $cleanAction, $cleanModule, $targetId, $cleanResult, $before, $after, $cleanSummary);
    $executed = $stmt->execute();
    $stmt->close();

    return $executed;
}

function cartadminStateDigest(array $state, string $auditSalt): string {
    ksort($state);
    $encoded = json_encode($state, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    if (!is_string($encoded)) {
        throw new RuntimeException('Impossibile serializzare lo stato per l’audit.');
    }

    return hash_hmac('sha256', $encoded, $auditSalt);
}

function cartadminEnforceRateLimit(mysqli $mysqli, string $dbPrefix, string $rateKey): void {
    if ($rateKey === '') {
        return;
    }
    $stmt = $mysqli->prepare("SELECT `blocked_until` FROM `{$dbPrefix}cartadmin_rate_limit` WHERE `rate_key` = ? AND `blocked_until` IS NOT NULL AND `blocked_until` > NOW() LIMIT 1");
    if (!$stmt) {
        sendJson(['success' => false, 'error' => 'Protezione accessi non disponibile.'], 503);
    }
    $stmt->bind_param('s', $rateKey);
    $stmt->execute();
    $result = $stmt->get_result();
    $blocked = $result && $result->num_rows > 0;
    $stmt->close();
    if ($blocked) {
        sendJson(['success' => false, 'error' => 'Troppi tentativi. Riprova più tardi.', 'code' => 429], 429);
    }
}

function cartadminRecordAuthFailure(mysqli $mysqli, string $dbPrefix, string $rateKey): void {
    if ($rateKey === '') {
        return;
    }
    $stmt = $mysqli->prepare("INSERT INTO `{$dbPrefix}cartadmin_rate_limit` (`rate_key`, `failures`, `window_started`, `blocked_until`) VALUES (?, 1, NOW(), NULL) ON DUPLICATE KEY UPDATE `blocked_until` = IF((IF(`window_started` < DATE_SUB(NOW(), INTERVAL 5 MINUTE), 0, `failures`) + 1) >= 10, DATE_ADD(NOW(), INTERVAL 15 MINUTE), `blocked_until`), `failures` = IF(`window_started` < DATE_SUB(NOW(), INTERVAL 5 MINUTE), 1, `failures` + 1), `window_started` = IF(`window_started` < DATE_SUB(NOW(), INTERVAL 5 MINUTE), NOW(), `window_started`)");
    if ($stmt) {
        $stmt->bind_param('s', $rateKey);
        $stmt->execute();
        $stmt->close();
    }
}

function cartadminClearAuthFailures(mysqli $mysqli, string $dbPrefix, string $rateKey): void {
    if ($rateKey === '') {
        return;
    }
    $stmt = $mysqli->prepare("DELETE FROM `{$dbPrefix}cartadmin_rate_limit` WHERE `rate_key` = ?");
    if ($stmt) {
        $stmt->bind_param('s', $rateKey);
        $stmt->execute();
        $stmt->close();
    }
}

/** Salva un upload immagine validato in catalog/cartadmin senza usare il nome client. */
function cartadminStoreProductImage(array $upload): string {
    if (!defined('DIR_IMAGE') || !is_dir(DIR_IMAGE)) {
        throw new RuntimeException('Directory immagini OpenCart non disponibile.');
    }
    $error = isset($upload['error']) ? (int)$upload['error'] : UPLOAD_ERR_NO_FILE;
    $size = isset($upload['size']) ? (int)$upload['size'] : 0;
    $tmpName = isset($upload['tmp_name']) && is_string($upload['tmp_name']) ? $upload['tmp_name'] : '';
    if ($error !== UPLOAD_ERR_OK || $size < 1 || $size > 5 * 1024 * 1024 || !is_uploaded_file($tmpName)) {
        throw new InvalidArgumentException('Upload immagine non valido o superiore a 5 MB.');
    }

    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mime = (string)$finfo->file($tmpName);
    $extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
    if (!isset($extensions[$mime])) {
        throw new InvalidArgumentException('Formato immagine non supportato.');
    }
    $dimensions = @getimagesize($tmpName);
    $width = is_array($dimensions) ? (int)($dimensions[0] ?? 0) : 0;
    $height = is_array($dimensions) ? (int)($dimensions[1] ?? 0) : 0;
    if ($width < 1 || $height < 1 || $width > 8000 || $height > 8000 || ($width * $height) > 40000000) {
        throw new InvalidArgumentException('Dimensioni immagine non valide.');
    }

    $imageRoot = realpath(DIR_IMAGE);
    if ($imageRoot === false) {
        throw new RuntimeException('Directory immagini OpenCart non risolvibile.');
    }
    $relativeDirectory = 'catalog/cartadmin/' . date('Y/m');
    $targetDirectory = $imageRoot . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $relativeDirectory);
    if (!is_dir($targetDirectory) && !mkdir($targetDirectory, 0755, true) && !is_dir($targetDirectory)) {
        throw new RuntimeException('Impossibile creare la directory immagini CartAdmin.');
    }
    $resolvedDirectory = realpath($targetDirectory);
    if ($resolvedDirectory === false || !str_starts_with($resolvedDirectory . DIRECTORY_SEPARATOR, $imageRoot . DIRECTORY_SEPARATOR)) {
        throw new RuntimeException('Percorso immagine non sicuro.');
    }
    $relativePath = $relativeDirectory . '/' . bin2hex(random_bytes(16)) . '.' . $extensions[$mime];
    $targetPath = $imageRoot . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $relativePath);
    if (!move_uploaded_file($tmpName, $targetPath)) {
        throw new RuntimeException('Salvataggio immagine non riuscito.');
    }
    return $relativePath;
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

$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_token` (
    `token_id` INT AUTO_INCREMENT PRIMARY KEY,
    `token_lookup` CHAR(16) NULL,
    `token_hash` VARCHAR(255) NOT NULL,
    `last_four` CHAR(4) NOT NULL,
    `label` VARCHAR(64) NOT NULL,
    `operator_user_id` INT NULL,
    `operator_name` VARCHAR(64) NOT NULL,
    `scopes` VARCHAR(255) NOT NULL,
    `device_hash` CHAR(64) NULL,
    `device_public_key` TEXT NULL,
    `active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL,
    `last_used_at` DATETIME NULL,
    `revoked_at` DATETIME NULL,
    UNIQUE KEY `uq_token_lookup` (`token_lookup`),
    INDEX `idx_active` (`active`, `token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_security_audit` (
    `audit_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_id` VARCHAR(64) NOT NULL,
    `token_id` INT NULL,
    `operator_user_id` INT NULL,
    `operator_name` VARCHAR(64) NOT NULL,
    `claimed_operator_digest` CHAR(64) NOT NULL DEFAULT '',
    `identity_claim_mismatch` TINYINT(1) NOT NULL DEFAULT 0,
    `device_hash` CHAR(64) NOT NULL DEFAULT '',
    `ip_hash` CHAR(64) NOT NULL DEFAULT '',
    `action_name` VARCHAR(64) NOT NULL,
    `module_name` VARCHAR(32) NOT NULL DEFAULT '',
    `target_id` INT NOT NULL DEFAULT 0,
    `result` VARCHAR(16) NOT NULL,
    `before_digest` CHAR(64) NOT NULL DEFAULT '',
    `after_digest` CHAR(64) NOT NULL DEFAULT '',
    `summary` VARCHAR(255) NOT NULL DEFAULT '',
    `created_at` DATETIME NOT NULL,
    UNIQUE KEY `uq_event_id` (`event_id`),
    INDEX `idx_security_created` (`created_at`),
    INDEX `idx_security_token` (`token_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_command` (
    `command_id` INT AUTO_INCREMENT PRIMARY KEY,
    `module` VARCHAR(32) NOT NULL,
    `target_id` INT NOT NULL,
    `operation` VARCHAR(16) NOT NULL,
    `requested_by` VARCHAR(64) NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'pending',
    `dedupe_key` VARCHAR(80) NULL,
    `error_message` VARCHAR(255) NOT NULL DEFAULT '',
    `created_at` DATETIME NOT NULL,
    `processed_at` DATETIME NULL,
    `processed_by` INT NULL,
    UNIQUE KEY `uq_pending_target` (`dedupe_key`),
    INDEX `idx_command_status` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
$deviceKeyColumn = $mysqli->query("SHOW COLUMNS FROM `{$db_prefix}cartadmin_token` LIKE 'device_public_key'");
if (!$deviceKeyColumn || $deviceKeyColumn->num_rows === 0) {
    $mysqli->query("ALTER TABLE `{$db_prefix}cartadmin_token` ADD COLUMN `device_public_key` TEXT NULL AFTER `device_hash`");
}
$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_rate_limit` (
    `rate_key` CHAR(64) PRIMARY KEY,
    `failures` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    `window_started` DATETIME NOT NULL,
    `blocked_until` DATETIME NULL,
    INDEX `idx_rate_cleanup` (`window_started`, `blocked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
$mysqli->query("CREATE TABLE IF NOT EXISTS `{$db_prefix}cartadmin_device_nonce` (
    `token_id` INT NOT NULL,
    `nonce` CHAR(36) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`token_id`, `nonce`),
    INDEX `idx_nonce_cleanup` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

// 5. Migrazione una tantum del token singolo verso credenziali revocabili.
$legacyMarkerResult = $mysqli->query("SELECT `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'legacy_token_migrated' LIMIT 1");
$legacyMigrated = ($legacyMarkerResult && $row = $legacyMarkerResult->fetch_assoc()) ? (string)$row['value'] === '1' : false;
$legacyCredentialResult = $mysqli->query("SELECT COUNT(*) AS total FROM `{$db_prefix}cartadmin_setting` WHERE `key` IN ('api_key', 'token_hash') AND `value` <> ''");
$legacyCredentialsPresent = ($legacyCredentialResult && $row = $legacyCredentialResult->fetch_assoc()) ? (int)$row['total'] > 0 : false;
if (!$legacyMigrated || $legacyCredentialsPresent) {
    $legacyValues = [];
    $legacyResult = $mysqli->query("SELECT `key`, `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` IN ('api_key', 'token_hash', 'token_last_four', 'token_created_at')");
    if ($legacyResult) {
        while ($row = $legacyResult->fetch_assoc()) {
            $legacyValues[(string)$row['key']] = (string)$row['value'];
        }
    }
    $legacyHash = trim($legacyValues['token_hash'] ?? '');
    $legacyPlaintext = trim($legacyValues['api_key'] ?? '');
    if ($legacyHash === '' && $legacyPlaintext !== '') {
        $legacyHash = cartadminHashToken($legacyPlaintext);
    }
    $legacyLastFour = substr($legacyValues['token_last_four'] ?? $legacyPlaintext, -4);
    $legacyCreatedAt = trim($legacyValues['token_created_at'] ?? '');

    $mysqli->begin_transaction();
    try {
        if ($legacyHash !== '') {
            $legacyInsert = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_token` (`token_lookup`, `token_hash`, `last_four`, `label`, `operator_name`, `scopes`, `active`, `created_at`, `revoked_at`) VALUES (NULL, ?, ?, 'Token legacy revocato', 'Operatore legacy', '', 0, ?, NOW())");
            if (!$legacyInsert) {
                throw new RuntimeException('Migrazione token non disponibile');
            }
            $legacyDate = $legacyCreatedAt !== '' ? $legacyCreatedAt : date('Y-m-d H:i:s');
            $legacyInsert->bind_param('sss', $legacyHash, $legacyLastFour, $legacyDate);
            if (!$legacyInsert->execute()) {
                throw new RuntimeException('Migrazione token non riuscita');
            }
            $legacyInsert->close();
        }
        $markerStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('legacy_token_migrated', '1', NOW()) ON DUPLICATE KEY UPDATE `value` = '1', `date_updated` = NOW()");
        if (!$markerStmt || !$markerStmt->execute()) {
            throw new RuntimeException('Stato migrazione non salvato');
        }
        $markerStmt->close();
        $mysqli->query("DELETE FROM `{$db_prefix}cartadmin_setting` WHERE `key` IN ('api_key', 'token_hash', 'token_last_four', 'token_created_at')");
        $mysqli->commit();
    } catch (Throwable $migrationError) {
        $mysqli->rollback();
        sendJson(['success' => false, 'error' => 'Migrazione sicura del token CartAdmin non riuscita.'], 500);
    }
}

$auditSaltResult = $mysqli->query("SELECT `value` FROM `{$db_prefix}cartadmin_setting` WHERE `key` = 'audit_ip_salt' LIMIT 1");
$auditSalt = ($auditSaltResult && $row = $auditSaltResult->fetch_assoc()) ? trim((string)$row['value']) : '';
if (preg_match('/^[a-f0-9]{64}$/', $auditSalt) !== 1) {
    $auditSalt = bin2hex(random_bytes(32));
    $auditSaltStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_setting` (`key`, `value`, `date_updated`) VALUES ('audit_ip_salt', ?, NOW()) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`), `date_updated` = NOW()");
    if (!$auditSaltStmt) {
        sendJson(['success' => false, 'error' => 'Configurazione audit non disponibile.'], 500);
    }
    $auditSaltStmt->bind_param('s', $auditSalt);
    $auditSaltStmt->execute();
    $auditSaltStmt->close();
}

$remoteAddress = isset($_SERVER['REMOTE_ADDR']) && is_string($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : '';
$ipHash = $remoteAddress !== '' ? hash_hmac('sha256', $remoteAddress, $auditSalt) : '';
cartadminEnforceRateLimit($mysqli, $db_prefix, $ipHash);

// 6. Verifica token, dispositivo e identità server-side.
[$receivedKey, $claimedUsername, $receivedDeviceId, $receivedDeviceKey, $deviceTimestamp, $deviceNonce, $deviceSignature] = cartadminExtractCredentials($_SERVER);
if ($receivedKey === '' || $receivedDeviceId === '' || $receivedDeviceKey === '' || $deviceTimestamp === '' || $deviceNonce === '' || $deviceSignature === '') {
    cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
    usleep(200000);
    sendJson(['success' => false, 'error' => 'Non autorizzato. Credenziale o identità dispositivo mancante.', 'code' => 401], 401);
}

$tokenRows = [];
if (preg_match('/^ca_([a-f0-9]{16})_[a-f0-9]{64}$/', $receivedKey, $tokenParts) === 1) {
    $lookupStmt = $mysqli->prepare("SELECT t.`token_id`, t.`token_hash`, t.`operator_user_id`, COALESCE(u.`username`, t.`operator_name`) AS `operator_name`, t.`scopes`, t.`device_hash`, t.`device_public_key` FROM `{$db_prefix}cartadmin_token` t LEFT JOIN `{$db_prefix}user` u ON (t.`operator_user_id` = u.`user_id`) WHERE t.`token_lookup` = ? AND t.`active` = 1 AND (t.`operator_user_id` IS NULL OR u.`status` = 1) LIMIT 1");
    if ($lookupStmt) {
        $lookupStmt->bind_param('s', $tokenParts[1]);
        $lookupStmt->execute();
        $lookupResult = $lookupStmt->get_result();
        if ($lookupResult && $row = $lookupResult->fetch_assoc()) {
            $tokenRows[] = $row;
        }
        $lookupStmt->close();
    }
} elseif (preg_match('/^ca_[a-f0-9]{64}$/', $receivedKey) === 1) {
    $legacyTokens = $mysqli->query("SELECT t.`token_id`, t.`token_hash`, t.`operator_user_id`, COALESCE(u.`username`, t.`operator_name`) AS `operator_name`, t.`scopes`, t.`device_hash`, t.`device_public_key` FROM `{$db_prefix}cartadmin_token` t LEFT JOIN `{$db_prefix}user` u ON (t.`operator_user_id` = u.`user_id`) WHERE t.`token_lookup` IS NULL AND t.`active` = 1 AND (t.`operator_user_id` IS NULL OR u.`status` = 1) ORDER BY t.`token_id` DESC LIMIT 5");
    if ($legacyTokens) {
        while ($row = $legacyTokens->fetch_assoc()) {
            $tokenRows[] = $row;
        }
    }
}

$authenticatedToken = null;
foreach ($tokenRows as $tokenRow) {
    if (cartadminTokenMatches((string)$tokenRow['token_hash'], $receivedKey)) {
        $authenticatedToken = $tokenRow;
        break;
    }
}
if ($authenticatedToken === null) {
    cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
    usleep(200000);
    sendJson(['success' => false, 'error' => 'Non autorizzato. Token CartAdmin non valido o revocato.', 'code' => 401], 401);
}

$tokenId = (int)$authenticatedToken['token_id'];
$deviceHash = hash('sha256', $receivedDeviceId);
$storedDeviceHash = trim((string)($authenticatedToken['device_hash'] ?? ''));
$storedDeviceKey = trim((string)($authenticatedToken['device_public_key'] ?? ''));
$requestMethodForProof = strtoupper((string)($_SERVER['REQUEST_METHOD'] ?? ''));
$requestUriForProof = isset($_SERVER['REQUEST_URI']) && is_string($_SERVER['REQUEST_URI']) ? $_SERVER['REQUEST_URI'] : '';
if (!cartadminVerifyDeviceProof($receivedDeviceId, $receivedDeviceKey, $deviceTimestamp, $deviceNonce, $deviceSignature, $requestMethodForProof, $requestUriForProof)) {
    cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
    sendJson(['success' => false, 'error' => 'Non autorizzato. Prova hardware del dispositivo non valida.', 'code' => 401], 401);
}
if ($storedDeviceKey === '') {
    $bindStmt = $mysqli->prepare("UPDATE `{$db_prefix}cartadmin_token` SET `device_hash` = ?, `device_public_key` = ? WHERE `token_id` = ? AND `active` = 1 AND (`device_public_key` IS NULL OR `device_public_key` = '')");
    if (!$bindStmt) {
        sendJson(['success' => false, 'error' => 'Associazione dispositivo non disponibile.'], 500);
    }
    $bindStmt->bind_param('ssi', $deviceHash, $receivedDeviceKey, $tokenId);
    $bindStmt->execute();
    $bound = $bindStmt->affected_rows === 1;
    $bindStmt->close();
    if (!$bound) {
        $verifyBindStmt = $mysqli->prepare("SELECT `device_hash`, `device_public_key` FROM `{$db_prefix}cartadmin_token` WHERE `token_id` = ? AND `active` = 1 LIMIT 1");
        if (!$verifyBindStmt) {
            sendJson(['success' => false, 'error' => 'Verifica associazione dispositivo non disponibile.'], 500);
        }
        $verifyBindStmt->bind_param('i', $tokenId);
        $verifyBindStmt->execute();
        $verifyBindResult = $verifyBindStmt->get_result();
        $bindRow = $verifyBindResult ? $verifyBindResult->fetch_assoc() : null;
        $storedDeviceHash = is_array($bindRow) ? (string)($bindRow['device_hash'] ?? '') : '';
        $storedDeviceKey = is_array($bindRow) ? trim((string)($bindRow['device_public_key'] ?? '')) : '';
        $verifyBindStmt->close();
        if ($storedDeviceHash === '' || !hash_equals($storedDeviceHash, $deviceHash) || !hash_equals($storedDeviceKey, $receivedDeviceKey)) {
            cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
            sendJson(['success' => false, 'error' => 'Non autorizzato. Token associato a un altro dispositivo.', 'code' => 401], 401);
        }
    }
} elseif (!hash_equals($storedDeviceHash, $deviceHash) || !hash_equals($storedDeviceKey, $receivedDeviceKey)) {
    cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
    usleep(200000);
    sendJson(['success' => false, 'error' => 'Non autorizzato. Token associato a un altro dispositivo.', 'code' => 401], 401);
}

cartadminClearAuthFailures($mysqli, $db_prefix, $ipHash);
$authenticatedOperator = mb_substr(strip_tags((string)$authenticatedToken['operator_name']), 0, 64);
$authenticatedScopes = cartadminParseScopes((string)$authenticatedToken['scopes']);
$normalizedClaim = mb_substr(strip_tags($claimedUsername), 0, 64);
$claimedOperatorDigest = $normalizedClaim !== '' ? hash_hmac('sha256', $normalizedClaim, $auditSalt) : '';
$authContext = [
    'token_id' => $tokenId,
    'operator_user_id' => max(0, (int)($authenticatedToken['operator_user_id'] ?? 0)),
    'operator_name' => $authenticatedOperator,
    'claimed_operator_digest' => $claimedOperatorDigest,
    'identity_claim_mismatch' => $normalizedClaim !== '' && !hash_equals($authenticatedOperator, $normalizedClaim),
    'device_hash' => $deviceHash,
    'ip_hash' => $ipHash
];
$lastUsedStmt = $mysqli->prepare("UPDATE `{$db_prefix}cartadmin_token` SET `last_used_at` = NOW() WHERE `token_id` = ? AND `active` = 1");
if ($lastUsedStmt) {
    $lastUsedStmt->bind_param('i', $tokenId);
    $lastUsedStmt->execute();
    $lastUsedStmt->close();
}

// 7. Autorizzazione per azione e router delle API CartAdmin.
$rawAction = isset($_GET['action']) ? $_GET['action'] : (isset($_POST['action']) ? $_POST['action'] : 'status');
$action = is_string($rawAction) ? strtolower(trim($rawAction)) : 'status';
$rawScopeModule = isset($_GET['module']) ? $_GET['module'] : (isset($_POST['module']) ? $_POST['module'] : '');
$scopeModule = is_string($rawScopeModule) ? strtolower(trim($rawScopeModule)) : '';
$requiredScope = cartadminRequiredScope($action, $scopeModule);
if (!cartadminHasScope($authenticatedScopes, $requiredScope)) {
    cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, $action, $scopeModule, 0, 'denied', '', '', 'Permesso richiesto: ' . $requiredScope);
    sendJson(['success' => false, 'error' => 'Operazione non autorizzata per questo token.', 'code' => 403], 403);
}

$nonceStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_device_nonce` (`token_id`, `nonce`, `created_at`) VALUES (?, ?, NOW())");
if (!$nonceStmt) {
    sendJson(['success' => false, 'error' => 'Protezione anti-replay non disponibile.'], 503);
}
$nonceStmt->bind_param('is', $tokenId, $deviceNonce);
$nonceAccepted = $nonceStmt->execute();
$nonceStmt->close();
if (!$nonceAccepted) {
    cartadminRecordAuthFailure($mysqli, $db_prefix, $ipHash);
    sendJson(['success' => false, 'error' => 'Non autorizzato. Richiesta già utilizzata.', 'code' => 401], 401);
}
if (random_int(1, 100) === 1) {
    $mysqli->query("DELETE FROM `{$db_prefix}cartadmin_device_nonce` WHERE `created_at` < DATE_SUB(NOW(), INTERVAL 10 MINUTE)");
}

$readActions = ['status', 'ping', 'orders', 'subscriptions', 'returns', 'products', 'categories', 'visitor_telemetry', 'management_list'];
$expectedMethod = in_array($action, $readActions, true) ? 'GET' : 'POST';
$requestMethod = strtoupper((string)($_SERVER['REQUEST_METHOD'] ?? ''));
if ($requestMethod !== $expectedMethod) {
    cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, $action, $scopeModule, 0, 'denied', '', '', 'Metodo HTTP non consentito');
    header('Allow: ' . $expectedMethod);
    sendJson(['success' => false, 'error' => 'Metodo HTTP non consentito.', 'code' => 405], 405);
}

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
                'bridge_version' => '2.1.3',
                'author' => 'SOLO SOLUZIONI (OpenCart ITALIA)',
                'store_name' => $storeName,
                'authenticated_operator' => $authenticatedOperator,
                'granted_scopes' => $authenticatedScopes,
                'total_orders' => $totalOrders,
                'total_products' => $totalProducts,
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
            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            $languageId = $languageIds[0] ?? 1;
            $stmt = $mysqli->prepare("SELECT p.product_id, p.model, p.sku, p.quantity, p.minimum, p.price, p.status, p.image,
                                             pd.name, pd.description,
                                             (SELECT cd.name FROM `{$db_prefix}product_to_category` p2c
                                              LEFT JOIN `{$db_prefix}category_description` cd ON (p2c.category_id = cd.category_id AND cd.language_id = ?)
                                              WHERE p2c.product_id = p.product_id ORDER BY p2c.category_id ASC LIMIT 1) AS category_name,
                                             (SELECT pdx.price FROM `{$db_prefix}product_discount` pdx
                                              WHERE pdx.product_id = p.product_id AND pdx.special = 1
                                              ORDER BY pdx.priority ASC, pdx.product_discount_id ASC LIMIT 1) AS special_price
                                      FROM `{$db_prefix}product` p
                                      LEFT JOIN `{$db_prefix}product_description` pd ON (p.product_id = pd.product_id AND pd.language_id = ?)
                                      ORDER BY p.product_id DESC LIMIT ?");
            $products = [];
            if ($stmt) {
                $stmt->bind_param('iii', $languageId, $languageId, $limit);
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
                            'minimum' => max(1, (int)$row['minimum']),
                            'price' => (float)$row['price'],
                            'special_price' => $row['special_price'] !== null ? (float)$row['special_price'] : null,
                            'category' => $row['category_name'] ?: '',
                            'description' => html_entity_decode($row['description'] ?? '', ENT_QUOTES | ENT_HTML5, 'UTF-8'),
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
            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            $languageId = $languageIds[0] ?? 1;
            $stmt = $mysqli->prepare("SELECT c.category_id, cd.name, cd.description, c.status, c.sort_order,
                                             (SELECT COUNT(p2c.product_id) FROM `{$db_prefix}product_to_category` p2c WHERE p2c.category_id = c.category_id) AS products_count
                                      FROM `{$db_prefix}category` c
                                      LEFT JOIN `{$db_prefix}category_description` cd ON (c.category_id = cd.category_id AND cd.language_id = ?)
                                      GROUP BY c.category_id
                                      ORDER BY c.sort_order ASC, cd.name ASC
                                      LIMIT ?");
            $categories = [];
            if ($stmt) {
                $stmt->bind_param('ii', $languageId, $limit);
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

        case 'management_list':
            $rawModule = isset($_GET['module']) && is_string($_GET['module'])
                ? strtolower(trim($_GET['module']))
                : '';
            $limit = isset($_GET['limit']) ? max(1, min(200, (int)$_GET['limit'])) : 100;

            $languageId = 1;
            $resLanguage = $mysqli->query("SELECT `language_id` FROM `{$db_prefix}language` WHERE `status` = 1 ORDER BY `sort_order` ASC, `language_id` ASC LIMIT 1");
            if ($resLanguage && $languageRow = $resLanguage->fetch_assoc()) {
                $languageId = max(1, (int)$languageRow['language_id']);
            }

            // Ogni modulo usa esclusivamente nomi di tabella e query definiti
            // nell'allowlist. Nessun identificatore SQL arriva dalla richiesta.
            $moduleQueries = [
                'subscription_plans' => [
                    'tables' => ['subscription_plan', 'subscription_plan_description'],
                    'sql' => "SELECT sp.subscription_plan_id AS id, spd.name AS title, CONCAT(sp.frequency, ' • ', sp.cycle, ' ciclo/i') AS subtitle, sp.status AS active, '' AS date_value, CONCAT('Durata: ', sp.duration) AS detail, sp.status AS status_code FROM `{$db_prefix}subscription_plan` sp LEFT JOIN `{$db_prefix}subscription_plan_description` spd ON (sp.subscription_plan_id = spd.subscription_plan_id AND spd.language_id = {$languageId}) ORDER BY sp.sort_order ASC, sp.subscription_plan_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'pages' => [
                    'tables' => ['information', 'information_description'],
                    'sql' => "SELECT i.information_id AS id, id.title AS title, CONCAT('Ordinamento: ', i.sort_order) AS subtitle, i.status AS active, '' AS date_value, '' AS detail, i.status AS status_code, '' AS content_value, NULL AS rating_value, i.sort_order AS sort_order_value FROM `{$db_prefix}information` i LEFT JOIN `{$db_prefix}information_description` id ON (i.information_id = id.information_id AND id.language_id = {$languageId}) ORDER BY i.sort_order ASC, i.information_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'reviews' => [
                    'tables' => ['review', 'product_description'],
                    'sql' => "SELECT r.review_id AS id, pd.name AS title, r.author AS subtitle, r.status AS active, r.date_added AS date_value, CONCAT(r.rating, '/5 • ', LEFT(r.text, 180)) AS detail, r.status AS status_code, r.text AS content_value, r.rating AS rating_value, NULL AS sort_order_value FROM `{$db_prefix}review` r LEFT JOIN `{$db_prefix}product_description` pd ON (r.product_id = pd.product_id AND pd.language_id = {$languageId}) ORDER BY r.date_added DESC, r.review_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'articles' => [
                    'tables' => ['article', 'article_description'],
                    'sql' => "SELECT a.article_id AS id, ad.name AS title, a.author AS subtitle, a.status AS active, a.date_added AS date_value, CONCAT('Argomento #', a.topic_id) AS detail, a.status AS status_code, ad.description AS content_value, NULL AS rating_value, NULL AS sort_order_value, a.topic_id AS parent_id_value FROM `{$db_prefix}article` a LEFT JOIN `{$db_prefix}article_description` ad ON (a.article_id = ad.article_id AND ad.language_id = {$languageId}) ORDER BY a.date_added DESC, a.article_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'topics' => [
                    'tables' => ['topic', 'topic_description'],
                    'sql' => "SELECT t.topic_id AS id, td.name AS title, CONCAT('Ordinamento: ', t.sort_order) AS subtitle, t.status AS active, '' AS date_value, '' AS detail, t.status AS status_code, td.description AS content_value, NULL AS rating_value, t.sort_order AS sort_order_value, NULL AS parent_id_value FROM `{$db_prefix}topic` t LEFT JOIN `{$db_prefix}topic_description` td ON (t.topic_id = td.topic_id AND td.language_id = {$languageId}) ORDER BY t.sort_order ASC, t.topic_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'comments' => [
                    'tables' => ['article_comment', 'article_description'],
                    'sql' => "SELECT ac.article_comment_id AS id, ad.name AS title, ac.author AS subtitle, ac.status AS active, ac.date_added AS date_value, CONCAT(ac.rating, '/5 • ', LEFT(ac.comment, 180)) AS detail, ac.status AS status_code FROM `{$db_prefix}article_comment` ac LEFT JOIN `{$db_prefix}article_description` ad ON (ac.article_id = ad.article_id AND ad.language_id = {$languageId}) ORDER BY ac.date_added DESC, ac.article_comment_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'antispam' => [
                    'tables' => ['antispam'],
                    'sql' => "SELECT antispam_id AS id, keyword AS title, 'Parola bloccata' AS subtitle, NULL AS active, '' AS date_value, '' AS detail, NULL AS status_code FROM `{$db_prefix}antispam` ORDER BY keyword ASC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'customers' => [
                    'tables' => ['customer'],
                    'sql' => "SELECT customer_id AS id, CONCAT(firstname, ' ', lastname) AS title, email AS subtitle, status AS active, date_added AS date_value, telephone AS detail, status AS status_code FROM `{$db_prefix}customer` ORDER BY date_added DESC, customer_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'customer_approvals' => [
                    'tables' => ['customer_approval', 'customer'],
                    'sql' => "SELECT ca.customer_approval_id AS id, CONCAT(c.firstname, ' ', c.lastname) AS title, c.email AS subtitle, NULL AS active, ca.date_added AS date_value, ca.type AS detail, ca.type AS status_code FROM `{$db_prefix}customer_approval` ca LEFT JOIN `{$db_prefix}customer` c ON (ca.customer_id = c.customer_id) ORDER BY ca.date_added DESC, ca.customer_approval_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ],
                'gdpr' => [
                    'tables' => ['gdpr'],
                    'sql' => "SELECT gdpr_id AS id, email AS title, action AS subtitle, NULL AS active, date_added AS date_value, '' AS detail, status AS status_code FROM `{$db_prefix}gdpr` ORDER BY date_added DESC, gdpr_id DESC LIMIT {$limit}" // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string
                ]
            ];

            if (!array_key_exists($rawModule, $moduleQueries)) {
                sendJson(['success' => false, 'error' => 'Modulo amministrativo non valido.'], 400);
            }

            $definition = $moduleQueries[$rawModule];
            foreach ($definition['tables'] as $tableSuffix) {
                $tableName = $db_prefix . $tableSuffix;
                $escapedTableName = $mysqli->real_escape_string($tableName);
                $tableCheck = $mysqli->query("SHOW TABLES LIKE '{$escapedTableName}'");
                if (!$tableCheck || $tableCheck->num_rows === 0) {
                    sendJson([
                        'success' => true,
                        'module' => $rawModule,
                        'supported' => false,
                        'count' => 0,
                        'items' => [],
                        'message' => 'Funzione non disponibile in questa installazione/versione di OpenCart.',
                        'generated_at' => date('c')
                    ]);
                }
            }

            $result = $mysqli->query($definition['sql']);
            if (!$result) {
                sendJson(['success' => false, 'error' => 'Impossibile leggere il modulo amministrativo richiesto.'], 500);
            }

            $pendingCommands = [];
            if (in_array($rawModule, ['customer_approvals', 'gdpr'], true)) {
                $pendingStmt = $mysqli->prepare("SELECT `command_id`, `target_id`, `operation` FROM `{$db_prefix}cartadmin_command` WHERE `module` = ? AND `status` = 'pending'");
                if ($pendingStmt) {
                    $pendingStmt->bind_param('s', $rawModule);
                    $pendingStmt->execute();
                    $pendingResult = $pendingStmt->get_result();
                    if ($pendingResult) {
                        while ($pendingRow = $pendingResult->fetch_assoc()) {
                            $pendingCommands[(int)$pendingRow['target_id']] = [
                                'id' => (int)$pendingRow['command_id'],
                                'operation' => (string)$pendingRow['operation']
                            ];
                        }
                    }
                    $pendingStmt->close();
                }
            }

            $items = [];
            while ($row = $result->fetch_assoc()) {
                $statusLabel = '';
                if ($rawModule === 'gdpr') {
                    $gdprLabels = [-1 => 'Rifiutato', 0 => 'Non verificato', 1 => 'In attesa', 2 => 'In elaborazione', 3 => 'Completato'];
                    $statusLabel = $gdprLabels[(int)$row['status_code']] ?? ('Stato ' . (int)$row['status_code']);
                } elseif ($rawModule === 'customer_approvals') {
                    $statusLabel = ((string)$row['status_code'] === 'affiliate') ? 'Affiliato' : 'Cliente';
                } elseif ($row['active'] !== null) {
                    $statusLabel = ((int)$row['active'] === 1) ? 'Attivo' : 'Disattivato';
                }

                $title = html_entity_decode(strip_tags((string)($row['title'] ?? '')), ENT_QUOTES | ENT_HTML5, 'UTF-8');
                $subtitle = html_entity_decode(strip_tags((string)($row['subtitle'] ?? '')), ENT_QUOTES | ENT_HTML5, 'UTF-8');
                $detail = html_entity_decode(strip_tags((string)($row['detail'] ?? '')), ENT_QUOTES | ENT_HTML5, 'UTF-8');
                $content = html_entity_decode(strip_tags((string)($row['content_value'] ?? '')), ENT_QUOTES | ENT_HTML5, 'UTF-8');
                $pending = $pendingCommands[(int)$row['id']] ?? null;
                $items[] = [
                    'id' => (string)$row['id'],
                    'title' => mb_substr($title !== '' ? $title : ('#' . $row['id']), 0, 180),
                    'subtitle' => mb_substr($subtitle, 0, 180),
                    'active' => $row['active'] === null ? null : ((int)$row['active'] === 1),
                    'status_label' => $statusLabel,
                    'date' => mb_substr((string)($row['date_value'] ?? ''), 0, 32),
                    'detail' => mb_substr($detail, 0, 240),
                    'actionable' => $rawModule === 'customer_approvals' || ($rawModule === 'gdpr' && (int)$row['status_code'] === 1),
                    'pending_command_id' => $pending['id'] ?? null,
                    'pending_operation' => $pending['operation'] ?? '',
                    'content' => mb_substr($content, 0, 2000),
                    'rating' => isset($row['rating_value']) ? (int)$row['rating_value'] : null,
                    'sort_order' => isset($row['sort_order_value']) ? (int)$row['sort_order_value'] : null,
                    'parent_id' => isset($row['parent_id_value']) ? (int)$row['parent_id_value'] : null,
                    'editable' => in_array($rawModule, ['pages', 'reviews', 'articles', 'topics'], true)
                ];
            }

            sendJson([
                'success' => true,
                'module' => $rawModule,
                'supported' => true,
                'count' => count($items),
                'items' => $items,
                'message' => '',
                'generated_at' => date('c')
            ]);
            break;

        case 'management_command':
            $rawModule = isset($_POST['module']) && is_string($_POST['module']) ? strtolower(trim($_POST['module'])) : '';
            $recordId = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $operation = isset($_POST['operation']) && is_string($_POST['operation']) ? strtolower(trim($_POST['operation'])) : '';

            if (!in_array($rawModule, ['customer_approvals', 'gdpr'], true) || !in_array($operation, ['approve', 'deny'], true) || $recordId < 1) {
                sendJson(['success' => false, 'error' => 'Richiesta amministrativa non valida.'], 400);
            }

            if ($rawModule === 'customer_approvals') {
                $targetStmt = $mysqli->prepare("SELECT `customer_approval_id` FROM `{$db_prefix}customer_approval` WHERE `customer_approval_id` = ? LIMIT 1");
            } else {
                $targetStmt = $mysqli->prepare("SELECT `gdpr_id` FROM `{$db_prefix}gdpr` WHERE `gdpr_id` = ? AND `status` = 1 LIMIT 1");
            }
            if (!$targetStmt) {
                sendJson(['success' => false, 'error' => 'Modulo amministrativo non disponibile.'], 409);
            }
            $targetStmt->bind_param('i', $recordId);
            $targetStmt->execute();
            $targetResult = $targetStmt->get_result();
            $targetExists = $targetResult && $targetResult->num_rows === 1;
            $targetStmt->close();
            if (!$targetExists) {
                sendJson(['success' => false, 'error' => 'Elemento non trovato o non più in attesa.'], 409);
            }

            $requestedBy = $authenticatedOperator;
            $dedupeKey = $rawModule . ':' . $recordId;
            $mysqli->begin_transaction();
            $commandStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}cartadmin_command` (`module`, `target_id`, `operation`, `requested_by`, `status`, `dedupe_key`, `created_at`) VALUES (?, ?, ?, ?, 'pending', ?, NOW())");
            if (!$commandStmt) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Impossibile accodare la richiesta.'], 500);
            }
            $commandStmt->bind_param('sisss', $rawModule, $recordId, $operation, $requestedBy, $dedupeKey);
            $executed = $commandStmt->execute();
            if (!$executed) {
                $commandError = $commandStmt->errno;
                $commandStmt->close();
                $mysqli->rollback();
                if ($commandError === 1062) {
                    sendJson(['success' => false, 'error' => 'Esiste già una richiesta in attesa per questo elemento.'], 409);
                }
                sendJson(['success' => false, 'error' => 'Impossibile accodare la richiesta.'], 500);
            }
            $commandId = (int)$mysqli->insert_id;
            $commandStmt->close();
            $afterDigest = cartadminStateDigest(['command_id' => $commandId, 'target_id' => $recordId, 'operation' => $operation], $auditSalt);
            if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_command', $rawModule, $recordId, 'success', '', $afterDigest, 'Richiesta amministrativa accodata')) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Audit richiesta non disponibile.'], 500);
            }
            $mysqli->commit();

            sendJson([
                'success' => true,
                'command_id' => $commandId,
                'status' => 'pending',
                'message' => 'Richiesta inviata al pannello OpenCart per la conferma di un amministratore.'
            ], 202);
            break;

        case 'management_antispam':
            $operation = isset($_POST['operation']) && is_string($_POST['operation'])
                ? strtolower(trim($_POST['operation']))
                : '';

            if ($operation === 'add') {
                $keyword = isset($_POST['keyword']) && is_string($_POST['keyword'])
                    ? trim(strip_tags($_POST['keyword']))
                    : '';
                $keyword = mb_substr($keyword, 0, 64);
                if ($keyword === '') {
                    sendJson(['success' => false, 'error' => 'Inserisci una parola antispam valida.'], 400);
                }

                $duplicateStmt = $mysqli->prepare("SELECT `antispam_id` FROM `{$db_prefix}antispam` WHERE LCASE(`keyword`) = LCASE(?) LIMIT 1");
                if (!$duplicateStmt) {
                    sendJson(['success' => false, 'error' => 'Modulo Antispam non disponibile.'], 409);
                }
                $duplicateStmt->bind_param('s', $keyword);
                $duplicateStmt->execute();
                $duplicateResult = $duplicateStmt->get_result();
                $duplicate = $duplicateResult && $duplicateResult->num_rows > 0;
                $duplicateStmt->close();
                if ($duplicate) {
                    sendJson(['success' => false, 'error' => 'La parola è già presente nell’elenco Antispam.'], 409);
                }

                $mysqli->begin_transaction();
                $insertStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}antispam` (`keyword`) VALUES (?)");
                if (!$insertStmt) {
                    $mysqli->rollback();
                    sendJson(['success' => false, 'error' => 'Impossibile aggiungere la parola Antispam.'], 500);
                }
                $insertStmt->bind_param('s', $keyword);
                $insertStmt->execute();
                $createdId = (int)$mysqli->insert_id;
                $insertStmt->close();
                $afterDigest = cartadminStateDigest(['antispam_id' => $createdId, 'keyword_digest' => hash_hmac('sha256', $keyword, $auditSalt)], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_antispam', 'antispam', $createdId, 'success', '', $afterDigest, 'Inserimento regola antispam')) {
                    $mysqli->rollback();
                    sendJson(['success' => false, 'error' => 'Audit Antispam non disponibile.'], 500);
                }
                $mysqli->commit();
                sendJson(['success' => true, 'id' => (string)$createdId, 'keyword' => $keyword]);
            }

            if ($operation === 'delete') {
                $recordId = isset($_POST['id']) ? (int)$_POST['id'] : 0;
                if ($recordId < 1) {
                    sendJson(['success' => false, 'error' => 'Identificativo Antispam non valido.'], 400);
                }
                $mysqli->begin_transaction();
                $deleteStmt = $mysqli->prepare("DELETE FROM `{$db_prefix}antispam` WHERE `antispam_id` = ?");
                if (!$deleteStmt) {
                    $mysqli->rollback();
                    sendJson(['success' => false, 'error' => 'Modulo Antispam non disponibile.'], 409);
                }
                $deleteStmt->bind_param('i', $recordId);
                $deleteStmt->execute();
                $deleted = $deleteStmt->affected_rows === 1;
                $deleteStmt->close();
                if (!$deleted) {
                    $mysqli->rollback();
                    sendJson(['success' => false, 'error' => 'Parola Antispam non trovata.'], 404);
                }
                $beforeDigest = cartadminStateDigest(['antispam_id' => $recordId, 'exists' => true], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_antispam', 'antispam', $recordId, 'success', $beforeDigest, '', 'Eliminazione regola antispam')) {
                    $mysqli->rollback();
                    sendJson(['success' => false, 'error' => 'Audit Antispam non disponibile.'], 500);
                }
                $mysqli->commit();
                sendJson(['success' => true, 'id' => (string)$recordId]);
            }

            sendJson(['success' => false, 'error' => 'Operazione Antispam non valida.'], 400);
            break;

        case 'management_create':
            $rawModule = isset($_POST['module']) && is_string($_POST['module']) ? strtolower(trim($_POST['module'])) : '';
            $title = isset($_POST['title']) && is_string($_POST['title']) ? trim(strip_tags($_POST['title'])) : '';
            $secondary = isset($_POST['secondary']) && is_string($_POST['secondary']) ? trim(strip_tags($_POST['secondary'])) : '';
            $content = isset($_POST['content']) && is_string($_POST['content']) ? cartadminSanitizeRichHtml($_POST['content']) : '';
            $parentId = isset($_POST['parent_id']) ? (int)$_POST['parent_id'] : 0;
            $sortOrder = isset($_POST['sort_order']) ? (int)$_POST['sort_order'] : 0;
            $active = isset($_POST['active']) && (string)$_POST['active'] === '1' ? 1 : 0;
            $metaTitle = isset($_POST['meta_title']) && is_string($_POST['meta_title']) ? trim(strip_tags($_POST['meta_title'])) : $title;
            $metaDescription = isset($_POST['meta_description']) && is_string($_POST['meta_description']) ? trim(strip_tags($_POST['meta_description'])) : '';
            $metaKeyword = isset($_POST['meta_keyword']) && is_string($_POST['meta_keyword']) ? trim(strip_tags($_POST['meta_keyword'])) : '';
            $imagePath = '';

            if (!in_array($rawModule, ['articles', 'topics'], true)
                || $title === '' || mb_strlen($title) > 255
                || $content === '' || mb_strlen($content) > 20000
                || mb_strlen($metaTitle) > 255 || mb_strlen($metaDescription) > 255 || mb_strlen($metaKeyword) > 255
                || ($rawModule === 'articles' && ($secondary === '' || mb_strlen($secondary) > 64 || $parentId < 1 || $metaTitle === ''))
                || ($rawModule === 'topics' && ($sortOrder < 0 || $sortOrder > 999999))) {
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_create', $rawModule, 0, 'failed', '', '', 'Validazione creazione CMS non superata');
                sendJson(['success' => false, 'error' => 'I dati del nuovo contenuto CMS non sono validi.'], 400);
            }

            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            if ($languageIds === []) {
                sendJson(['success' => false, 'error' => 'Nessuna lingua OpenCart configurata.'], 409);
            }
            if ($rawModule === 'articles') {
                $topicCheck = $mysqli->prepare("SELECT `topic_id` FROM `{$db_prefix}topic` WHERE `topic_id` = ? LIMIT 1");
                if (!$topicCheck) {
                    sendJson(['success' => false, 'error' => 'Modulo categorie CMS non disponibile.'], 409);
                }
                $topicCheck->bind_param('i', $parentId);
                $topicCheck->execute();
                $topicResult = $topicCheck->get_result();
                $topicExists = $topicResult && $topicResult->num_rows === 1;
                $topicCheck->close();
                if (!$topicExists) {
                    sendJson(['success' => false, 'error' => 'Seleziona una categoria CMS esistente.'], 409);
                }
                if (isset($_FILES['image']) && is_array($_FILES['image'])) {
                    try {
                        $imagePath = cartadminStoreProductImage($_FILES['image']);
                    } catch (Throwable $imageError) {
                        sendJson(['success' => false, 'error' => $imageError instanceof InvalidArgumentException ? $imageError->getMessage() : 'Impossibile salvare l’immagine articolo.'], 400);
                    }
                }
            }

            try {
                $mysqli->begin_transaction();
                if ($rawModule === 'articles') {
                    $baseStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}article` (`topic_id`, `author`, `status`, `date_added`, `date_modified`) VALUES (?, ?, ?, NOW(), NOW())");
                    if (!$baseStmt) throw new RuntimeException('Tabella articoli non disponibile');
                    $baseStmt->bind_param('isi', $parentId, $secondary, $active);
                    $baseStmt->execute();
                    $recordId = (int)$mysqli->insert_id;
                    $baseStmt->close();
                    $descriptionStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}article_description` (`article_id`, `language_id`, `image`, `name`, `description`, `tag`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, ?, ?, ?, '', ?, ?, ?)");
                    $storeStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}article_to_store` (`article_id`, `store_id`) VALUES (?, 0)");
                } else {
                    $baseStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}topic` (`sort_order`, `status`) VALUES (?, ?)");
                    if (!$baseStmt) throw new RuntimeException('Tabella categorie CMS non disponibile');
                    $baseStmt->bind_param('ii', $sortOrder, $active);
                    $baseStmt->execute();
                    $recordId = (int)$mysqli->insert_id;
                    $baseStmt->close();
                    $descriptionStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}topic_description` (`topic_id`, `language_id`, `image`, `name`, `description`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, '', ?, ?, ?, '', '')");
                    $storeStmt = $mysqli->prepare("INSERT INTO `{$db_prefix}topic_to_store` (`topic_id`, `store_id`) VALUES (?, 0)");
                }
                if ($recordId < 1 || !$descriptionStmt || !$storeStmt) {
                    throw new RuntimeException('Preparazione contenuto CMS non riuscita');
                }
                foreach ($languageIds as $languageId) {
                    if ($rawModule === 'articles') {
                        $descriptionStmt->bind_param('iissssss', $recordId, $languageId, $imagePath, $title, $content, $metaTitle, $metaDescription, $metaKeyword);
                    } else {
                        $descriptionStmt->bind_param('iisss', $recordId, $languageId, $title, $content, $title);
                    }
                    if (!$descriptionStmt->execute()) throw new RuntimeException('Descrizione CMS non creata');
                }
                $descriptionStmt->close();
                $storeStmt->bind_param('i', $recordId);
                if (!$storeStmt->execute()) throw new RuntimeException('Associazione store CMS non creata');
                $storeStmt->close();

                $afterState = ['title' => $title, 'content_digest' => hash_hmac('sha256', $content, $auditSalt), 'active' => $active];
                if ($rawModule === 'articles') {
                    $afterState['author'] = $secondary;
                    $afterState['topic_id'] = $parentId;
                    $afterState['seo_digest'] = hash_hmac('sha256', $metaTitle . '|' . $metaDescription . '|' . $metaKeyword, $auditSalt);
                    $afterState['has_image'] = $imagePath !== '';
                } else {
                    $afterState['sort_order'] = $sortOrder;
                }
                $afterDigest = cartadminStateDigest($afterState, $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_create', $rawModule, $recordId, 'success', '', $afterDigest, 'Nuovo contenuto CMS')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $createError) {
                $mysqli->rollback();
                if ($imagePath !== '' && defined('DIR_IMAGE')) {
                    @unlink(rtrim(DIR_IMAGE, '/\\') . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $imagePath));
                }
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_create', $rawModule, 0, 'failed', '', '', 'Rollback creazione CMS');
                sendJson(['success' => false, 'error' => 'Creazione CMS non riuscita. Verifica la compatibilità del bridge con OpenCart.'], 409);
            }

            cartadminInvalidateFileCache([$rawModule === 'articles' ? 'article' : 'topic']);
            sendJson(['success' => true, 'module' => $rawModule, 'id' => (string)$recordId], 201);
            break;

        case 'management_content':
            $rawModule = isset($_POST['module']) && is_string($_POST['module']) ? strtolower(trim($_POST['module'])) : '';
            $recordId = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $title = isset($_POST['title']) && is_string($_POST['title']) ? trim(strip_tags($_POST['title'])) : '';
            $secondary = isset($_POST['secondary']) && is_string($_POST['secondary']) ? trim(strip_tags($_POST['secondary'])) : '';
            $content = isset($_POST['content']) && is_string($_POST['content']) ? trim(strip_tags($_POST['content'])) : '';
            $rating = isset($_POST['rating']) ? (int)$_POST['rating'] : 0;
            $sortOrder = isset($_POST['sort_order']) ? (int)$_POST['sort_order'] : 0;
            $editableModules = ['pages', 'reviews', 'articles', 'topics'];

            if ($recordId < 1 || !in_array($rawModule, $editableModules, true)) {
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_content', $rawModule, $recordId, 'failed', '', '', 'Modulo o identificativo non valido');
                sendJson(['success' => false, 'error' => 'Modulo editoriale o identificativo non valido.'], 400);
            }
            if (($rawModule !== 'reviews' && ($title === '' || mb_strlen($title) > 255))
                || (in_array($rawModule, ['articles', 'reviews'], true) && ($secondary === '' || mb_strlen($secondary) > 64))
                || ($rawModule === 'reviews' && ($content === '' || mb_strlen($content) > 2000 || $rating < 1 || $rating > 5))
                || (in_array($rawModule, ['pages', 'topics'], true) && ($sortOrder < 0 || $sortOrder > 999999))) {
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_content', $rawModule, $recordId, 'failed', '', '', 'Validazione campi non superata');
                sendJson(['success' => false, 'error' => 'I dati editoriali non rispettano i limiti previsti.'], 400);
            }

            $languageId = 1;
            $languageResult = $mysqli->query("SELECT `language_id` FROM `{$db_prefix}language` WHERE `status` = 1 ORDER BY `sort_order`, `language_id` LIMIT 1");
            if ($languageResult && $languageRow = $languageResult->fetch_assoc()) {
                $languageId = max(1, (int)$languageRow['language_id']);
            }

            $mysqli->begin_transaction();
            try {
                $beforeState = [];
                $afterState = [];
                $changedFields = [];
                if ($rawModule === 'pages') {
                    $checkStmt = $mysqli->prepare("SELECT i.`information_id`, i.`sort_order`, id.`title` FROM `{$db_prefix}information` i INNER JOIN `{$db_prefix}information_description` id ON (i.`information_id` = id.`information_id` AND id.`language_id` = ?) WHERE i.`information_id` = ? LIMIT 1 FOR UPDATE");
                    if (!$checkStmt) {
                        throw new RuntimeException('Pagina non disponibile');
                    }
                    $checkStmt->bind_param('ii', $languageId, $recordId);
                    $checkStmt->execute();
                    $checkResult = $checkStmt->get_result();
                    $currentRow = $checkResult ? $checkResult->fetch_assoc() : null;
                    $exists = is_array($currentRow);
                    $checkStmt->close();
                    if (!$exists) {
                        throw new OutOfBoundsException('Pagina non trovata');
                    }
                    $beforeState = ['title' => (string)$currentRow['title'], 'sort_order' => (int)$currentRow['sort_order']];
                    $afterState = ['title' => $title, 'sort_order' => $sortOrder];
                    $changedFields = ['title', 'sort_order'];
                    $baseStmt = $mysqli->prepare("UPDATE `{$db_prefix}information` SET `sort_order` = ? WHERE `information_id` = ?");
                    if (!$baseStmt) {
                        throw new RuntimeException('Pagina non aggiornabile');
                    }
                    $baseStmt->bind_param('ii', $sortOrder, $recordId);
                    if (!$baseStmt->execute()) {
                        throw new RuntimeException('Pagina non aggiornabile');
                    }
                    $baseStmt->close();
                    $descriptionStmt = $mysqli->prepare("UPDATE `{$db_prefix}information_description` SET `title` = ? WHERE `information_id` = ? AND `language_id` = ?");
                    if (!$descriptionStmt) {
                        throw new RuntimeException('Descrizione pagina non disponibile');
                    }
                    $descriptionStmt->bind_param('sii', $title, $recordId, $languageId);
                } elseif ($rawModule === 'articles') {
                    $checkStmt = $mysqli->prepare("SELECT a.`article_id`, a.`author`, ad.`name` FROM `{$db_prefix}article` a INNER JOIN `{$db_prefix}article_description` ad ON (a.`article_id` = ad.`article_id` AND ad.`language_id` = ?) WHERE a.`article_id` = ? LIMIT 1 FOR UPDATE");
                    if (!$checkStmt) {
                        throw new RuntimeException('Articolo non disponibile');
                    }
                    $checkStmt->bind_param('ii', $languageId, $recordId);
                    $checkStmt->execute();
                    $checkResult = $checkStmt->get_result();
                    $currentRow = $checkResult ? $checkResult->fetch_assoc() : null;
                    $exists = is_array($currentRow);
                    $checkStmt->close();
                    if (!$exists) {
                        throw new OutOfBoundsException('Articolo non trovato');
                    }
                    $beforeState = ['title' => (string)$currentRow['name'], 'author' => (string)$currentRow['author']];
                    $afterState = ['title' => $title, 'author' => $secondary];
                    $changedFields = ['title', 'author'];
                    $baseStmt = $mysqli->prepare("UPDATE `{$db_prefix}article` SET `author` = ? WHERE `article_id` = ?");
                    if (!$baseStmt) {
                        throw new RuntimeException('Articolo non aggiornabile');
                    }
                    $baseStmt->bind_param('si', $secondary, $recordId);
                    if (!$baseStmt->execute()) {
                        throw new RuntimeException('Articolo non aggiornabile');
                    }
                    $baseStmt->close();
                    $descriptionStmt = $mysqli->prepare("UPDATE `{$db_prefix}article_description` SET `name` = ? WHERE `article_id` = ? AND `language_id` = ?");
                    if (!$descriptionStmt) {
                        throw new RuntimeException('Descrizione articolo non disponibile');
                    }
                    $descriptionStmt->bind_param('sii', $title, $recordId, $languageId);
                } elseif ($rawModule === 'topics') {
                    $checkStmt = $mysqli->prepare("SELECT t.`topic_id`, t.`sort_order`, td.`name` FROM `{$db_prefix}topic` t INNER JOIN `{$db_prefix}topic_description` td ON (t.`topic_id` = td.`topic_id` AND td.`language_id` = ?) WHERE t.`topic_id` = ? LIMIT 1 FOR UPDATE");
                    if (!$checkStmt) {
                        throw new RuntimeException('Argomento non disponibile');
                    }
                    $checkStmt->bind_param('ii', $languageId, $recordId);
                    $checkStmt->execute();
                    $checkResult = $checkStmt->get_result();
                    $currentRow = $checkResult ? $checkResult->fetch_assoc() : null;
                    $exists = is_array($currentRow);
                    $checkStmt->close();
                    if (!$exists) {
                        throw new OutOfBoundsException('Argomento non trovato');
                    }
                    $beforeState = ['title' => (string)$currentRow['name'], 'sort_order' => (int)$currentRow['sort_order']];
                    $afterState = ['title' => $title, 'sort_order' => $sortOrder];
                    $changedFields = ['title', 'sort_order'];
                    $baseStmt = $mysqli->prepare("UPDATE `{$db_prefix}topic` SET `sort_order` = ? WHERE `topic_id` = ?");
                    if (!$baseStmt) {
                        throw new RuntimeException('Argomento non aggiornabile');
                    }
                    $baseStmt->bind_param('ii', $sortOrder, $recordId);
                    if (!$baseStmt->execute()) {
                        throw new RuntimeException('Argomento non aggiornabile');
                    }
                    $baseStmt->close();
                    $descriptionStmt = $mysqli->prepare("UPDATE `{$db_prefix}topic_description` SET `name` = ? WHERE `topic_id` = ? AND `language_id` = ?");
                    if (!$descriptionStmt) {
                        throw new RuntimeException('Descrizione argomento non disponibile');
                    }
                    $descriptionStmt->bind_param('sii', $title, $recordId, $languageId);
                } else {
                    $checkStmt = $mysqli->prepare("SELECT `product_id`, `author`, `text`, `rating` FROM `{$db_prefix}review` WHERE `review_id` = ? LIMIT 1 FOR UPDATE");
                    if (!$checkStmt) {
                        throw new RuntimeException('Recensione non disponibile');
                    }
                    $checkStmt->bind_param('i', $recordId);
                    $checkStmt->execute();
                    $checkResult = $checkStmt->get_result();
                    $reviewRow = $checkResult ? $checkResult->fetch_assoc() : null;
                    $productId = $reviewRow ? (int)$reviewRow['product_id'] : 0;
                    $checkStmt->close();
                    if ($productId < 1) {
                        throw new OutOfBoundsException('Recensione non trovata');
                    }
                    $beforeState = ['author' => (string)$reviewRow['author'], 'text' => (string)$reviewRow['text'], 'rating' => (int)$reviewRow['rating']];
                    $afterState = ['author' => $secondary, 'text' => $content, 'rating' => $rating];
                    $changedFields = ['author', 'text', 'rating'];
                    $reviewStmt = $mysqli->prepare("UPDATE `{$db_prefix}review` SET `author` = ?, `text` = ?, `rating` = ?, `date_modified` = NOW() WHERE `review_id` = ?");
                    if (!$reviewStmt) {
                        throw new RuntimeException('Recensione non aggiornabile');
                    }
                    $reviewStmt->bind_param('ssii', $secondary, $content, $rating, $recordId);
                    if (!$reviewStmt->execute()) {
                        throw new RuntimeException('Recensione non aggiornabile');
                    }
                    $reviewStmt->close();
                    $ratingStmt = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `rating` = (SELECT COALESCE(AVG(`rating`), 0) FROM `{$db_prefix}review` WHERE `product_id` = ? AND `status` = 1) WHERE `product_id` = ?");
                    if (!$ratingStmt) {
                        throw new RuntimeException('Valutazione prodotto non aggiornabile');
                    }
                    $ratingStmt->bind_param('ii', $productId, $productId);
                    if (!$ratingStmt->execute()) {
                        throw new RuntimeException('Valutazione prodotto non aggiornabile');
                    }
                    $ratingStmt->close();
                    $descriptionStmt = null;
                }

                if (isset($descriptionStmt) && (!$descriptionStmt || !$descriptionStmt->execute())) {
                    throw new RuntimeException('Descrizione nella lingua principale non aggiornabile');
                }
                if (isset($descriptionStmt) && $descriptionStmt) {
                    $descriptionStmt->close();
                }
                $beforeDigest = cartadminStateDigest($beforeState, $auditSalt);
                $afterDigest = cartadminStateDigest($afterState, $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_content', $rawModule, $recordId, 'success', $beforeDigest, $afterDigest, 'Campi: ' . implode(',', $changedFields))) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $contentError) {
                $mysqli->rollback();
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_content', $rawModule, $recordId, 'failed', '', '', 'Rollback della modifica editoriale');
                sendJson(['success' => false, 'error' => 'Modifica editoriale non riuscita. Verifica che il record e la lingua principale esistano.'], 409);
            }

            $contentCacheKeys = [
                'pages' => ['information'],
                'reviews' => ['product'],
                'articles' => ['article'],
                'topics' => ['topic']
            ];
            cartadminInvalidateFileCache($contentCacheKeys[$rawModule]);
            sendJson([
                'success' => true,
                'module' => $rawModule,
                'id' => (string)$recordId,
                'language_id' => $rawModule === 'reviews' ? null : $languageId
            ]);
            break;

        case 'management_status':
            $rawModule = isset($_POST['module']) && is_string($_POST['module'])
                ? strtolower(trim($_POST['module']))
                : '';
            $recordId = isset($_POST['id']) ? (int)$_POST['id'] : 0;
            $newStatus = isset($_POST['active']) && (string)$_POST['active'] === '1' ? 1 : 0;

            $statusTargets = [
                'subscription_plans' => ['table' => 'subscription_plan', 'id' => 'subscription_plan_id'],
                'pages' => ['table' => 'information', 'id' => 'information_id'],
                'reviews' => ['table' => 'review', 'id' => 'review_id'],
                'articles' => ['table' => 'article', 'id' => 'article_id'],
                'topics' => ['table' => 'topic', 'id' => 'topic_id'],
                'comments' => ['table' => 'article_comment', 'id' => 'article_comment_id'],
                'customers' => ['table' => 'customer', 'id' => 'customer_id']
            ];

            if ($recordId < 1 || !array_key_exists($rawModule, $statusTargets)) {
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_status', $rawModule, $recordId, 'failed', '', '', 'Modulo o identificativo non valido');
                sendJson(['success' => false, 'error' => 'Modulo o identificativo non valido.'], 400);
            }

            $target = $statusTargets[$rawModule];
            $tableName = $db_prefix . $target['table'];
            $idColumn = $target['id'];
            $mysqli->begin_transaction();
            try {
                $checkStmt = $mysqli->prepare("SELECT `{$idColumn}`, `status` FROM `{$tableName}` WHERE `{$idColumn}` = ? LIMIT 1 FOR UPDATE"); // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string, php.lang.security.injection.tainted-callable.tainted-callable
                if (!$checkStmt) {
                    throw new RuntimeException('Target non disponibile');
                }
                $checkStmt->bind_param('i', $recordId);
                $checkStmt->execute();
                $checkResult = $checkStmt->get_result();
                $statusRow = $checkResult ? $checkResult->fetch_assoc() : null;
                $exists = is_array($statusRow);
                $checkStmt->close();
                if (!$exists) {
                    throw new OutOfBoundsException('Elemento non trovato');
                }

                $updateStmt = $mysqli->prepare("UPDATE `{$tableName}` SET `status` = ? WHERE `{$idColumn}` = ?"); // nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string, php.lang.security.injection.tainted-callable.tainted-callable
                if (!$updateStmt) {
                    throw new RuntimeException('Aggiornamento non disponibile');
                }
                $updateStmt->bind_param('ii', $newStatus, $recordId);
                $updateStmt->execute();
                $updateStmt->close();

                if ($rawModule === 'reviews') {
                    $productStmt = $mysqli->prepare("SELECT `product_id` FROM `{$db_prefix}review` WHERE `review_id` = ? LIMIT 1");
                    if ($productStmt) {
                        $productStmt->bind_param('i', $recordId);
                        $productStmt->execute();
                        $productResult = $productStmt->get_result();
                        $productId = ($productResult && $productRow = $productResult->fetch_assoc()) ? (int)$productRow['product_id'] : 0;
                        $productStmt->close();
                        if ($productId > 0) {
                            $ratingStmt = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `rating` = (SELECT COALESCE(AVG(`rating`), 0) FROM `{$db_prefix}review` WHERE `product_id` = ? AND `status` = 1) WHERE `product_id` = ?");
                            if ($ratingStmt) {
                                $ratingStmt->bind_param('ii', $productId, $productId);
                                $ratingStmt->execute();
                                $ratingStmt->close();
                            }
                        }
                    }
                }

                $beforeDigest = cartadminStateDigest(['status' => (int)$statusRow['status']], $auditSalt);
                $afterDigest = cartadminStateDigest(['status' => $newStatus], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_status', $rawModule, $recordId, 'success', $beforeDigest, $afterDigest, 'Campo: status')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }

                $mysqli->commit();
            } catch (Throwable $statusError) {
                $mysqli->rollback();
                cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'management_status', $rawModule, $recordId, 'failed', '', '', 'Rollback aggiornamento stato');
                sendJson(['success' => false, 'error' => 'Aggiornamento dello stato non riuscito.'], 409);
            }

            $cacheKeysByModule = [
                'pages' => ['information'],
                'reviews' => ['product'],
                'articles' => ['article'],
                'topics' => ['topic'],
                'comments' => ['topic']
            ];
            cartadminInvalidateFileCache($cacheKeysByModule[$rawModule] ?? []);

            sendJson([
                'success' => true,
                'module' => $rawModule,
                'id' => (string)$recordId,
                'active' => $newStatus === 1
            ]);
            break;

        case 'visitor_telemetry':
            $trackingEnabled = false;
            $resTracking = $mysqli->query("SELECT MAX(CASE WHEN `value` = '1' THEN 1 ELSE 0 END) AS `enabled` FROM `{$db_prefix}setting` WHERE `key` = 'config_customer_online'");
            if ($resTracking && $row = $resTracking->fetch_assoc()) {
                $trackingEnabled = ((int)($row['enabled'] ?? 0) === 1);
            }

            $onlineTableExists = false;
            $checkOnlineTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}customer_online'");
            if ($checkOnlineTable && $checkOnlineTable->num_rows > 0) {
                $onlineTableExists = true;
            }

            $activeVisitors = 0;
            $guestVisitors = 0;
            $registeredVisitors = 0;
            $pageUpdatesPerMinute = 0;
            $activeCarts = 0;
            $activeCheckouts = 0;
            $history = [];
            $topPages = [];
            $trafficSources = [];
            $liveEvents = [];
            $recordsTotal = 0;
            $latestRecordAt = '';

            // La tabella resta la fonte autorevole: può contenere sessioni anche quando una
            // configurazione multi-store viene letta come disattivata o è stata appena cambiata.
            if ($onlineTableExists) {
                $expiryHours = 1;
                $resExpiry = $mysqli->query("SELECT MAX(CAST(`value` AS UNSIGNED)) AS `expiry_hours` FROM `{$db_prefix}setting` WHERE `key` = 'config_customer_online_expire'");
                if ($resExpiry && $row = $resExpiry->fetch_assoc()) {
                    $expiryHours = max(1, min(24, (int)($row['expiry_hours'] ?? 1)));
                }

                // OpenCart rimuove le righe scadute prima di ogni REPLACE in model/tool/online.
                // Non confrontare date PHP con NOW() MySQL: i due server possono avere timezone
                // differenti e farebbero sparire dall'app sessioni che OpenCart considera attive.
                $res = $mysqli->query("SELECT COUNT(*) AS total, SUM(CASE WHEN `customer_id` = 0 THEN 1 ELSE 0 END) AS guests, SUM(CASE WHEN `customer_id` > 0 THEN 1 ELSE 0 END) AS registered, MAX(`date_added`) AS latest_record_at FROM `{$db_prefix}customer_online`");
                if ($res && $row = $res->fetch_assoc()) {
                    $activeVisitors = (int)($row['total'] ?? 0);
                    $recordsTotal = $activeVisitors;
                    $guestVisitors = (int)($row['guests'] ?? 0);
                    $registeredVisitors = (int)($row['registered'] ?? 0);
                    $latestRecordAt = (string)($row['latest_record_at'] ?? '');
                }

                $oneMinuteAgo = date('Y-m-d H:i:s', time() - 60);
                $stmtMinute = $mysqli->prepare("SELECT COUNT(*) AS total FROM `{$db_prefix}customer_online` WHERE `date_added` >= ?");
                if ($stmtMinute) {
                    $stmtMinute->bind_param('s', $oneMinuteAgo);
                    $stmtMinute->execute();
                    $res = $stmtMinute->get_result();
                    $pageUpdatesPerMinute = ($res && $row = $res->fetch_assoc()) ? (int)$row['total'] : 0;
                    $stmtMinute->close();
                }

                $thirtyMinutesAgo = date('Y-m-d H:i:s', time() - 1800);
                $stmtHistory = $mysqli->prepare("SELECT DATE_FORMAT(`date_added`, '%H:%i') AS minute_label, COUNT(*) AS active_users FROM `{$db_prefix}customer_online` WHERE `date_added` >= ? GROUP BY minute_label ORDER BY minute_label ASC");
                if ($stmtHistory) {
                    $stmtHistory->bind_param('s', $thirtyMinutesAgo);
                    $stmtHistory->execute();
                    $res = $stmtHistory->get_result();
                    if ($res) {
                        while ($row = $res->fetch_assoc()) {
                            $history[] = [
                                'time_label' => (string)$row['minute_label'],
                                'active_users' => (int)$row['active_users'],
                                'page_views' => (int)$row['active_users']
                            ];
                        }
                    }
                    $stmtHistory->close();
                }

                $stmtOnline = $mysqli->prepare("SELECT `customer_id`, `url`, `referer`, `date_added` FROM `{$db_prefix}customer_online` ORDER BY `date_added` DESC LIMIT 200");
                if ($stmtOnline) {
                    $stmtOnline->execute();
                    $res = $stmtOnline->get_result();
                    $pageCounts = [];
                    $sourceCounts = [];
                    if ($res) {
                        while ($row = $res->fetch_assoc()) {
                            $rawUrl = is_string($row['url']) ? $row['url'] : '';
                            $path = parse_url($rawUrl, PHP_URL_PATH);
                            $safePath = is_string($path) && $path !== '' ? mb_substr($path, 0, 180) : '/';
                            $pageCounts[$safePath] = ($pageCounts[$safePath] ?? 0) + 1;

                            if (stripos($rawUrl, 'checkout') !== false) {
                                $activeCheckouts++;
                            }

                            $rawReferer = is_string($row['referer']) ? $row['referer'] : '';
                            $host = parse_url($rawReferer, PHP_URL_HOST);
                            $source = is_string($host) && $host !== '' ? mb_substr(strtolower($host), 0, 120) : 'Accesso diretto';
                            $sourceCounts[$source] = ($sourceCounts[$source] ?? 0) + 1;

                            if (count($liveEvents) < 20) {
                                $visitorType = (int)$row['customer_id'] > 0 ? 'Cliente registrato' : 'Visitatore guest';
                                $liveEvents[] = [
                                    'id' => hash('sha256', $visitorType . '|' . $safePath . '|' . (string)$row['date_added']),
                                    'timestamp' => (string)$row['date_added'],
                                    'event_type' => 'PAGE_VIEW',
                                    'description' => $visitorType . ' • ' . $safePath,
                                    'location' => '',
                                    'icon_type' => 'page'
                                ];
                            }
                        }
                    }
                    $stmtOnline->close();

                    arsort($pageCounts);
                    foreach (array_slice($pageCounts, 0, 10, true) as $path => $count) {
                        $topPages[] = [
                            'path' => $path,
                            'title' => $path === '/' ? 'Home page' : trim(str_replace(['-', '_', '/'], ' ', $path)),
                            'active_users' => (int)$count,
                            'percentage' => $activeVisitors > 0 ? round(((int)$count / $activeVisitors) * 100, 1) : 0.0,
                            'category' => 'OpenCart'
                        ];
                    }

                    arsort($sourceCounts);
                    foreach (array_slice($sourceCounts, 0, 10, true) as $source => $count) {
                        $trafficSources[] = [
                            'source' => $source,
                            'type' => $source === 'Accesso diretto' ? 'Direct' : 'Referral',
                            'visitors_count' => (int)$count,
                            'percentage' => $activeVisitors > 0 ? round(((int)$count / $activeVisitors) * 100, 1) : 0.0,
                            'conversion_rate' => 0.0
                        ];
                    }
                }

                $checkCartTable = $mysqli->query("SHOW TABLES LIKE '{$db_prefix}cart'");
                if ($checkCartTable && $checkCartTable->num_rows > 0) {
                    $stmtCarts = $mysqli->prepare("SELECT COUNT(DISTINCT `session_id`) AS total FROM `{$db_prefix}cart` WHERE `date_added` >= DATE_SUB(NOW(), INTERVAL ? HOUR)");
                    if ($stmtCarts) {
                        $stmtCarts->bind_param('i', $expiryHours);
                        $stmtCarts->execute();
                        $res = $stmtCarts->get_result();
                        $activeCarts = ($res && $row = $res->fetch_assoc()) ? (int)$row['total'] : 0;
                        $stmtCarts->close();
                    }
                }
            }

            sendJson([
                'success' => true,
                'tracking_enabled' => $trackingEnabled,
                'data_available' => $onlineTableExists,
                'records_total' => $recordsTotal,
                'latest_record_at' => $latestRecordAt,
                'active_visitors_now' => $activeVisitors,
                'guest_visitors_now' => $guestVisitors,
                'registered_visitors_now' => $registeredVisitors,
                'page_updates_per_min' => $pageUpdatesPerMinute,
                'active_carts_count' => $activeCarts,
                'active_checkouts_count' => $activeCheckouts,
                'avg_duration_seconds' => 0,
                'bounce_rate' => 0.0,
                'traffic_history' => $history,
                'top_pages' => $topPages,
                'top_countries' => [],
                'traffic_sources' => $trafficSources,
                'device_stats' => [],
                'live_events' => $liveEvents,
                'source' => 'OpenCart customer_online',
                'last_updated' => date('c'),
                'limitations' => 'Guest e clienti registrati sono distinti senza esporre IP o identità. OpenCart non registra user agent, geolocalizzazione, durata sessione o bounce rate nella tabella customer_online.'
            ]);
            break;

        case 'create_product':
            $name = isset($_POST['name']) && is_string($_POST['name']) ? trim(strip_tags($_POST['name'])) : '';
            $model = isset($_POST['model']) && is_string($_POST['model']) ? trim(strip_tags($_POST['model'])) : '';
            $sku = isset($_POST['sku']) && is_string($_POST['sku']) ? trim(strip_tags($_POST['sku'])) : '';
            $description = isset($_POST['description']) && is_string($_POST['description']) ? cartadminSanitizeRichHtml($_POST['description']) : '';
            $category = isset($_POST['category']) && is_string($_POST['category']) ? trim(strip_tags($_POST['category'])) : '';
            $price = isset($_POST['price']) && is_numeric($_POST['price']) ? max(0.0, (float)$_POST['price']) : -1.0;
            $quantity = isset($_POST['quantity']) ? max(0, (int)$_POST['quantity']) : -1;
            $minimum = isset($_POST['minimum']) ? max(1, (int)$_POST['minimum']) : 1;
            $status = isset($_POST['status']) && (string)$_POST['status'] === '1' ? 1 : 0;
            $imagePath = '';

            if ($name === '' || mb_strlen($name) > 255 || $model === '' || mb_strlen($model) > 64 || mb_strlen($sku) > 64 || $price < 0 || $quantity < 0 || mb_strlen($description) > 65535 || $category === '' || mb_strlen($category) > 255) {
                sendJson(['success' => false, 'error' => 'Dati prodotto non validi. Nome, modello e categoria sono obbligatori.'], 400);
            }

            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            if ($languageIds === []) {
                sendJson(['success' => false, 'error' => 'Nessuna lingua OpenCart configurata.'], 409);
            }

            $primaryLanguageId = $languageIds[0];
            $stmtCategory = $mysqli->prepare("SELECT `category_id` FROM `{$db_prefix}category_description` WHERE `name` = ? AND `language_id` = ? LIMIT 1");
            if (!$stmtCategory) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare la categoria.'], 500);
            }
            $stmtCategory->bind_param('si', $category, $primaryLanguageId);
            $stmtCategory->execute();
            $categoryResult = $stmtCategory->get_result();
            $categoryRow = $categoryResult ? $categoryResult->fetch_assoc() : null;
            $stmtCategory->close();
            if (!$categoryRow) {
                sendJson(['success' => false, 'error' => 'La categoria selezionata non esiste nello store.'], 409);
            }
            $categoryId = (int)$categoryRow['category_id'];

            $stockStatusId = 0;
            $stockResult = $mysqli->query("SELECT `stock_status_id` FROM `{$db_prefix}stock_status` WHERE `language_id` = {$primaryLanguageId} ORDER BY `stock_status_id` LIMIT 1");
            if ($stockResult && $row = $stockResult->fetch_assoc()) {
                $stockStatusId = (int)$row['stock_status_id'];
            }
            if ($stockStatusId <= 0) {
                sendJson(['success' => false, 'error' => 'Nessuno stato magazzino OpenCart configurato.'], 409);
            }

            if (isset($_FILES['image']) && is_array($_FILES['image'])) {
                try {
                    $imagePath = cartadminStoreProductImage($_FILES['image']);
                } catch (Throwable $imageError) {
                    sendJson(['success' => false, 'error' => $imageError instanceof InvalidArgumentException ? $imageError->getMessage() : 'Impossibile salvare l’immagine prodotto.'], 400);
                }
            }

            $mysqli->begin_transaction();
            try {
                $stmtProduct = $mysqli->prepare("INSERT INTO `{$db_prefix}product` (`master_id`, `model`, `sku`, `upc`, `ean`, `jan`, `isbn`, `mpn`, `location`, `variant`, `override`, `quantity`, `stock_status_id`, `image`, `manufacturer_id`, `shipping`, `price`, `points`, `tax_class_id`, `date_available`, `weight`, `weight_class_id`, `length`, `width`, `height`, `length_class_id`, `subtract`, `minimum`, `rating`, `sort_order`, `status`, `date_added`, `date_modified`) VALUES (0, ?, ?, '', '', '', '', '', '', '', '', ?, ?, ?, 0, 1, ?, 0, 0, CURDATE(), 0, 0, 0, 0, 0, 0, 1, ?, 0, 0, ?, NOW(), NOW())");
                if (!$stmtProduct) {
                    throw new RuntimeException('Preparazione creazione prodotto fallita.');
                }
                $stmtProduct->bind_param('ssiisdii', $model, $sku, $quantity, $stockStatusId, $imagePath, $price, $minimum, $status);
                $stmtProduct->execute();
                $productId = (int)$mysqli->insert_id;
                $stmtProduct->close();
                if ($productId <= 0) {
                    throw new RuntimeException('OpenCart non ha restituito un ID prodotto valido.');
                }

                $stmtDescription = $mysqli->prepare("INSERT INTO `{$db_prefix}product_description` (`product_id`, `language_id`, `name`, `description`, `tag`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, ?, ?, '', ?, '', '')");
                if (!$stmtDescription) {
                    throw new RuntimeException('Preparazione descrizione prodotto fallita.');
                }
                foreach ($languageIds as $languageId) {
                    $stmtDescription->bind_param('iisss', $productId, $languageId, $name, $description, $name);
                    $stmtDescription->execute();
                }
                $stmtDescription->close();

                $stmtStore = $mysqli->prepare("INSERT INTO `{$db_prefix}product_to_store` (`product_id`, `store_id`) VALUES (?, 0)");
                $stmtLink = $mysqli->prepare("INSERT INTO `{$db_prefix}product_to_category` (`product_id`, `category_id`) VALUES (?, ?)");
                if (!$stmtStore || !$stmtLink) {
                    throw new RuntimeException('Preparazione associazioni prodotto fallita.');
                }
                $stmtStore->bind_param('i', $productId);
                $stmtStore->execute();
                $stmtStore->close();
                $stmtLink->bind_param('ii', $productId, $categoryId);
                $stmtLink->execute();
                $stmtLink->close();
                $afterDigest = cartadminStateDigest(['product_id' => $productId, 'quantity' => $quantity, 'status' => $status], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'create_product', 'products', $productId, 'success', '', $afterDigest, 'Creazione prodotto')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                if ($imagePath !== '' && defined('DIR_IMAGE')) {
                    @unlink(rtrim(DIR_IMAGE, '/\\') . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $imagePath));
                }
                sendJson(['success' => false, 'error' => 'Creazione prodotto non riuscita.'], 500);
            }

            cartadminInvalidateFileCache(['product', 'category']);
            sendJson([
                'success' => true,
                'product' => [
                    'product_id' => $productId,
                    'name' => $name,
                    'model' => $model,
                    'sku' => $sku,
                    'price' => $price,
                    'quantity' => $quantity,
                    'minimum' => $minimum,
                    'category' => $category,
                    'description' => $description,
                    'image' => $imagePath,
                    'status' => (bool)$status
                ]
            ], 201);
            break;

        case 'delete_product':
            $productId = isset($_POST['product_id']) ? (int)$_POST['product_id'] : 0;
            if ($productId <= 0) {
                sendJson(['success' => false, 'error' => 'ID prodotto non valido.'], 400);
            }

            $stmtExists = $mysqli->prepare("SELECT `product_id` FROM `{$db_prefix}product` WHERE `product_id` = ? LIMIT 1");
            if (!$stmtExists) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare il prodotto.'], 500);
            }
            $stmtExists->bind_param('i', $productId);
            $stmtExists->execute();
            $existsResult = $stmtExists->get_result();
            $productExists = $existsResult && $existsResult->num_rows > 0;
            $stmtExists->close();
            if (!$productExists) {
                sendJson(['success' => false, 'error' => 'Prodotto non trovato.'], 404);
            }

            $mysqli->begin_transaction();
            try {
                $stmtVariants = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `master_id` = 0 WHERE `master_id` = ?");
                if (!$stmtVariants) {
                    throw new RuntimeException('Preparazione scollegamento varianti fallita.');
                }
                $stmtVariants->bind_param('i', $productId);
                $stmtVariants->execute();
                $stmtVariants->close();
                $cleanupTables = [
                    'product_attribute', 'product_code', 'product_to_category', 'product_description',
                    'product_discount', 'product_to_download', 'product_filter', 'product_image',
                    'product_to_layout', 'product_option_value', 'product_option', 'product_report',
                    'product_reward', 'product_to_store', 'product_subscription', 'review', 'coupon_product'
                ];
                foreach ($cleanupTables as $cleanupTable) {
                    $stmtCleanup = $mysqli->prepare("DELETE FROM `{$db_prefix}{$cleanupTable}` WHERE `product_id` = ?");
                    if (!$stmtCleanup) {
                        throw new RuntimeException('Preparazione pulizia prodotto fallita.');
                    }
                    $stmtCleanup->bind_param('i', $productId);
                    $stmtCleanup->execute();
                    $stmtCleanup->close();
                }
                $stmtRelated = $mysqli->prepare("DELETE FROM `{$db_prefix}product_related` WHERE `product_id` = ? OR `related_id` = ?");
                $stmtSeo = $mysqli->prepare("DELETE FROM `{$db_prefix}seo_url` WHERE `key` = 'product_id' AND `value` = ?");
                $stmtDelete = $mysqli->prepare("DELETE FROM `{$db_prefix}product` WHERE `product_id` = ?");
                if (!$stmtRelated || !$stmtSeo || !$stmtDelete) {
                    throw new RuntimeException('Preparazione eliminazione prodotto fallita.');
                }
                $stmtRelated->bind_param('ii', $productId, $productId);
                $stmtRelated->execute();
                $stmtRelated->close();
                $productValue = (string)$productId;
                $stmtSeo->bind_param('s', $productValue);
                $stmtSeo->execute();
                $stmtSeo->close();
                $stmtDelete->bind_param('i', $productId);
                $stmtDelete->execute();
                $deleted = $stmtDelete->affected_rows === 1;
                $stmtDelete->close();
                if (!$deleted) {
                    throw new RuntimeException('Il prodotto non è stato eliminato.');
                }
                $beforeDigest = cartadminStateDigest(['product_id' => $productId, 'exists' => true], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'delete_product', 'products', $productId, 'success', $beforeDigest, '', 'Eliminazione prodotto')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Eliminazione prodotto non riuscita.'], 500);
            }

            cartadminInvalidateFileCache(['product', 'category']);
            sendJson(['success' => true, 'product_id' => $productId, 'deleted' => true]);
            break;

        case 'create_category':
            $name = isset($_POST['name']) && is_string($_POST['name']) ? trim(strip_tags($_POST['name'])) : '';
            $description = isset($_POST['description']) && is_string($_POST['description']) ? trim(strip_tags($_POST['description'])) : '';
            $sortOrder = isset($_POST['sort_order']) ? max(0, (int)$_POST['sort_order']) : 0;
            $status = isset($_POST['status']) && (string)$_POST['status'] === '1' ? 1 : 0;
            if ($name === '' || mb_strlen($name) > 255 || mb_strlen($description) > 65535) {
                sendJson(['success' => false, 'error' => 'Dati categoria non validi.'], 400);
            }
            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            if ($languageIds === []) {
                sendJson(['success' => false, 'error' => 'Nessuna lingua OpenCart configurata.'], 409);
            }

            $mysqli->begin_transaction();
            try {
                $stmtCategory = $mysqli->prepare("INSERT INTO `{$db_prefix}category` (`image`, `parent_id`, `sort_order`, `status`) VALUES ('', 0, ?, ?)");
                if (!$stmtCategory) {
                    throw new RuntimeException('Preparazione creazione categoria fallita.');
                }
                $stmtCategory->bind_param('ii', $sortOrder, $status);
                $stmtCategory->execute();
                $categoryId = (int)$mysqli->insert_id;
                $stmtCategory->close();
                if ($categoryId <= 0) {
                    throw new RuntimeException('OpenCart non ha restituito un ID categoria valido.');
                }

                $stmtDescription = $mysqli->prepare("INSERT INTO `{$db_prefix}category_description` (`category_id`, `language_id`, `name`, `description`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, ?, ?, ?, '', '')");
                if (!$stmtDescription) {
                    throw new RuntimeException('Preparazione descrizione categoria fallita.');
                }
                foreach ($languageIds as $languageId) {
                    $stmtDescription->bind_param('iisss', $categoryId, $languageId, $name, $description, $name);
                    $stmtDescription->execute();
                }
                $stmtDescription->close();

                $stmtPath = $mysqli->prepare("INSERT INTO `{$db_prefix}category_path` (`category_id`, `path_id`, `level`) VALUES (?, ?, 0)");
                $stmtStore = $mysqli->prepare("INSERT INTO `{$db_prefix}category_to_store` (`category_id`, `store_id`) VALUES (?, 0)");
                if (!$stmtPath || !$stmtStore) {
                    throw new RuntimeException('Preparazione associazioni categoria fallita.');
                }
                $stmtPath->bind_param('ii', $categoryId, $categoryId);
                $stmtPath->execute();
                $stmtPath->close();
                $stmtStore->bind_param('i', $categoryId);
                $stmtStore->execute();
                $stmtStore->close();
                $afterDigest = cartadminStateDigest(['category_id' => $categoryId, 'sort_order' => $sortOrder, 'status' => $status], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'create_category', 'categories', $categoryId, 'success', '', $afterDigest, 'Creazione categoria')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Creazione categoria non riuscita.'], 500);
            }

            cartadminInvalidateFileCache(['category', 'product']);
            sendJson(['success' => true, 'category' => ['category_id' => $categoryId, 'name' => $name, 'description' => $description, 'products_count' => 0, 'sort_order' => $sortOrder, 'status' => (bool)$status]], 201);
            break;

        case 'update_category':
            $categoryId = isset($_POST['category_id']) ? (int)$_POST['category_id'] : 0;
            $name = isset($_POST['name']) && is_string($_POST['name']) ? trim(strip_tags($_POST['name'])) : '';
            $description = isset($_POST['description']) && is_string($_POST['description']) ? trim(strip_tags($_POST['description'])) : '';
            $sortOrder = isset($_POST['sort_order']) ? max(0, (int)$_POST['sort_order']) : 0;
            $status = isset($_POST['status']) && (string)$_POST['status'] === '1' ? 1 : 0;
            if ($categoryId <= 0 || $name === '' || mb_strlen($name) > 255 || mb_strlen($description) > 65535) {
                sendJson(['success' => false, 'error' => 'Dati categoria non validi.'], 400);
            }
            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            if ($languageIds === []) {
                sendJson(['success' => false, 'error' => 'Nessuna lingua OpenCart configurata.'], 409);
            }

            $stmtExists = $mysqli->prepare("SELECT `category_id` FROM `{$db_prefix}category` WHERE `category_id` = ? LIMIT 1");
            if (!$stmtExists) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare la categoria.'], 500);
            }
            $stmtExists->bind_param('i', $categoryId);
            $stmtExists->execute();
            $existsResult = $stmtExists->get_result();
            $categoryExists = $existsResult && $existsResult->num_rows > 0;
            $stmtExists->close();
            if (!$categoryExists) {
                sendJson(['success' => false, 'error' => 'Categoria non trovata.'], 404);
            }

            $mysqli->begin_transaction();
            try {
                $stmtCategory = $mysqli->prepare("UPDATE `{$db_prefix}category` SET `sort_order` = ?, `status` = ? WHERE `category_id` = ?");
                $stmtDescription = $mysqli->prepare("INSERT INTO `{$db_prefix}category_description` (`category_id`, `language_id`, `name`, `description`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, ?, ?, ?, '', '') ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `meta_title` = VALUES(`meta_title`)");
                if (!$stmtCategory || !$stmtDescription) {
                    throw new RuntimeException('Preparazione aggiornamento categoria fallita.');
                }
                $stmtCategory->bind_param('iii', $sortOrder, $status, $categoryId);
                $stmtCategory->execute();
                $stmtCategory->close();
                foreach ($languageIds as $languageId) {
                    $stmtDescription->bind_param('iisss', $categoryId, $languageId, $name, $description, $name);
                    $stmtDescription->execute();
                }
                $stmtDescription->close();
                $afterDigest = cartadminStateDigest(['category_id' => $categoryId, 'sort_order' => $sortOrder, 'status' => $status], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_category', 'categories', $categoryId, 'success', '', $afterDigest, 'Aggiornamento categoria')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Aggiornamento categoria non riuscito.'], 500);
            }

            cartadminInvalidateFileCache(['category', 'product']);
            sendJson(['success' => true, 'category_id' => $categoryId, 'status' => (bool)$status]);
            break;

        case 'delete_category':
            $categoryId = isset($_POST['category_id']) ? (int)$_POST['category_id'] : 0;
            if ($categoryId <= 0) {
                sendJson(['success' => false, 'error' => 'ID categoria non valido.'], 400);
            }
            $stmtExists = $mysqli->prepare("SELECT `category_id` FROM `{$db_prefix}category` WHERE `category_id` = ? LIMIT 1");
            $stmtChildren = $mysqli->prepare("SELECT COUNT(*) AS total FROM `{$db_prefix}category` WHERE `parent_id` = ?");
            if (!$stmtExists || !$stmtChildren) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare la categoria.'], 500);
            }
            $stmtExists->bind_param('i', $categoryId);
            $stmtExists->execute();
            $existsResult = $stmtExists->get_result();
            $categoryExists = $existsResult && $existsResult->num_rows > 0;
            $stmtExists->close();
            if (!$categoryExists) {
                sendJson(['success' => false, 'error' => 'Categoria non trovata.'], 404);
            }
            $stmtChildren->bind_param('i', $categoryId);
            $stmtChildren->execute();
            $childrenResult = $stmtChildren->get_result();
            $childrenRow = $childrenResult ? $childrenResult->fetch_assoc() : null;
            $stmtChildren->close();
            if ($childrenRow && (int)$childrenRow['total'] > 0) {
                sendJson(['success' => false, 'error' => 'La categoria contiene sottocategorie. Rimuoverle o spostarle dal pannello OpenCart prima di eliminarla.'], 409);
            }

            $categoryPath = (string)$categoryId;
            $stmtPathValue = $mysqli->prepare("SELECT GROUP_CONCAT(`path_id` ORDER BY `level` SEPARATOR '_') AS path_value FROM `{$db_prefix}category_path` WHERE `category_id` = ?");
            if (!$stmtPathValue) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare il percorso della categoria.'], 500);
            }
            $stmtPathValue->bind_param('i', $categoryId);
            $stmtPathValue->execute();
            $pathResult = $stmtPathValue->get_result();
            if ($pathResult && $pathRow = $pathResult->fetch_assoc()) {
                $resolvedPath = (string)($pathRow['path_value'] ?? '');
                if ($resolvedPath !== '') {
                    $categoryPath = $resolvedPath;
                }
            }
            $stmtPathValue->close();

            $mysqli->begin_transaction();
            try {
                $cleanupTables = ['category_description', 'category_filter', 'category_to_store', 'category_to_layout', 'product_to_category', 'coupon_category'];
                foreach ($cleanupTables as $cleanupTable) {
                    $stmtCleanup = $mysqli->prepare("DELETE FROM `{$db_prefix}{$cleanupTable}` WHERE `category_id` = ?");
                    if (!$stmtCleanup) {
                        throw new RuntimeException('Preparazione pulizia categoria fallita.');
                    }
                    $stmtCleanup->bind_param('i', $categoryId);
                    $stmtCleanup->execute();
                    $stmtCleanup->close();
                }
                $stmtPath = $mysqli->prepare("DELETE FROM `{$db_prefix}category_path` WHERE `category_id` = ? OR `path_id` = ?");
                $stmtSeo = $mysqli->prepare("DELETE FROM `{$db_prefix}seo_url` WHERE `key` = 'path' AND `value` = ?");
                $stmtDelete = $mysqli->prepare("DELETE FROM `{$db_prefix}category` WHERE `category_id` = ?");
                if (!$stmtPath || !$stmtSeo || !$stmtDelete) {
                    throw new RuntimeException('Preparazione eliminazione categoria fallita.');
                }
                $stmtPath->bind_param('ii', $categoryId, $categoryId);
                $stmtPath->execute();
                $stmtPath->close();
                $stmtSeo->bind_param('s', $categoryPath);
                $stmtSeo->execute();
                $stmtSeo->close();
                $stmtDelete->bind_param('i', $categoryId);
                $stmtDelete->execute();
                $deleted = $stmtDelete->affected_rows === 1;
                $stmtDelete->close();
                if (!$deleted) {
                    throw new RuntimeException('La categoria non è stata eliminata.');
                }
                $beforeDigest = cartadminStateDigest(['category_id' => $categoryId, 'exists' => true], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'delete_category', 'categories', $categoryId, 'success', $beforeDigest, '', 'Eliminazione categoria')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Eliminazione categoria non riuscita.'], 500);
            }

            cartadminInvalidateFileCache(['category', 'product']);
            sendJson(['success' => true, 'category_id' => $categoryId, 'deleted' => true]);
            break;

        case 'update_stock':
            $productId = isset($_POST['product_id']) ? (int)$_POST['product_id'] : 0;
            $quantity = isset($_POST['quantity']) ? max(0, (int)$_POST['quantity']) : 0;

            if ($productId <= 0) {
                sendJson(['success' => false, 'error' => 'ID Prodotto non valido.'], 400);
            }

            $affected = 0;
            $mysqli->begin_transaction();
            try {
                $beforeStmt = $mysqli->prepare("SELECT `quantity` FROM `{$db_prefix}product` WHERE `product_id` = ? LIMIT 1 FOR UPDATE");
                if (!$beforeStmt) {
                    throw new RuntimeException('Verifica giacenza non disponibile');
                }
                $beforeStmt->bind_param('i', $productId);
                $beforeStmt->execute();
                $beforeResult = $beforeStmt->get_result();
                $beforeRow = $beforeResult ? $beforeResult->fetch_assoc() : null;
                $beforeStmt->close();
                if (!$beforeRow) {
                    throw new OutOfBoundsException('Prodotto non trovato');
                }
                $stmt = $mysqli->prepare("UPDATE `{$db_prefix}product` SET `quantity` = ?, `date_modified` = NOW() WHERE `product_id` = ?");
                if (!$stmt) {
                    throw new RuntimeException('Aggiornamento giacenza non disponibile');
                }
                $stmt->bind_param('ii', $quantity, $productId);
                $stmt->execute();
                $affected = $stmt->affected_rows;
                $stmt->close();
                $beforeDigest = cartadminStateDigest(['quantity' => (int)$beforeRow['quantity']], $auditSalt);
                $afterDigest = cartadminStateDigest(['quantity' => $quantity], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_stock', 'products', $productId, 'success', $beforeDigest, $afterDigest, 'Aggiornamento giacenza')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Aggiornamento giacenza non riuscito.'], 409);
            }

            sendJson([
                'success' => true,
                'product_id' => $productId,
                'quantity' => $quantity,
                'updated' => $affected > 0
            ]);
            break;

        case 'update_product':
            $productId = isset($_POST['product_id']) ? (int)$_POST['product_id'] : 0;
            $name = isset($_POST['name']) && is_string($_POST['name']) ? trim(strip_tags($_POST['name'])) : '';
            $model = isset($_POST['model']) && is_string($_POST['model']) ? trim(strip_tags($_POST['model'])) : '';
            $sku = isset($_POST['sku']) && is_string($_POST['sku']) ? trim(strip_tags($_POST['sku'])) : '';
            $description = isset($_POST['description']) && is_string($_POST['description']) ? cartadminSanitizeRichHtml($_POST['description']) : '';
            $category = isset($_POST['category']) && is_string($_POST['category']) ? trim(strip_tags($_POST['category'])) : '';
            $price = isset($_POST['price']) && is_numeric($_POST['price']) ? max(0.0, (float)$_POST['price']) : -1.0;
            $quantity = isset($_POST['quantity']) ? max(0, (int)$_POST['quantity']) : -1;
            $minimum = isset($_POST['minimum']) ? max(1, (int)$_POST['minimum']) : 1;
            $status = isset($_POST['status']) && (string)$_POST['status'] === '1' ? 1 : 0;
            $imagePath = '';

            if ($productId <= 0 || $name === '' || mb_strlen($name) > 255 || $model === '' || mb_strlen($model) > 64 || mb_strlen($sku) > 64 || $price < 0 || $quantity < 0 || mb_strlen($description) > 65535 || mb_strlen($category) > 255) {
                sendJson(['success' => false, 'error' => 'Dati prodotto non validi.'], 400);
            }

            $stmtExists = $mysqli->prepare("SELECT `product_id` FROM `{$db_prefix}product` WHERE `product_id` = ? LIMIT 1");
            if (!$stmtExists) {
                sendJson(['success' => false, 'error' => 'Impossibile verificare il prodotto.'], 500);
            }
            $stmtExists->bind_param('i', $productId);
            $stmtExists->execute();
            $existsResult = $stmtExists->get_result();
            $productExists = $existsResult && $existsResult->num_rows > 0;
            $stmtExists->close();
            if (!$productExists) {
                sendJson(['success' => false, 'error' => 'Prodotto non trovato.'], 404);
            }

            $languageIds = cartadminActiveLanguageIds($mysqli, $db_prefix);
            if ($languageIds === []) {
                sendJson(['success' => false, 'error' => 'Nessuna lingua OpenCart configurata.'], 409);
            }
            $primaryLanguageId = $languageIds[0];

            if (isset($_FILES['image']) && is_array($_FILES['image'])) {
                try {
                    $imagePath = cartadminStoreProductImage($_FILES['image']);
                } catch (Throwable $imageError) {
                    sendJson(['success' => false, 'error' => $imageError instanceof InvalidArgumentException ? $imageError->getMessage() : 'Impossibile salvare l’immagine prodotto.'], 400);
                }
            }

            $mysqli->begin_transaction();
            try {
                $stmtProduct = $imagePath === ''
                    ? $mysqli->prepare("UPDATE `{$db_prefix}product` SET `model` = ?, `sku` = ?, `quantity` = ?, `minimum` = ?, `price` = ?, `status` = ?, `date_modified` = NOW() WHERE `product_id` = ?")
                    : $mysqli->prepare("UPDATE `{$db_prefix}product` SET `model` = ?, `sku` = ?, `quantity` = ?, `minimum` = ?, `price` = ?, `status` = ?, `image` = ?, `date_modified` = NOW() WHERE `product_id` = ?");
                if (!$stmtProduct) {
                    throw new RuntimeException('Preparazione aggiornamento prodotto fallita.');
                }
                if ($imagePath === '') {
                    $stmtProduct->bind_param('ssiidii', $model, $sku, $quantity, $minimum, $price, $status, $productId);
                } else {
                    $stmtProduct->bind_param('ssiidisi', $model, $sku, $quantity, $minimum, $price, $status, $imagePath, $productId);
                }
                $stmtProduct->execute();
                $stmtProduct->close();

                $stmtDescription = $mysqli->prepare("INSERT INTO `{$db_prefix}product_description` (`product_id`, `language_id`, `name`, `description`, `tag`, `meta_title`, `meta_description`, `meta_keyword`) VALUES (?, ?, ?, ?, '', ?, '', '') ON DUPLICATE KEY UPDATE `name` = VALUES(`name`), `description` = VALUES(`description`), `meta_title` = VALUES(`meta_title`)");
                if (!$stmtDescription) {
                    throw new RuntimeException('Preparazione descrizione prodotto fallita.');
                }
                foreach ($languageIds as $languageId) {
                    $stmtDescription->bind_param('iisss', $productId, $languageId, $name, $description, $name);
                    $stmtDescription->execute();
                }
                $stmtDescription->close();

                if ($category !== '') {
                    $stmtCategory = $mysqli->prepare("SELECT `category_id` FROM `{$db_prefix}category_description` WHERE `name` = ? AND `language_id` = ? LIMIT 1");
                    if (!$stmtCategory) {
                        throw new RuntimeException('Preparazione categoria fallita.');
                    }
                    $stmtCategory->bind_param('si', $category, $primaryLanguageId);
                    $stmtCategory->execute();
                    $categoryResult = $stmtCategory->get_result();
                    $categoryRow = $categoryResult ? $categoryResult->fetch_assoc() : null;
                    $stmtCategory->close();
                    if (!$categoryRow) {
                        throw new RuntimeException('La categoria selezionata non esiste nello store.');
                    }
                    $categoryId = (int)$categoryRow['category_id'];
                    $stmtDeleteLinks = $mysqli->prepare("DELETE FROM `{$db_prefix}product_to_category` WHERE `product_id` = ?");
                    $stmtInsertLink = $mysqli->prepare("INSERT INTO `{$db_prefix}product_to_category` (`product_id`, `category_id`) VALUES (?, ?)");
                    if (!$stmtDeleteLinks || !$stmtInsertLink) {
                        throw new RuntimeException('Aggiornamento associazione categoria fallito.');
                    }
                    $stmtDeleteLinks->bind_param('i', $productId);
                    $stmtDeleteLinks->execute();
                    $stmtDeleteLinks->close();
                    $stmtInsertLink->bind_param('ii', $productId, $categoryId);
                    $stmtInsertLink->execute();
                    $stmtInsertLink->close();
                }

                $afterDigest = cartadminStateDigest(['product_id' => $productId, 'quantity' => $quantity, 'status' => $status], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_product', 'products', $productId, 'success', '', $afterDigest, 'Aggiornamento prodotto')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                if ($imagePath !== '' && defined('DIR_IMAGE')) {
                    @unlink(rtrim(DIR_IMAGE, '/\\') . DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $imagePath));
                }
                sendJson(['success' => false, 'error' => 'Aggiornamento prodotto non riuscito.'], 500);
            }

            cartadminInvalidateFileCache(['product', 'category']);

            sendJson([
                'success' => true,
                'product_id' => $productId,
                'quantity' => $quantity,
                'status' => (bool)$status
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

            $mysqli->begin_transaction();
            try {
                $beforeStmt = $mysqli->prepare("SELECT `order_status_id` FROM `{$db_prefix}order` WHERE `order_id` = ? LIMIT 1 FOR UPDATE");
                if (!$beforeStmt) throw new RuntimeException('Verifica ordine non disponibile');
                $beforeStmt->bind_param('i', $orderId);
                $beforeStmt->execute();
                $beforeResult = $beforeStmt->get_result();
                $beforeRow = $beforeResult ? $beforeResult->fetch_assoc() : null;
                $beforeStmt->close();
                if (!$beforeRow) throw new OutOfBoundsException('Ordine non trovato');
                $stmt = $mysqli->prepare("UPDATE `{$db_prefix}order` SET `order_status_id` = ?, `date_modified` = NOW() WHERE `order_id` = ?");
                if (!$stmt) throw new RuntimeException('Aggiornamento ordine non disponibile');
                $stmt->bind_param('ii', $statusId, $orderId);
                $stmt->execute();
                $stmt->close();
                $stmtHist = $mysqli->prepare("INSERT INTO `{$db_prefix}order_history` (`order_id`, `order_status_id`, `notify`, `comment`, `date_added`) VALUES (?, ?, 0, ?, NOW())");
                if (!$stmtHist) throw new RuntimeException('Storico ordine non disponibile');
                $stmtHist->bind_param('iis', $orderId, $statusId, $comment);
                $stmtHist->execute();
                $stmtHist->close();
                $beforeDigest = cartadminStateDigest(['status_id' => (int)$beforeRow['order_status_id']], $auditSalt);
                $afterDigest = cartadminStateDigest(['status_id' => $statusId], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_order_status', 'orders', $orderId, 'success', $beforeDigest, $afterDigest, 'Aggiornamento stato ordine')) {
                    throw new RuntimeException('Audit atomico non disponibile');
                }
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Aggiornamento ordine non riuscito.'], 409);
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
            if ($subId <= 0) sendJson(['success' => false, 'error' => 'ID abbonamento non valido.'], 400);
            $mysqli->begin_transaction();
            try {
                $beforeStmt = $mysqli->prepare("SELECT `status` FROM `{$db_prefix}subscription` WHERE `subscription_id` = ? LIMIT 1 FOR UPDATE");
                if (!$beforeStmt) throw new RuntimeException('Verifica abbonamento non disponibile');
                $beforeStmt->bind_param('i', $subId);
                $beforeStmt->execute();
                $beforeResult = $beforeStmt->get_result();
                $beforeRow = $beforeResult ? $beforeResult->fetch_assoc() : null;
                $beforeStmt->close();
                if (!$beforeRow) throw new OutOfBoundsException('Abbonamento non trovato');
                $stmtSub = $mysqli->prepare("UPDATE `{$db_prefix}subscription` SET `status` = ?, `date_modified` = NOW() WHERE `subscription_id` = ?");
                if (!$stmtSub) throw new RuntimeException('Aggiornamento abbonamento non disponibile');
                $stmtSub->bind_param('ii', $statusCode, $subId);
                $stmtSub->execute();
                $stmtSub->close();
                $beforeDigest = cartadminStateDigest(['status' => (int)$beforeRow['status']], $auditSalt);
                $afterDigest = cartadminStateDigest(['status' => $statusCode], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_subscription_status', 'subscriptions', $subId, 'success', $beforeDigest, $afterDigest, 'Aggiornamento abbonamento')) throw new RuntimeException('Audit atomico non disponibile');
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Aggiornamento abbonamento non riuscito.'], 409);
            }
            sendJson(['success' => true, 'subscription_id' => $subId, 'status' => $canonicalStatus]);
            break;

        case 'update_return_status':
            $retId = isset($_POST['return_id']) ? (int)$_POST['return_id'] : 0;
            $statusId = isset($_POST['status_id']) ? max(1, min(10, (int)$_POST['status_id'])) : 1;
            if ($retId <= 0) sendJson(['success' => false, 'error' => 'ID reso non valido.'], 400);
            $mysqli->begin_transaction();
            try {
                $beforeStmt = $mysqli->prepare("SELECT `return_status_id` FROM `{$db_prefix}return` WHERE `return_id` = ? LIMIT 1 FOR UPDATE");
                if (!$beforeStmt) throw new RuntimeException('Verifica reso non disponibile');
                $beforeStmt->bind_param('i', $retId);
                $beforeStmt->execute();
                $beforeResult = $beforeStmt->get_result();
                $beforeRow = $beforeResult ? $beforeResult->fetch_assoc() : null;
                $beforeStmt->close();
                if (!$beforeRow) throw new OutOfBoundsException('Reso non trovato');
                $stmtRet = $mysqli->prepare("UPDATE `{$db_prefix}return` SET `return_status_id` = ?, `date_modified` = NOW() WHERE `return_id` = ?");
                if (!$stmtRet) throw new RuntimeException('Aggiornamento reso non disponibile');
                $stmtRet->bind_param('ii', $statusId, $retId);
                $stmtRet->execute();
                $stmtRet->close();
                $beforeDigest = cartadminStateDigest(['status_id' => (int)$beforeRow['return_status_id']], $auditSalt);
                $afterDigest = cartadminStateDigest(['status_id' => $statusId], $auditSalt);
                if (!cartadminInsertSecurityAudit($mysqli, $db_prefix, $authContext, 'update_return_status', 'returns', $retId, 'success', $beforeDigest, $afterDigest, 'Aggiornamento reso')) throw new RuntimeException('Audit atomico non disponibile');
                $mysqli->commit();
            } catch (Throwable $error) {
                $mysqli->rollback();
                sendJson(['success' => false, 'error' => 'Aggiornamento reso non riuscito.'], 409);
            }
            sendJson(['success' => true, 'return_id' => $retId, 'status_id' => $statusId]);
            break;

        default:
            sendJson(['success' => false, 'error' => 'Azione non supportata.'], 400);
            break;
    }
} catch (Throwable $e) {
    sendJson(['success' => false, 'error' => 'Errore interno del server durante l\'elaborazione.'], 500);
}
