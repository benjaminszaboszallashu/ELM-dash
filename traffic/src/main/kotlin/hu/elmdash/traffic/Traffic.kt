package hu.elmdash.traffic

import java.time.Instant
import java.util.Locale
import kotlin.math.*

data class GeoPoint(val lat: Double, val lon: Double) {
    init { require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) }
    fun distance(other: GeoPoint): Double {
        val a = sin(Math.toRadians(other.lat - lat) / 2).pow(2) + cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(Math.toRadians(other.lon - lon) / 2).pow(2)
        return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
    fun bearing(other: GeoPoint): Double {
        val delta = Math.toRadians(other.lon - lon)
        return (Math.toDegrees(atan2(sin(delta) * cos(Math.toRadians(other.lat)), cos(Math.toRadians(lat)) * sin(Math.toRadians(other.lat)) - sin(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * cos(delta))) + 360) % 360
    }
}
data class TrafficFix(val point: GeoPoint, val accuracy: Double, val bearing: Double?, val speed: Double, val elapsedMs: Long)
data class TrafficEvent(val id: String, val point: GeoPoint, val road: String, val label: String,
    val direction: Double?, val start: Instant?, val end: Instant?, val priority: Int)
data class TrafficFeed(val published: Instant, val events: List<TrafficEvent>, val skipped: Int)
data class TrafficMatch(val event: TrafficEvent, val meters: Double) {
    fun line(): String {
        val distance = if (meters < 1000) "<1 km" else "≈${String.format(Locale.forLanguageTag("hu-HU"), "%.0f", meters / 1000)} km"
        return "Útinform · ${event.road.ifBlank { "közelben" }} · ${event.label} $distance"
    }
}
object TrafficFilter {
    fun angle(a: Double, b: Double) = abs(((a - b + 540) % 360) - 180)
    fun nearby(feed: TrafficFeed, fix: TrafficFix, now: Instant, elapsed: Long): List<TrafficMatch> {
        if (elapsed - fix.elapsedMs !in 0..20_000 || !fix.accuracy.isFinite() || fix.accuracy !in 0.0..100.0) return emptyList()
        if (now.toEpochMilli() - feed.published.toEpochMilli() !in -120_000..900_000) return emptyList()
        val heading = fix.bearing?.takeIf { it.isFinite() && it in 0.0..<360.0 && fix.speed >= 2.0 }
        val radius = if (heading == null) 1500.0 else (fix.speed * 180).coerceIn(2000.0, 8000.0)
        return feed.events.asSequence().filter { (it.start == null || !now.isBefore(it.start)) && (it.end == null || now.isBefore(it.end)) }
            .map { TrafficMatch(it, fix.point.distance(it.point)) }
            .filter { it.meters <= radius }
            .filter { heading == null || it.meters < 150 || angle(heading, fix.point.bearing(it.event.point)) <= 55 }
            .filter { heading == null || it.event.direction == null || angle(heading, it.event.direction) <= 65 }
            .sortedWith(compareByDescending<TrafficMatch> { it.event.priority }.thenBy { it.meters }).toList()
    }
}

/** A bounded display slot, once per event per 30 minutes. A disappearing match is removed immediately. */
class TrafficPresenter {
    private val shown = mutableMapOf<String, Long>()
    private var current: String? = null
    private var currentLine: String? = null
    private var until = 0L
    private var next = 0L
    fun line(matches: List<TrafficMatch>, elapsed: Long): String? {
        shown.entries.removeAll { elapsed - it.value >= 1_800_000 }
        current?.let { id ->
            val match = matches.firstOrNull { it.event.id == id }
            if (elapsed < until && match != null) return currentLine
            current = null
        }
        if (elapsed < next) return null
        val selected = matches.firstOrNull { it.event.id !in shown } ?: return null
        current = selected.event.id; currentLine = selected.line(); shown[selected.event.id] = elapsed
        until = elapsed + 20_000; next = elapsed + 60_000
        return currentLine
    }
}
