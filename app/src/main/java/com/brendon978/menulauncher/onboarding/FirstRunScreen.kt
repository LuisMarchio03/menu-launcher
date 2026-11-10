package com.brendon978.menulauncher.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FirstRunStep { HOME, MUSIC, AI, STYLE, DONE }

/**
 * Um passo por tela, cada um pulável; o que já está resolvido é pulado sozinho.
 * O passo da IA é opcional: conectar ou pular, sem insistir.
 */
@Composable
fun FirstRunScreen(
    isDefaultHome: Boolean,
    hasMusicAccess: Boolean,
    onRequestHome: () -> Unit,
    onRequestMusic: () -> Unit,
    hasAi: Boolean,
    aiChooser: @Composable () -> Unit,
    /** Tema e modo, com prévia. */
    styleChooser: @Composable () -> Unit = {},
    onDone: () -> Unit,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val steps = FirstRunStep.entries
    // Avança por cima do que já está feito (ex.: voltou das configurações com o acesso dado).
    while (index < steps.lastIndex && when (steps[index]) {
            FirstRunStep.HOME -> isDefaultHome
            FirstRunStep.MUSIC -> hasMusicAccess
            FirstRunStep.AI -> hasAi
            FirstRunStep.STYLE -> false
            FirstRunStep.DONE -> false
        }
    ) index++

    val step = steps[index]
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
    ) {
        val (title, body, action) = when (step) {
            FirstRunStep.HOME -> Triple("Usar como tela inicial", "Para o botão home abrir este launcher no lugar do One UI.", "Definir")
            FirstRunStep.MUSIC -> Triple("Mostrar a música que está tocando", "Libere o acesso às notificações para ver a faixa e controlar a música na tela inicial.", "Liberar")
            FirstRunStep.AI -> Triple("Tem uma IA para conectar?", "O símbolo dela fica na tela inicial: toque para abrir, segure para falar com ela.", "")
            FirstRunStep.STYLE -> Triple("Escolha um visual", "Um tema e o jeito da lista. Dá para mudar tudo depois nos ajustes.", "Continuar")
            FirstRunStep.DONE -> Triple("Pronto", "Segure um app para favoritar ou trocar o ícone. Segure uma área vazia para abrir os ajustes.", "Começar")
        }
        Text(title, color = Color.White, fontSize = 26.sp)
        Spacer(Modifier.height(10.dp))
        Text(body, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
        Spacer(Modifier.height(28.dp))
        if (step == FirstRunStep.AI) {
            aiChooser()
            Spacer(Modifier.height(16.dp))
        }
        if (step == FirstRunStep.STYLE) {
            styleChooser()
            Spacer(Modifier.height(24.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step != FirstRunStep.AI) Button(action, primary = true) {
                when (step) {
                    FirstRunStep.HOME -> onRequestHome()
                    FirstRunStep.MUSIC -> onRequestMusic()
                    FirstRunStep.AI -> Unit
                    FirstRunStep.STYLE -> index++
                    FirstRunStep.DONE -> onDone()
                }
            }
            if (step != FirstRunStep.DONE && step != FirstRunStep.STYLE) Button("Pular", primary = false) { index++ }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Button(text: String, primary: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (primary) Color.Black else Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .background(if (primary) Color.White else Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
    )
}
