package com.example.rytima.feature.feed.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.rytima.core.designsystem.RytimaTheme
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.base64EncodedStringWithOptions
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

@Composable
internal actual fun FeedMediaPickerButton(
    modifier: Modifier,
    enabled: Boolean,
    onMediaPicked: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    OutlinedButton(
        modifier = modifier,
        enabled = enabled,
        onClick = {
            scope.launch {
                pickFeedImageFromGallery()?.let(onMediaPicked)
            }
        },
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = RytimaTheme.colors.primary,
        )
        Text("Pick photo")
    }
}

private var activeFeedPickerDelegate: FeedPickerDelegate? = null

private class FeedPickerDelegate(
    private val onResult: (String?) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)

        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        val provider = results.firstOrNull()?.itemProvider
        if (provider == null) {
            finish(null)
            return
        }

        val typeId = "public.image"
        if (!provider.hasItemConformingToTypeIdentifier(typeId)) {
            finish(null)
            return
        }

        provider.loadDataRepresentationForTypeIdentifier(typeId) { nsData, error ->
            if (error != null || nsData == null) {
                finish(null)
                return@loadDataRepresentationForTypeIdentifier
            }

            val image = UIImage.imageWithData(nsData)
            if (image == null) {
                finish(null)
                return@loadDataRepresentationForTypeIdentifier
            }

            @OptIn(ExperimentalForeignApi::class)
            val jpegData = UIImageJPEGRepresentation(image, 0.88)
            if (jpegData == null) {
                finish(null)
                return@loadDataRepresentationForTypeIdentifier
            }

            @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
            val base64 = jpegData.base64EncodedStringWithOptions(options = 0UL)
            finish("data:image/jpeg;base64,$base64")
        }
    }

    private fun finish(result: String?) {
        onResult(result)
        activeFeedPickerDelegate = null
    }
}

private fun feedTopViewController(from: UIViewController? = null): UIViewController? {
    val root = from
        ?: UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<platform.UIKit.UIWindowScene>()
            .firstOrNull()
            ?.windows
            ?.filterIsInstance<platform.UIKit.UIWindow>()
            ?.firstOrNull { it.isKeyWindow() }
            ?.rootViewController
        ?: @Suppress("DEPRECATION") UIApplication.sharedApplication.keyWindow?.rootViewController

    val presented = root?.presentedViewController
    return if (presented != null) feedTopViewController(presented) else root
}

private suspend fun pickFeedImageFromGallery(): String? = suspendCancellableCoroutine { cont ->
    val config = PHPickerConfiguration().apply {
        selectionLimit = 1
        filter = PHPickerFilter.imagesFilter
    }

    val delegate = FeedPickerDelegate { result ->
        if (cont.isActive) cont.resume(result)
    }
    activeFeedPickerDelegate = delegate

    val picker = PHPickerViewController(configuration = config)
    picker.setDelegate(delegate)

    val presenter = feedTopViewController()
    if (presenter != null) {
        presenter.presentViewController(picker, animated = true, completion = null)
    } else {
        activeFeedPickerDelegate = null
        if (cont.isActive) cont.resume(null)
    }

    cont.invokeOnCancellation {
        activeFeedPickerDelegate = null
    }
}
