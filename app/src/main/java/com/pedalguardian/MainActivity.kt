package com.pedalguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pedalguardian.data.VehicleRepository
import com.pedalguardian.ui.HistoryScreen
import com.pedalguardian.ui.IndicatorSettings
import com.pedalguardian.ui.PedalIndicator
import com.pedalguardian.ui.SettingsScreen

private enum class Screen { Main, History, Settings }

class MainActivity : ComponentActivity() {

    private lateinit var repo: VehicleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = VehicleRepository(applicationContext)
        repo.start()

        setContent {
            val state by repo.state.collectAsState()
            var screen by remember { mutableStateOf(Screen.Main) }
            var settings by remember { mutableStateOf(IndicatorSettings.load(applicationContext)) }

            // 안전 원칙: 차가 움직이기 시작하면 기록·설정 화면을 닫고 메인(표시만)으로 돌아간다.
            LaunchedEffect(state.stopped) {
                if (!state.stopped && screen != Screen.Main) {
                    settings.save(applicationContext)
                    screen = Screen.Main
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                when (screen) {
                    Screen.Main -> {
                        // 차속 표시 (시연 확인용)
                        Text(
                            "%.0f km/h".format(state.speedKmh),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        // 터치 요소는 정차 중에만 보인다
                        if (state.stopped) {
                            Row(
                                Modifier.align(Alignment.TopCenter).padding(24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Button(onClick = { screen = Screen.History }) { Text("기록") }
                                Button(onClick = { screen = Screen.Settings }) { Text("설정") }
                            }
                        }
                        PedalIndicator(
                            state,
                            Modifier.align(settings.corner.alignment).padding(24.dp),
                            size = settings.sizeDp.dp,
                            opacity = settings.opacity,
                        )
                    }

                    Screen.History -> HistoryScreen(
                        recent = repo.log.snapshot(),
                        segments = repo.savedSegments(),
                        load = repo::loadSegment,
                        onBack = { screen = Screen.Main },
                    )

                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        onChange = { settings = it },
                        onBack = {
                            settings.save(applicationContext)
                            screen = Screen.Main
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        repo.stop()
        super.onDestroy()
    }
}
