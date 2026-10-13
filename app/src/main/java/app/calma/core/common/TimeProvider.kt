package app.calma.core.common

/**
 * Injectable time source.
 *
 * Timers and breathing sessions must never use wall-clock time for their deadlines:
 * `System.currentTimeMillis()` can jump when the user or the system changes the clock,
 * which would corrupt a running session. They use [elapsedRealtime] instead, which is
 * monotonic and keeps counting while the device sleeps.
 *
 * Tests substitute a fake so the elapsed-time maths can be verified deterministically.
 */
interface TimeProvider {
    /** Milliseconds since boot, including deep sleep. Resets only on reboot. */
    fun elapsedRealtime(): Long

    /** Wall-clock time, used only for timestamps written to the database. */
    fun currentTimeMillis(): Long
}

/** Production implementation backed by [android.os.SystemClock]. */
class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/**
 * Test double. Also usable from previews to render a fixed countdown.
 */
class FakeTimeProvider(
    var elapsed: Long = 0L,
    var wallClock: Long = 1_700_000_000_000L,
) : TimeProvider {
    override fun elapsedRealtime(): Long = elapsed
    override fun currentTimeMillis(): Long = wallClock

    fun advance(millis: Long) {
        elapsed += millis
        wallClock += millis
    }
}