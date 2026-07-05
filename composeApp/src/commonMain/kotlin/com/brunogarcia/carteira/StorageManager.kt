package com.brunogarcia.carteira

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StorageManager {
    // Inicializa a biblioteca Settings (o armazenamento local)
    private val settings = Settings()

    // A "chave" com a qual vamos guardar a nossa lista no telemóvel
    private val CHAVE_DOCUMENTOS = "lista_documentos"

    // Função para ler os documentos guardados
    fun carregarDocumentos(): List<Documento> {
        val jsonGuardado = settings.getString(CHAVE_DOCUMENTOS, "")

        if (jsonGuardado.isEmpty()) {
            return emptyList() // Se não houver nada, devolvemos uma lista vazia
        }

        // Se houver texto guardado, transformamos de volta numa Lista de Documentos
        return try {
            Json.decodeFromString(jsonGuardado)
        } catch (e: Exception) {
            println("Erro ao ler documentos: ${e.message}")
            emptyList()
        }
    }

    // Função para guardar toda a lista de documentos de uma vez
    fun guardarListaDocumentos(novaLista: List<Documento>) {
        val novoJson = Json.encodeToString(novaLista)
        settings.putString(CHAVE_DOCUMENTOS, novoJson)
    }

    // Função para guardar um novo documento
    fun guardarDocumento(novoDoc: Documento) {
        // 1. Lemos os que já existem
        val listaAtual = carregarDocumentos().toMutableList()

        // 2. Adicionamos o novo à lista
        listaAtual.add(novoDoc)

        // 3. Guardamos a lista completa no Settings
        guardarListaDocumentos(listaAtual)
    }

    // Função para REMOVER um documento
    fun removerDocumento(idParaRemover: String) {
        // 1. Carregamos a lista atual
        val listaAtual = carregarDocumentos()

        // 2. Filtramos a lista (mantemos todos os que NÃO TÊM este ID)
        val novaLista = listaAtual.filter { it.id != idParaRemover }

        // 3. Guardamos a nova lista por cima da antiga
        guardarListaDocumentos(novaLista)
    }

    // A "chave" para o PIN de segurança
    private val CHAVE_PIN = "pin_seguranca"

    // Obtém o PIN guardado (retorna null se não existir)
    fun obterPin(): String? {
        val pin = settings.getString(CHAVE_PIN, "")
        return if (pin.isEmpty()) null else pin
    }

    // Guarda o novo PIN de segurança
    fun guardarPin(novoPin: String) {
        settings.putString(CHAVE_PIN, novoPin)
    }
}