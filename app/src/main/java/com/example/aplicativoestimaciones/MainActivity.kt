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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aplicativoestimaciones.ui.theme.AplicativoEstimacionesTheme

val CALIBRES = listOf("C5", "C6", "C7", "C8", "C9", "C10", "C8P", "Guapita", "Baby Guapa")
val DEFECTOS = listOf("Enferma", "Quema Sol Severo", "Deforme", "Daño Insecto", "Daño Mecánico")
val FUERA_ESPEC_CATS = listOf("Cuello", "Cónica", "Cicatriz", "Base café", "Cónica Inclinada", "Corona Pequeña", "Corona Grande", "Corona Múltiple", "Cochinilla", "Off Color", "Quema Sol Leve")
val FUERA_ESPEC_SINGLE = "Deforme"
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
    // Add detailed maps for "todos los conteos"
    val calidadCounts: Map<String, Int> = emptyMap(),
    val noRecuperadaCounts: Map<String, Int> = emptyMap(),
    val noRecCalibreCounts: Map<String, Int> = emptyMap(),
    val fueraEspecCounts: Map<String, Int> = emptyMap()
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
            // Serialize maps
            put("calidadCounts", JSONObject(r.calidadCounts))
            put("noRecCounts", JSONObject(r.noRecuperadaCounts))
            put("noRecCalCounts", JSONObject(r.noRecCalibreCounts))
            put("fueraEspecCounts", JSONObject(r.fueraEspecCounts))
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
            put("noRecTotal", r.noRecuperadaTotal)
            put("noRecCalTotal", r.noRecuperadaCalibreTotal)
            put("fueraEspec", r.fueraEspecTotal)
            put("calidadCounts", JSONObject(r.calidadCounts))
            put("noRecCounts", JSONObject(r.noRecuperadaCounts))
            put("noRecCalCounts", JSONObject(r.noRecCalibreCounts))
            put("fueraEspecCounts", JSONObject(r.fueraEspecCounts))
        }
        jsonArray.put(obj)
    }
    try {
        FileOutputStream(file).use { it.write(jsonArray.toString().toByteArray()) }
    } catch (e: Exception) { e.printStackTrace() }
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
                calidadCounts = jsonToMap(obj.optJSONObject("calidadCounts")),
                noRecuperadaCounts = jsonToMap(obj.optJSONObject("noRecCounts")),
                noRecCalibreCounts = jsonToMap(obj.optJSONObject("noRecCalCounts")),
                fueraEspecCounts = jsonToMap(obj.optJSONObject("fueraEspecCounts"))
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

