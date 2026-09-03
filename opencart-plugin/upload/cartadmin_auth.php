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
    $devicePublicKey = '';
    $deviceTimestamp = '';
    $deviceNonce = '';
    $deviceSignature = '';

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
        return ['', '', '', '', '', '', ''];
    }

    $devicePublicKey = isset($server['HTTP_X_CARTADMIN_DEVICE_KEY']) && is_string($server['HTTP_X_CARTADMIN_DEVICE_KEY'])
        ? trim($server['HTTP_X_CARTADMIN_DEVICE_KEY']) : '';
    $deviceTimestamp = isset($server['HTTP_X_CARTADMIN_TIMESTAMP']) && is_string($server['HTTP_X_CARTADMIN_TIMESTAMP'])
        ? trim($server['HTTP_X_CARTADMIN_TIMESTAMP']) : '';
    $deviceNonce = isset($server['HTTP_X_CARTADMIN_NONCE']) && is_string($server['HTTP_X_CARTADMIN_NONCE'])
        ? strtolower(trim($server['HTTP_X_CARTADMIN_NONCE'])) : '';
    $deviceSignature = isset($server['HTTP_X_CARTADMIN_SIGNATURE']) && is_string($server['HTTP_X_CARTADMIN_SIGNATURE'])
        ? trim($server['HTTP_X_CARTADMIN_SIGNATURE']) : '';
    if (strlen($devicePublicKey) > 512
        || preg_match('/^[0-9]{10}$/', $deviceTimestamp) !== 1
        || preg_match('/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/', $deviceNonce) !== 1
        || strlen($deviceSignature) > 256) {
        return [$key, $username, $deviceId, '', '', '', ''];
    }

    return [$key, $username, $deviceId, $devicePublicKey, $deviceTimestamp, $deviceNonce, $deviceSignature];
}

function cartadminVerifyDeviceProof(
    string $deviceId,
    string $publicKeyBase64,
    string $timestamp,
    string $nonce,
    string $signatureBase64,
    string $method,
    string $requestUri,
    ?int $now = null
): bool {
    $publicKeyDer = base64_decode($publicKeyBase64, true);
    $signature = base64_decode($signatureBase64, true);
    if ($publicKeyDer === false || $signature === false || strlen($publicKeyDer) < 64 || strlen($publicKeyDer) > 256) {
        return false;
    }
    if (!hash_equals(substr(hash('sha256', $publicKeyDer), 0, 32), $deviceId)) {
        return false;
    }
    $timestampValue = (int)$timestamp;
    if (abs(($now ?? time()) - $timestampValue) > 120) {
        return false;
    }
    $publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
        . chunk_split(base64_encode($publicKeyDer), 64, "\n")
        . "-----END PUBLIC KEY-----\n";
    $canonical = strtoupper($method) . "\n" . $requestUri . "\n" . $timestamp . "\n" . $nonce;
    return openssl_verify($canonical, $signature, $publicKeyPem, OPENSSL_ALGO_SHA256) === 1;
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
        'status.read',
        'orders.read',
        'catalog.read',
        'content.read',
        'customers.read',
        'telemetry.read',
        'orders.write',
        'catalog.write',
        'content.write',
        'customers.write'
    ];
    $parsed = array_filter(array_map('trim', explode(',', strtolower($scopes))));

    return array_values(array_intersect($allowed, array_unique($parsed)));
}

function cartadminHasScope(array $scopes, string $required): bool {
    return in_array($required, $scopes, true);
}
