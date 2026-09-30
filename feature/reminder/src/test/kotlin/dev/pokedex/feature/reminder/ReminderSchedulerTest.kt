package dev.pokedex.feature.reminder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSchedulerTest {

    private val nine = LocalTime.of(9, 0)

    @Test
    fun `before nine waits until nine today`() {
        assertEquals(Duration.ofHours(2), ReminderScheduler.untilNext(nine, LocalDateTime.of(2026, 9, 30, 7, 0)))
    }

    @Test
    fun `after nine waits until nine tomorrow`() {
        assertEquals(Duration.ofHours(23), ReminderScheduler.untilNext(nine, LocalDateTime.of(2026, 9, 30, 10, 0)))
    }

    @Test
    fun `exactly nine waits a full day`() {
        assertEquals(Duration.ofDays(1), ReminderScheduler.untilNext(nine, LocalDateTime.of(2026, 9, 30, 9, 0)))
    }
}
