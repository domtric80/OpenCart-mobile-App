package com.example.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSecurityInstrumentedTest {
    @Test
    fun databaseOpensWithSecureDeleteEnabled() {
        val database = AppDatabase.getDatabase(ApplicationProvider.getApplicationContext())
        database.openHelper.writableDatabase.query("PRAGMA secure_delete").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }
}
