package com.pedalguardian.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** 화면이 그리는 현재 상태. */
data class PedalState(
    val brake: Boolean = false,
    val accel: Boolean = false, // 가속 페달 API 미확인: 지금은 항상 false
    val speedKmh: Float = 0f,
) {
    val stopped: Boolean get() = speedKmh < 0.5f
}

/**
 * 차량 신호(브레이크, 차속)를 받아 [state]로 내보내고 [PedalLog]에 기록한다.
 * 읽기 전용이다. 차량을 제어하는 호출은 절대 넣지 않는다.
 */
class VehicleRepository(private val context: Context) {

    private val _state = MutableStateFlow(PedalState())
    val state: StateFlow<PedalState> = _state

    val log = PedalLog()

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    // 차속 콜백. PERF_VEHICLE_SPEED 단위는 m/s.
    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val mps = value.value as? Float ?: return
            update(_state.value.copy(speedKmh = mps * 3.6f))
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Log.w(TAG, "차속 읽기 오류 prop=$propertyId area=$areaId")
        }
    }

    fun start() {
        val c = Car.createCar(context) ?: return
        car = c
        val pm = c.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return
        propertyManager = pm
        pm.registerCallback(speedCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)

        // TODO(Brake API): Pleos Vehicle SDK의 Brake API로 브레이크 페달 상태 콜백을 등록하고,
        //  콜백에서 onBrake(pressed)를 부른다. 지원 여부는 checkXXXCapabilities 계열로 먼저 확인.
        //  클래스·메서드 이름은 API 레퍼런스에서 확인한 뒤 쓴다(추측 금지).
    }

    fun stop() {
        propertyManager?.unregisterCallback(speedCallback)
        car?.disconnect()
        propertyManager = null
        car = null
    }

    /** Brake API 콜백에서 부른다. */
    fun onBrake(pressed: Boolean) = update(_state.value.copy(brake = pressed))

    private fun update(new: PedalState) {
        _state.value = new
        val segment = log.add(PedalSample(System.currentTimeMillis(), new.brake, new.accel, new.speedKmh))
        if (segment != null) saveSegment(segment)
    }

    /** 이상 구간을 내부 저장소 CSV로 보존한다. */
    private fun saveSegment(segment: List<PedalSample>) {
        val file = File(context.filesDir, "anomaly_${segment.last().timeMs}.csv")
        runCatching { file.writeText(PedalLog.toCsv(segment)) }
            .onFailure { Log.e(TAG, "이상 구간 저장 실패", it) }
    }

    /** 보존된 이상 구간 파일 목록 (최신순). */
    fun savedSegments(): List<File> =
        context.filesDir.listFiles { f -> f.name.startsWith("anomaly_") }?.sortedByDescending { it.name } ?: emptyList()

    fun loadSegment(file: File): List<PedalSample> =
        runCatching { PedalLog.fromCsv(file.readText()) }.getOrDefault(emptyList())

    companion object {
        private const val TAG = "VehicleRepository"
    }
}
