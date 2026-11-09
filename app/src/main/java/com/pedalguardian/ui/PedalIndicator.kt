package com.pedalguardian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pedalguardian.data.PedalState

private val LightRed = Color(0xFFFFB3B3)
private val LightGreen = Color(0xFFB8F0C0)
private val Idle = Color(0x33FFFFFF)

/**
 * 화면 구석의 페달 표시. 터치 요소 없음, 알림 없음. 밟은 페달에만 연한 색을 입힌다.
 * 왼쪽부터 브레이크, 가속 (실제 페달 배치와 같은 순서).
 */
@Composable
fun PedalIndicator(
    state: PedalState,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    opacity: Float = 0.9f,
) {
    Row(modifier.alpha(opacity), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pedal("B", if (state.brake) LightRed else Idle, size)
        Pedal("A", if (state.accel) LightGreen else Idle, size)
    }
}

@Composable
private fun Pedal(label: String, fill: Color, size: Dp) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(size * 0.7f, size).background(fill, shape).border(1.dp, Color.Gray, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.DarkGray)
    }
}
