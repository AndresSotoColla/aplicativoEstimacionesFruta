package com.example.aplicativoestimaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    var semana by remember { mutableStateOf("") }
    var grupoForza by remember { mutableStateOf("") }
    var lote by remember { mutableStateOf("") }
    var bloque by remember { mutableStateOf("") }
    var desarrollo by remember { mutableStateOf("PC") } // Default value
    
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
                    
                    val selectedDateText = datePickerState.selectedDateMillis?.let {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        formatter.format(Date(it))
                    } ?: "Seleccionar fecha"
                    
                    OutlinedTextField(
                        value = selectedDateText,
                        onValueChange = {},
                        label = { Text("Fecha de Actividad") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Text("📅", fontSize = 24.sp)
                            }
                        },
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
                    
                    OutlinedTextField(
                        value = grupoForza,
                        onValueChange = { grupoForza = it },
                        label = { Text("Grupo Forza") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lote,
                        onValueChange = { input ->
                            if (input.isEmpty()) {
                                lote = ""
                            } else {
                                val num = input.toIntOrNull()
                                if (num != null && num in 1..87) {
                                    lote = input
                                }
                            }
                        },
                        label = { Text("Lote (1-87)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = bloque,
                        onValueChange = { bloque = it },
                        label = { Text("Número del Bloque") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Desarrollo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = desarrollo == "PC",
                            onClick = { desarrollo = "PC" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("PC")
                        }
                        SegmentedButton(
                            selected = desarrollo == "SC",
                            onClick = { desarrollo = "SC" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("SC")
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