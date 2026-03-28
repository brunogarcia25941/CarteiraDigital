package com.brunogarcia.carteira

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
// 1. Importamos a permissão especial
import kotlinx.cinterop.ExperimentalForeignApi

// 2. Colocamos o "carimbo" de autorização na nossa classe
@OptIn(ExperimentalForeignApi::class)
actual class BiometricAuthenticator {
    actual fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val context = LAContext()

        // 3. Passamos 'null' no erro porque só queremos o resultado Booleano (True/False)
        if (context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)) {
            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                "Desbloquear Carteira"
            ) { success, error ->
                if (success) {
                    onSuccess()
                } else {
                    onError(error?.localizedDescription ?: "Erro desconhecido")
                }
            }
        } else {
            onError("Biometria não disponível neste dispositivo")
        }
    }
}

@Composable
actual fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    return remember { BiometricAuthenticator() }
}