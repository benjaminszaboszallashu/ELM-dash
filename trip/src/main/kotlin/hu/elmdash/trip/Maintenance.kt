package hu.elmdash.trip

import java.time.LocalDate

data class ServiceEntry(val id: String, val name: String, val performed: LocalDate, val odometerKm: Double,
    val intervalKm: Double?, val intervalMonths: Int?, val note: String = "", val costHuf: Double? = null) {
    init {
        require(costHuf == null || costHuf.isFinite() && costHuf >= 0)
        require(name.isNotBlank() && name.length <= 100)
        require(odometerKm.isFinite() && odometerKm >= 0)
        require(intervalKm == null || intervalKm.isFinite() && intervalKm > 0)
        require(intervalMonths == null || intervalMonths in 1..240)
    }
    fun due(today: LocalDate, currentKm: Double?): Boolean =
        intervalMonths?.let { !today.isBefore(performed.plusMonths(it.toLong())) } == true ||
        (currentKm != null && intervalKm?.let { currentKm >= odometerKm + it } == true)
    fun soon(today: LocalDate, currentKm: Double?): Boolean =
        intervalMonths?.let { !today.plusDays(30).isBefore(performed.plusMonths(it.toLong())) } == true ||
        (currentKm != null && intervalKm?.let { currentKm + 500 >= odometerKm + it } == true)
}

/** Inclusive local dates, assigned by departure; only retained completed journeys. */
data class ServicePeriod(val journeys: Int, val distanceKm: Double)
fun servicePeriod(records: List<JourneyRecord>, from: LocalDate, to: LocalDate, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): ServicePeriod {
    require(!to.isBefore(from))
    val selected = records.filter { r ->
        val date = java.time.Instant.ofEpochMilli(r.startedAtMs).atZone(zone).toLocalDate()
        r.endedAtMs != null && !date.isBefore(from) && !date.isAfter(to)
    }
    return ServicePeriod(selected.size, selected.sumOf { it.summary.distanceKm })
}
fun inspectionSoon(due: LocalDate?, today: LocalDate) = due != null && !today.plusWeeks(6).isBefore(due)
