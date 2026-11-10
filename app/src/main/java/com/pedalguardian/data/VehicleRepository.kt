package com.pedalguardian.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
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

    // Pleos Vehicle SDK: 브레이크 페달 상태
    private val vehicle = Vehicle(context)
    private var brakeRegistered = false

    // SDK 콜백은 IPC 스레드에서 올 수 있으므로 메인 스레드로 넘겨서 처리한다.
    // (차속 콜백도 메인 스레드라 PedalLog를 한 스레드에서만 만지게 된다)
    private val main = Handler(Looper.getMainLooper())

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            if (status != null) main.post { onBrake(status) } // null = 값 없음, 무시
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "브레이크 상태 구독 오류", e)
        }
    }

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

        startBrake()
    }

    /**
     * 브레이크: ① 지원 여부 확인 → ② 현재 값 한 번 읽기 → ③ 변화 구독.
     * 읽기 전용 API만 쓴다(set 계열은 호출하지 않는다).
     */
    private fun startBrake() {
        vehicle.initialize()
        val brake = vehicle.getBrake()
        brake.checkBrakePedalStatusCapabilities(
            onComplete = { config ->
                val readable = config.accessType == AccessType.READ || config.accessType == AccessType.READ_WRITE
                if (!readable) {
                    Log.w(TAG, "이 차량은 브레이크 페달 상태를 지원하지 않음: ${config.accessType}")
                    return@checkBrakePedalStatusCapabilities
                }
                brake.getCurrentBrakePedalStatus(
                    onComplete = { status -> if (status != null) main.post { onBrake(status) } },
                    onFailed = { Log.w(TAG, "브레이크 현재 값 읽기 실패", it) },
                )
                brake.registerBrakePedalStatus(brakeListener)
                brakeRegistered = true
            },
            onFailed = { Log.w(TAG, "브레이크 지원 여부 확인 실패", it) },
        )
    }

    fun stop() {
        if (brakeRegistered) vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
        brakeRegistered = false
        vehicle.release()
        main.removeCallbacksAndMessages(null)
        propertyManager?.unregisterCallback(speedCallback)
        car?.disconnect()
        propertyManager = null
        car = null
    }

    /** 브레이크 상태 반영 (메인 스레드에서 호출). */
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
