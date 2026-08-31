package com.example.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPickerCompatibilityRegressionTest {
    private val pickerSource = File("src/main/java/com/example/ui/components/SecureImagePicker.kt").readText()
    private val manifestSource = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun galleryHasDocumentProviderFallback() {
        assertTrue(pickerSource.contains("ActivityResultContracts.PickVisualMedia()"))
        assertTrue(pickerSource.contains("ActivityResultContracts.OpenDocument()"))
        assertTrue(pickerSource.contains("documentLauncher.launch(arrayOf(\"image/*\"))"))
    }

    @Test
    fun cameraHasPreviewFallback() {
        assertTrue(pickerSource.contains("ActivityResultContracts.TakePicture()"))
        assertTrue(pickerSource.contains("ActivityResultContracts.TakePicturePreview()"))
        assertTrue(pickerSource.contains("cameraPreviewLauncher.launch(null)"))
    }

    @Test
    fun manifestDeclaresExternalMediaHandlersWithoutBroadMediaPermissions() {
        assertTrue(manifestSource.contains("android.media.action.IMAGE_CAPTURE"))
        assertTrue(manifestSource.contains("android.intent.action.OPEN_DOCUMENT"))
        assertTrue(manifestSource.contains("android.provider.action.PICK_IMAGES"))
        assertTrue(!manifestSource.contains("android.permission.CAMERA"))
        assertTrue(!manifestSource.contains("android.permission.READ_MEDIA_IMAGES"))
    }
}
