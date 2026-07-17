package com.strobingn.wildlifefieldops.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strobingn.wildlifefieldops.ai.PhotoAIHelper
import com.strobingn.wildlifefieldops.data.remote.AiService
import com.strobingn.wildlifefieldops.data.remote.SpeciesIdResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class SpeciesIdViewModel @Inject constructor(
    private val aiService: AiService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isConfigured: Boolean get() = aiService.isConfigured
    val providerLabel: String get() = aiService.providerLabel

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _analyzing = MutableStateFlow(false)
    val analyzing = _analyzing.asStateFlow()

    private val _result = MutableStateFlow<SpeciesIdResult?>(null)
    val result = _result.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _imageUri.value = uri
        _result.value = null
        _message.value = null
    }

    /** Camera capture path: TakePicturePreview returns a Bitmap — cache it and reuse the file Uri. */
    fun onCameraBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "species_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                withContext(Dispatchers.Main) { onImageSelected(Uri.fromFile(file)) }
            } catch (e: Exception) {
                _message.value = "Could not save camera photo: ${e.message}"
            }
        }
    }

    fun analyze() {
        val uri = _imageUri.value ?: return
        if (_analyzing.value) return
        _analyzing.value = true
        _message.value = null
        viewModelScope.launch {
            try {
                // On-device first pass (ML Kit labels + damage hints)
                val offline = PhotoAIHelper.analyzePhotoForFormFilling(context, uri)
                val labels = (offline.species + offline.damageTypes + offline.objectDetections).distinct()
                // Downscaled JPEG for the vision model
                val base64 = withContext(Dispatchers.IO) { uriToDownscaledBase64(context, uri) }
                _result.value = aiService.identifySpecies(base64, labels)
            } catch (e: Exception) {
                _message.value = "Analysis failed: ${e.message}"
            }
            _analyzing.value = false
        }
    }

    fun clear() {
        _imageUri.value = null
        _result.value = null
        _message.value = null
    }
}

private fun uriToDownscaledBase64(context: Context, uri: Uri, maxDim: Int = 1024, quality: Int = 80): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            // cache files and content URIs both decode here; bail if unreadable
            if (uri.scheme == "file") {
                val direct = BitmapFactory.decodeFile(uri.path)
                if (direct != null) return encodeJpeg(direct, maxDim, quality)
            }
            return null
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim || bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return null
        encodeJpeg(bitmap, maxDim, quality)
    } catch (e: Exception) {
        null
    }
}

private fun encodeJpeg(bitmap: Bitmap, maxDim: Int, quality: Int): String {
    val scale = if (maxOf(bitmap.width, bitmap.height) > maxDim) {
        maxDim.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
    } else 1f
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else bitmap
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
