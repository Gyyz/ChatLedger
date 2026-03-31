package com.chatledger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chatledger.ai.model.AiConfig
import com.chatledger.ai.model.AiProvider
import com.chatledger.util.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    val aiConfig: StateFlow<AiConfig> = settingsManager.aiConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiConfig())

    val currency: StateFlow<String> = settingsManager.currency
        .stateIn(viewModelScope, SharingStarted.Eagerly, "¥")

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun saveConfig(
        provider: AiProvider,
        apiKey: String,
        baseUrl: String,
        model: String,
        maxTokens: Int
    ) {
        viewModelScope.launch {
            settingsManager.saveAiConfig(
                AiConfig(
                    provider = provider,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    maxTokens = maxTokens
                )
            )
            _saveSuccess.value = true
        }
    }

    fun saveCurrency(currency: String) {
        viewModelScope.launch {
            settingsManager.saveCurrency(currency)
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}
