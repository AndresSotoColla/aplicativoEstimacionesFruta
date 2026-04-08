package com.example.aplicativoestimaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.nio.charset.StandardCharsets
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
import com.example.aplicativoestimaciones.ui.theme.AplicativoEstimacionesTheme

val CALIBRES = listOf("C5", "C6", "C7", "C8", "C9", "C10", "C8P", "Guapita", "Baby Guapa")
val DEFECTOS = listOf("Enferma", "Quema Sol Severo", "Deforme", "Daño Insecto", "Daño Mecánico")
val FUERA_ESPEC_CATS = listOf("Cuello", "Cónica", "Cicatriz", "Base café", "Cónica Inclinada", "Corona Pequeña", "Corona Grande", "Corona Múltiple", "Cochinilla", "Off Color", "Quema Sol Leve")
val FUERA_ESPEC_SINGLE = "Deforme"
val ESPEC_TYPES = listOf("Tolerable", "No Tolerable")

data class BloqueData(
    val bloque: String,
    val grupoForza: String
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
    val fueraEspecTotal: Int
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
                calidadTotal = obj.getInt("calidad"),
                noRecuperadaTotal = obj.getInt("noRecTotal"),
                noRecuperadaCalibreTotal = obj.getInt("noRecCalTotal"),
                fueraEspecTotal = obj.getInt("fueraEspec")
            ))
        }
    } catch (e: Exception) { e.printStackTrace() }
    return list
}

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

fun exportRecordsToCSV(context: Context, records: List<EstimationRecord>) {
    if (records.isEmpty()) {
        Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
        return
    }

    val csvHeader = "ID,Fecha,Semana,Grupo Forza,Bloque,Calidad Total,No Recuperada Total,No Rec Calibre Total,Fuera Espec Total\n"
    val csvContent = StringBuilder(csvHeader)
    records.forEach { r ->
        csvContent.append("${r.id},${r.date},${r.week},${r.grupoForza},${r.bloque},${r.calidadTotal},${r.noRecuperadaTotal},${r.noRecuperadaCalibreTotal},${r.fueraEspecTotal}\n")
    }

    val filename = "estimaciones_fruta_${System.currentTimeMillis()}.csv"
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
                outputStream.write(csvContent.toString().toByteArray(StandardCharsets.UTF_8))
            }
            Toast.makeText(context, "Archivo guardado en Descargas", Toast.LENGTH_LONG).show()
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
            AplicativoEstimacionesTheme {
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
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(
            onNavigateToIngresar = { currentScreen = Screen.IngresarDatos },
            onNavigateToHistorial = { currentScreen = Screen.Historial }
        )
        Screen.IngresarDatos ->  IngresarDatosScreen(onBack = { currentScreen = Screen.Home })
        Screen.Historial -> HistorialScreen(onBack = { currentScreen = Screen.Home })
    }
}

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
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)
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
            HomeButton("Guardar Datos en Memoria", Icons.Default.Check) { /* TODO */ }
        }
    }
}

