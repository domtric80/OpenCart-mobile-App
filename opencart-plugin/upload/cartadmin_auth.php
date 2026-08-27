<?php
/**
 * Authentication helpers for the CartAdmin OpenCart bridge.
 *
 * Credentials are accepted only from HTTP headers. Query-string and form credentials are
 * intentionally ignored because web servers, proxies, analytics, and browser history may log
 * request URLs.
 */

function cartadminExtractCredentials(array $server): array {
    $key = '';
    $username = '';

    if (isset($server['HTTP_X_CARTADMIN_KEY']) && is_string($server['HTTP_X_CARTADMIN_KEY'])) {
        $key = trim($server['HTTP_X_CARTADMIN_KEY']);
    } elseif (isset($server['HTTP_AUTHORIZATION']) && is_string($server['HTTP_AUTHORIZATION'])) {
        $authorization = trim($server['HTTP_AUTHORIZATION']);
        if (preg_match('/^Bearer\s+([^\s]+)$/i', $authorization, $matches) === 1) {
            $key = $matches[1];
        }
    }

    if (isset($server['HTTP_X_CARTADMIN_USER']) && is_string($server['HTTP_X_CARTADMIN_USER'])) {
        $username = trim($server['HTTP_X_CARTADMIN_USER']);
    }

    if (strlen($key) > 512 || strlen($username) > 128) {
        return ['', ''];
    }

    return [$key, $username];
}

function cartadminLegacyKeyMatches(string $configuredKey, string $receivedKey): bool {
    return $configuredKey !== ''
        && $receivedKey !== ''
        && hash_equals($configuredKey, $receivedKey);
}

function cartadminNativeCredentialsAreComplete(string $username, string $key): bool {
    return $username !== '' && $key !== '';
}
