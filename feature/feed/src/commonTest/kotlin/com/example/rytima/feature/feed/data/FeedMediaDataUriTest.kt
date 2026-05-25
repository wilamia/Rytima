package com.example.rytima.feature.feed.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedMediaDataUriTest {

    @Test
    fun parse_returns_payload_for_valid_png_data_uri() {
        val payload = FeedMediaDataUri.parse("data:image/png;base64,SGVsbG8=")

        requireNotNull(payload)
        assertEquals("image/png", payload.contentType)
        assertEquals("feed-image.png", payload.fileName)
        assertContentEquals("Hello".encodeToByteArray(), payload.bytes)
    }

    @Test
    fun parse_returns_null_for_non_image_payload() {
        val payload = FeedMediaDataUri.parse("data:text/plain;base64,SGVsbG8=")

        assertNull(payload)
    }

    @Test
    fun parse_returns_null_for_invalid_base64() {
        val payload = FeedMediaDataUri.parse("data:image/jpeg;base64,%%%")

        assertNull(payload)
    }
}
