package hu.elmdash.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.elmdash.connection.*

@Composable
fun TrafficSettingsScreen() {
    val context = LocalContext.current
    val settings = remember { TrafficSettings(context) }
    val initial = remember { runCatching { settings.load() } }
    var enabled by remember { mutableStateOf(initial.getOrNull()?.enabled ?: false) }
    // Subscription links may contain secrets. Never place in savedInstanceState or display in clear text.
    var url by remember { mutableStateOf(initial.getOrNull()?.url.orEmpty()) }
    var message by remember { mutableStateOf(if (initial.isFailure) "A mentett link nem olvasható. Add meg és mentsd újra." else "") }
    val status by TrafficRuntime.status.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        message = "Automatikus AA-induláshoz a helyengedély legyen Mindig engedélyezett."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Útinform az Android Autóban", style = MaterialTheme.typography.headlineSmall)
        Text("A közeli eseményeket a helyzet és a haladási irány alapján szűrjük. Nincs Waze-útvonal vagy térképi útillesztés: párhuzamos útról is jöhet találat. A távolság légvonalbeli becslés.")
        Row { Switch(enabled, { enabled = it }); Text("Útinform bekapcsolása", Modifier.padding(12.dp)) }
        Text("A helyfigyelés nem ment útvonalat és nem küld koordinátát a NAP-nak. Az OBD-től és az útnaplótól független. Internetet és GPS-t használ.")
        OutlinedTextField(url, { url = it.trim() }, label = { Text("NAP HTTP Pull lekérési link (HTTPS)") },
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("NAP: regisztráció → Útinform datex → előfizetés → HTTP / Pull / DATEX II 2.3 XML / teljes adathalmaz (delta nélkül). A generált adatlekérési link kell, nem a böngésző címe. A link titkosítva tárolódik.")
        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://napportal.kozut.hu/"))) }) { Text("NAP regisztráció megnyitása") }
        OutlinedButton(onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Pontos hely engedélyezése") }
        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("Helyengedély: Mindig engedélyezett") }
        Button(onClick = {
            runCatching { settings.save(TrafficConfig(enabled, url)) }.onSuccess {
                TrafficRuntime.stop(context)
                if (enabled) TrafficRuntime.start(context)
                message = if (!enabled) "Mentve. Útinform kikapcsolva." else "Mentve. AA-csatlakozáskor automatikusan indul. A kézi próba AA nélkül legfeljebb 30 perc."
            }.onFailure { message = "Nem menthető. Ellenőrizd a HTTPS-linket; beágyazott felhasználónév nem támogatott." }
        }) { Text("Mentés és próba indítása") }
        OutlinedButton(onClick = { TrafficRuntime.stop(context) }) { Text("Figyelés leállítása") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
        Text(status)
        Text("Az AA alsó médiakártyasorán 20 másodpercig látható egy találat. Legfeljebb percenként új esemény; ugyanaz legkorábban 30 perc után. A fogyasztási főcím marad. A kártya láthatóságát és vágását az Android Auto szabályozza.")
        Text("Első változat: koordinátás DATEX II 2.x teljes pillanatképek. A csak TMC/OpenLR hivatkozással megadott eseményeket nem jelenítjük meg. A NAP éles formátuma hozzáférés után ellenőrizendő. A forrás 5 perces frissítést jelöl; 15 percnél régebbi adatból nincs jelzés.")
        Spacer(Modifier.height(24.dp))
    }
}
