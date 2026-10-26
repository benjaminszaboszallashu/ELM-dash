package hu.elmdash.trip

/** Presentation-only trailing 10 seconds. Totals must continue using unsmoothed fuel samples.
 * Integrate fuel and distance over the same window, rather than averaging ratios at different speeds.
 */
class FuelWindow(private val windowMs: Long = 10_000) {
    private data class Point(val time: Long, val rate: Double, val speed: Double, val source: FuelSource)
    private val points = ArrayDeque<Point>()
    init { require(windowMs > 0) }
    fun reset() = points.clear()

    fun update(fuel: FuelReading, speed: Double?, nowMs: Long): FuelReading {
        val rate = fuel.litersPerHour
        if (rate == null || !rate.isFinite() || rate < 0 || speed == null || !speed.isFinite() || speed < 0) {
            reset()
            return fuel.copy(litersPerHour = null, litersPer100Km = null)
        }
        val last = points.lastOrNull()
        // Only short pauses are held; longer gaps and clock resets discard the window.
        if (last != null && (nowMs < last.time || nowMs - last.time > 10_000)) reset()
        if (points.lastOrNull()?.time == nowMs) points.removeLast()
        points.addLast(Point(nowMs, rate, speed, fuel.source))
        val cutoff = nowMs - windowMs
        while (points.size > 1 && points.elementAt(1).time <= cutoff) points.removeFirst()
        var fuelIntegral = 0.0
        var speedIntegral = 0.0
        var duration = 0.0
        for ((a, b) in points.zipWithNext()) {
            val begin = maxOf(a.time, cutoff)
            if (b.time <= begin) continue
            val fraction = (begin - a.time).toDouble() / (b.time - a.time)
            val held = b.time - a.time > 2_000
            val endRate = if (held) a.rate else b.rate
            val endSpeed = if (held) a.speed else b.speed
            val beginRate = a.rate + (endRate - a.rate) * fraction
            val beginSpeed = a.speed + (endSpeed - a.speed) * fraction
            val dt = (b.time - begin).toDouble()
            fuelIntegral += (beginRate + endRate) * 0.5 * dt
            speedIntegral += (beginSpeed + endSpeed) * 0.5 * dt
            duration += dt
        }
        if (duration == 0.0) return fuel // First sample: use the available observation, no invented history.
        val meanRate = fuelIntegral / duration
        val meanSpeed = speedIntegral / duration
        val source = when {
            points.any { it.source == FuelSource.MAP_ESTIMATE } -> FuelSource.MAP_ESTIMATE
            points.any { it.source == FuelSource.MAF_ESTIMATE } -> FuelSource.MAF_ESTIMATE
            points.any { it.source == FuelSource.ECU } -> FuelSource.ECU
            else -> fuel.source
        }
        return FuelReading(meanRate, if (meanSpeed > 0 && speed >= 5) fuelIntegral * 100 / speedIntegral else null, source)
    }
}
