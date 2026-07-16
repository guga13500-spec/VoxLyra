package com.voxlyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxlyra.viewmodel.VoxLyraState
import com.voxlyra.viewmodel.VoxLyraViewModel

@Composable
fun VoxLyraScreen(viewModel: VoxLyraViewModel) {
    val state by viewModel.state.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val response by viewModel.response.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val counterText by viewModel.counterText.collectAsState()

    val scrollState = rememberScrollState()
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabeçalho
        Text(
            text = "VoxLyra",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics {
                contentDescription = "Vox Lyra, assistente de voz"
                stateDescription = when (state) {
                    VoxLyraState.IDLE -> "Pronto para ouvir"
                    VoxLyraState.LISTENING -> "Gravando"
                    VoxLyraState.PROCESSING -> "Processando"
                    VoxLyraState.SPEAKING -> "Falando resposta"
                    VoxLyraState.ERROR -> "Erro"
                }
            }
        )

        Text(
            text = "Assistente de voz com Gemini AI",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card de status/transcrição
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status
                Text(
                    text = when (state) {
                        VoxLyraState.IDLE -> "🎤 Toque no microfone e fale"
                        VoxLyraState.LISTENING -> "🔴 Ouvindo... $counterText"
                        VoxLyraState.PROCESSING -> "🤔 Processando com Gemini..."
                        VoxLyraState.SPEAKING -> "🔊 Falando resposta"
                        VoxLyraState.ERROR -> "⚠️ $errorMessage"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = when (state) {
                        VoxLyraState.LISTENING -> Color(0xFFEF5350)
                        VoxLyraState.PROCESSING -> MaterialTheme.colorScheme.primary
                        VoxLyraState.SPEAKING -> Color(0xFF4CAF50)
                        VoxLyraState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.SemiBold
                )

                // Transcrição
                if (transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Você: $transcript",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Resposta
                if (response.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = response,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botão do microfone grandão
        MicButton(
            state = state,
            onClick = {
                when (state) {
                    VoxLyraState.IDLE -> viewModel.startListening()
                    VoxLyraState.LISTENING -> viewModel.stopListening()
                    VoxLyraState.ERROR -> viewModel.clearError()
                    else -> { /* ignorar */ }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Entrada de texto alternativa
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Campo de texto para digitar mensagem"
                    },
                placeholder = { Text("Ou digite aqui...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendTextMessage(textInput)
                        textInput = ""
                    }
                },
                enabled = state != VoxLyraState.PROCESSING && textInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.semantics {
                    contentDescription = "Enviar mensagem de texto"
                }
            ) {
                Text("Enviar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botão reset
        IconButton(
            onClick = { viewModel.reset() },
            modifier = Modifier
                .size(40.dp)
                .semantics {
                    contentDescription = "Limpar conversa"
                }
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Limpar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MicButton(
    state: VoxLyraState,
    onClick: () -> Unit
) {
    val isActive = state == VoxLyraState.LISTENING
    val isError = state == VoxLyraState.ERROR
    val isProcessing = state == VoxLyraState.PROCESSING ||
            state == VoxLyraState.SPEAKING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFEF5350)
            isError -> MaterialTheme.colorScheme.error
            isProcessing -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "bgColor"
    )

    val description = when (state) {
        VoxLyraState.IDLE -> "Botão para começar a gravar"
        VoxLyraState.LISTENING -> "Botão para parar de gravar e processar"
        VoxLyraState.PROCESSING -> "Processando sua mensagem"
        VoxLyraState.SPEAKING -> "Falando a resposta"
        VoxLyraState.ERROR -> "Tocar para limpar erro"
    }

    val icon = when {
        isActive -> Icons.Default.Mic
        isError -> Icons.Default.Refresh
        else -> Icons.Default.Mic
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(if (isActive) pulseScale else 1f)
            .clip(CircleShape)
            .background(bgColor)
            .semantics {
                contentDescription = description
                stateDescription = when (state) {
                    VoxLyraState.IDLE -> "Pronto"
                    VoxLyraState.LISTENING -> "Gravando, toque para parar"
                    else -> state.name
                }
            },
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(120.dp),
            enabled = !isProcessing
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}