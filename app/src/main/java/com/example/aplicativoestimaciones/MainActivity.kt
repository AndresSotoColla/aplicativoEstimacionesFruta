package com.example.aplicativoestimaciones

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.aplicativoestimaciones.ui.theme.AplicativoEstimacionesTheme

val CALIBRES = listOf("C5", "C6", "C7", "C8P", "C8", "C9", "C10", "Guapita", "Baby Guapa")
val DEFECTOS = listOf("Enferma", "Quema Sol Severo", "Deforme", "Daño Insecto", "Daño Mecánico")
val FUERA_ESPEC_CATS = listOf("Cuello", "Cónica", "Cicatriz", "Base café", "Corona Pequeña", "Corona Grande", "Corona Múltiple", "Cochinilla", "Off Color", "Quema Sol Leve", "Quema Sol Severa")
val FUERA_ESPEC_SINGLE = "Deforme"
val FUERA_ESPEC_ADELANTADA = "Fruta Adelantada"
val ESPEC_TYPES = listOf("Tolerable", "No Tolerable")

// --- ELEGANT COLOR PALETTE ---
val PrimaryEarth = Color(0xFF7D725C)
val SecondaryGold = Color(0xFFBAAA89)
val BackgroundCream = Color(0xFFFBF9F5)
val SurfaceCream = Color(0xFFFCFBF9)

@Composable
fun FruitTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = PrimaryEarth,
        onPrimary = Color.White,
        secondary = SecondaryGold,
        onSecondary = Color.Black,
        background = BackgroundCream,
        surface = SurfaceCream,
        onSurface = Color.Black,
        primaryContainer = PrimaryEarth,
        onPrimaryContainer = Color.White,
        secondaryContainer = SecondaryGold,
        onSecondaryContainer = Color.Black
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

data class BloqueData(
    val bloque: String,
    val grupoForza: String
)

@Composable
fun blackTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedBorderColor = PrimaryEarth,
    unfocusedBorderColor = PrimaryEarth.copy(alpha = 0.5f),
    focusedLabelColor = PrimaryEarth,
    unfocusedLabelColor = PrimaryEarth,
    cursorColor = PrimaryEarth
)


data class EstimationRecord(
    val id: String,
    val date: String,
    val week: String,
    val grupoForza: String,
    val bloque: String,
    val calidadTotal: Int,
    val noRecuperadaTotal: Int,
    val noRecuperadaCalibreTotal: Int,
    val fueraEspecTotal: Int,
    val fueraEspecSinCalidadTotal: Int,
    // Detailed maps
    val calidadCounts: Map<String, Int> = emptyMap(),
    val noRecuperadaCounts: Map<String, Int> = emptyMap(),
    val noRecCalibreCounts: Map<String, Int> = emptyMap(),
    val fueraEspecCounts: Map<String, Int> = emptyMap(),
    val fueraEspecSinCalidadCounts: Map<String, Int> = emptyMap(),
    val isUploaded: Boolean = false
)



fun saveEstimation(context: Context, record: EstimationRecord) {
    val file = File(context.filesDir, "historial.json")
    val list = getHistorial(context).toMutableList()
    list.add(0, record) // Newest first
    
    val jsonArray = JSONArray()
    list.forEach { r ->
        val obj = JSONObject().apply {
            put("id", r.id)
            put("date", r.date)
            put("week", r.week)
            put("grupoForza", r.grupoForza)
            put("bloque", r.bloque)
            put("calidad", r.calidadTotal)
            put("noRecTotal", r.noRecuperadaTotal)
            put("noRecCalTotal", r.noRecuperadaCalibreTotal)
            put("fueraEspec", r.fueraEspecTotal)
            put("fueraEspecSC", r.fueraEspecSinCalidadTotal)
            // Serialize maps
            put("calidadCounts", JSONObject(r.calidadCounts))
            put("noRecCounts", JSONObject(r.noRecuperadaCounts))
            put("noRecCalCounts", JSONObject(r.noRecCalibreCounts))
            put("fueraEspecCounts", JSONObject(r.fueraEspecCounts))
            put("fueraEspecSCCounts", JSONObject(r.fueraEspecSinCalidadCounts))
            put("isUploaded", r.isUploaded)

        }
        jsonArray.put(obj)
    }
    
    try {
        FileOutputStream(file).use { it.write(jsonArray.toString().toByteArray()) }
    } catch (e: Exception) { e.printStackTrace() }
}

fun deleteEstimation(context: Context, id: String) {
    val list = getHistorial(context).filterNot { it.id == id }
    persistFullList(context, list)
}

fun clearAllHistorial(context: Context) {
    persistFullList(context, emptyList())
}

