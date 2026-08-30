<?php

require_once __DIR__ . '/../upload/cartadmin_auth.php';

function assertSameValue($expected, $actual, string $message): void {
    if ($expected !== $actual) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
}

function assertSourceContains(string $source, string $needle, string $message): void {
    if (strpos($source, $needle) === false) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
}

function assertSourceOmits(string $source, string $needle, string $message): void {
    if (strpos($source, $needle) !== false) {
        fwrite(STDERR, "FAIL: {$message}\n");
        exit(1);
    }
}

[$key, $username] = cartadminExtractCredentials([
    'HTTP_X_CARTADMIN_KEY' => ' header-secret ',
    'HTTP_X_CARTADMIN_USER' => ' operator ',
    'HTTP_AUTHORIZATION' => 'Bearer ignored-because-explicit-header',
]);
assertSameValue('header-secret', $key, 'The explicit CartAdmin token header must be used.');
assertSameValue('operator', $username, 'The operator header must be preserved for audit data.');

[$bearerKey, $bearerUsername] = cartadminExtractCredentials([
    'HTTP_AUTHORIZATION' => 'Bearer fallback-secret',
]);
assertSameValue('fallback-secret', $bearerKey, 'A well-formed Bearer token may be used.');
assertSameValue('', $bearerUsername, 'No username may be inferred from request data.');

[$requestKey, $requestUsername] = cartadminExtractCredentials([
    'api_key' => 'query-secret',
    'username' => 'query-user',
]);
assertSameValue('', $requestKey, 'Query or form tokens must be ignored.');
assertSameValue('', $requestUsername, 'Query or form usernames must be ignored.');

[$oversizedKey, $oversizedUsername] = cartadminExtractCredentials([
    'HTTP_X_CARTADMIN_KEY' => str_repeat('k', 513),
    'HTTP_X_CARTADMIN_USER' => str_repeat('u', 129),
]);
assertSameValue('', $oversizedKey, 'Oversized credentials must be rejected.');
assertSameValue('', $oversizedUsername, 'Oversized credentials must be rejected together.');

$token = 'ca_' . bin2hex(random_bytes(32));
$hash = cartadminHashToken($token);
assertSameValue(true, cartadminTokenMatches($hash, $token), 'The generated token must match its hash.');
assertSameValue(false, cartadminTokenMatches($hash, $token . 'x'), 'A different token must not match.');
assertSameValue(false, strpos($hash, $token) !== false, 'The stored hash must not contain the plaintext token.');

$bridgeSource = file_get_contents(__DIR__ . '/../upload/cartadmin_api.php');
$adminModelSource = file_get_contents(__DIR__ . '/../admin/model/module/cartadmin.php');
$manifest = json_decode(file_get_contents(__DIR__ . '/../install.json'), true);

assertSameValue('2.1.0-dev.1', $manifest['version'] ?? '', 'The OpenCart manifest version must match the development release.');
assertSourceOmits($bridgeSource, 'get_key_setup', 'The bridge must not expose public token setup.');
assertSourceOmits($bridgeSource, "\$_REQUEST['api_key']", 'The bridge must ignore URL/form credentials.');
assertSourceOmits($bridgeSource, '`username` = ? AND `key` = ?', 'The bridge must not authenticate against plaintext native API keys.');
assertSourceContains($bridgeSource, "`key` = 'token_hash'", 'The bridge must load only the token hash.');
assertSourceContains($bridgeSource, 'cartadminTokenMatches($configuredHash, $receivedKey)', 'Authentication must use password_verify via the token helper.');
assertSourceContains($adminModelSource, "DELETE FROM `\" . DB_PREFIX . \"cartadmin_setting` WHERE `key` = 'api_key'", 'Legacy plaintext tokens must be removed after migration.');
assertSourceContains($adminModelSource, "'ca_' . bin2hex(random_bytes(32))", 'Tokens must use a cryptographically secure generator.');
assertSourceContains($bridgeSource, "case 'visitor_telemetry':", 'The bridge must expose authenticated OpenCart visitor telemetry.');
assertSourceContains($bridgeSource, "case 'update_product':", 'The bridge must expose verified product updates.');
assertSourceContains($bridgeSource, "case 'create_product':", 'The bridge must expose authenticated product creation.');
assertSourceContains($bridgeSource, "case 'delete_product':", 'The bridge must expose authenticated product deletion.');
assertSourceContains($bridgeSource, "case 'create_category':", 'The bridge must expose authenticated category creation.');
assertSourceContains($bridgeSource, "case 'update_category':", 'The bridge must expose authenticated category updates.');
assertSourceContains($bridgeSource, "case 'delete_category':", 'The bridge must expose authenticated category deletion.');
assertSourceContains($bridgeSource, '$mysqli->begin_transaction();', 'Catalog writes must be transactional.');
assertSourceContains($bridgeSource, 'cartadminActiveLanguageIds($mysqli, $db_prefix)', 'Catalog descriptions must follow active OpenCart languages.');
assertSourceContains($bridgeSource, 'La categoria contiene sottocategorie.', 'Category deletion must refuse implicit recursive removal.');
assertSourceContains($bridgeSource, 'cartadminInvalidateFileCache([\'product\', \'category\'])', 'Catalog mutations must invalidate native caches.');
assertSourceContains($bridgeSource, 'DELETE FROM `{$db_prefix}product_to_category` WHERE `product_id` = ?', 'Product category changes must replace the selected association.');
assertSourceContains($bridgeSource, 'pdx.special = 1', 'OpenCart 4.1 special prices must use the native product_discount schema.');
assertSourceOmits($bridgeSource, 'product_special', 'The bridge must not depend on the legacy product_special table.');
assertSourceContains($bridgeSource, "case 'management_list':", 'The bridge must expose authenticated management lists.');
assertSourceContains($bridgeSource, "case 'management_status':", 'The bridge must verify management status changes remotely.');
assertSourceContains($bridgeSource, "case 'management_antispam':", 'The bridge must expose verified Antispam mutations.');
assertSourceContains($bridgeSource, '$moduleQueries = [', 'Management queries must use a server-side module allowlist.');
assertSourceContains($bridgeSource, '$statusTargets = [', 'Management mutations must use a server-side target allowlist.');
assertSourceContains($bridgeSource, 'mb_substr($keyword, 0, 64)', 'Antispam keywords must respect the native OpenCart field length.');
assertSourceContains($bridgeSource, 'function cartadminInvalidateFileCache', 'Native file-cache invalidation must be present.');
assertSourceContains($bridgeSource, "'reviews' => ['product']", 'Review mutations must invalidate the product cache.');
assertSourceContains($bridgeSource, "preg_match('/^[a-zA-Z0-9_]{1,32}$/', \$rawPrefix)", 'Database prefixes used in identifiers must be allowlisted.');
assertSameValue(12, substr_count($bridgeSource, 'nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string'), 'SQL suppressions must remain limited to reviewed allowlisted identifiers.');
assertSameValue(2, substr_count($bridgeSource, 'php.lang.security.injection.tainted-callable.tainted-callable'), 'Callable suppressions must remain limited to the two reviewed status queries.');

fwrite(STDOUT, "CartAdmin bridge authentication tests passed.\n");
