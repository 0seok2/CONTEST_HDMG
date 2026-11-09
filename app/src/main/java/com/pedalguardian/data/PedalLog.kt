package com.pedalguardian.data

/** 한 시점의 페달·차속 기록. */
data class PedalSample(
    val timeMs: Long,
    val brake: Boolean,
    val accel: Boolean,
    val speedKmh: Float,
)

/**
 * 최근 [bufferMs] 동안의 기록을 들고 있는 순환 버퍼 + 이상 구간 판정기.
 *
 * 이상 구간: 브레이크를 밟고 있는 동안 차속이 그 누름 중 최저값보다 [riseKmh] 이상 오르면 이상으로 본다.
 * 판정 후 [tailMs] 만큼 더 기록한 뒤 버퍼 전체(판정 전 + 판정 후)를 [add]의 반환값으로 내준다.
 * 호출하는 쪽이 그 목록을 파일로 저장하면 순환 버퍼가 덮어써도 사라지지 않는다.
 *
 * 안드로이드 의존성이 없는 순수 Kotlin이라 JVM 단위 테스트로 바로 검증할 수 있다.
 */
class PedalLog(
    private val bufferMs: Long = BUFFER_MS,
    private val tailMs: Long = TAIL_MS,
    private val riseKmh: Float = SPEED_RISE_KMH,
) {
    private val buffer = ArrayDeque<PedalSample>()

    private var minSpeedWhileBraking: Float? = null // 이번 브레이크 누름 중 최저 차속
    private var flaggedThisPress = false            // 한 번 누를 때 한 번만 판정
    private var saveAtMs: Long? = null              // 이 시각이 지나면 구간을 내보낸다

    /** 샘플을 추가한다. 보존할 이상 구간이 완성되면 그 구간을 돌려주고, 아니면 null. */
    fun add(s: PedalSample): List<PedalSample>? {
        buffer.addLast(s)
        while (s.timeMs - buffer.first().timeMs > bufferMs) buffer.removeFirst()

        if (s.brake) {
            val min = minOf(minSpeedWhileBraking ?: s.speedKmh, s.speedKmh)
            minSpeedWhileBraking = min
            if (!flaggedThisPress && s.speedKmh - min >= riseKmh) {
                flaggedThisPress = true
                if (saveAtMs == null) saveAtMs = s.timeMs + tailMs
            }
        } else {
            minSpeedWhileBraking = null
            flaggedThisPress = false
        }

        val due = saveAtMs ?: return null
        if (s.timeMs < due) return null
        saveAtMs = null
        return buffer.toList()
    }

    /** 조회 화면용: 현재 버퍼 복사본. */
    fun snapshot(): List<PedalSample> = buffer.toList()

    companion object {
        /** 순환 버퍼 길이. 이상 구간 "앞" 부분은 BUFFER_MS - TAIL_MS 만큼 남는다. */
        const val BUFFER_MS = 30_000L
        /** 이상 판정 뒤 추가로 기록할 시간("뒤" 부분). */
        const val TAIL_MS = 5_000L
        /** 이상 판정 기준: 브레이크 중 최저 차속 대비 상승량(km/h). 차량·센서 잡음에 맞춰 조정한다. */
        const val SPEED_RISE_KMH = 3f

        fun toCsv(samples: List<PedalSample>): String = buildString {
            appendLine("timeMs,brake,accel,speedKmh")
            samples.forEach { appendLine("${it.timeMs},${it.brake},${it.accel},${it.speedKmh}") }
        }
    }
}
