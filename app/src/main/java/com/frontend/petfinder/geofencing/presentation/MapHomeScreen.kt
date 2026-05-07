package com.frontend.petfinder.geofencing.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapHomeScreen(
    mapViewModel: MapViewModel = viewModel()
    // ELIMINADO: Ya no necesitamos onNavigateToMyPets aquí
) {
    val snapshot by mapViewModel.snapshot.collectAsState()

    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val cochabamba = LatLng(-17.3895, -66.1568)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cochabamba, 13f)
    }

    Scaffold { paddingValues ->
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = hasLocationPermission,
                zoomControlsEnabled = false
            )
        ) {
            snapshot?.let { data ->
                data.marcadores.usuariosCompartidos.forEach { user ->
                    Marker(
                        state = MarkerState(position = LatLng(user.lat, user.lng)),
                        title = user.nombre,
                        snippet = "Usuario conectado"
                    )
                }

                data.marcadores.desaparecidas.forEach { pet ->
                    Marker(
                        state = MarkerState(position = LatLng(pet.lat, pet.lng)),
                        title = "¡${pet.nombre} desaparecida!",
                        snippet = "Tipo: ${pet.tipo}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                data.zonas.forEach { zona ->
                    if (zona.tipo == "circulo" && zona.centro != null) {
                        Circle(
                            center = LatLng(zona.centro.lat, zona.centro.lng),
                            radius = zona.radioMetros ?: 100.0,
                            fillColor = Color(0x221976D2),
                            strokeColor = Color(0xFF1976D2),
                            strokeWidth = 2f
                        )
                    } else if (zona.tipo == "poligono" && zona.geometria != null) {
                        val puntos = zona.geometria.coordinates[0].map { coord ->
                            LatLng(coord[1], coord[0])
                        }
                        Polygon(
                            points = puntos,
                            fillColor = Color(0x22FF9800),
                            strokeColor = Color(0xFFFF9800),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }
    }
}