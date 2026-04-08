package com.example.aplicativoestimaciones

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val CALIBRES = listOf("C5", "C6", "C7", "C8", "C9", "C10", "C8P", "Guapita", "Baby Guapa")
val DEFECTOS = listOf("Enferma", "Quema Sol Severo", "Deforme", "Daño Insecto", "Daño Mecánico")
val FUERA_ESPEC_CATS = listOf("Cuello", "Cónica", "Cicatriz", "Base café", "Cónica Inclinada", "Corona Pequeña", "Corona Grande", "Corona Múltiple", "Cochinilla", "Off Color", "Quema Sol Leve")
val FUERA_ESPEC_SINGLE = "Deforme"
val ESPEC_TYPES = listOf("Tolerable", "No Tolerable")

data class BloqueData(
    val bloque: String,
    val grupoForza: String
)

fun readCsvData(filePath: String): List<BloqueData> {
    val list = mutableListOf<BloqueData>()
    val file = File(filePath)
    if (!file.exists()) return list
    
    try {
        BufferedReader(FileReader(file)).use { reader ->
            var line: String? = reader.readLine() // Read header
            while (reader.readLine().also { line = it } != null) {
                val tokens = line?.split(",") ?: continue
                if (tokens.size >= 5) {
                    val bloque = tokens[0].trim().removeSurrounding("\"")
                    val gf = tokens[4].trim().removeSurrounding("\"")
                    list.add(BloqueData(bloque, gf))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
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
    Home, IngresarDatos
}

@Composable
fun MainApp() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    when (currentScreen) {
        Screen.Home -> HomeScreen(onNavigateToIngresar = { currentScreen = Screen.IngresarDatos })
        Screen.IngresarDatos ->  IngresarDatosScreen(onBack = { currentScreen = Screen.Home })
    }
}

@Composable
fun HomeScreen(onNavigateToIngresar: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Gestor de Fruta", 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            
            HomeButton("Ingresar Datos", Icons.Default.Edit) { onNavigateToIngresar() }
            HomeButton("Subir Datos", Icons.Default.Share) { /* TODO */ }
            HomeButton("Actualizar Bloques", Icons.Default.Refresh) { /* TODO */ }
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
    val focusManager = LocalFocusManager.current
    
    // CSV Data
    val csvData = remember { 
        readCsvData("C:\\Users\\sotoc\\Downloads\\grupo_forza.csv")
    }
    
    // Current Time and Week Logic
    val calendar = Calendar.getInstance()
    val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR).toString()
    val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

    var semana by remember { mutableStateOf(currentWeek) }
    var grupoForza by remember { mutableStateOf("") }
    var lote by remember { mutableStateOf("") }
    var bloque by remember { mutableStateOf("") }
    
    // Dropdown States
    var expandedGrupo by remember { mutableStateOf(false) }
    var expandedBloque by remember { mutableStateOf(false) }
    
    // Unique options
    val gruposUnicos = remember(csvData) { csvData.map { it.grupoForza }.distinct().sorted() }
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

                        val filteredGrupos = gruposUnicos.filter { it.contains(grupoForza, ignoreCase = true) }

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

                    OutlinedTextField(
                        value = lote,
                        onValueChange = { input ->
                            if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 1..87)) {
                                lote = input
                            }
                        },
                        label = { Text("Lote (1-87)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
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

                        val filteredBloques = bloquesUnicos.filter { it.contains(bloque, ignoreCase = true) }

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
                        onValueChange(0) // Empty means 0, but visually could be empty. We leave textValue as empty.
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
                PrettyCounter(value = val1, onValueChange = onVal1Change)
            }
            if (isDouble) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("No Tolerable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    PrettyCounter(value = val2, onValueChange = onVal2Change)
                }
            }
        }
    }
}

@Composable
fun PrettyCounter(value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { if (value > 0) onValueChange(value - 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                .width(75.dp)
                .padding(horizontal = 4.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            shape = RoundedCornerShape(8.dp)
        )

        FilledTonalIconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
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

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    AplicativoEstimacionesTheme {
        MainApp()
    }
}