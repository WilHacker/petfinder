package com.frontend.petfinder.pets.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.frontend.petfinder.core.presentation.components.PetFinderErrorBanner
import com.frontend.petfinder.core.presentation.components.PetFinderTextField
import com.frontend.petfinder.core.theme.PrimaryOrange
import com.frontend.petfinder.core.theme.PrimaryOrangeLight

private val colorSugerencias = listOf(
    "Negro", "Blanco", "Café", "Gris", "Dorado", "Atigrado", "Naranja", "Crema"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPetScreen(
    viewModel: RegisterPetViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val nombre by viewModel.nombre.collectAsStateWithLifecycle()
    val tiposMascota by viewModel.tiposMascota.collectAsStateWithLifecycle()
    val tipoSeleccionado by viewModel.tipoSeleccionado.collectAsStateWithLifecycle()
    val sexo by viewModel.sexo.collectAsStateWithLifecycle()
    val colorPrimario by viewModel.colorPrimario.collectAsStateWithLifecycle()
    val rasgosParticulares by viewModel.rasgosParticulares.collectAsStateWithLifecycle()
    val fotosSeleccionadas by viewModel.fotosSeleccionadas.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var expandedTipo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.onFotosChange(uris)
    }

    LaunchedEffect(uiState) {
        if (uiState is RegisterPetViewModel.RegisterPetState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nueva Mascota",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Completa el perfil de tu compañero",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── SECCIÓN 1: Información básica ─────────────────────────────
            SectionHeader(title = "Información básica")

            PetFinderTextField(
                value = nombre,
                onValueChange = { viewModel.onNombreChange(it) },
                placeholder = "Nombre de la mascota *"
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Tipo de mascota — Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = { expandedTipo = !expandedTipo }
            ) {
                OutlinedTextField(
                    value = when {
                        tiposMascota.isEmpty() -> "Cargando tipos..."
                        tipoSeleccionado != null -> tipoSeleccionado!!.nombre
                        else -> "Tipo de mascota *"
                    },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryOrange)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = Color(0xFFEAEAEA),
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = if (tipoSeleccionado != null)
                            MaterialTheme.colorScheme.onBackground else Color.Gray
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { expandedTipo = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    tiposMascota.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo.nombre) },
                            onClick = {
                                viewModel.onTipoSeleccionado(tipo)
                                expandedTipo = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Sexo — Segmented Button
            Text(
                text = "Sexo",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("M" to "Macho", "H" to "Hembra").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = sexo == value,
                        onClick = { viewModel.onSexoChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = PrimaryOrangeLight,
                            activeContentColor = PrimaryOrange,
                            activeBorderColor = PrimaryOrange
                        )
                    ) {
                        Text(label, fontWeight = if (sexo == value) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECCIÓN 2: Apariencia ──────────────────────────────────────
            SectionHeader(title = "Apariencia")

            // Color primario con chips de sugerencia
            Text(
                text = "Color principal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                colorSugerencias.forEach { color ->
                    val isSelected = colorPrimario.equals(color, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { viewModel.onColorPrimarioChange(if (isSelected) "" else color) }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) PrimaryOrange else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(50.dp)
                            ),
                        color = if (isSelected) PrimaryOrangeLight else Color.White,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = color,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            PetFinderTextField(
                value = colorPrimario,
                onValueChange = { viewModel.onColorPrimarioChange(it) },
                placeholder = "O escribe el color..."
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Rasgos particulares
            OutlinedTextField(
                value = rasgosParticulares,
                onValueChange = { viewModel.onRasgosChange(it) },
                placeholder = { Text("Rasgos particulares (mancha en la oreja, cojea, etc.)", color = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color(0xFFEAEAEA),
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── SECCIÓN 3: Fotos ───────────────────────────────────────────
            SectionHeader(title = "Fotos")

            if (fotosSeleccionadas.isEmpty()) {
                // Estado vacío — área de carga
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(16.dp)),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = PrimaryOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Agregar fotos",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryOrange
                        )
                        Text(
                            "Máximo 4 fotos · La primera será la principal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Miniaturas con botón de eliminar por foto
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fotosSeleccionadas.forEachIndexed { index, uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Foto ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (index == 0) 2.dp else 0.dp,
                                        color = if (index == 0) PrimaryOrange else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )
                            // Ícono principal en la primera foto
                            if (index == 0) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp),
                                    color = PrimaryOrange,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Principal",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            // Botón eliminar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(20.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable { viewModel.onFotoRemoved(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Eliminar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    // Botón agregar más (si hay menos de 4)
                    if (fotosSeleccionadas.size < 4) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5))
                                .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(12.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Agregar más",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── ERROR + BOTÓN ──────────────────────────────────────────────
            if (uiState is RegisterPetViewModel.RegisterPetState.Error) {
                PetFinderErrorBanner(
                    message = (uiState as RegisterPetViewModel.RegisterPetState.Error).message,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (uiState is RegisterPetViewModel.RegisterPetState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryOrange)
                }
            } else {
                Button(
                    onClick = { viewModel.registerPet(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(50.dp)),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Guardar y Generar QR", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryOrange
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFF0F0F0), thickness = 1.5.dp)
    }
}
