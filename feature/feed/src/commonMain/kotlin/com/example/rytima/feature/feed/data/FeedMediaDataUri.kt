package com.example.rytima.feature.feed.data

import com.example.rytima.core.designsystem.AvatarUtils
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class FeedMediaUploadPayload(
    val bytes: ByteArray,
    val contentType: String,
    val fileName: String,
)

internal object FeedMediaDataUri {

    @OptIn(ExperimentalEncodingApi::class)
    fun parse(value: String): FeedMediaUploadPayload? {
        val trimmed = value.trim()
        if (!AvatarUtils.isDataUri(trimmed)) return null

        val commaIndex = trimmed.indexOf(',')
        if (commaIndex <= 0) return null

        val metadata = trimmed.substring(0, commaIndex)
        val contentType = metadata
            .substringAfter("data:", missingDelimiterValue = "")
            .substringBefore(';')
            .lowercase()
            .takeIf { it.startsWith("image/") }
            ?: return null

        if (!metadata.contains(";base64", ignoreCase = true)) return null

        val payload = trimmed.substring(commaIndex + 1).replace("\\s".toRegex(), "")
        val bytes = runCatching { Base64.Default.decode(payload) }.getOrNull() ?: return null

        return FeedMediaUploadPayload(
            bytes = bytes,
            contentType = contentType,
            fileName = "feed-image.${extensionFor(contentType)}",
        )
    }

    private fun extensionFor(contentType: String): String = when (contentType.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> contentType.substringAfter('/', "bin").substringBefore(';')
    }
}
