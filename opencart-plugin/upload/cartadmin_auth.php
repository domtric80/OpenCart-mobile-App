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
    $deviceId = '';

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

    if (isset($server['HTTP_X_CARTADMIN_DEVICE']) && is_string($server['HTTP_X_CARTADMIN_DEVICE'])) {
        $candidate = strtolower(trim($server['HTTP_X_CARTADMIN_DEVICE']));
        if (preg_match('/^[a-f0-9]{32}$/', $candidate) === 1) {
            $deviceId = $candidate;
        }
    }

    if (strlen($key) > 512 || strlen($username) > 128) {
        return ['', '', ''];
    }

    return [$key, $username, $deviceId];
}

function cartadminHashToken(string $token): string {
    $algorithm = defined('PASSWORD_ARGON2ID') ? PASSWORD_ARGON2ID : PASSWORD_DEFAULT;
    $hash = password_hash($token, $algorithm);

    if (!is_string($hash) || $hash === '') {
        throw new RuntimeException('Impossibile proteggere il token CartAdmin.');
    }

    return $hash;
}

function cartadminTokenMatches(string $configuredHash, string $receivedToken): bool {
    return $configuredHash !== ''
        && $receivedToken !== ''
        && password_verify($receivedToken, $configuredHash);
}

function cartadminParseScopes(string $scopes): array {
    $allowed = [
        'read',
        'orders.write',
        'catalog.write',
        'content.write',
        'customers.write',
        'audit.write'
    ];
    $parsed = array_filter(array_map('trim', explode(',', strtolower($scopes))));

    return array_values(array_intersect($allowed, array_unique($parsed)));
}

function cartadminHasScope(array $scopes, string $required): bool {
    return in_array($required, $scopes, true);
}
