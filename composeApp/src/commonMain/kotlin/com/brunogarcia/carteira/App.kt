package com.brunogarcia.carteira

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import carteiradigital.composeapp.generated.resources.Res
import carteiradigital.composeapp.generated.resources.compose_multiplatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {

    // variável que controla se o pop-up está visível ou não
    var mostrarDialogo by remember { mutableStateOf(false) }

    CarteiraTheme {
        // O Scaffold é o esqueleto do projeto
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Carteira Digital",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                        onClick = { mostrarDialogo = true } // Abre o pop-up
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Documento"
                    )
                }
            }
        ) { innerPadding ->

            // Box é o ecrã central
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                // Estado "Vazio" da Carteira
                Text(
                    text = "A tua carteira está vazia.\nClica no '+' para adicionar um documento.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            }
        } // fim scaffold
        // Se a variável for verdadeira, Pop-up no ecrã
        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false }, // Se clicar fora, fecha
                title = {
                    Text(text = "Novo Documento")
                },
                text = {
                    Text(text = "Aqui vamos colocar o formulário e o botão para abrir a câmara/galeria!")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // TODO: Guardar o documento
                            mostrarDialogo = false
                        }
                    ) {
                        Text("Guardar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { mostrarDialogo = false }
                    ) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }// fim carteira theme
}