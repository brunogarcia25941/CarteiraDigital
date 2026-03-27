package com.brunogarcia.carteira

import androidx.compose.ui.graphics.ImageBitmap


expect fun converterBytesParaBitmap(bytes: ByteArray): ImageBitmap?