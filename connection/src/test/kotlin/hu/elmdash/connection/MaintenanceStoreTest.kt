package hu.elmdash.connection

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[30])
class MaintenanceStoreTest {
    @Test fun oldRecordsKeepTheirNotesAndSortByDateNotIdOrInsertion() {
        val context=RuntimeEnvironment.getApplication()
        context.getSharedPreferences("elm-maintenance",0).edit().clear().putString("entries", """[
            {"id":"999","name":"Olaj","date":"2025-10-01","km":100000,"note":"régi megjegyzés"},
            {"id":"001","name":"Szíj","date":"2026-01-01","km":110000,"costHuf":75000}
        ]""").commit()
        val entries=MaintenanceStore(context).entries()
        assertEquals(listOf("001","999"),entries.map { it.id })
        assertEquals(75000.0,entries.first().costHuf!!,0.0)
        assertNull(entries.last().costHuf)
        assertEquals("régi megjegyzés",entries.last().note)
    }
}
