package com.bolaodosvei.app.data

/**
 * Endereço de sincronização embutido no APK do Bolão dos Véi.
 * Os usuários só precisam digitar o Código da Sala para sincronizar os palpites em tempo real.
 */
object AppConfig {
    const val SYNC_BASE_URL: String = "https://bolao-dos-vei-default-rtdb.firebaseio.com"
}
