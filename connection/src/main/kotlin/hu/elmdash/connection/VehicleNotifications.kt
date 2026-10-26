package hu.elmdash.connection

import android.app.*
import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import hu.elmdash.trip.VehicleAlert

/** AA media metadata fallback: never impersonates a message, phone call or navigation instruction. */
object CarAlertDisplay {
    @Volatile var live: List<VehicleAlert> = emptyList()
    private val notices = linkedMapOf<String, Pair<VehicleAlert,Long>>()
    @Synchronized fun notice(alert: VehicleAlert) { notices[alert.id]=alert to SystemClock.elapsedRealtime()+60_000 }
    @Synchronized fun clear(id:String) { notices.remove(id) }
    @Synchronized fun line(): String? {
        val now=SystemClock.elapsedRealtime()
        notices.entries.removeAll { it.value.second<=now }
        val alert=live.firstOrNull() ?: notices.values.lastOrNull()?.first ?: return null
        return "⚠ ${alert.title} • ${alert.detail}"
    }
}
class VehicleNotifications(private val context: Context) {
    fun show(alert:VehicleAlert):Boolean {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,"Járműriasztások",NotificationManager.IMPORTANCE_HIGH).apply {
            description="Töltési probléma, túlmelegedés és alacsony becsült hatótáv"; enableVibration(true)
        })
        if(!manager.areNotificationsEnabled() || manager.getNotificationChannel(CHANNEL).importance==NotificationManager.IMPORTANCE_NONE)return false
        val builder=NotificationCompat.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(alert.title).setContentText(alert.detail).setStyle(NotificationCompat.BigTextStyle().bigText(alert.detail))
            .setCategory(NotificationCompat.CATEGORY_ERROR).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            builder.setContentIntent(PendingIntent.getActivity(context,338,it,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        return try { manager.notify(alert.id,338,builder.build()); true } catch (_:SecurityException){false}
    }
    companion object { const val CHANNEL="vehicle-alerts" }
}
