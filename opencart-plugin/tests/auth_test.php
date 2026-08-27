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
    'HTTP_X_CARTADMIN_USER' => ' api-user ',
    'HTTP_AUTHORIZATION' => 'Bearer ignored-because-explicit-header',
]);
assertSameValue('header-secret', $key, 'The explicit CartAdmin key header must be used.');
assertSameValue('api-user', $username, 'The explicit CartAdmin user header must be used.');

[$bearerKey, $bearerUsername] = cartadminExtractCredentials([
    'HTTP_AUTHORIZATION' => 'Bearer fallback-secret',
]);
assertSameValue('fallback-secret', $bearerKey, 'A well-formed Bearer token may be used for legacy clients.');
assertSameValue('', $bearerUsername, 'No username may be inferred from request data.');

[$requestKey, $requestUsername] = cartadminExtractCredentials([
    'api_key' => 'query-secret',
    'username' => 'query-user',
]);
assertSameValue('', $requestKey, 'Query or form keys must be ignored.');
assertSameValue('', $requestUsername, 'Query or form usernames must be ignored.');

[$malformedKey] = cartadminExtractCredentials([
    'HTTP_AUTHORIZATION' => 'Basic should-not-be-accepted',
]);
assertSameValue('', $malformedKey, 'Non-Bearer authorization must be rejected.');

[$oversizedKey, $oversizedUsername] = cartadminExtractCredentials([
    'HTTP_X_CARTADMIN_KEY' => str_repeat('k', 513),
    'HTTP_X_CARTADMIN_USER' => str_repeat('u', 129),
]);
assertSameValue('', $oversizedKey, 'Oversized credentials must be rejected.');
assertSameValue('', $oversizedUsername, 'Oversized credentials must be rejected together.');

assertSameValue(true, cartadminLegacyKeyMatches('known', 'known'), 'Equal legacy keys must match.');
assertSameValue(false, cartadminLegacyKeyMatches('known', 'other'), 'Different legacy keys must not match.');
assertSameValue(true, cartadminNativeCredentialsAreComplete('api-user', 'secret'), 'Both native credentials are required.');
assertSameValue(false, cartadminNativeCredentialsAreComplete('', 'secret'), 'A key alone must not authenticate natively.');

$bridgeSource = file_get_contents(__DIR__ . '/../upload/cartadmin_api.php');
$embeddedSource = file_get_contents(__DIR__ . '/../../app/src/main/java/com/example/data/OpenCartBridgeModule.kt');

assertSourceOmits($bridgeSource, 'get_key_setup', 'The packaged bridge must not expose public key setup.');
assertSourceOmits($bridgeSource, "\$_REQUEST['api_key']", 'The packaged bridge must ignore URL/form credentials.');
assertSourceOmits($bridgeSource, '(`key` = ? OR `username` = ?)', 'A username alone must never be accepted as a key.');
assertSourceContains($bridgeSource, '`username` = ? AND `key` = ?', 'Native authentication must match username and key.');
assertSourceOmits($embeddedSource, 'get_key_setup', 'The generated standalone bridge must not expose public key setup.');
assertSourceOmits($embeddedSource, "_REQUEST['api_key']", 'The generated standalone bridge must ignore URL/form credentials.');

fwrite(STDOUT, "CartAdmin bridge authentication tests passed.\n");
