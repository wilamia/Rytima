package com.example.rytima.feature.feed.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Base64.NO_WRAP
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.rytima.core.designsystem.RytimaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
internal actual fun FeedMediaPickerButton(
    modifier: Modifier,
    enabled: Boolean,
    onMediaPicked: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { picked ->
            scope.launch {
                val payload = withContext(Dispatchers.IO) { picked.toFeedDataUri(context) }
                payload?.let(onMediaPicked)
            }
        }
    }

    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = { launcher.launch("image/*") },
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = RytimaTheme.colors.primary,
        )
        Text("Pick photo")
    }
}

private fun Uri.toFeedDataUri(context: Context): String? =
    context.contentResolver.openInputStream(this)?.use { input ->
        BitmapFactory.decodeStream(input)
    }?.toFeedDataUri()

private fun Bitmap.toFeedDataUri(): String? {
    val output = ByteArrayOutputStream()
    val didCompress = compress(Bitmap.CompressFormat.JPEG, 88, output)
    if (!didCompress) return null
    val base64 = Base64.encodeToString(output.toByteArray(), NO_WRAP)
    return "data:image/jpeg;base64,$base64"
}
