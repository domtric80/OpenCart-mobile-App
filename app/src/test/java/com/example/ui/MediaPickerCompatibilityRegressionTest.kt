package com.example.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPickerCompatibilityRegressionTest {
    private val pickerSource = File("src/main/java/com/example/ui/components/SecureImagePicker.kt").readText()
    private val manifestSource = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun galleryUsesMinimalSystemContentContract() {
        assertTrue(pickerSource.contains("ActivityResultContracts.GetContent()"))
        assertTrue(pickerSource.contains("galleryLauncher.launch(\"image/*\")"))
        assertTrue(!pickerSource.contains("PickVisualMedia"))
    }

    @Test
    fun cameraUsesMinimalPreviewContractWithoutFileProvider() {
        assertTrue(pickerSource.contains("ActivityResultContracts.TakePicturePreview()"))
        assertTrue(pickerSource.contains("cameraLauncher.launch(null)"))
        assertTrue(!pickerSource.contains("FileProvider"))
        assertTrue(!pickerSource.contains("EXTRA_OUTPUT"))
    }

    @Test
    fun manifestDeclaresExternalMediaHandlersWithoutBroadMediaPermissions() {
        assertTrue(!manifestSource.contains("android.permission.CAMERA"))
        assertTrue(!manifestSource.contains("android.permission.READ_MEDIA_IMAGES"))
        assertTrue(!manifestSource.contains("androidx.core.content.FileProvider"))
        assertTrue(!manifestSource.contains("photopicker_activity"))
    }
}