private fun persistFullList(context: Context, list: List<EstimationRecord>) {
    val file = File(context.filesDir, "historial.json")
    val jsonArray = JSONArray()
    list.forEach { r ->
        val obj = JSONObject().apply {
            put("id", r.id)
            put("date", r.date)
            put("week", r.week)
            put("grupoForza", r.grupoForza)
            put("bloque", r.bloque)
            put("calidad", r.calidadTotal)
            put("calidadSC", r.calidadSinCalidadTotal)
            put("noRecTotal", r.noRecuperadaTotal)
            put("noRecCalTotal", r.noRecuperadaCalibreTotal)
            put("fueraEspec", r.fueraEspecTotal)
            put("fueraEspecSC", r.fueraEspecSinCalidadTotal)
            put("calidadCounts", JSONObject(r.calidadCounts))
            put("calidadSCCounts", JSONObject(r.calidadSinCalidadCounts))
            put("noRecCounts", JSONObject(r.noRecuperadaCounts))
            put("noRecCalCounts", JSONObject(r.noRecCalibreCounts))
            put("fueraEspecCounts", JSONObject(r.fueraEspecCounts))
            put("fueraEspecSCCounts", JSONObject(r.fueraEspecSinCalidadCounts))
            put("isUploaded", r.isUploaded)

        }
        jsonArray.put(obj)
    }
    try {
        FileOutputStream(file).use { it.write(jsonArray.toString().toByteArray()) }
    } catch (e: Exception) { e.printStackTrace() }
}

fun markAsUploaded(context: Context, id: String) {
    val list = getHistorial(context).map { 
        if (it.id == id) it.copy(isUploaded = true) else it
    }
    persistFullList(context, list)
}

fun getHistorial(context: Context): List<EstimationRecord> {
    val file = File(context.filesDir, "historial.json")
    if (!file.exists()) return emptyList()
    
    val list = mutableListOf<EstimationRecord>()
    try {
        val content = file.readText()
        val jsonArray = JSONArray(content)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(EstimationRecord(
                id = obj.getString("id"),
                date = obj.getString("date"),
                week = obj.getString("week"),
                grupoForza = obj.getString("grupoForza"),
                bloque = obj.getString("bloque"),
                calidadTotal = obj.optInt("calidad", 0),
                noRecuperadaTotal = obj.optInt("noRecTotal", 0),
                noRecuperadaCalibreTotal = obj.optInt("noRecCalTotal", 0),
                fueraEspecTotal = obj.optInt("fueraEspec", 0),
                fueraEspecSinCalidadTotal = obj.optInt("fueraEspecSC", 0),
                calidadCounts = jsonToMap(obj.optJSONObject("calidadCounts")),
                noRecuperadaCounts = jsonToMap(obj.optJSONObject("noRecCounts")),
                noRecCalibreCounts = jsonToMap(obj.optJSONObject("noRecCalCounts")),
                fueraEspecCounts = jsonToMap(obj.optJSONObject("fueraEspecCounts")),
                fueraEspecSinCalidadCounts = jsonToMap(obj.optJSONObject("fueraEspecSCCounts")),
                isUploaded = obj.optBoolean("isUploaded", false)
            ))

        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}

fun jsonToMap(json: JSONObject?): Map<String, Int> {
    if (json == null) return emptyMap()
    val map = mutableMapOf<String, Int>()
    val keys = json.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        map[key] = json.getInt(key)
    }
    return map
}

// --- API MAPS & UPLOAD LOGIC ---
val API_CALIBRE_MAP = mapOf(
    "BABY GUAPA" to 1, "GUAPITA" to 2, "C10" to 3, "C9" to 4, "C8" to 5, "C7" to 6, "C6" to 7, "C5" to 8,
    "SOBRE PESO" to 9, "BAJO PESO" to 10, "SIN CALIBRE" to 11, "C8P" to 12
)

val API_RAZON_MAP = mapOf(
    "Ausente" to 1, "Daño" to 2, "Sin inducir" to 3, "Bajo peso" to 4, "Muestreo" to 5, "Fruta Joven" to 6,
    "Enferma" to 7, "Quema Sol Severo" to 8, "Deforme" to 9, "Daño insecto" to 10, "Daño mecanico" to 11
)

val API_AFECTACION_MAP = mapOf(
    "Fruta Adelantada" to 13, "Deforme" to 1, "Cuello" to 2, "Cónica" to 3, "Cicatriz" to 4, "Base café" to 5,
    "Cónica Inclinada" to 6, "Corona Pequeña" to 7, "Corona Grande" to 8, "Corona Múltiple" to 9, "Cochinilla" to 10,
    "Off Color" to 11, "Quema Sol Leve" to 12, "Quema Sol Severa" to 14
)

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun normString(s: String): String {
    val normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
    return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").uppercase().trim()
}

