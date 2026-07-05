package com.brunogarcia.carteira

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Implementação em Android usando a biblioteca oficial do Google ML Kit (local e offline)
actual class TextRecognizer {
    
    // Inicializa o cliente do ML Kit para reconhecimento de texto (alfabeto latino)
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    actual fun recognizeText(
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // 1. Descodifica os bytes na imagem Bitmap do Android
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                onError("Não foi possível descodificar a imagem.")
                return
            }

            // 2. Transforma o bitmap em InputImage, que é o formato exigido pelo ML Kit
            val image = InputImage.fromBitmap(bitmap, 0)

            // 3. Processa a imagem de forma assíncrona
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Callback com o texto reconhecido com sucesso
                    onSuccess(visionText.text)
                }
                .addOnFailureListener { e ->
                    // Callback caso ocorra alguma falha no processamento
                    onError(e.localizedMessage ?: "Falha ao processar texto na imagem.")
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Erro inesperado no processo de OCR.")
        }
    }
}

// Lembra a instância do TextRecognizer no Compose para Android
@Composable
actual fun rememberTextRecognizer(): TextRecognizer {
    return remember { TextRecognizer() }
}
