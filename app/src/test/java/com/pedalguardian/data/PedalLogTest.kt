package com.pedalguardian.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PedalLogTest {

    private fun s(t: Long, brake: Boolean, speed: Float) = PedalSample(t, brake, false, speed)

    @Test
    fun 순환버퍼는_오래된_샘플을_버린다() {
        val log = PedalLog(bufferMs = 1_000)
        for (t in 0L..3_000L step 100) log.add(s(t, false, 50f))
        val snap = log.snapshot()
        assertEquals(2_000L, snap.first().timeMs)
        assertEquals(3_000L, snap.last().timeMs)
    }

    @Test
    fun 정상_제동은_이상이_아니다() {
        val log = PedalLog(bufferMs = 10_000, tailMs = 1_000, riseKmh = 3f)
        var speed = 60f
        for (t in 0L..5_000L step 100) {
            assertNull(log.add(s(t, true, speed)))
            speed = maxOf(0f, speed - 1f) // 감속
        }
    }

    @Test
    fun 브레이크중_차속증가는_앞뒤구간을_돌려준다() {
        val log = PedalLog(bufferMs = 10_000, tailMs = 1_000, riseKmh = 3f)
        for (t in 0L until 2_000L step 100) assertNull(log.add(s(t, false, 30f)))
        // 2초부터 브레이크를 밟았는데 차속이 오른다
        var result: List<PedalSample>? = null
        var speed = 30f
        for (t in 2_000L..5_000L step 100) {
            result = log.add(s(t, true, speed)) ?: result
            speed += 0.5f
        }
        assertNotNull(result)
        val r = result!!
        // 판정(30→33km/h, 2.6초) 후 1초 뒤에 내보낸다. 앞 구간(0초~)도 포함
        assertEquals(0L, r.first().timeMs)
        assertEquals(3_600L, r.last().timeMs)
    }

    @Test
    fun 한번_누를때_한번만_판정한다() {
        val log = PedalLog(bufferMs = 10_000, tailMs = 0, riseKmh = 3f)
        var count = 0
        var speed = 30f
        for (t in 0L..5_000L step 100) {
            if (log.add(s(t, true, speed)) != null) count++
            speed += 0.5f
        }
        assertEquals(1, count)
    }

    @Test
    fun CSV로_저장하고_다시_읽으면_같다() {
        val samples = listOf(s(0, false, 10f), s(100, true, 12.5f))
        assertEquals(samples, PedalLog.fromCsv(PedalLog.toCsv(samples)))
    }
}