fun uploadRecord(context: Context, record: EstimationRecord, onResult: (Boolean, String) -> Unit) {
    if (!isNetworkAvailable(context)) {
        onResult(false, "No hay internet. Conéctate para subir datos.")
        return
    }

    val thread = Thread {
        try {
            val url = java.net.URL("https://interno.control.agricolaguapa.com/consultor/api/cargue_estimacion")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true

            // Data splitting for API (dd/MM/yyyy HH:mm)
            val parts = record.date.split(" ")
            val dateParts = parts[0].split("/")
            val apiDate = "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}" // YYYY-MM-DD
            val apiTime = if (parts.size > 1) "${parts[1]}:00" else "00:00:00"
            
            val grupoForzaNum = record.grupoForza.filter { it.isDigit() }.let { if (it.isEmpty()) "0" else it }

            val jsonOutput = JSONObject().apply {
                put("bloque", record.bloque)
                put("grupo_forza", grupoForzaNum)
                put("fecha", apiDate)
                put("hora", apiTime)
                put("semana", record.week)

                val itemsArray = JSONArray()

                // 1. CALIDAD
                record.calidadCounts.forEach { (calibre, count) ->
                    if (count > 0) {
                        itemsArray.put(JSONObject().apply {
                            put("tipo", "CALIDAD")
                            put("calibre", normString(calibre))
                            put("conteo", count)
                        })
                    }
                }

                // 2. FE (Fuera Espec - Con Calidad)
                record.fueraEspecCounts.forEach { (key, count) ->
                    if (count > 0) {
                        val kParts = key.split("_")
                        if (kParts.size >= 2) {
                            itemsArray.put(JSONObject().apply {
                                put("tipo", "FE")
                                put("calibre", normString(kParts[1]))
                                put("afectacion", normString(kParts[0]))
                                put("conteo", count)
                            })
                        }
                    }
                }

                // 3. NOREC (No Recuperada Simple)
                record.noRecuperadaCounts.forEach { (label, count) ->
                    if (count > 0) {
                        itemsArray.put(JSONObject().apply {
                            put("tipo", "NOREC")
                            put("afectacion", normString(label))
                            put("conteo", count)
                        })
                    }
                }

                // 4. NRC (No Recuperada x Calibre)
                record.noRecCalibreCounts.forEach { (key, count) ->
                    if (count > 0) {
                        val kParts = key.split("_")
                        if (kParts.size >= 2) {
                            itemsArray.put(JSONObject().apply {
                                put("tipo", "NRC")
                                put("calibre", normString(kParts[1]))
                                put("afectacion", normString(kParts[0]))
                                put("conteo", count)
                            })
                        }
                    }
                }

                // 5. FESC (Sin Calidad)
                record.fueraEspecSinCalidadCounts.forEach { (key, count) ->
                    if (count > 0) {
                        val kParts = key.split("_")
                        if (kParts.size >= 2) {
                            itemsArray.put(JSONObject().apply {
                                put("tipo", "FESC")
                                put("calibre", normString(kParts[1]))
                                put("afectacion", normString(kParts[0]))
                                put("conteo", count)
                            })
                        }
                    }
                }

                put("items", itemsArray)
            }

            conn.outputStream.use { it.write(jsonOutput.toString().toByteArray(StandardCharsets.UTF_8)) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                onResult(true, "Subida exitosa")
            } else {
                onResult(false, "Error servidor ($responseCode)")
            }
        } catch (e: Exception) {
            onResult(false, "Error: ${e.message}")
        }
    }
    thread.start()
}


// Custom saver for SnapshotStateMap to survive rotation
val MapSaver = Saver<SnapshotStateMap<String, Int>, HashMap<String, Int>>(
    save = { HashMap(it) }, 
    restore = { it?.let { data -> mutableStateMapOf<String, Int>().apply { putAll(data) } } }
)

