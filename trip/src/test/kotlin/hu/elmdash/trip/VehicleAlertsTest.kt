package hu.elmdash.trip
import org.junit.Assert.*
import org.junit.Test
class VehicleAlertsTest {
    @Test fun startupHoldEscalationAndRecoveryAreNotRepeated() {
        val engine=VehicleAlerts()
        fun tick(t:Long,v:Double?)=engine.tick(t,true,v,85.0,300.0)
        for(t in 0L..59_000 step 1000) assertTrue(tick(t,12.5).raised.isEmpty())
        assertEquals("voltage",tick(60_000,12.5).raised.single().id)
        for(t in 61_000L..70_000 step 1000) assertTrue(tick(t,11.8).raised.isEmpty())
        assertTrue(tick(71_000,11.8).raised.single().critical)
        for(t in 72_000L..80_000 step 1000) assertTrue(tick(t,11.8).raised.isEmpty())
        for(t in 81_000L..111_000 step 1000) assertTrue(tick(t,14.0).raised.isEmpty())
        for(t in 112_000L..141_000 step 1000) assertTrue(tick(t,12.5).raised.isEmpty())
        assertEquals(1,tick(142_000,12.5).raised.size)
    }
    @Test fun missingDataCannotCompleteHoldAndEngineOffClearsEverything() {
        val engine=VehicleAlerts()
        for(t in 0L..40_000 step 1000)engine.tick(t,true,12.5,null,null)
        assertTrue(engine.tick(41_000,true,null,null,null).active.isEmpty())
        for(t in 42_000L..71_000 step 1000)assertTrue(engine.tick(t,true,12.5,null,null).raised.isEmpty())
        assertEquals(1,engine.tick(72_000,true,12.5,null,null).raised.size)
        assertTrue(engine.tick(73_000,false,11.0,120.0,50.0).active.isEmpty())
    }
    @Test fun hotAndLowRangeHaveIndependentDebounceAndNoRepeatedAlarm() {
        val engine=VehicleAlerts()
        for(t in 0L..4000 step 1000)assertTrue(engine.tick(t,true,14.0,111.0,80.0).raised.isEmpty())
        assertEquals("coolant",engine.tick(5000,true,14.0,111.0,80.0).raised.single().id)
        for(t in 6000L..29000 step 1000)assertTrue(engine.tick(t,true,14.0,111.0,80.0).raised.isEmpty())
        assertEquals("range",engine.tick(30000,true,14.0,111.0,80.0).raised.single().id)
        assertEquals(2,engine.tick(31000,true,14.0,111.0,80.0).active.size)
    }
}
