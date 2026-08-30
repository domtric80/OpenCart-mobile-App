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

function assertSourceOrder(string $source, array $needles, string $message): void {
    $offset = 0;
    foreach ($needles as $needle) {
        $position = strpos($source, $needle, $offset);
        if ($position === false) {
            fwrite(STDERR, "FAIL: {$message} (missing or out of order: {$needle})\n");
            exit(1);
        }
        $offset = $position + strlen($needle);
    }
}

[$key, $username, $deviceId] = cartadminExtractCredentials([
    'HTTP_X_CARTADMIN_KEY' => ' header-secret ',
    'HTTP_X_CARTADMIN_USER' => ' operator ',
    'HTTP_X_CARTADMIN_DEVICE' => '0123456789ABCDEF0123456789ABCDEF',
    'HTTP_AUTHORIZATION' => 'Bearer ignored-because-explicit-header',
]);
assertSameValue('header-secret', $key, 'The explicit CartAdmin token header must be used.');
assertSameValue('operator', $username, 'The claimed operator header may be preserved only for audit comparison.');
assertSameValue('0123456789abcdef0123456789abcdef', $deviceId, 'The installation identifier must be normalized.');

[$bearerKey, $bearerUsername, $bearerDevice] = cartadminExtractCredentials([
    'HTTP_AUTHORIZATION' => 'Bearer fallback-secret',
    'HTTP_X_CARTADMIN_DEVICE' => 'not-a-device-id',
]);
assertSameValue('fallback-secret', $bearerKey, 'A well-formed Bearer token may be used.');
assertSameValue('', $bearerUsername, 'No username may be inferred from request data.');
assertSameValue('', $bearerDevice, 'Malformed installation identifiers must be rejected.');

[$requestKey, $requestUsername, $requestDevice] = cartadminExtractCredentials([
    'api_key' => 'query-secret',
    'username' => 'query-user',
    'device' => '0123456789abcdef0123456789abcdef',
]);
assertSameValue('', $requestKey, 'Query or form tokens must be ignored.');
assertSameValue('', $requestUsername, 'Query or form usernames must be ignored.');
assertSameValue('', $requestDevice, 'Query or form device identifiers must be ignored.');

[$oversizedKey, $oversizedUsername, $oversizedDevice] = cartadminExtractCredentials([
    'HTTP_X_CARTADMIN_KEY' => str_repeat('k', 513),
    'HTTP_X_CARTADMIN_USER' => str_repeat('u', 129),
    'HTTP_X_CARTADMIN_DEVICE' => '0123456789abcdef0123456789abcdef',
]);
assertSameValue('', $oversizedKey, 'Oversized credentials must be rejected.');
assertSameValue('', $oversizedUsername, 'Oversized credentials must be rejected together.');
assertSameValue('', $oversizedDevice, 'Oversized credentials must reject the complete credential tuple.');

$token = 'ca_' . bin2hex(random_bytes(32));
$hash = cartadminHashToken($token);
assertSameValue(true, cartadminTokenMatches($hash, $token), 'The generated token must match its hash.');
assertSameValue(false, cartadminTokenMatches($hash, $token . 'x'), 'A different token must not match.');
assertSameValue(false, strpos($hash, $token) !== false, 'The stored hash must not contain the plaintext token.');
assertSameValue(['read', 'content.write'], cartadminParseScopes('read,unknown,content.write,read'), 'Only allowlisted unique scopes may be granted.');
assertSameValue(true, cartadminHasScope(['read', 'content.write'], 'content.write'), 'An explicitly granted scope must authorize its operation.');
assertSameValue(false, cartadminHasScope(['read'], 'content.write'), 'Read-only tokens must not authorize writes.');

$bridgeSource = file_get_contents(__DIR__ . '/../upload/cartadmin_api.php');
$adminModelSource = file_get_contents(__DIR__ . '/../admin/model/module/cartadmin.php');
$adminControllerSource = file_get_contents(__DIR__ . '/../admin/controller/module/cartadmin.php');
$adminViewSource = file_get_contents(__DIR__ . '/../admin/view/template/module/cartadmin.twig');
$manifest = json_decode(file_get_contents(__DIR__ . '/../install.json'), true);
$contentCaseStart = strpos($bridgeSource, "case 'management_content':");
$contentCaseEnd = strpos($bridgeSource, "case 'management_status':", $contentCaseStart ?: 0);
$contentSource = ($contentCaseStart !== false && $contentCaseEnd !== false)
    ? substr($bridgeSource, $contentCaseStart, $contentCaseEnd - $contentCaseStart)
    : '';

