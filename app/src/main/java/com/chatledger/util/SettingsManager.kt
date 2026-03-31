package com.chatledger.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chatledger.ai.model.AiConfig
import com.chatledger.ai.model.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_MODEL = stringPreferencesKey("model_name")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_CURRENCY = stringPreferencesKey("currency")
    }

    val aiConfig: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        AiConfig(
            provider = try {
                AiProvider.valueOf(prefs[KEY_PROVIDER] ?: AiProvider.CLAUDE.name)
            } catch (_: Exception) {
                AiProvider.CLAUDE
            },
            apiKey = prefs[KEY_API_KEY] ?: "",
            baseUrl = prefs[KEY_BASE_URL] ?: "",
            model = prefs[KEY_MODEL] ?: "",
            maxTokens = prefs[KEY_MAX_TOKENS] ?: 1024
        )
    }

    val currency: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: "¥"
    }

    suspend fun saveAiConfig(config: AiConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = config.provider.name
            prefs[KEY_API_KEY] = config.apiKey
            prefs[KEY_BASE_URL] = config.baseUrl
            prefs[KEY_MODEL] = config.model
            prefs[KEY_MAX_TOKENS] = config.maxTokens
        }
    }

    suspend fun saveCurrency(currency: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY] = currency
        }
    }
}
