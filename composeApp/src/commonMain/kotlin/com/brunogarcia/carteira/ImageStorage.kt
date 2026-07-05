package com.brunogarcia.carteira

import androidx.compose.runtime.Composable

// Interface comum para guardar, carregar e apagar ficheiros de imagem localmente
interface ImageStorage {
    // Guarda os bytes da imagem no disco e devolve o caminho absoluto
    fun saveImage(bytes: ByteArray, fileName: String): String?
    
    // Lê os bytes da imagem a partir do caminho absoluto
    fun loadImage(path: String): ByteArray?
    
    // Apaga a imagem do disco
    fun deleteImage(path: String): Boolean
}

// Helper do Compose para obter o ImageStorage correto para cada plataforma
@Composable
expect fun rememberImageStorage(): ImageStorage
