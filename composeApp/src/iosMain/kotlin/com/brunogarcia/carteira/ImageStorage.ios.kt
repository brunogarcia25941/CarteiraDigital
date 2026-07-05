package com.brunogarcia.carteira

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.*
import platform.posix.memcpy
import kotlinx.cinterop.*

// Implementação em iOS que guarda os ficheiros na pasta Documents da aplicação
@OptIn(ExperimentalForeignApi::class)
class IosImageStorage : ImageStorage {
    
    override fun saveImage(bytes: ByteArray, fileName: String): String? {
        // Encontra o caminho para a pasta Documents no Sandbox da app
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.firstOrNull() as? String ?: return null
        val filePath = (documentsDirectory as NSString).stringByAppendingPathComponent(fileName)
        
        // Converte a ByteArray do Kotlin para NSData do iOS/Objective-C
        val nsData = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        
        // Grava no disco
        return if (nsData.writeToFile(filePath, true)) {
            filePath
        } else {
            null
        }
    }

    override fun loadImage(path: String): ByteArray? {
        // Lê o ficheiro em formato NSData
        val nsData = NSData.dataWithContentsOfFile(path) ?: return null
        val size = nsData.length.toInt()
        val byteArray = ByteArray(size)
        
        // Copia os dados do NSData de volta para a ByteArray do Kotlin
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), nsData.bytes, nsData.length)
        }
        return byteArray
    }

    override fun deleteImage(path: String): Boolean {
        // Apaga o ficheiro usando o NSFileManager nativo
        val fileManager = NSFileManager.defaultManager
        return fileManager.removeItemAtPath(path, null)
    }
}

// Cria e lembra a instância do ImageStorage em iOS (não necessita de Context nativo)
@Composable
actual fun rememberImageStorage(): ImageStorage {
    return remember { IosImageStorage() }
}