assertSameValue('2.1.0-dev.4', $manifest['version'] ?? '', 'The OpenCart manifest version must match the development release.');
assertSourceContains($bridgeSource, "'bridge_version' => '2.1.0-dev.4'", 'The status endpoint must report the same development release.');
assertSourceOmits($bridgeSource, 'get_key_setup', 'The bridge must not expose public token setup.');
assertSourceOmits($bridgeSource, "\$_REQUEST['api_key']", 'The bridge must ignore URL/form credentials.');
assertSourceOmits($bridgeSource, '`username` = ? AND `key` = ?', 'The bridge must not authenticate against plaintext native API keys.');
assertSourceContains($bridgeSource, 't.`token_lookup` = ? AND t.`active` = 1', 'The bridge must select only a single active indexed token.');
assertSourceContains($bridgeSource, '(t.`operator_user_id` IS NULL OR u.`status` = 1)', 'Disabling a linked OpenCart user must also block that user’s token.');
assertSourceContains($bridgeSource, 'cartadminTokenMatches((string)$tokenRow[\'token_hash\'], $receivedKey)', 'Authentication must use password_verify via the token helper.');
assertSourceContains($bridgeSource, 'AND (`device_hash` IS NULL OR `device_hash` = \'\')', 'The first successful request must atomically bind the token to one installation.');
assertSourceContains($bridgeSource, '!hash_equals($storedDeviceHash, $deviceHash)', 'A token bound to another installation must be rejected.');
assertSourceContains($bridgeSource, 'cartadminRequiredScope($action, $scopeModule)', 'Every routed operation must resolve a server-side scope.');
assertSourceContains($bridgeSource, 'cartadminHasScope($authenticatedScopes, $requiredScope)', 'Every routed operation must enforce its required scope.');
assertSourceContains($bridgeSource, "? 'read' : 'forbidden'", 'Unknown future operations must fail closed instead of inheriting read access.');
assertSourceContains($bridgeSource, "'operator_name' => \$authenticatedOperator", 'Security audit identity must come from the authenticated token.');
assertSourceContains($bridgeSource, "'operator_user_id' => max(0, (int)(\$authenticatedToken['operator_user_id'] ?? 0))", 'Security audit must retain the verified OpenCart user id.');
assertSourceContains($bridgeSource, "'claimed_operator_digest' => \$claimedOperatorDigest", 'Any legacy client identity claim must be retained only as a keyed digest.');
assertSourceContains($bridgeSource, "'identity_claim_mismatch' => \$normalizedClaim !== ''", 'A conflicting legacy identity claim must be marked without replacing the verified identity.');
assertSourceContains($adminModelSource, "FROM `\" . DB_PREFIX . \"user` WHERE `user_id` = '", 'Token creation must resolve an active OpenCart user server-side.');
assertSourceContains($adminModelSource, "AND `status` = '1' LIMIT 1", 'Disabled OpenCart users must not be assigned to new tokens.');
assertSourceContains($adminModelSource, "`active` = '0', `revoked_at` = NOW()", 'Individual tokens must be immediately revocable.');
assertSourceContains($adminModelSource, "WHERE `key` IN ('api_key', 'token_hash', 'token_last_four', 'token_created_at')", 'Legacy token settings must be removed after migration.');
assertSourceContains($bridgeSource, '$legacyCredentialsPresent', 'A token recreated after a rollback must be migrated on the next upgrade.');
assertSourceContains($adminModelSource, "&& \$hash === '' && \$legacy === ''", 'The migration marker must not hide credentials recreated by an older rollback.');
assertSourceContains($adminModelSource, "'ca_' . \$lookup . '_' . bin2hex(random_bytes(32))", 'Tokens must use an indexed prefix and a cryptographically secure secret.');
assertSourceContains($bridgeSource, 'cartadminStateDigest($beforeState, $auditSalt)', 'Editorial before-state must be represented by a keyed digest.');
assertSourceContains($bridgeSource, 'cartadminStateDigest($afterState, $auditSalt)', 'Editorial after-state must be represented by a keyed digest.');
assertSourceContains($bridgeSource, "'success', \$beforeDigest, \$afterDigest", 'Successful editorial audit must be inserted before transaction commit.');
assertSourceContains($bridgeSource, "'failed', '', '', 'Rollback della modifica editoriale'", 'Failed editorial audit must be recorded separately after rollback without content.');
assertSourceOmits($bridgeSource, '$operator = mb_substr(strip_tags((string)($input[\'operator_username\']', 'Client audit payloads must not define the authoritative operator.');
assertSourceOmits($bridgeSource, '`claimed_operator` VARCHAR', 'Security audit must not store a raw client-provided operator claim.');
assertSourceOrder($contentSource, [
    '$mysqli->begin_transaction();',
    'cartadminStateDigest($beforeState, $auditSalt)',
    'cartadminInsertSecurityAudit(',
    "'management_content'",
    "'success'",
    '$mysqli->commit();'
], 'Editorial mutation and successful audit must commit atomically.');
assertSourceOrder($contentSource, [
    '$mysqli->rollback();',
    "'management_content'",
    "'failed'"
], 'A failure event must be inserted only after the failed editorial transaction rolls back.');
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
assertSourceContains($bridgeSource, "case 'management_content':", 'The bridge must expose allowlisted editorial mutations.');
assertSourceContains($bridgeSource, "\$editableModules = ['pages', 'reviews', 'articles', 'topics'];", 'Editorial mutations must use an explicit module allowlist.');
assertSourceContains($bridgeSource, 'UPDATE `{$db_prefix}review` SET `author` = ?, `text` = ?, `rating` = ?', 'Review edits must use prepared values.');
assertSourceContains($bridgeSource, 'INNER JOIN `{$db_prefix}information_description`', 'Page edits must verify the primary-language record before mutation.');
assertSourceContains($bridgeSource, "case 'management_command':", 'Sensitive customer operations must use the authenticated command queue.');
assertSourceContains($bridgeSource, "'status' => 'pending'", 'The mobile bridge must only acknowledge queued sensitive operations.');
assertSourceContains($bridgeSource, 'UNIQUE KEY `uq_pending_target` (`dedupe_key`)', 'Pending sensitive commands must be deduplicated per target.');
assertSourceContains($adminModelSource, "->approveCustomer((int)\$approval['customer_id'])", 'Customer approval must use the native OpenCart model.');
assertSourceContains($adminModelSource, 'model_customer_gdpr->editStatus($target_id, $status)', 'GDPR processing must use the native OpenCart model.');
assertSourceContains($adminModelSource, "in_array(\$operation, ['approve', 'deny'], true)", 'Queued operations must be allowlisted again before native execution.');
assertSourceContains($adminControllerSource, "hasPermission('modify', \$this->route)", 'Command execution must require OpenCart modify permission.');
assertSourceContains($adminControllerSource, "['execute', 'reject']", 'The admin controller must allow only reviewed command decisions.');
assertSourceContains($adminViewSource, 'data-decision="execute"', 'The admin panel must require an explicit execution action.');
assertSourceContains($bridgeSource, '$moduleQueries = [', 'Management queries must use a server-side module allowlist.');
assertSourceContains($bridgeSource, '$statusTargets = [', 'Management mutations must use a server-side target allowlist.');
assertSourceContains($bridgeSource, 'mb_substr($keyword, 0, 64)', 'Antispam keywords must respect the native OpenCart field length.');
assertSourceContains($bridgeSource, 'function cartadminInvalidateFileCache', 'Native file-cache invalidation must be present.');
assertSourceContains($bridgeSource, "'reviews' => ['product']", 'Review mutations must invalidate the product cache.');
assertSourceContains($bridgeSource, "preg_match('/^[a-zA-Z0-9_]{1,32}$/', \$rawPrefix)", 'Database prefixes used in identifiers must be allowlisted.');
assertSameValue(12, substr_count($bridgeSource, 'nosemgrep: php.lang.security.injection.tainted-sql-string.tainted-sql-string'), 'SQL suppressions must remain limited to reviewed allowlisted identifiers.');
assertSameValue(2, substr_count($bridgeSource, 'php.lang.security.injection.tainted-callable.tainted-callable'), 'Callable suppressions must remain limited to the two reviewed status queries.');

fwrite(STDOUT, "CartAdmin bridge authentication tests passed.\n");
