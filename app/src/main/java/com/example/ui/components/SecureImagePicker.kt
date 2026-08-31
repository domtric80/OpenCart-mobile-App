package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.model.ProductImageUpload
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Selettore Photo Picker/fotocamera che converte gli errori dei provider esterni in messaggi UI. */
@Composable
fun SecureImagePicker(
    tagPrefix: String,
    onImageSelected: (ProductImageUpload) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun importImage(uri: Uri, fallbackName: String) {
        scope.launch {
            runCatching { readValidatedImage(context, uri, fallbackName) }
                .onSuccess(onImageSelected)
                .onFailure { onError(it.message ?: "Immagine non valida") }
        }
    }

    fun importCameraPreview(bitmap: Bitmap) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val output = ByteArrayOutputStream()
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                        "Impossibile convertire la foto"
                    }
                    ProductImageUpload(output.toByteArray(), "image/jpeg", "camera-preview.jpg")
                }
            }.onSuccess(onImageSelected)
                .onFailure { onError(it.message ?: "Foto non valida") }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { importImage(it, "gallery-image") }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importImage(it, "document-image") }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) importImage(uri, "camera-photo.jpg")
    }
    val cameraPreviewLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let(::importCameraPreview)
    }

    fun launchGallery() {
        runCatching {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }.onFailure { primaryError ->
            Log.w("CartAdminMedia", "Photo Picker unavailable; trying OpenDocument", primaryError)
            runCatching { documentLauncher.launch(arrayOf("image/*")) }
                .onFailure { fallbackError ->
                    Log.e("CartAdminMedia", "No image picker available", fallbackError)
                    onError("Impossibile aprire la galleria (${fallbackError.javaClass.simpleName})")
                }
        }
    }

    fun launchCamera(uri: Uri) {
        runCatching { cameraLauncher.launch(uri) }
            .onFailure { primaryError ->
                Log.w("CartAdminMedia", "Full-size camera unavailable; trying preview", primaryError)
                runCatching { cameraPreviewLauncher.launch(null) }
                    .onFailure { fallbackError ->
                        Log.e("CartAdminMedia", "No camera handler available", fallbackError)
                        onError("Impossibile aprire la fotocamera (${fallbackError.javaClass.simpleName})")
                    }
            }
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                runCatching {
                    val file = File.createTempFile("cartadmin-camera-", ".jpg", context.cacheDir)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.onSuccess { uri ->
                    cameraUri = uri
                    launchCamera(uri)
                }.onFailure { onError("Impossibile preparare la fotocamera") }
            },
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_camera")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Text(" Fotocamera")
        }
        OutlinedButton(
            onClick = {
                launchGallery()
            },
            modifier = Modifier.weight(1f).testTag("${tagPrefix}_gallery")
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Text(" Galleria")
        }
    }
}

private suspend fun readValidatedImage(context: Context, uri: Uri, fallbackName: String): ProductImageUpload =
    withContext(Dispatchers.IO) {
        val maxBytes = 5 * 1024 * 1024
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "L'immagine supera il limite di 5 MB" }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: error("Impossibile leggere l'immagine selezionata")
        val mime = when {
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
            bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "image/png"
            bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
            else -> error("Formato non supportato: usa JPEG, PNG o WebP")
        }
        val extension = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        ProductImageUpload(bytes, mime, "${fallbackName.substringBeforeLast('.')}.$extension")
    }
