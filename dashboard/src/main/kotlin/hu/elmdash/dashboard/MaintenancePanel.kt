package hu.elmdash.dashboard

import android.app.DatePickerDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import hu.elmdash.connection.MaintenanceStore
import hu.elmdash.trip.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun LocalDate.label() = format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))
private fun amount(n: Double) = String.format(java.util.Locale.forLanguageTag("hu-HU"), "%,.0f", n)
@Composable
private fun ServiceDate(label: String, value: LocalDate, onDate: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        DatePickerDialog(context, { _, y, m, d -> onDate(LocalDate.of(y, m + 1, d)) }, value.year, value.monthValue - 1, value.dayOfMonth).show()
    }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${value.label()}") }
}
@Composable
private fun ServiceBox(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
}
@Composable
internal fun MaintenanceScreen(journeys: List<JourneyRecord>) {
    val context = LocalContext.current
    val store = remember { MaintenanceStore(context) }
    val prefs = remember { context.getSharedPreferences("elm-maintenance", android.content.Context.MODE_PRIVATE) }
    var weather by remember { mutableStateOf(prefs.getBoolean("weather", false)) }
    var entries by remember { mutableStateOf(store.entries()) }
    var inspection by remember { mutableStateOf(store.inspectionDue) }
    var savedOdo by remember { mutableStateOf(store.odometer) }
    var odo by rememberSaveable { mutableStateOf(store.odometer?.toString().orEmpty()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("Olaj és olajszűrő") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var km by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var interval by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var delete by remember { mutableStateOf<ServiceEntry?>(null) }
    val today = LocalDate.now()
    var from by rememberSaveable { mutableStateOf(today.withDayOfYear(1).toString()) }
    var to by rememberSaveable { mutableStateOf(today.toString()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Szerviz", style = MaterialTheme.typography.headlineMedium)
        ServiceBox {
            Text("Szervizköltségek", style = MaterialTheme.typography.titleLarge)
            for (year in listOf(today.year, today.year - 1)) {
                val records = entries.filter { it.performed.year == year }
                val total = if (records.isNotEmpty() && records.all { it.costHuf == null }) "—" else "${amount(records.sumOf { it.costHuf ?: 0.0 })} Ft"
                Text("$year · $total", style = MaterialTheme.typography.titleLarge)
                Text("${records.size} szerviz · ${records.count { it.costHuf == null }} költség nélkül", style = MaterialTheme.typography.bodySmall)
            }
            Text("Csak a külön összegként rögzített költségeket adjuk össze.", style = MaterialTheme.typography.bodySmall)
        }
        ServiceBox {
            Text("Műszaki vizsga", style = MaterialTheme.typography.titleLarge)
            Text(inspection?.let { "Esedékes: ${it.label()}" } ?: "Még nincs megadva határidő")
            if (inspectionSoon(inspection, today)) Text("Műszaki vizsga esedékes – egyeztess időpontot!", color = MaterialTheme.colorScheme.tertiary)
            ServiceDate("Határidő megadása", inspection ?: today.plusYears(2)) { inspection = it; store.inspectionDue = it }
            ServiceDate("Sikeres vizsga dátuma", today) {
                if (!it.isAfter(today)) { inspection = it.plusYears(2); store.inspectionDue = inspection; error = null }
                else error = "Elvégzett vizsga nem lehet jövőbeli."
            }
            Text("Sikeres vizsgától 2 évvel számolunk; a hivatalos lejáratot a Határidő megadásával pontosíthatod. Előjelzés 6 héttől, naponta; értesítési engedély szükséges.", style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = {
            editId = null; name = "Olaj és olajszűrő"; date = today.toString(); km = ""; cost = ""; interval = ""; months = ""; note = ""; error = null; editing = true
        }, modifier = Modifier.fillMaxWidth()) { Text("Új szerviz rögzítése") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (editing) Dialog(onDismissRequest = { editing = false }) {
          Column(Modifier.verticalScroll(rememberScrollState())) { ServiceBox {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(if (editId == null) "Új elvégzett szerviz" else "Szerviz szerkesztése", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Művelet / alkatrész") })
            ServiceDate("Elvégzés", LocalDate.parse(date)) { date = it.toString() }
            OutlinedTextField(km, { km = it }, Modifier.fillMaxWidth(), label = { Text("Kilométeróra a szervizkor (km)") })
            OutlinedTextField(cost, { cost = it }, Modifier.fillMaxWidth(), label = { Text("Teljes szervizköltség (Ft, opcionális)") })
            OutlinedTextField(interval, { interval = it }, Modifier.fillMaxWidth(), label = { Text("Újra ennyi km után (opcionális)") })
            OutlinedTextField(months, { months = it }, Modifier.fillMaxWidth(), label = { Text("Újra ennyi hónap után (opcionális)") })
            OutlinedTextField(note, { note = it.take(2000) }, Modifier.fillMaxWidth(), label = { Text("Megjegyzés") })
            Row {
                Button(onClick = {
                    runCatching {
                        val d = LocalDate.parse(date); require(!d.isAfter(today))
                        val entry = ServiceEntry(editId ?: UUID.randomUUID().toString(), name.trim(), d, km.replace(',', '.').toDouble(),
                            interval.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble(), months.takeIf { it.isNotBlank() }?.toInt(), note,
                            cost.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDouble())
                        store.save(entry); entries = store.entries(); editing = false; error = null
                    }.onFailure { error = "Ellenőrizd a nevet, dátumot, kilométert, költséget és intervallumot." }
                }) { Text("Mentés") }
                TextButton(onClick = { editing = false; error = null }) { Text("Mégse") }
            }
        }
        } }
        Text("Elvégzett szervizek · legújabb elöl", style = MaterialTheme.typography.titleLarge)
        if (entries.isEmpty()) Text("Még nincs rögzített szerviz.")
        entries.forEach { entry ->
            ServiceBox {
                Text(entry.name, style = MaterialTheme.typography.titleLarge)
                Text("${entry.performed.label()} · ${amount(entry.odometerKm)} km")
                Text(entry.costHuf?.let { "${amount(it)} Ft" } ?: "Költség nincs megadva", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                val next = listOfNotNull(entry.intervalKm?.let { "${amount(entry.odometerKm + it)} km" }, entry.intervalMonths?.let { entry.performed.plusMonths(it.toLong()).label() })
                if (next.isNotEmpty()) Text("Következő: ${next.joinToString(" vagy ")}")
                if (entry.soon(today, savedOdo) && store.latest().any { it.id == entry.id }) Text(if (entry.due(today, savedOdo)) "Esedékes" else "Hamarosan esedékes", color = MaterialTheme.colorScheme.tertiary)
                if (entry.note.isNotBlank()) Text(entry.note)
                Row {
                    TextButton(onClick = { editId = entry.id; name = entry.name; date = entry.performed.toString(); km = entry.odometerKm.toString(); cost = entry.costHuf?.toString().orEmpty(); interval = entry.intervalKm?.toString().orEmpty(); months = entry.intervalMonths?.toString().orEmpty(); note = entry.note; editing = true }) { Text("Szerkesztés") }
                    TextButton(onClick = { delete = entry }) { Text("Törlés") }
                }
            }
        }
        ServiceBox {
            Text("Utak két dátum között", style = MaterialTheme.typography.titleLarge)
            ServiceDate("Ettől", LocalDate.parse(from)) { from = it.toString() }
            ServiceDate("Eddig", LocalDate.parse(to)) { to = it.toString() }
            if (LocalDate.parse(to).isBefore(LocalDate.parse(from))) Text("A záró dátum legyen a kezdő dátum után.")
            else {
                val period = servicePeriod(journeys, LocalDate.parse(from), LocalDate.parse(to))
                Text("${period.journeys} rögzített út · ${String.format(java.util.Locale.forLanguageTag("hu-HU"), "%.2f", period.distanceKm)} km", style = MaterialTheme.typography.titleLarge)
            }
            Text("Mindkét dátum beleszámít, az indulás napja szerint. Csak a helyben megőrzött, lezárt utak (legfeljebb 200); nem a teljes kilométeróra-változás.", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { settings = !settings }) { Text("Kilométeróra és emlékeztetők") }
        if (settings) ServiceBox {
            OutlinedTextField(odo, { odo = it }, Modifier.fillMaxWidth(), label = { Text("Aktuális kilométeróra (km)") })
            Button(onClick = { val n = odo.replace(',', '.').toDoubleOrNull(); if (n != null && n.isFinite() && n >= 0) { store.odometer = n; savedOdo = n; error = null } else error = "Érvényes kilométeróra szükséges." }) { Text("Kilométeróra mentése") }
            Text("Kézzel frissítendő. Szervizelőjelzés: 30 nap vagy 500 km.", style = MaterialTheme.typography.bodySmall)
            Row { Switch(weather, { weather = it; prefs.edit().putBoolean("weather", it).apply(); hu.elmdash.connection.MaintenanceWorker.schedule(context) }); Text("Időjárásos guminyomás-emlékeztető") }
            Text("Open-Meteo: a legutóbbi parkolóhely kb. 1 km-re kerekített helyét használja. 7 °C alatt vagy 10 °C lehűléskor, legfeljebb hetente szól. Hely és internet nélkül nincs jelzés.", style = MaterialTheme.typography.bodySmall)
        }
    }
    delete?.let { entry -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("Szerviz törlése?") }, text = { Text(entry.name) }, confirmButton = { TextButton(onClick = { store.remove(entry.id); entries = store.entries(); delete = null }) { Text("Törlés") } }, dismissButton = { TextButton(onClick = { delete = null }) { Text("Mégse") } }) }
}
