package hu.elmdash.connection
import android.app.NotificationManager
import hu.elmdash.trip.VehicleAlert
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[30])
class VehicleNotificationsTest {
    @Test fun alarmUsesHighImportanceChannelAndOrdinaryErrorCategory() {
        val context=RuntimeEnvironment.getApplication()
        val alerts=VehicleNotifications(context)
        assertTrue(alerts.show(VehicleAlert("voltage","Alacsony feszültség","12 V",true)))
        val manager=context.getSystemService(NotificationManager::class.java)
        assertEquals(NotificationManager.IMPORTANCE_HIGH,manager.getNotificationChannel(VehicleNotifications.CHANNEL).importance)
        val n=manager.activeNotifications.single { it.tag=="voltage" }.notification
        assertEquals("err",n.category)
        assertEquals("Alacsony feszültség",n.extras.getString("android.title"))
    }
    @Test fun liveFaultTakesPriorityOverMaintenanceAndClearRemovesConnectionNotice() {
        CarAlertDisplay.live=emptyList()
        CarAlertDisplay.notice(VehicleAlert("connection","Nincs kapcsolat",""))
        assertTrue(CarAlertDisplay.line()!!.contains("Nincs kapcsolat"))
        CarAlertDisplay.live=listOf(VehicleAlert("voltage","Feszültség","",true))
        assertTrue(CarAlertDisplay.line()!!.contains("Feszültség"))
        CarAlertDisplay.clear("connection");CarAlertDisplay.live=emptyList()
        assertNull(CarAlertDisplay.line())
    }
}