fun readCsvData(context: Context): List<BloqueData> {
    val list = mutableListOf<BloqueData>()
    val fileName = "grupo_forza.csv"
    val internalFile = File(context.filesDir, fileName)
    
    // Fallback data for extreme cases
    val fallback = listOf(
        BloqueData("BLOQUE_PRUEBA_1", "GRUPO_PRUEBA_A"),
        BloqueData("BLOQUE_PRUEBA_2", "GRUPO_PRUEBA_A"),
        BloqueData("BLOQUE_PRUEBA_10", "GRUPO_PRUEBA_B")
    )
    
    try {
        val inputStream: InputStream = if (internalFile.exists()) {
            internalFile.inputStream()
        } else {
            context.assets.open(fileName)
        }

                inputStream.bufferedReader(charset = Charsets.UTF_8).use { reader ->
            reader.forEachLine { rawLine ->
                val line = rawLine.replace("\uFEFF", "").trim()
                if (line.isEmpty()) return@forEachLine
                
                // Skip header if it starts with known keywords
                if (line.startsWith("Bloque", ignoreCase = true) || 
                    line.startsWith("GF", ignoreCase = true) || 
                    line.startsWith("Grupo", ignoreCase = true)) return@forEachLine
                
                val delimiters = listOf(";", ",", "\t")
                var tokens = emptyList<String>()
                for (delim in delimiters) {
                    val t = line.split(delim)
                    if (t.size >= 2) {
                        tokens = t
                        break
                    }
                }
                
                if (tokens.size >= 2) {
                    val bloque = tokens[0].trim().removeSurrounding("\"").trim()
                    val gf = tokens[1].trim().removeSurrounding("\"").trim()
                    if (bloque.isNotEmpty() && gf.isNotEmpty()) {
                        list.add(BloqueData(bloque, gf))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return if (list.isEmpty()) fallback else list
}

@RequiresApi(Build.VERSION_CODES.Q)
fun exportRecordsToCSV(context: Context, records: List<EstimationRecord>) {
    if (records.isEmpty()) {
        Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
        return
    }

    val sb = StringBuilder()
    
    // --- BUILD HEADER ---
    sb.append("ID,Fecha,Semana,Grupo Forza,Bloque,Calidad Total,No Recuperada Total,No Rec Calibre Total,Fuera Espec Total,Fuera Espec SC Total")


    
    // Calidad breakdown
    CALIBRES.forEach { sb.append(",Calidad_$it") }


    
    // No Recuperada (Simple) breakdown
    val noRecCats = listOf("Ausente", "Daño", "Sin Inducir", "Bajo Peso", "Muestreo", "Fruta Joven")
    noRecCats.forEach { sb.append(",NoRec_$it") }
    
    // No Recuperada Calibre matrix
    DEFECTOS.forEach { defecto ->
        CALIBRES.forEach { calibre ->
            sb.append(",NRC_${defecto}_$calibre")
        }
    }
    
    // Fuera Especificación matrix (Simplified)
    // Single category: Deforme
    CALIBRES.forEach { calibre ->
        sb.append(",FE_${FUERA_ESPEC_SINGLE}_$calibre")
        sb.append(",FESC_${FUERA_ESPEC_SINGLE}_$calibre")
    }
    // Single category: Fruta Adelantada
    CALIBRES.forEach { calibre ->
        sb.append(",FE_${FUERA_ESPEC_ADELANTADA.replace(" ", "_")}_$calibre")
        sb.append(",FESC_${FUERA_ESPEC_ADELANTADA.replace(" ", "_")}_$calibre")
    }
    // General categories
    FUERA_ESPEC_CATS.forEach { cat ->
        CALIBRES.forEach { calibre ->
            sb.append(",FE_${cat}_$calibre")
            sb.append(",FESC_${cat}_$calibre")
        }
    }

    sb.append("\n")


    // --- BUILD ROWS ---
    records.forEach { r ->
        // Basic info & Totals
        sb.append("${r.id},\"${r.date}\",\"${r.week}\",\"${r.grupoForza}\",\"${r.bloque}\",${r.calidadTotal},${r.noRecuperadaTotal},${r.noRecuperadaCalibreTotal},${r.fueraEspecTotal},${r.fueraEspecSinCalidadTotal}")


        
        // Calidad counts
        CALIBRES.forEach { sb.append(",${r.calidadCounts[it] ?: 0}") }


        
        // No Recuperada (Simple) counts
        noRecCats.forEach { sb.append(",${r.noRecuperadaCounts[it] ?: 0}") }
        
        // No Recuperada Calibre counts
        DEFECTOS.forEach { defecto ->
            CALIBRES.forEach { calibre ->
                sb.append(",${r.noRecCalibreCounts["${defecto}_$calibre"] ?: 0}")
            }
        }
        
        // Fuera Especificación counts (Simplified)
        // Deforme
        CALIBRES.forEach { calibre ->
            sb.append(",${r.fueraEspecCounts["${FUERA_ESPEC_SINGLE}_$calibre"] ?: 0}")
            sb.append(",${r.fueraEspecSinCalidadCounts["${FUERA_ESPEC_SINGLE}_$calibre"] ?: 0}")
        }
        // Fruta Adelantada
        CALIBRES.forEach { calibre ->
            sb.append(",${r.fueraEspecCounts["${FUERA_ESPEC_ADELANTADA}_$calibre"] ?: 0}")
            sb.append(",${r.fueraEspecSinCalidadCounts["${FUERA_ESPEC_ADELANTADA}_$calibre"] ?: 0}")
        }
        // General categories
        FUERA_ESPEC_CATS.forEach { cat ->
            CALIBRES.forEach { calibre ->
                sb.append(",${r.fueraEspecCounts["${cat}_$calibre"] ?: 0}")
                sb.append(",${r.fueraEspecSinCalidadCounts["${cat}_$calibre"] ?: 0}")
            }
        }

        sb.append("\n")
    }

    val filename = "estimaciones_completo_${System.currentTimeMillis()}.csv"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    try {
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
            }
            Toast.makeText(context, "Excel completo guardado en Descargas", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(context, "Error al crear el archivo", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FruitTheme {
                MainApp()
            }
        }
    }
}

enum class Screen {
    Home, IngresarDatos, Historial
}

@Composable
fun MainApp() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigateToIngresar = { currentScreen = Screen.IngresarDatos },
            onNavigateToHistorial = { currentScreen = Screen.Historial }
        )
        Screen.IngresarDatos ->  IngresarDatosScreen(onBack = { currentScreen = Screen.Home })
        Screen.Historial -> HistorialScreen(onBack = { currentScreen = Screen.Home })
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(onNavigateToIngresar: () -> Unit, onNavigateToHistorial: () -> Unit) {
    val context = LocalContext.current
    
    // File picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val outputFile = File(context.filesDir, "grupo_forza.csv")
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Bloques actualizados correctamente", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al actualizar bloques", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "Estimación Fruta",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            HomeButton("Ingresar Datos", Icons.Default.Edit) { onNavigateToIngresar() }
            HomeButton("Historico", Icons.Default.List) { onNavigateToHistorial() }
            HomeButton("Actualizar Bloques", Icons.Default.Refresh) { 
                pickerLauncher.launch("text/*") 
            }
            HomeButton("Descargar Excel", Icons.Default.Check) { 
                exportRecordsToCSV(context, getHistorial(context))
            }
        }
    }
}

@Composable
fun HomeButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // 🔥 antes 72 → ahora estándar
        shape = RoundedCornerShape(14.dp), // 🔽 menos “inflado”
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryEarth,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp) // 🔽 más proporcional
        )

        Spacer(modifier = Modifier.width(6.dp)) // 🔽 menos espacio

        Text(
            text,
            fontSize = 16.sp, // 🔽 más compacto
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresarDatosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // Intercept system back button
    BackHandler { onBack() }
    
    // CSV Data (reloads when entering screen)
    val csvData = remember { 
        readCsvData(context)
    }
    
    // Current Time and Week Logic
    val calendar = Calendar.getInstance()
    val initialWeek = calendar.get(Calendar.WEEK_OF_YEAR).toString()
    val initialTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    var semana by rememberSaveable { mutableStateOf(initialWeek) }
    var currentTime by rememberSaveable { mutableStateOf(initialTime) }
    var grupoForza by rememberSaveable { mutableStateOf("") }
    var bloque by rememberSaveable { mutableStateOf("") }
    
    // Dropdown States
    var expandedGrupo by rememberSaveable { mutableStateOf(false) }
    var expandedBloque by rememberSaveable { mutableStateOf(false) }
    
    // Unique options (These can be remember because they are derived from csvData and saved state)
    val gruposUnicos = remember(csvData, bloque) { 
        if (bloque.isEmpty()) {
            csvData.map { it.grupoForza }.distinct().sorted()
        } else {
            csvData.filter { it.bloque == bloque }.map { it.grupoForza }.distinct().sorted()
        }
    }
    val bloquesUnicos = remember(csvData, grupoForza) { 
        if (grupoForza.isEmpty()) {
            csvData.map { it.bloque }.distinct().sorted()
        } else {
            csvData.filter { it.grupoForza == grupoForza }.map { it.bloque }.distinct().sorted()
        }
    }

    // Counters - use rememberSaveable so they persist on rotation!
    var c5 by rememberSaveable { mutableStateOf(0) }
    var c6 by rememberSaveable { mutableStateOf(0) }
    var c7 by rememberSaveable { mutableStateOf(0) }
    var c8P by rememberSaveable { mutableStateOf(0) }
    var c8 by rememberSaveable { mutableStateOf(0) }
    var c9 by rememberSaveable { mutableStateOf(0) }
    var c10 by rememberSaveable { mutableStateOf(0) }
    var guapita by rememberSaveable { mutableStateOf(0) }
    var babyGuapa by rememberSaveable { mutableStateOf(0) }


    // Non-recovered fruit counters
    var ausente by rememberSaveable { mutableStateOf(0) }
    var dano by rememberSaveable { mutableStateOf(0) }
    var sinInducir by rememberSaveable { mutableStateOf(0) }
    var bajoPeso by rememberSaveable { mutableStateOf(0) }
    var muestreo by rememberSaveable { mutableStateOf(0) }
    var frutaJoven by rememberSaveable { mutableStateOf(0) }

    // Multi-level counters for Non-Recovered by Calibre
    val nonRecoveredByCalibre = rememberSaveable(saver = MapSaver) { mutableStateMapOf<String, Int>() }
    // Initialize map IF IT IS NEW (not restored)
    LaunchedEffect(Unit) {
        if (nonRecoveredByCalibre.isEmpty()) {
            DEFECTOS.forEach { defect ->
                CALIBRES.forEach { calibre ->
                    nonRecoveredByCalibre["${defect}_${calibre}"] = 0
                }
            }
        }
    }
    
    // Counters for Fuera Especificación (Now just simplified single counters)
    val fueraEspecificacionCounters = rememberSaveable(saver = MapSaver) { mutableStateMapOf<String, Int>() }
    val fueraEspecificacionSCCounters = rememberSaveable(saver = MapSaver) { mutableStateMapOf<String, Int>() }
    
    var selectedQualityTab by rememberSaveable { mutableStateOf(0) }
    
    val displayedCats = remember(selectedQualityTab) {
        FUERA_ESPEC_CATS.filter { cat ->
            if (selectedQualityTab == 0) cat != "Quema Sol Severa"
            else cat != "Quema Sol Leve"
        }
    }
    
    LaunchedEffect(Unit) {
        if (fueraEspecificacionCounters.isEmpty()) {
            FUERA_ESPEC_CATS.forEach { cat ->
                CALIBRES.forEach { calibre ->
                    fueraEspecificacionCounters["${cat}_${calibre}"] = 0
                    fueraEspecificacionSCCounters["${cat}_${calibre}"] = 0
                }
            }
            CALIBRES.forEach { calibre ->
                fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
                fueraEspecificacionSCCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
                
                fueraEspecificacionCounters["${FUERA_ESPEC_ADELANTADA}_${calibre}"] = 0
                fueraEspecificacionSCCounters["${FUERA_ESPEC_ADELANTADA}_${calibre}"] = 0
            }
        }
    }



    // REAL-TIME TOTALS
    val calidadTotal by remember { derivedStateOf { c5 + c6 + c7 + c8P + c8 + c9 + c10 + guapita + babyGuapa } }
    val noRecTotal by remember { derivedStateOf { ausente + dano + sinInducir + bajoPeso + muestreo + frutaJoven } }
    val noRecCalTotal by remember { derivedStateOf { nonRecoveredByCalibre.values.sum() } }
    val fueraEspecTotal by remember { derivedStateOf { fueraEspecificacionCounters.values.sum() } }
    val fueraEspecSCTotal by remember { derivedStateOf { fueraEspecificacionSCCounters.values.sum() } }
    val totalGeneral by remember { derivedStateOf { calidadTotal + noRecTotal + noRecCalTotal + fueraEspecTotal + fueraEspecSCTotal } }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ingresar Datos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total General", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$totalGeneral", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text("Calidad: $calidadTotal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Text("Sin Calidad: $calidadSCTotal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color.Red)
                            Text("No Rec.: $noRecTotal", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text("No Rec. Cal: $noRecCalTotal", style = MaterialTheme.typography.labelSmall)
                            Text("F. Espec.: $fueraEspecTotal", style = MaterialTheme.typography.labelSmall)
                            Text("F. Espec. SC: $fueraEspecSCTotal", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Información General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = currentTime,
                        onValueChange = {},
                        label = { Text("Fecha", fontSize = 12.sp) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = semana,
                        onValueChange = { semana = it },
                        label = { Text("Semana", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = expandedGrupo,
                            onExpandedChange = { expandedGrupo = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = grupoForza,
                                onValueChange = { grupoForza = it },
                                label = { Text("GF", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .onFocusChanged { expandedGrupo = it.isFocused },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = blackTextFieldColors(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGrupo) }
                            )

                            val filteredGrupos = gruposUnicos.filter { it.trim().contains(grupoForza.trim(), ignoreCase = true) }

                            if (expandedGrupo && filteredGrupos.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = expandedGrupo,
                                    onDismissRequest = { expandedGrupo = false },
                                    modifier = Modifier.exposedDropdownSize().heightIn(max = 200.dp).background(Color(0xFFEAD7BC))
                                ) {
                                    filteredGrupos.forEach { gf ->
                                        DropdownMenuItem(
                                            text = { Text(gf, color = Color.Black, fontSize = 14.sp) },
                                            onClick = {
                                                grupoForza = gf
                                                expandedGrupo = false
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = expandedBloque,
                            onExpandedChange = { expandedBloque = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = bloque,
                                onValueChange = { bloque = it },
                                label = { Text("Bloque", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .onFocusChanged { expandedBloque = it.isFocused },
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = blackTextFieldColors(),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBloque) }
                            )

                            val filteredBloques = bloquesUnicos.filter { it.trim().contains(bloque.trim(), ignoreCase = true) }

                            if (expandedBloque && filteredBloques.isNotEmpty()) {
                                DropdownMenu(
                                    expanded = expandedBloque,
                                    onDismissRequest = { expandedBloque = false },
                                    modifier = Modifier.exposedDropdownSize().heightIn(max = 200.dp).background(Color(0xFFEAD7BC))
                                ) {
                                    filteredBloques.forEach { blq ->
                                        DropdownMenuItem(
                                            text = { Text(blq, color = Color.Black, fontSize = 14.sp) },
                                            onClick = {
                                                bloque = blq
                                                expandedBloque = false
                                                val matchedGf = csvData.firstOrNull { it.bloque == blq }?.grupoForza
                                                if (matchedGf != null) grupoForza = matchedGf
                                                focusManager.clearFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // --- NEW SECTION 2: FRUTA CALIDAD (MATRIX) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Fruta Calidad (Matriz)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        val totalTab = if (selectedQualityTab == 0) calidadTotal + fueraEspecTotal else calidadSCTotal + fueraEspecSCTotal
                        Text("Total Pestaña: $totalTab", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = if (selectedQualityTab == 1) Color.Red else Color.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // --- TAB SELECTION ---
                    TabRow(
                        selectedTabIndex = selectedQualityTab,
                        containerColor = SurfaceCream,
                        contentColor = PrimaryEarth,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedQualityTab]),
                                color = if (selectedQualityTab == 1) Color.Red else PrimaryEarth
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedQualityTab == 0,
                            onClick = { selectedQualityTab = 0 },
                            text = { Text("Con Calidad", fontWeight = FontWeight.Bold, color = if (selectedQualityTab == 0) PrimaryEarth else Color.Gray) }
                        )
                        Tab(
                            selected = selectedQualityTab == 1,
                            onClick = { selectedQualityTab = 1 },
                            text = { Text("Sin Calidad", fontWeight = FontWeight.Bold, color = if (selectedQualityTab == 1) Color.Red else Color.Gray) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // --- FRUTA CALIDAD MATRIX WITH STICKY COLUMN ---
                    Row {
                        // 1. STICKY COLUMN (LEFT)
                        Column(modifier = Modifier.width(80.dp)) {
                            // Header Spacer
                            Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Calibre", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = PrimaryEarth)
                            }
                            // Calibre Rows
                            CALIBRES.forEach { calibre ->
                                Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    Text(calibre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                        }

                        // 2. SCROLLABLE DATA
                        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            Column {
                                // Header Row
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
                                    if (selectedQualityTab == 0) {
                                        Text(text = "Sin afectación", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                    }
                                    if (selectedQualityTab == 1) {
                                        Text(text = FUERA_ESPEC_SINGLE, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                    }
                                    Text(text = "F. Adel.", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                    displayedCats.forEach { cat ->
                                        Text(text = cat, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                                    }
                                }
                                
                                // Data Rows
                                CALIBRES.forEach { calibre ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
                                        // Sin afectación (Solo en Con Calidad)
                                        if (selectedQualityTab == 0) {
                                            Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                                                val valSin = when(calibre) {
                                                    "C5" -> c5; "C6" -> c6; "C7" -> c7; "C8P" -> c8P; "C8" -> c8; "C9" -> c9; "C10" -> c10; "Guapita" -> guapita; "Baby Guapa" -> babyGuapa
                                                    else -> 0
                                                }
                                                CompactCounterRow(label = "", value = valSin, onValueChange = { nv ->
                                                    when(calibre) { "C5" -> c5 = nv; "C6" -> c6 = nv; "C7" -> c7 = nv; "C8P" -> c8P = nv; "C8" -> c8 = nv; "C9" -> c9 = nv; "C10" -> c10 = nv; "Guapita" -> guapita = nv; "Baby Guapa" -> babyGuapa = nv }
                                                })
                                            }
                                        }

                                        // Deforme (Solo en Sin Calidad)
                                        if (selectedQualityTab == 1) {
                                            Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                                                val currentMap = fueraEspecificacionSCCounters
                                                CompactCounterRow(label = "", value = currentMap["${FUERA_ESPEC_SINGLE}_${calibre}"] ?: 0, onValueChange = { currentMap["${FUERA_ESPEC_SINGLE}_${calibre}"] = it })
                                            }
                                        }

                                        // F. Adelantada
                                        Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                                            val currentMap = if (selectedQualityTab == 0) fueraEspecificacionCounters else fueraEspecificacionSCCounters
                                            CompactCounterRow(label = "", value = currentMap["${FUERA_ESPEC_ADELANTADA}_${calibre}"] ?: 0, onValueChange = { currentMap["${FUERA_ESPEC_ADELANTADA}_${calibre}"] = it })
                                        }
                                        // Others
                                        displayedCats.forEach { cat ->
                                            val currentMap = if (selectedQualityTab == 0) fueraEspecificacionCounters else fueraEspecificacionSCCounters
                                            Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                                                CompactCounterRow(label = "", value = currentMap["${cat}_${calibre}"] ?: 0, onValueChange = { currentMap["${cat}_${calibre}"] = it })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 3: Fruta No Recuperada (Counters)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Fruta No Recuperada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    CompactCounterRow("Ausente", ausente) { ausente = it }
                    CompactCounterRow("Daño", dano) { dano = it }
                    CompactCounterRow("Sin Inducir", sinInducir) { sinInducir = it }
                    CompactCounterRow("Bajo Peso", bajoPeso) { bajoPeso = it }
                    CompactCounterRow("Muestreo", muestreo) { muestreo = it }
                    CompactCounterRow("Fruta Joven", frutaJoven) { frutaJoven = it }
                }
            }

            
            Spacer(modifier = Modifier.height(12.dp))

            
            // Section 3: Fruta No Recuperada Calibre (Matrix Table)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Fruta No Recuperada Calibre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // --- NO RECUPERADA MATRIX WITH STICKY COLUMN ---
                    Row {
                        // 1. STICKY COLUMN
                        Column(modifier = Modifier.width(80.dp)) {
                            Box(modifier = Modifier.height(30.dp)) // Header Spacer
                            CALIBRES.forEach { calibre ->
                                Box(modifier = Modifier.height(48.dp).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    Text(calibre, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }


                        // 2. SCROLLABLE DATA
                        Box(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                            Column {
                                // Header Row
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(30.dp)) {
                                    DEFECTOS.forEach { defecto ->
                                        Text(text = defecto, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                                
                                // Data Rows
                                CALIBRES.forEach { calibre ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp)) {
                                        DEFECTOS.forEach { defecto ->
                                            Box(modifier = Modifier.width(140.dp), contentAlignment = Alignment.Center) {
                                                CompactCounterRow(label = "", value = nonRecoveredByCalibre["${defecto}_${calibre}"] ?: 0, onValueChange = { nonRecoveredByCalibre["${defecto}_${calibre}"] = it })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // --- END OF DATA SECTIONS ---



            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val r = EstimationRecord(
                        id = System.currentTimeMillis().toString(),
                        date = currentTime,
                        week = semana,
                        grupoForza = grupoForza,
                        bloque = bloque,
                        calidadTotal = calidadTotal,
                        noRecuperadaTotal = noRecTotal,
                        noRecuperadaCalibreTotal = noRecCalTotal,
                        fueraEspecTotal = fueraEspecTotal,
                        fueraEspecSinCalidadTotal = fueraEspecSCTotal,
                        calidadCounts = mapOf(
                            "C5" to c5, "C6" to c6, "C7" to c7, "C8P" to c8P, "C8" to c8, "C9" to c9,
                            "C10" to c10, "Guapita" to guapita, "Baby Guapa" to babyGuapa
                        ),
                        noRecuperadaCounts = mapOf(
                            "Ausente" to ausente, "Daño" to dano, "Sin Inducir" to sinInducir,
                            "Bajo Peso" to bajoPeso, "Muestreo" to muestreo, "Fruta Joven" to frutaJoven
                        ),
                        noRecCalibreCounts = nonRecoveredByCalibre.toMap(),
                        fueraEspecCounts = fueraEspecificacionCounters.toMap(),
                        fueraEspecSinCalidadCounts = fueraEspecificacionSCCounters.toMap()
                    )


                    saveEstimation(context, r)
                    Toast.makeText(context, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                    
                    // --- RESET ALL STATES FOR NEW ENTRY ---
                    val newCal = Calendar.getInstance()
                    currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(newCal.time)
                    semana = newCal.get(Calendar.WEEK_OF_YEAR).toString()
                    grupoForza = ""
                    bloque = ""
                    
                    // Reset counters
                    c5 = 0; c6 = 0; c7 = 0; c8P = 0; c8 = 0; c9 = 0; c10 = 0; guapita = 0; babyGuapa = 0
                    ausente = 0; dano = 0; sinInducir = 0; bajoPeso = 0; muestreo = 0; frutaJoven = 0

                    
                    // Clear and re-initialize maps
                    nonRecoveredByCalibre.clear()
                    DEFECTOS.forEach { defect ->
                        CALIBRES.forEach { calibre ->
                            nonRecoveredByCalibre["${defect}_${calibre}"] = 0
                        }
                    }
                    
                    fueraEspecificacionCounters.clear()
                    fueraEspecificacionSCCounters.clear()
                    FUERA_ESPEC_CATS.forEach { cat ->
                        CALIBRES.forEach { calibre ->
                            fueraEspecificacionCounters["${cat}_${calibre}"] = 0
                            fueraEspecificacionSCCounters["${cat}_${calibre}"] = 0
                        }
                    }
                    CALIBRES.forEach { calibre ->
                        fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
                        fueraEspecificacionSCCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
                        
                        fueraEspecificacionCounters["${FUERA_ESPEC_ADELANTADA}_${calibre}"] = 0
                        fueraEspecificacionSCCounters["${FUERA_ESPEC_ADELANTADA}_${calibre}"] = 0
                    }


                    
                    Toast.makeText(context, "Formulario listo para nuevo bloque", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryEarth, contentColor = Color.White)
            ) {
                Text("Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun CompactCounterRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    val royalBlue = Color(0xFF002366)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label, 
                modifier = Modifier.weight(1f), 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.Black,
                color = Color.Black,
                maxLines = 1
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Decrement Button (Red & Square)
            Button(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier.size(36.dp),
                shape = androidx.compose.ui.graphics.RectangleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
            ) {
                Text("-", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            
            // Custom Input Box (Fixed Truncation)
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .height(36.dp)
                    .padding(horizontal = 2.dp)
                    .background(Color.White)
                    .border(2.dp, Color.Black), // Square border
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = textValue,
                    onValueChange = { 
                        textValue = it
                        val intValue = it.toIntOrNull()
                        if (intValue != null && intValue >= 0) {
                            onValueChange(intValue)
                        } else if (it.isEmpty()) {
                            onValueChange(0)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center, 
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                )
            }
            
            // Increment Button (Royal Blue & Square)
            Button(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(36.dp),
                shape = androidx.compose.ui.graphics.RectangleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = royalBlue, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(22.dp))
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Intercept system back button
    BackHandler { onBack() }
    
    var records by remember { mutableStateOf(getHistorial(context)) }
    
    // Deletion states
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<EstimationRecord?>(null) }
    
    val scope = rememberCoroutineScope()
    val groupedRecords = records.groupBy { "${it.grupoForza} - ${it.bloque}" }

    // Dialog: Delete Single
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("¿Borrar registro?") },
            text = { Text("¿Estás seguro de que deseas borrar esta estimación?") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { deleteEstimation(context, it.id) }
                    records = getHistorial(context)
                    itemToDelete = null
                }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    // Dialog: Delete All
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Borrar TODO el historial") },
            text = { Text("Esta acción eliminará todos los registros guardados. ¿Estás seguro?") },
            confirmButton = {
                TextButton(onClick = {
                    clearAllHistorial(context)
                    records = getHistorial(context)
                    showDeleteAllDialog = false
                }) { Text("Eliminar Todo", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Estimaciones", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    val pendingCount = records.count { !it.isUploaded }
                    if (pendingCount > 0) {
                        IconButton(onClick = {
                            if (!isNetworkAvailable(context)) {
                                Toast.makeText(context, "Conéctate a internet para subir datos", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Subiendo $pendingCount registros...", Toast.LENGTH_SHORT).show()
                                records.filter { !it.isUploaded }.forEach { rec ->
                                    uploadRecord(context, rec) { success, msg ->
                                        if (success) {
                                            markAsUploaded(context, rec.id)
                                            // Refresh UI from main thread if needed or just reload records
                                        }
                                    }
                                }
                                // Poor man's refresh - ideally use a Flow or ViewModel
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    kotlinx.coroutines.delay(2000)
                                    records = getHistorial(context)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Subir Todo", tint = Color.Cyan)
                        }
                    }

                    IconButton(onClick = { exportRecordsToCSV(context, records) }) {
                        Icon(Icons.Default.Share, contentDescription = "Descargar CSV")
                    }
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Default.Remove, contentDescription = "Borrar Todo", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryEarth,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("No hay registros guardados", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                groupedRecords.keys.forEach { groupKey ->
                    val groupRecords = groupedRecords[groupKey] ?: emptyList()
                    item {
                        Surface(
                            color = SecondaryGold.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = groupKey,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(groupRecords) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!record.isUploaded) Color(0xFFE8F5E9) else SurfaceCream
                            )
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(record.date, style = MaterialTheme.typography.labelSmall, color = PrimaryEarth)
                                        Text("Semana ${record.week}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                        if (record.isUploaded) {
                                            Text("SUBIDO", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Row {
                                        if (!record.isUploaded) {
                                            IconButton(onClick = {
                                                uploadRecord(context, record) { success, msg ->
                                                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        if (success) {
                                                            markAsUploaded(context, record.id)
                                                            records = getHistorial(context)
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.Share, contentDescription = "Subir", tint = PrimaryEarth)
                                            }
                                        }
                                        IconButton(onClick = { itemToDelete = record }) {
                                            Icon(Icons.Default.Remove, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    SummaryMiniItem("Calidad", record.calidadTotal)
                                    SummaryMiniItem("No Rec.", record.noRecuperadaTotal)
                                    SummaryMiniItem("F. Espec", record.fueraEspecTotal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMiniItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryEarth)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    FruitTheme {
        MainApp()
    }
}