package com.example.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPickerCompatibilityRegressionTest {
    private val pickerSource = File("src/main/java/com/example/ui/components/SecureImagePicker.kt").readText()
    private val manifestSource = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun galleryHasDocumentProviderFallback() {
        assertTrue(pickerSource.contains("ActivityResultContracts.StartActivityForResult()"))
        assertTrue(pickerSource.contains("Intent.ACTION_OPEN_DOCUMENT"))
        assertTrue(pickerSource.contains("Intent.ACTION_GET_CONTENT"))
        assertTrue(pickerSource.indexOf("Intent.ACTION_GET_CONTENT") < pickerSource.indexOf("Intent.ACTION_OPEN_DOCUMENT"))
    }

    @Test
    fun cameraHasPreviewFallback() {
        assertTrue(pickerSource.contains("MediaStore.ACTION_IMAGE_CAPTURE"))
        assertTrue(pickerSource.contains("MediaStore.EXTRA_OUTPUT"))
        assertTrue(pickerSource.contains("Intent.FLAG_GRANT_WRITE_URI_PERMISSION"))
        assertTrue(pickerSource.contains("context.grantUriPermission("))
        assertTrue(pickerSource.contains("cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))"))
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
