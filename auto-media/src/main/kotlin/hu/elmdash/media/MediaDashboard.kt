package hu.elmdash.media

import hu.elmdash.connection.DashboardState
import hu.elmdash.connection.Phase
import hu.elmdash.graphics.DashboardTiles
import hu.elmdash.obd.Pid
import java.util.Locale

internal enum class MediaPage(val id: String, val label: String) {
    ENGINE("engine", "Motor / napi fogyasztás"), FUEL("fuel", "Mai nap / útátlag"), SENSORS("sensors", "MAP / TPS / feszültség"), TANK("tank", "Becsült üzemanyagszint");
    companion object {
        fun fromId(id: String?) = entries.firstOrNull { it.id == id } ?: ENGINE
    }
}

/** Value-based equality avoids publishing every 250 ms clock tick to the media host. */
internal data class MediaFrame(
    val title: String, val subtitles: List<String>, val status: String,
    val tiles: List<DashboardTiles.Tile>, val active: Boolean, val simulated: Boolean, val dailyDetail: String, val rangeText: String? = null, val journeyDetail: String = ""
)

internal object MediaDashboard {
    fun number(v: Double?, digits: Int = 0) = v?.let {
        String.format(Locale.forLanguageTag("hu-HU"), "%.${digits}f", it)
    } ?: "—"

    /** The user requested a stable station-only subtitle, including when traffic is available. */
    @Suppress("UNUSED_PARAMETER")
    fun subtitle(frame: MediaFrame, page: MediaPage, radio: RadioState, elapsedMs: Long, traffic: String? = null): String =
        radio.selected.name

    fun frame(s: DashboardState): MediaFrame {
        fun pid(p: Pid, digits: Int = 0): String {
            val current = s.telemetry.value(p, s.nowMs)
            val last = current ?: s.telemetry.readings[p]?.lastKnownValue
            return "${number(last, digits)} ${p.unit}" + if (current == null && last != null) " (utolsó)" else ""
        }
        val tank = s.journal.tank
        val tankText = tank.percent?.let { "Hatótáv ${s.range.displayKm?.let { km -> "≈$km km" } ?: "—"} • Tank ≈${number(it)} %" + if (s.range.uncertain) " • bizonytalan" else " • becslés" } ?: "Tank: jelöld a teletankolást a telefon Napló lapján"
        val tiles = DashboardTiles.create(s)
        val demo = s.simulated || s.phase == Phase.DEMO
        val prefix = if (demo) "DEMÓ • " else ""
        val cue = when (tiles[0].symbol) { "up" -> "↑ "; "down" -> "↓ "; "check" -> "✓ "; else -> "" }
        val average = number(s.trip.averageL100, 1)
        val instant = number(s.fuelDisplay.value, 1)
        val daily = number(s.daily.averageL100, 1)
        val estimate = if (s.fuelDisplay.source.estimated && s.fuelDisplay.value != null) "≈" else ""
        val dailyEstimate = if (s.daily.containsEstimate) "≈" else ""
        val source = if (s.fuelDisplay.fresh) s.fuelDisplay.source.label else s.fuel.unavailableReason ?: "Adatra vár"
        val instantText = "$estimate$instant"
        val stale = if (!s.fuelDisplay.fresh && s.fuelDisplay.value != null) " (utolsó)" else ""
        return MediaFrame(
            title = if (s.carUnavailable) "Nincs kapcsolat az autóval" else "${prefix}$instantText • Ma $dailyEstimate$daily l/100 km$stale",
            subtitles = listOf(
                "$cue${pid(Pid.RPM)} • ${pid(Pid.COOLANT)} • ${pid(Pid.LOAD)}",
                "${prefix}Ma ${number(s.daily.fuelLiters, 2)} l / ${number(s.daily.distanceKm, 1)} km • Út $average l/100 km" + if (s.daily.hasGaps) " • részleges" else "",
                "${prefix}MAP ${pid(Pid.MAP)} • TPS ${pid(Pid.TPS)} • ${pid(Pid.VOLTAGE, 1)}",
                tankText
            ),
            status = "$prefix${s.phase.label} • $source • 10 s átlag • ${s.today}" + if (!tiles[0].fresh || !tiles[1].fresh) " • Nincs friss motoradat" else "",
            tiles = tiles, active = s.active, simulated = demo,
            journeyDetail = (if (demo) s.trip else s.journal.active?.summary).let {
                "${number(it?.distanceKm, 1)}/${number(it?.averageL100, 1)}/${it?.containsEstimate}"
            },
            rangeText = s.range.displayKm?.let { "Hatótáv ≈$it km" + if (s.range.uncertain) " • bizonytalan" else " • becslés" },
            dailyDetail = "${s.today}/${number(s.daily.distanceKm, 1)}/${s.daily.averageL100}/${s.daily.hasGaps}/${s.daily.containsEstimate}"
        )
    }
}

internal object MediaBrowserAccess {
    const val ANDROID_AUTO = "com.google.android.projection.gearhead"
    fun allowed(pkg: String, uid: Int, ownUid: Int, packagesForUid: Array<String>?): Boolean =
        packagesForUid?.contains(pkg) == true && (uid == ownUid || pkg == ANDROID_AUTO)
}