// Custom saver for SnapshotStateMap to survive rotation
val MapSaver = Saver<SnapshotStateMap<String, Int>, Map<String, Int>>(
    save = { it.toMap() },
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
                
                if (line.startsWith("Bloque", ignoreCase = true) || line.startsWith("GF", ignoreCase = true)) return@forEachLine
                
                val delimiters = listOf(";", ",", "\t")
                var tokens = emptyList<String>()
                for (delim in delimiters) {
                    val t = line.split(delim)
                    if (t.size >= 5) {
                        tokens = t
                        break
                    }
                }
                
                if (tokens.size >= 5) {
                    val bloque = tokens[2].trim().removeSurrounding("\"").trim()
                    val gf = tokens[4].trim().removeSurrounding("\"").trim()
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
    sb.append("ID,Fecha,Semana,Grupo Forza,Bloque,Calidad Total,No Recuperada Total,No Rec Calibre Total,Fuera Espec Total")
    
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
    
    // Fuera Especificación matrix
    // Single category: Deforme
    CALIBRES.forEach { calibre ->
        sb.append(",FE_${FUERA_ESPEC_SINGLE}_$calibre")
    }
    // Dual categories
    FUERA_ESPEC_CATS.forEach { cat ->
        CALIBRES.forEach { calibre ->
            ESPEC_TYPES.forEach { type ->
                sb.append(",FE_${cat}_${calibre}_$type")
            }
        }
    }
    sb.append("\n")

    // --- BUILD ROWS ---
    records.forEach { r ->
        // Basic info & Totals
        sb.append("${r.id},\"${r.date}\",\"${r.week}\",\"${r.grupoForza}\",\"${r.bloque}\",${r.calidadTotal},${r.noRecuperadaTotal},${r.noRecuperadaCalibreTotal},${r.fueraEspecTotal}")
        
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
        
        // Fuera Especificación counts
        // Deforme
        CALIBRES.forEach { calibre ->
            sb.append(",${r.fueraEspecCounts["${FUERA_ESPEC_SINGLE}_$calibre"] ?: 0}")
        }
        // Dual categories
        FUERA_ESPEC_CATS.forEach { cat ->
            CALIBRES.forEach { calibre ->
                ESPEC_TYPES.forEach { type ->
                    sb.append(",${r.fueraEspecCounts["${cat}_${calibre}_$type"] ?: 0}")
                }
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
            HomeButton("Subir Datos", Icons.Default.Share) { /* TODO */ }
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
    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR).toString()
    val currentTime = rememberSaveable { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()) }

    var semana by rememberSaveable { mutableStateOf(currentWeek) }
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
    var c8 by rememberSaveable { mutableStateOf(0) }
    var c9 by rememberSaveable { mutableStateOf(0) }
    var c10 by rememberSaveable { mutableStateOf(0) }
    var c8p by rememberSaveable { mutableStateOf(0) }
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
    
    // Counters for Fuera Especificación
    val fueraEspecificacionCounters = rememberSaveable(saver = MapSaver) { mutableStateMapOf<String, Int>() }
    
    LaunchedEffect(Unit) {
        if (fueraEspecificacionCounters.isEmpty()) {
            FUERA_ESPEC_CATS.forEach { cat ->
                CALIBRES.forEach { calibre ->
                    ESPEC_TYPES.forEach { type ->
                        fueraEspecificacionCounters["${cat}_${calibre}_${type}"] = 0
                    }
                }
            }
            CALIBRES.forEach { calibre ->
                fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
            }
        }
    }

    // REAL-TIME TOTALS
    val calidadTotal by remember { derivedStateOf { c5 + c6 + c7 + c8 + c9 + c10 + c8p + guapita + babyGuapa } }
    val noRecTotal by remember { derivedStateOf { ausente + dano + sinInducir + bajoPeso + muestreo + frutaJoven } }
    val noRecCalTotal by remember { derivedStateOf { nonRecoveredByCalibre.values.sum() } }
    val fueraEspecTotal by remember { derivedStateOf { fueraEspecificacionCounters.values.sum() } }
    val totalGeneral by remember { derivedStateOf { calidadTotal + noRecTotal + noRecCalTotal + fueraEspecTotal } }

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
            // --- SUMMARY CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total General", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("$totalGeneral", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("Calidad: $calidadTotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("No Rec.: $noRecTotal", style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text("No Rec. Cal: $noRecCalTotal", style = MaterialTheme.typography.bodyMedium)
                            Text("F. Espec.: $fueraEspecTotal", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Información General", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = currentTime,
                        onValueChange = {},
                        label = { Text("Fecha de Actividad") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = semana,
                        onValueChange = { semana = it },
                        label = { Text("Semana (Número)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedGrupo,
                        onExpandedChange = { expandedGrupo = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = grupoForza,
                            onValueChange = { grupoForza = it },
                            label = { Text("Grupo Forza") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .onFocusChanged { expandedGrupo = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = blackTextFieldColors(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGrupo) }
                        )

                        val filteredGrupos = gruposUnicos.filter { it.trim().contains(grupoForza.trim(), ignoreCase = true) }

                        if (expandedGrupo && filteredGrupos.isNotEmpty()) {
                            DropdownMenu(
                                expanded = expandedGrupo,
                                onDismissRequest = { expandedGrupo = false },
                                modifier = Modifier.exposedDropdownSize().heightIn(max = 250.dp).background(Color(0xFFEAD7BC))
                            ) {
                                filteredGrupos.forEach { gf ->
                                    DropdownMenuItem(
                                        text = { Text(gf, color = Color.Black) },
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
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedBloque,
                        onExpandedChange = { expandedBloque = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = bloque,
                            onValueChange = { bloque = it },
                            label = { Text("Número del Bloque") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .onFocusChanged { expandedBloque = it.isFocused },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = blackTextFieldColors(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBloque) }
                        )

                        val filteredBloques = bloquesUnicos.filter { it.trim().contains(bloque.trim(), ignoreCase = true) }

                        if (expandedBloque && filteredBloques.isNotEmpty()) {
                            DropdownMenu(
                                expanded = expandedBloque,
                                onDismissRequest = { expandedBloque = false },
                                modifier = Modifier.exposedDropdownSize().heightIn(max = 250.dp).background(Color(0xFFEAD7BC))
                            ) {
                                filteredBloques.forEach { blq ->
                                    DropdownMenuItem(
                                        text = { Text(blq, color = Color.Black) },
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Fruta Calidad", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CounterRow("C5", c5) { c5 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C6", c6) { c6 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C7", c7) { c7 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C8", c8) { c8 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C9", c9) { c9 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C10", c10) { c10 = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("C8P", c8p) { c8p = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Guapita", guapita) { guapita = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Baby Guapa", babyGuapa) { babyGuapa = it }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Fruta No Recuperada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    CounterRow("Ausente", ausente) { ausente = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Daño", dano) { dano = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Sin Inducir", sinInducir) { sinInducir = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Bajo Peso", bajoPeso) { bajoPeso = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Muestreo", muestreo) { muestreo = it }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    CounterRow("Fruta Joven", frutaJoven) { frutaJoven = it }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Fruta No Recuperada Calibre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DEFECTOS.forEach { defecto ->
                        DefectCategorySection(
                            category = defecto,
                            calibreValues = nonRecoveredByCalibre,
                            onValueChange = { key, newValue ->
                                nonRecoveredByCalibre[key] = newValue
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Fruta Fuera Especificación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FueraEspecificacionDeepCategorySection(
                        category = FUERA_ESPEC_SINGLE,
                        isDouble = false,
                        counters = fueraEspecificacionCounters,
                        onValueChange = { key, newValue ->
                            fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${key}"] = newValue
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

                    FUERA_ESPEC_CATS.forEach { cat ->
                        FueraEspecificacionDeepCategorySection(
                            category = cat,
                            isDouble = true,
                            counters = fueraEspecificacionCounters,
                            onValueChange = { key, newValue ->
                                fueraEspecificacionCounters["${cat}_${key}"] = newValue
                            }
                        )
                    }
                }
            }

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
                        calidadCounts = mapOf(
                            "C5" to c5, "C6" to c6, "C7" to c7, "C8" to c8, "C9" to c9,
                            "C10" to c10, "C8P" to c8p, "Guapita" to guapita, "Baby Guapa" to babyGuapa
                        ),
                        noRecuperadaCounts = mapOf(
                            "Ausente" to ausente, "Daño" to dano, "Sin Inducir" to sinInducir,
                            "Bajo Peso" to bajoPeso, "Muestreo" to muestreo, "Fruta Joven" to frutaJoven
                        ),
                        noRecCalibreCounts = nonRecoveredByCalibre.toMap(),
                        fueraEspecCounts = fueraEspecificacionCounters.toMap()
                    )
                    saveEstimation(context, r)
                    Toast.makeText(context, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                    onBack()
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
fun blackTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedBorderColor = PrimaryEarth,
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = PrimaryEarth,
    unfocusedLabelColor = Color.Gray
)

@Composable
fun CounterRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SecondaryGold, contentColor = Color.Black)
            ) {
                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            
            OutlinedTextField(
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
                modifier = Modifier
                    .width(80.dp)
                    .padding(horizontal = 8.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEarth,
                    unfocusedBorderColor = SecondaryGold
                )
            )
            
            FilledIconButton(
                onClick = { onValueChange(value + 1) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryEarth, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Más")
            }
        }
    }
}

@Composable
fun FueraEspecificacionDeepCategorySection(
    category: String,
    isDouble: Boolean,
    counters: Map<String, Int>,
    onValueChange: (String, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) SecondaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEarth
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Cerrar" else "Abrir", fontWeight = FontWeight.SemiBold, color = PrimaryEarth)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    CALIBRES.forEachIndexed { index, calibre ->
                        val baseKey = "${category}_${calibre}"
                        FueraEspecificacionCalibreRow(
                            label = calibre,
                            isDouble = isDouble,
                            val1 = if (isDouble) (counters["${baseKey}_Tolerable"] ?: 0) else (counters[baseKey] ?: 0),
                            val2 = if (isDouble) (counters["${baseKey}_No Tolerable"] ?: 0) else 0,
                            onVal1Change = { if (isDouble) onValueChange("${calibre}_Tolerable", it) else onValueChange(calibre, it) },
                            onVal2Change = { if (isDouble) onValueChange("${calibre}_No Tolerable", it) }
                        )
                        if (index < CALIBRES.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FueraEspecificacionCalibreRow(
    label: String,
    isDouble: Boolean,
    val1: Int,
    val2: Int,
    onVal1Change: (Int) -> Unit,
    onVal2Change: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryEarth)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isDouble) Text("Tolerable", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                CounterRow(label = "", value = val1, onValueChange = onVal1Change)
            }
            if (isDouble) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("No Tolerable", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    CounterRow(label = "", value = val2, onValueChange = onVal2Change)
                }
            }
        }
    }
}

@Composable
fun DefectCategorySection(
    category: String,
    calibreValues: Map<String, Int>,
    onValueChange: (String, Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) SecondaryGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryEarth
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Cerrar" else "Abrir", fontWeight = FontWeight.SemiBold, color = PrimaryEarth)
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    CALIBRES.forEachIndexed { index, calibre ->
                        val key = "${category}_${calibre}"
                        val value = calibreValues[key] ?: 0
                        CounterRow(
                            label = calibre,
                            value = value,
                            onValueChange = { newValue -> onValueChange(key, newValue) }
                        )
                        if (index < CALIBRES.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
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
                            colors = CardDefaults.cardColors(containerColor = SurfaceCream)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(record.date, style = MaterialTheme.typography.labelSmall, color = PrimaryEarth)
                                        Text("Semana ${record.week}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    IconButton(onClick = { itemToDelete = record }) {
                                        Icon(Icons.Default.Remove, contentDescription = "Eliminar", tint = Color.Red.copy(alpha = 0.7f))
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