package com.pedalguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pedalguardian.data.VehicleRepository
import com.pedalguardian.ui.PedalIndicator

class MainActivity : ComponentActivity() {

    private lateinit var repo: VehicleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = VehicleRepository(applicationContext)
        repo.start()

        setContent {
            val state by repo.state.collectAsState()
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                // 차속 표시 (디버그·시연 확인용)
                Text(
                    "%.0f km/h".format(state.speedKmh),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center),
                )
                PedalIndicator(state, Modifier.align(Alignment.BottomEnd).padding(24.dp))
            }
        }
    }

    override fun onDestroy() {
        repo.stop()
        super.onDestroy()
    }
}
