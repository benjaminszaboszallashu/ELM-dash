package hu.elmdash.trip
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class MaintenanceTest {
    private val date = LocalDate.of(2025,10,21)
    private val entry = ServiceEntry("1","Olaj",date,100000.0,10000.0,12)
    @Test fun inspectionStartsExactlySixWeeksBeforeAndIncludesOverdue() {
        assertFalse(inspectionSoon(null,date))
        assertFalse(inspectionSoon(date.plusDays(43),date))
        assertTrue(inspectionSoon(date.plusDays(42),date))
        assertTrue(inspectionSoon(date.minusDays(1),date))
    }
    @Test fun periodIncludesBothLocalDatesButNotActiveJourneys() {
        val zone=java.time.ZoneId.of("Europe/Budapest")
        fun record(day: LocalDate, km: Double, ended: Boolean=true): JourneyRecord {
            val t=day.atStartOfDay(zone).toInstant().toEpochMilli()
            return JourneyRecord(day.toString(),t,t+1000,if(ended)t+1000 else null,summary=TripSummary(distanceKm=km))
        }
        val result=servicePeriod(listOf(record(date,2.0),record(date.plusDays(1),3.0),record(date.minusDays(1),9.0),record(date,8.0,false)),date,date.plusDays(1),zone)
        assertEquals(ServicePeriod(2,5.0),result)
        assertThrows(IllegalArgumentException::class.java) { servicePeriod(emptyList(),date,date.minusDays(1)) }
        assertThrows(IllegalArgumentException::class.java) { entry.copy(costHuf=-1.0) }
    }
    @Test fun eitherLimitCanTriggerIndependently() {
        assertTrue(entry.due(date.plusMonths(12),null))
        assertTrue(entry.due(date.plusMonths(2),110000.0))
        assertFalse(entry.due(date.plusMonths(2),109999.0))
        assertFalse(entry.due(date.plusMonths(2),null))
        assertTrue(entry.soon(date.plusMonths(2),109500.0))
    }
    @Test fun fuelComparisonCannotCalibrateMissingObservations() {
        val full = FullRefill("x",1,30.0,25.0,false,18000.0)
        assertEquals(5.0,full.differenceLiters!!,0.0)
        assertEquals(20.0,full.differencePercent!!,0.0)
        assertEquals(1.2,full.suggestedMultiplier!!,0.0)
        assertNull(full.copy(previousHasGaps=true).suggestedMultiplier)
        assertNull(full.copy(previousConsumedLiters=null).differenceLiters)
        assertNull(full.copy(previousConsumedLiters=0.0).differencePercent)
    }
}
