package com.brunogarcia.carteira

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

// Implementação em Android que guarda os ficheiros na pasta interna de ficheiros da aplicação
class AndroidImageStorage(private val context: Context) : ImageStorage {
    
    override fun saveImage(bytes: ByteArray, fileName: String): String? {
        return try {
            // Guarda o ficheiro no diretório de ficheiros privados da app (context.filesDir)
            val file = File(context.filesDir, fileName)
            file.writeBytes(bytes)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun loadImage(path: String): ByteArray? {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun deleteImage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

// Cria e lembra a instância do ImageStorage em Android obtendo o Context atual do Compose
@Composable
actual fun rememberImageStorage(): ImageStorage {
    val context = LocalContext.current
    return remember(context) { AndroidImageStorage(context) }
}
