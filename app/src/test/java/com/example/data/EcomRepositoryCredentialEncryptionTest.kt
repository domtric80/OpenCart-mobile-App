package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.StoreProfileDao
import com.example.data.local.entity.StoreProfileEntity
import com.example.network.OpenCartApiClient
import com.example.security.CredentialField
import com.example.security.CredentialProtector
import com.example.security.HardwareSecurityLevel
import com.example.security.RevealedCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EcomRepositoryCredentialEncryptionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun plaintextRoomCredentialsAreMigratedBeforeExposure() = runTest {
        val dao = FakeStoreProfileDao(
            StoreProfileEntity(
                id = "store_legacy",
                name = "Legacy",
                url = "https://shop.example",
                adminUsername = "api-user",
                apiKey = "plain-secret",
                isPrimary = true
            )
        )
        val repository = repository(dao)

        val migrated = repository.loadPersistedStores()

        assertTrue(migrated)
        assertEquals("api-user", repository.stores.value.single().apiUsername)
        assertEquals("plain-secret", repository.stores.value.single().apiKey)
        assertTrue(dao.row!!.adminUsername.startsWith("test-protected:"))
        assertTrue(dao.row!!.apiKey.startsWith("test-protected:"))
        assertFalse(dao.row!!.apiKey.contains("plain-secret"))
    }

    @Test
    fun newCredentialsCrossRoomBoundaryOnlyAsCiphertext() = runTest {
        val dao = FakeStoreProfileDao()
        val repository = repository(dao)

        val store = repository.addStore(
            name = "Secure",
            url = "https://shop.example",
            version = "4.x",
            username = "api-user",
            key = "new-secret"
        )

        assertEquals("new-secret", store.apiKey)
        assertTrue(dao.row!!.apiKey.startsWith("test-protected:"))
        assertFalse(dao.row!!.apiKey.contains("new-secret"))
        assertTrue(dao.row!!.adminUsername.startsWith("test-protected:"))
    }

    private fun repository(dao: StoreProfileDao): EcomRepository = EcomRepository(
        storeProfileDao = dao,
        apiClient = OpenCartApiClient(context),
        credentialProtector = ReversibleTestProtector()
    )

    private class ReversibleTestProtector : CredentialProtector {
        override fun protect(storeId: String, field: CredentialField, plainText: String): String {
            if (plainText.isEmpty()) return ""
            return "test-protected:$storeId:${field.storageName}:${plainText.reversed()}"
        }

        override fun reveal(
            storeId: String,
            field: CredentialField,
            persistedValue: String
        ): RevealedCredential {
            if (persistedValue.isEmpty()) return RevealedCredential("", false)
            val prefix = "test-protected:$storeId:${field.storageName}:"
            return if (persistedValue.startsWith(prefix)) {
                RevealedCredential(persistedValue.removePrefix(prefix).reversed(), false)
            } else {
                RevealedCredential(persistedValue, true)
            }
        }

        override fun hardwareSecurityLevel() = HardwareSecurityLevel.TRUSTED_ENVIRONMENT
    }

    private class FakeStoreProfileDao(initial: StoreProfileEntity? = null) : StoreProfileDao {
        private val flow = MutableStateFlow(initial?.let(::listOf) ?: emptyList())
        var row: StoreProfileEntity? = initial
            private set

        override fun getAllStoresFlow(): Flow<List<StoreProfileEntity>> = flow
        override suspend fun getAllStores(): List<StoreProfileEntity> = row?.let(::listOf) ?: emptyList()
        override suspend fun getStoreById(storeId: String) = row?.takeIf { it.id == storeId }
        override suspend fun getPrimaryStore() = row?.takeIf { it.isPrimary }

        override suspend fun insertOrUpdate(store: StoreProfileEntity) {
            row = store
            flow.value = listOf(store)
        }

        override suspend fun update(store: StoreProfileEntity) = insertOrUpdate(store)

        override suspend fun updateProtectedCredentials(
            storeId: String,
            protectedUsername: String,
            protectedApiKey: String
        ) {
            row = row?.takeIf { it.id == storeId }?.copy(
                adminUsername = protectedUsername,
                apiKey = protectedApiKey
            )
            flow.value = row?.let(::listOf) ?: emptyList()
        }

        override suspend fun setPrimaryStore(storeId: String) {
            row = row?.copy(isPrimary = row?.id == storeId)
        }

        override suspend fun deleteStoreById(storeId: String) {
            if (row?.id == storeId) row = null
            flow.value = row?.let(::listOf) ?: emptyList()
        }

        override suspend fun deleteAllStores() {
            row = null
            flow.value = emptyList()
        }
    }
}
