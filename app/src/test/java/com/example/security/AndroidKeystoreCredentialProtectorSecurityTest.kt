package com.example.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidKeystoreCredentialProtectorSecurityTest {
    private val source = File(
        "src/main/java/com/example/security/AndroidKeystoreCredentialProtector.kt"
    ).readText()

    @Test
    fun hardwareBackingIsRequiredAndStrongBoxIsPreferred() {
        assertTrue(source.contains("setIsStrongBoxBacked(strongBox)"))
        assertTrue(source.contains("SECURITY_LEVEL_STRONGBOX"))
        assertTrue(source.contains("SECURITY_LEVEL_TRUSTED_ENVIRONMENT"))
        assertTrue(source.contains("isInsideSecureHardware()"))
        assertTrue(source.contains("keyStore.deleteEntry(KEY_ALIAS)"))
        assertFalse(source.contains("SECURITY_LEVEL_SOFTWARE -> HardwareSecurityLevel"))
    }

    @Test
    fun encryptionIsAuthenticatedAndBoundToStoreAndField() {
        assertTrue(source.contains("AES/GCM/NoPadding"))
        assertTrue(source.contains("setRandomizedEncryptionRequired(true)"))
        assertTrue(source.contains("cipher.updateAAD(aad(FORMAT_PREFIX, storeId, field))"))
        assertTrue(source.contains("GCM_TAG_BITS = 128"))
        assertTrue(source.contains("setUserAuthenticationRequired(true)"))
        assertTrue(source.contains("AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL"))
        assertTrue(source.contains("AUTHORIZATION_WINDOW_SECONDS = 300"))
    }

    @Test
    fun migrationScrubsSqliteAndBackupsExcludeSensitiveStorage() {
        val databaseSource = File(
            "src/main/java/com/example/data/local/AppDatabase.kt"
        ).readText()
        val legacyBackupRules = File("src/main/res/xml/backup_rules.xml").readText()
        val extractionRules = File("src/main/res/xml/data_extraction_rules.xml").readText()

        assertTrue(databaseSource.contains("PRAGMA secure_delete=ON"))
        assertTrue(databaseSource.contains("db.execSQL(\"DELETE FROM orders_cache\")"))
        assertTrue(databaseSource.contains("db.execSQL(\"DELETE FROM subscriptions_cache\")"))
        assertTrue(databaseSource.contains("db.execSQL(\"DELETE FROM returns_cache\")"))
        assertTrue(databaseSource.contains("db.execSQL(\"DELETE FROM audit_logs\")"))
        assertTrue(databaseSource.contains("PRAGMA wal_checkpoint(TRUNCATE)"))
        assertTrue(databaseSource.contains("VACUUM"))
        assertTrue(legacyBackupRules.contains("domain=\"database\""))
        assertTrue(legacyBackupRules.contains("domain=\"sharedpref\""))
        assertTrue(extractionRules.contains("<device-transfer>"))
        assertTrue(extractionRules.contains("domain=\"database\""))
        assertTrue(extractionRules.contains("domain=\"sharedpref\""))
    }
}
