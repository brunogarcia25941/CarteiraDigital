package com.brunogarcia.carteira

import platform.UIKit.UIDevice
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()


// A forma como o iOS (usando o motor gráfico Skia) transforma os Bytes
actual fun converterBytesParaBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}