package com.example.security

/**
 * Protects OpenCart credentials before they cross the Room persistence boundary.
 * Implementations must fail closed when secure key storage is unavailable.
 */
interface CredentialProtector {
    fun protect(storeId: String, field: CredentialField, plainText: String): String
    fun reveal(storeId: String, field: CredentialField, persistedValue: String): RevealedCredential
    fun hardwareSecurityLevel(): HardwareSecurityLevel
}

enum class CredentialField(val storageName: String) {
    STORE_NAME("store_name"),
    STORE_URL("store_url"),
    STORE_VERSION("store_version"),
    API_USERNAME("api_username"),
    API_KEY("api_key")
}

enum class HardwareSecurityLevel {
    STRONGBOX,
    TRUSTED_ENVIRONMENT
}

data class RevealedCredential(
    val value: String,
    val requiresMigration: Boolean
)

class CredentialProtectionException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)
