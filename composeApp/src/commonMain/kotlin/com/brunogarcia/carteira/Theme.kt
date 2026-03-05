package com.brunogarcia.carteira

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 1. As Formas
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), // Para os cartões e pop-ups!
    large = RoundedCornerShape(24.dp)
)

// 2. Cores
private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C), // Um verde/azul forte estilo "Cofre/Carteira"
    onPrimary = Color.White,
    primaryContainer = Color(0xFF89F8C7),
    onPrimaryContainer = Color(0xFF002114),
    background = Color(0xFFFBFDF9),
    surface = Color(0xFFFBFDF9),
    surfaceVariant = Color(0xFFDBE5DD), // Cor de fundo dos cartões
)

// As nossas cores exatas da Lista de Compras
val BackgroundNavy = Color(0xFF0B132B)    // Fundo da app
val CardSurfaceBlue = Color(0xFF1C2541)   // Fundo dos cartões e Barra de Topo
val PrimaryAccent = Color(0xFF5BC0BE)     // Ciano vibrante
val TextWhite = Color(0xFFF0F6F6)         // Texto principal
val TextGray = Color(0xFFA0AAB2)          // Texto secundário

// O Tema Escuro
private val DarkColors = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = BackgroundNavy,          // Texto em cima do botão ciano deve ser escuro
    primaryContainer = CardSurfaceBlue,  // O fundo da TopAppBar
    onPrimaryContainer = TextWhite,      // O texto da TopAppBar
    background = BackgroundNavy,         // Fundo principal da App
    onBackground = TextWhite,            // Texto normal
    surface = CardSurfaceBlue,           // O fundo dos Pop-ups e dos Cartões
    onSurface = TextWhite,               // Texto em cima dos cartões
    surfaceVariant = CardSurfaceBlue,    // Variantes de superfície (caixas de texto, etc)
    onSurfaceVariant = TextGray          // Ícones secundários e texto descritivo
)


// 3. O Tema Principal
@Composable
fun CarteiraTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = DarkColors,
        shapes = AppShapes,
        content = content
    )
}