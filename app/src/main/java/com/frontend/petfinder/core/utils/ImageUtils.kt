package com.frontend.petfinder.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    private const val MAX_PHOTOS_ALLOWED = 4 //

    /**
     * Procesa una lista de URIs, verifica el límite de cantidad,
     * comprime las imágenes si superan los 5MB y las convierte en MultipartBody.Part
     */
    fun processImagesForUpload(
        context: Context,
        uris: List<Uri>,
        partName: String = "fotos"
    ): List<MultipartBody.Part> {

        // Regla 1: Máximo 4 fotos
        val limitedUris = uris.take(MAX_PHOTOS_ALLOWED)
        val multipartList = mutableListOf<MultipartBody.Part>()

        limitedUris.forEachIndexed { index, uri ->
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()

                // Regla 2: Límite de 5MB
                val finalBytes = if (bytes.size > MAX_FILE_SIZE_BYTES) {
                    compressImage(bytes)
                } else {
                    bytes
                }

                // Determinar el MIME type correcto
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val requestBody = finalBytes.toRequestBody(mimeType.toMediaTypeOrNull())

                // Añadimos el archivo a la lista Multipart
                val fileName = "pet_photo_${System.currentTimeMillis()}_$index.jpg"
                multipartList.add(
                    MultipartBody.Part.createFormData(partName, fileName, requestBody)
                )
            }
        }
        return multipartList
    }

    /**
     * Comprime agresivamente la imagen para asegurar que baje de los 5MB
     */
    private fun compressImage(originalBytes: ByteArray): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size)
        val outputStream = ByteArrayOutputStream()

        // Comprimimos al 70% de calidad en formato JPEG
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

        // Si sigue siendo mayor a 5MB, forzamos un reescalado (se podría iterar aquí)
        var compressedBytes = outputStream.toByteArray()
        var quality = 70

        while (compressedBytes.size > MAX_FILE_SIZE_BYTES && quality > 10) {
            outputStream.reset()
            quality -= 15
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedBytes = outputStream.toByteArray()
        }

        return compressedBytes
    }
}