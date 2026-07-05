package com.brunogarcia.carteira

import kotlinx.serialization.Serializable

@Serializable
data class Documento(
    val id: String, // Um ID único para cada documento
    val nome: String,
    val notas: String,
    val caminhoFoto: String? = null,
    val categoria: String = "Outros" // Categoria do documento (ex: Identidade, Finanças, Saúde, etc.)
)