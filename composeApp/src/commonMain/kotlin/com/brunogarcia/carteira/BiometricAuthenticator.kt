package com.brunogarcia.carteira

import androidx.compose.runtime.Composable

// A classe que vai fazer a leitura
expect class BiometricAuthenticator {
    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit)
}

// Uma função mágica do Compose para criar o autenticador
@Composable
expect fun rememberBiometricAuthenticator(): BiometricAuthenticator