package com.brunogarcia.carteira

import androidx.compose.runtime.Composable

// Classe expect para reconhecimento ótico de caracteres (OCR) nas imagens
expect class TextRecognizer {
    // Processa a imagem a partir de bytes e devolve o texto reconhecido ou erro
    fun recognizeText(
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )
}

// Helper Composable para lembrar a instância do TextRecognizer em cada plataforma
@Composable
expect fun rememberTextRecognizer(): TextRecognizer
