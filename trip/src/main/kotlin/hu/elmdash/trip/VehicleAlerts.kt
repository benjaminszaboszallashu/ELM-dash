package hu.elmdash.trip

/** Advisory thresholds, not an alternator diagnosis or a battery state-of-charge estimate. */
data class VehicleAlert(val id: String, val title: String, val detail: String, val critical: Boolean = false)
data class AlertEvaluation(val active: List<VehicleAlert>, val raised: List<VehicleAlert>)
class VehicleAlerts {
    private var runningSince: Long? = null
    private var previous: Long? = null
    private val gates = mutableMapOf<String, Gate>()
    private class Gate { var since: Long? = null; var level = 0; var candidate = 0; var recovery: Long? = null }
    fun tick(now: Long, running: Boolean, voltage: Double?, coolant: Double?, rangeKm: Double?): AlertEvaluation {
        if (!running) { runningSince=null; gates.clear(); previous=null; return AlertEvaluation(emptyList(),emptyList()) }
        if (previous?.let { now < it || now-it > 5000 } == true) {
            runningSince=null; gates.values.forEach { it.since=null; it.recovery=null }
        }
        previous=now
        if (runningSince == null) runningSince=now
        val active=mutableListOf<VehicleAlert>(); val raised=mutableListOf<VehicleAlert>()
        fun check(id:String, value:Double?, level:Int, hold:Long, recovered:Boolean, alert:VehicleAlert) {
            val g=gates.getOrPut(id){Gate()}
            if(value==null || !value.isFinite()) { g.since=null; g.candidate=0; g.recovery=null; return }
            if(level>0) {
                g.recovery=null
                if(g.candidate!=level) { g.candidate=level; g.since=now }
                if(g.since==null) g.since=now
                if(now-g.since!!>=hold) {
                    active+=alert
                    if(level>g.level) { raised+=alert; g.level=level }
                }
            } else {
                g.since=null; g.candidate=0
                if(recovered) { if(g.recovery==null)g.recovery=now; if(now-g.recovery!!>=30_000)g.level=0 }
                else g.recovery=null
            }
        }
        if(now-runningSince!!>=30_000) {
            val low=voltage?.let { if(it<12)2 else if(it<13)1 else 0 } ?: 0
            check("voltage",voltage,low,if(low==2)10_000 else 30_000,voltage!=null&&voltage>=13.3,
                VehicleAlert("voltage",if(low==2)"Kritikusan alacsony feszültség" else "Alacsony töltőfeszültség","Járó motor mellett ${voltage?.let { "%.1f".format(it) }} V. Töltési hiba lehetséges; állj meg biztonságos helyen és ellenőriztesd.",low==2))
        }
        val hot=coolant?.let { if(it>=110)2 else if(it>=103)1 else 0 } ?: 0
        check("coolant",coolant,hot,if(hot==2)5_000 else 15_000,coolant!=null&&coolant<100,
            VehicleAlert("coolant",if(hot==2)"Túl magas vízhőfok" else "Emelkedett vízhőfok","Hűtőfolyadék: ${coolant?.toInt()} °C. Ellenőrizd a műszereket; túlmelegedéskor állj meg biztonságosan.",hot==2))
        val lowRange=rangeKm?.let { if(it<100)2 else if(it<150)1 else 0 } ?: 0
        check("range",rangeKm,lowRange,30_000,rangeKm!=null&&rangeKm>=170,
            VehicleAlert("range","Alacsony becsült hatótáv","Becsült hatótáv: ${rangeKm?.toInt()} km. Tervezz tankolást; a becslés hiányos adatokból is származhat.",false))
        return AlertEvaluation(active.sortedByDescending { it.critical },raised)
    }
}
