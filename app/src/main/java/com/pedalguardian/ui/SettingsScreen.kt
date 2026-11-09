package com.pedalguardian.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedalguardian.data.PedalState

/** 표시 위치(화면 네 구석). */
enum class Corner(val label: String, val alignment: Alignment) {
    TopStart("왼쪽 위", Alignment.TopStart),
    TopEnd("오른쪽 위", Alignment.TopEnd),
    BottomStart("왼쪽 아래", Alignment.BottomStart),
    BottomEnd("오른쪽 아래", Alignment.BottomEnd),
}

/** 페달 표시 설정. SharedPreferences에 저장한다. */
data class IndicatorSettings(
    val corner: Corner = Corner.BottomEnd,
    val sizeDp: Float = 56f,
    val opacity: Float = 0.9f,
) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("corner", corner.name)
            .putFloat("size", sizeDp)
            .putFloat("opacity", opacity)
            .apply()
    }

    companion object {
        private const val PREFS = "indicator"
        const val MIN_SIZE = 40f
        const val MAX_SIZE = 120f
        const val MIN_OPACITY = 0.3f // 너무 투명해서 안 보이는 일이 없도록 하한을 둔다

        fun load(context: Context): IndicatorSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = IndicatorSettings()
            return IndicatorSettings(
                corner = runCatching { Corner.valueOf(p.getString("corner", null)!!) }.getOrDefault(d.corner),
                sizeDp = p.getFloat("size", d.sizeDp),
                opacity = p.getFloat("opacity", d.opacity),
            )
        }
    }
}

/**
 * 정차 중에만 여는 설정 화면. 바꾼 값은 바로 미리보기에 반영되고, 화면을 나갈 때 저장된다.
 */
@Composable
fun SettingsScreen(
    settings: IndicatorSettings,
    onChange: (IndicatorSettings) -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(32.dp).width(520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("표시 설정", color = Color.White, fontSize = 28.sp)

            Text("위치", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Corner.entries.forEach { c ->
                    FilterChip(
                        selected = settings.corner == c,
                        onClick = { onChange(settings.copy(corner = c)) },
                        label = { Text(c.label) },
                    )
                }
            }

            Text("크기 ${settings.sizeDp.toInt()}dp", color = Color.White)
            Slider(
                value = settings.sizeDp,
                onValueChange = { onChange(settings.copy(sizeDp = it)) },
                valueRange = IndicatorSettings.MIN_SIZE..IndicatorSettings.MAX_SIZE,
            )

            Text("투명도 ${(settings.opacity * 100).toInt()}%", color = Color.White)
            Slider(
                value = settings.opacity,
                onValueChange = { onChange(settings.copy(opacity = it)) },
                valueRange = IndicatorSettings.MIN_OPACITY..1f,
            )

            Box(Modifier.height(8.dp))
            Button(onClick = onBack) { Text("돌아가기") }
        }

        // 미리보기: 브레이크를 밟은 상태로 그린다
        PedalIndicator(
            PedalState(brake = true),
            Modifier.align(settings.corner.alignment).padding(24.dp),
            size = settings.sizeDp.dp,
            opacity = settings.opacity,
        )
    }
}
