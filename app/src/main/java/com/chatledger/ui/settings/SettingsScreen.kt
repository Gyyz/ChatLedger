package com.chatledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatledger.ai.model.AiProvider
import com.chatledger.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val currentConfig by viewModel.aiConfig.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var selectedProvider by remember(currentConfig) {
        mutableStateOf(currentConfig.provider)
    }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var model by remember(currentConfig) { mutableStateOf(currentConfig.model) }
    var maxTokens by remember(currentConfig) {
        mutableStateOf(currentConfig.maxTokens.toString())
    }
    var showApiKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    // 保存成功提示
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSaveSuccess()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("设置", style = MaterialTheme.typography.titleLarge) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI 供应商配置
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI 配置",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 供应商选择
                    ExposedDropdownMenuBox(
                        expanded = providerExpanded,
                        onExpandedChange = { providerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProvider.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("AI 供应商") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {
                            AiProvider.entries.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName) },
                                    onClick = {
                                        selectedProvider = provider
                                        providerExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // API Key
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "切换显示"
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    // 自定义 Base URL
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("自定义 API 地址（可选）") },
                        placeholder = { Text("留空使用默认地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    // 模型名称
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("模型名称（可选）") },
                        placeholder = {
                            val config = com.chatledger.ai.model.AiConfig(provider = selectedProvider)
                            Text("默认: ${config.getEffectiveModel()}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    // Max Tokens
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it.filter { c -> c.isDigit() } },
                        label = { Text("最大 Token 数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // 保存按钮
                    Button(
                        onClick = {
                            viewModel.saveConfig(
                                provider = selectedProvider,
                                apiKey = apiKey,
                                baseUrl = baseUrl,
                                model = model,
                                maxTokens = maxTokens.toIntOrNull() ?: 1024
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存配置")
                    }

                    // 保存成功提示
                    AnimatedVisibility(visible = saveSuccess) {
                        Text(
                            "✅ 配置已保存",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                        )
                    }
                }
            }

            // 关于
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "关于",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "ChatLedger v1.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "聊天式智能记账 · 支持多 AI 供应商",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "支持的 AI 供应商：Claude, OpenAI, Gemini, DeepSeek, 以及任何 OpenAI API 兼容服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnimatedVisibility(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(visible = visible) {
        content()
    }
}
