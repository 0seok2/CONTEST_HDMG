package com.pedalguardian.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedalguardian.data.PedalSample
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 정차 중에만 여는 기록 조회 화면.
 * 왼쪽: "최근 기록"(순환 버퍼) + 보존된 이상 구간 목록. 오른쪽: 선택한 기록의 시간축 그래프.
 */
@Composable
fun HistoryScreen(
    recent: List<PedalSample>,
    segments: List<File>,
    load: (File) -> List<PedalSample>,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf<File?>(null) } // null = 최근 기록
    val samples = remember(selected) { selected?.let(load) ?: recent }
    val fmt = remember { SimpleDateFormat("MM/dd HH:mm:ss", Locale.KOREA) }

    Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("기록 조회", color = Color.White, fontSize = 28.sp)
            FilterChip(selected == null, onClick = { selected = null }, label = { Text("최근 기록") })
            Text("보존된 이상 구간 ${segments.size}건", color = Color.LightGray)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(segments) { f ->
                    val t = f.name.removePrefix("anomaly_").removeSuffix(".csv").toLongOrNull()
                    FilterChip(
                        selected = selected == f,
                        onClick = { selected = f },
                        label = { Text(t?.let { fmt.format(Date(it)) } ?: f.name) },
                    )
                }
            }
            Button(onClick = onBack) { Text("돌아가기") }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("━ 차속   ■ 브레이크   ■ 가속", color = Color.LightGray)
            if (samples.size < 2) {
                Text("표시할 기록이 없습니다.", color = Color.LightGray)
            } else {
                PedalGraph(samples, Modifier.fillMaxWidth().height(320.dp))
                val sec = (samples.last().timeMs - samples.first().timeMs) / 1000f
                val max = samples.maxOf { it.speedKmh }
                Text("구간 %.1f초 · 최고 %.0f km/h".format(sec, max), color = Color.LightGray)
            }
        }
    }
}

private val BrakeBand = Color(0x66FF8080)
private val AccelBand = Color(0x6680E090)

/**
 * 시간축 그래프. x축 = 시간, y축 = 차속.
 * 브레이크 ON 구간은 위쪽 절반에 빨간 띠, 가속 ON 구간은 아래쪽 절반에 초록 띠로 칠한다.
 */
@Composable
private fun PedalGraph(samples: List<PedalSample>, modifier: Modifier) {
    Canvas(modifier) {
        val t0 = samples.first().timeMs
        val span = (samples.last().timeMs - t0).coerceAtLeast(1).toFloat()
        val maxSpeed = maxOf(10f, samples.maxOf { it.speedKmh })
        fun x(t: Long) = (t - t0) / span * size.width
        fun y(v: Float) = size.height - v / maxSpeed * size.height

        // 페달 띠: 다음 샘플까지 그 상태가 유지됐다고 본다
        samples.zipWithNext { a, b ->
            val left = x(a.timeMs)
            val w = x(b.timeMs) - left
            if (a.brake) drawRect(BrakeBand, Offset(left, 0f), Size(w, size.height / 2))
            if (a.accel) drawRect(AccelBand, Offset(left, size.height / 2), Size(w, size.height / 2))
        }

        val path = Path()
        samples.forEachIndexed { i, s ->
            if (i == 0) path.moveTo(x(s.timeMs), y(s.speedKmh)) else path.lineTo(x(s.timeMs), y(s.speedKmh))
        }
        drawPath(path, Color.White, style = Stroke(width = 3f))
        drawLine(Color.Gray, Offset(0f, size.height), Offset(size.width, size.height))
    }
}