@Composable
fun HomeButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngresarDatosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // CSV Data (reloads when entering screen)
    val csvData = remember { 
        readCsvData(context)
    }
    
    // Current Time and Week Logic
    val calendar = Calendar.getInstance()
    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR).toString()
    val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    var semana by remember { mutableStateOf(currentWeek) }
    var grupoForza by remember { mutableStateOf("") }
    var bloque by remember { mutableStateOf("") }
    // var lote removed
    
    // Dropdown States
    var expandedGrupo by remember { mutableStateOf(false) }
    var expandedBloque by remember { mutableStateOf(false) }
    
    // Unique options (MUALLY FILTERED)
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

    // Counters
    var c5 by remember { mutableIntStateOf(0) }
    var c6 by remember { mutableIntStateOf(0) }
    var c7 by remember { mutableIntStateOf(0) }
    var c8 by remember { mutableIntStateOf(0) }
    var c9 by remember { mutableIntStateOf(0) }
    var c10 by remember { mutableIntStateOf(0) }
    var c8p by remember { mutableIntStateOf(0) }
    var guapita by remember { mutableIntStateOf(0) }
    var babyGuapa by remember { mutableIntStateOf(0) }
    
    // Non-recovered fruit counters
    var ausente by remember { mutableIntStateOf(0) }
    var dano by remember { mutableIntStateOf(0) }
    var sinInducir by remember { mutableIntStateOf(0) }
    var bajoPeso by remember { mutableIntStateOf(0) }
    var muestreo by remember { mutableIntStateOf(0) }
    var frutaJoven by remember { mutableIntStateOf(0) }

    // Multi-level counters for Non-Recovered by Calibre
    val nonRecoveredByCalibre = remember { mutableStateMapOf<String, Int>() }
    // Initialize map if empty (recommended for stability)
    LaunchedEffect(Unit) {
        if (nonRecoveredByCalibre.isEmpty()) {
            DEFECTOS.forEach { defect ->
                CALIBRES.forEach { calibre ->
                    nonRecoveredByCalibre["${defect}_${calibre}"] = 0
                }
            }
        }
    }
    
    // Counters for Fuera Especificación (~200 items)
    val fueraEspecificacionCounters = remember { mutableStateMapOf<String, Int>() }
    
    LaunchedEffect(Unit) {
        if (fueraEspecificacionCounters.isEmpty()) {
            // Default 11 categories (Dual counter per calibre)
            FUERA_ESPEC_CATS.forEach { cat ->
                CALIBRES.forEach { calibre ->
                    ESPEC_TYPES.forEach { type ->
                        fueraEspecificacionCounters["${cat}_${calibre}_${type}"] = 0
                    }
                }
            }
            // Deforme category (Single counter per calibre)
            CALIBRES.forEach { calibre ->
                fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${calibre}"] = 0
            }
        }
    }

    // --- REAL-TIME TOTALS CALCULATION ---
    val calidadTotal by remember { derivedStateOf { c5 + c6 + c7 + c8 + c9 + c10 + c8p + guapita + babyGuapa } }
    val noRecTotal by remember { derivedStateOf { ausente + dano + sinInducir + bajoPeso + muestreo + frutaJoven } }
    val noRecCalTotal by remember { derivedStateOf { nonRecoveredByCalibre.values.sum() } }
    val fueraEspecTotal by remember { derivedStateOf { fueraEspecificacionCounters.values.sum() } }
    val totalGeneral by remember { derivedStateOf { calidadTotal + noRecTotal + noRecCalTotal + fueraEspecTotal } }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
            // --- SUMMARY CARD (TOTAL REAL-TIME) ---
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
                        Text("$totalGeneral", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
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
                        onExpandedChange = { expandedGrupo = !expandedGrupo },
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
                        onExpandedChange = { expandedBloque = !expandedBloque },
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
                    
                    // Single counter category: Deforme
                    FueraEspecificacionDeepCategorySection(
                        category = FUERA_ESPEC_SINGLE,
                        isDouble = false,
                        counters = fueraEspecificacionCounters,
                        onValueChange = { key, newValue ->
                            fueraEspecificacionCounters["${FUERA_ESPEC_SINGLE}_${key}"] = newValue
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)

                    // Double counter categories
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
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
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
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
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
            FilledTonalIconButton(onClick = { 
                if (value > 0) onValueChange(value - 1) 
            }) {
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
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                shape = RoundedCornerShape(8.dp)
            )
            
            FilledTonalIconButton(onClick = { onValueChange(value + 1) }) {
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
            containerColor = if (expanded) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
                    color = MaterialTheme.colorScheme.secondary
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Cerrar" else "Abrir", fontWeight = FontWeight.SemiBold)
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
        Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isDouble) Text("Tolerable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                CounterRow(label = "", value = val1, onValueChange = onVal1Change)
            }
            if (isDouble) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("No Tolerable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
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
            containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Cerrar" else "Abrir", fontWeight = FontWeight.SemiBold)
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
    val records = remember { getHistorial(context) }
    
    // Group records by metadata for better UI organization
    val groupedRecords = records.groupBy { "${it.grupoForza} - ${it.bloque}" }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                groupedRecords.forEach { (groupKey, groupRecords) ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = groupKey,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    items(groupRecords) { record ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(record.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("Semana ${record.week}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}


@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AplicativoEstimacionesTheme {
        MainApp()
    }
}