package com.brunogarcia.carteira

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Vision.*
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.cinterop.*

// Implementação em iOS usando a framework nativa Vision (rápida, local e sem dependências externas)
@OptIn(ExperimentalForeignApi::class)
actual class TextRecognizer {
    
    actual fun recognizeText(
        imageBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            // 1. Converte a array de bytes Kotlin para NSData do iOS
            val nsData = imageBytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), imageBytes.size.toULong())
            }
            
            // 2. Converte NSData para UIImage e extrai a CGImage (necessário para a framework Vision)
            val uiImage = UIImage.imageWithData(nsData) ?: run {
                onError("Não foi possível processar os bytes da imagem no iOS.")
                return
            }
            val cgImage = uiImage.CGImage ?: run {
                onError("Erro ao converter a imagem para CGImage.")
                return
            }

            // 3. Inicializa o processador de imagem nativo
            val requestHandler = VNImageRequestHandler(cgImage = cgImage, options = NSDictionary())
            
            // 4. Cria o pedido de reconhecimento de texto
            val request = VNRecognizeTextRequest { request, error ->
                if (error != null) {
                    onError(error.localizedDescription)
                    return@VNRecognizeTextRequest
                }
                
                // Converte os resultados obtidos em texto legível
                val results = request?.results as? List<VNRecognizedTextObservation> ?: emptyList()
                val textFound = results.joinToString("\n") { observation ->
                    val topCandidate = observation.topCandidates(1).firstOrNull() as? VNRecognizedText
                    topCandidate?.string ?: ""
                }
                onSuccess(textFound)
            }
            
            // Configura os idiomas preferenciais de deteção
            request.recognitionLanguages = listOf("pt-PT", "pt-BR", "en-US")
            
            // Executa a tarefa de processamento
            requestHandler.performRequests(listOf(request), null)
            
        } catch (e: Exception) {
            onError(e.message ?: "Erro inesperado no OCR nativo do iOS.")
        }
    }
}

// Lembra a instância do TextRecognizer no Compose para iOS
@Composable
actual fun rememberTextRecognizer(): TextRecognizer {
    return remember { TextRecognizer() }
}
