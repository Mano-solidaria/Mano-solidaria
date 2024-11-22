package com.mano_solidaria.app.solicitantes

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.mano_solidaria.app.solicitantes.ReservasRepository

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class ReservasActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VerReservasApp() }
    }

    @Composable
    fun VerReservasApp() {
        val navController = rememberNavController()
        val reservas = remember { mutableStateListOf<Reserva>() } // Asegúrate de que 'reservas' esté disponible

        // Cargar las reservas antes de navegar, o usar un estado de cargado
        LaunchedEffect(true) {
            val reservasList = ReservasRepository.getReservas()
            reservas.clear()
            reservas.addAll(reservasList)
        }

        NavHost(navController, startDestination = "list") {
            composable("list") { ReservasListScreen(navController) }
            composable("detail/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val reserva = reservas.find { it.id == itemId } // Comparar correctamente

                if (reserva != null) {
                    ReservaDetailScreen(reserva = reserva, navController = navController)
                } else {
                    // Manejo de error si no se encuentra la reserva (opcional)
                    Text("Reserva no encontrada")
                }
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservasListScreen(navController: NavController) {
        val reservas = remember { mutableStateListOf<Reserva>() }
        val geoPointsAMostrar = remember { mutableStateListOf<GeoPoint>() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val reservasList = ReservasRepository.getReservas()
                reservas.clear()
                reservas.addAll(reservasList)

                geoPointsAMostrar.clear()
                geoPointsAMostrar.addAll(reservasList.mapNotNull { reserva ->
                    reserva.ubicacionDonante
                })
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Reservas activas") }) }
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    MostrarMapaMultiplesPuntos(geoPointsAMostrar)
                }

                items(reservas.size) { index ->
                    ReservaItem(reservas[index]) {
                        // Pasamos la reserva completa al detalle, no solo el ID
                        navController.navigate("detail/${reservas[index].id}")
                    }
                }
            }
        }
    }

    @Composable
    fun ReservaItem(reserva: Reserva, onClick: (Reserva) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { onClick(reserva) })
                .padding(vertical = 8.dp)
                .border(1.dp, color = Color.Black)
        ) {
            AsyncImage(
                model = reserva.imagenURL,
                contentDescription = "Imagen de reserva",
                modifier = Modifier.size(80.dp).padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Peso reservado: ${reserva.pesoReservado} kg")
                Text("Distancia: ${reserva.distancia} km")
            }
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Donante: ${reserva.nombreDonante}")
                Text("Fecha de publicación de donación: ${reserva.tiempoInicial}")
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservaDetailScreen(reserva: Reserva, navController: NavController) {
        Scaffold(topBar = { TopAppBar(title = { Text("Detalle de la reserva") }) }) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Ahora ya tienes la reserva completa directamente
                Column {
                    ReservaDetails(reserva = reserva, navController = navController)
                }
            }
        }
    }

    @Composable
    fun ReservaDetails(reserva: Reserva, navController: NavController) {
        var showDialog by remember { mutableStateOf(false) } // estado para mostrar el dialogo
        var showSnackbar by remember { mutableStateOf(false) } // estado para mostrar el snackbar
        var showSnackbarModif by remember { mutableStateOf(false) } // estado para mostrar el snackbar de modificacion
        val snackbarHostState = remember { SnackbarHostState() } // host para el snackbar
        var showModifyDialog by remember { mutableStateOf(false) } // estado para mostrar el dialogo de modificar reserva
        var newReservaValue by remember { mutableStateOf("") } // estado para manejar el nuevo valor de la reserva


        LaunchedEffect(showSnackbar) {
            if (showSnackbar) {
                snackbarHostState.showSnackbar(
                    message = "Operación cancelada",
                    duration = SnackbarDuration.Short
                )
                showSnackbar = false // restablecer el estado despues de mostrar el mensaje
            }
        }

        LaunchedEffect(showSnackbarModif) {
            if (showSnackbarModif) {
                snackbarHostState.showSnackbar(
                    message = "Número no válido",
                    duration = SnackbarDuration.Short
                )
                showSnackbar = false
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    )
                    {
                        MostrarMapaUnPunto(reserva.ubicacionDonante)
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ){
                        AsyncImage(
                            model = reserva.imagenURL,
                            contentDescription = "Imagen de reserva",
                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // fila 1: nombre de alimento, peso reservado y rango de reserva
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Nombre de alimento: ${reserva.alimento}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Peso reservado: ${reserva.pesoReservado} kg",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text("Máxima reserva posible: ${reserva.rangoReserva} kg", modifier = Modifier.weight(1f).padding(8.dp))
                    }
                }

                // fila 2: distancia, fecha inicio de la donacion y donante
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Distancia: ${reserva.distancia} km",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Fecha de publicación de donación: ${reserva.tiempoInicial}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Donante: ${reserva.nombreDonante}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // fila 3: descripcion
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black)
                    ) {
                        Text(
                            "Descripción: ${reserva.descripcion}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // fila 5: palabra(s) clave
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black)
                    ) {
                        Text(
                            "Palabra(s) clave: ${reserva.palabraClave}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // boton de modificar reserva
                item {
                    BotonModificarReserva(
                        onModifyClick = {
                            showModifyDialog = true // mostrar el dialogo de modificar reserva
                        }
                    )
                }

                // boton de cancelar reserva
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                showDialog = true // Mostrar el dialogo cuando se presiona el botón
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar reserva")
                        }
                    }
                }
            }

            // mostrar el dialogo de confirmacion si showDialog es true
            if (showDialog) {
                ConfirmacionCancelarReservaDialog(
                    onConfirm = {
                        // logica para cancelar la reserva
                        ReservasRepository.cancelarReserva(reserva.id)

                        // mostrar el snackbar
                        showSnackbar = true

                        // volver a la pantalla anterior
                        navController.navigate("list") {
                            popUpTo("detail/{itemId}") { inclusive = true }
                        }

                        showDialog = false // cerrar dialogo despues de confirmacion
                    },
                    onDismiss = {
                        showDialog = false // cerrar dialogo si el usuario presiona no
                    }
                )
            }

            // mostrar el dialogo para modificar reserva
            if (showModifyDialog) {
                AlertDialog(
                    onDismissRequest = { showModifyDialog = false },
                    title = { Text("Modificar reserva") },
                    text = {
                        Column {
                            Text("Máxima reserva posible: mínimo 1 kg, máximo ${reserva.rangoReserva} kg")
                            TextField(
                                value = newReservaValue,
                                onValueChange = { newReservaValue = it },
                                label = { Text("Nuevo peso reservado") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newValue = newReservaValue.toLongOrNull()
                                if (newValue != null && newValue in 1..reserva.rangoReserva) {
                                    // si el valor es valido, modificar la reserva
                                    reserva.pesoReservado.toLongOrNull()?.let {
                                        ReservasRepository.modificarReserva(
                                            reserva.id,
                                            it,
                                            newValue
                                        )
                                    }

                                    // Actualizar la reserva localmente (solo la reserva modificada)
                                    reserva.pesoReservado = newValue.toString()

                                    showModifyDialog = false

                                } else {
                                    // si el valor no es valido, mostrar un mensaje
                                    showSnackbarModif = true
                                    showModifyDialog = false
                                }
                            }
                        ) {
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showModifyDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ConfirmacionCancelarReservaDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Confirmación") },
            text = { Text("¿Estás seguro de que deseás cancelar la reserva?") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("No")
                }
            }
        )
    }

    @Composable
    fun BotonModificarReserva(onModifyClick: () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onModifyClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Modificar reserva")
            }
        }
    }

    @Composable
    fun MostrarMapaUnPunto(geoPoint: GeoPoint) {
        val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
        val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(latLng, 15f)
        }

        GoogleMap(
            modifier = Modifier.height(250.dp),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
        ) {
            Marker(
                state = MarkerState(position = latLng),
                title = "Marker",
                snippet = "Lat: ${geoPoint.latitude}, Lng: ${geoPoint.longitude}"
            )
        }
    }

    @Composable
    fun MostrarMapaMultiplesPuntos(geoPoints: List<GeoPoint>) {
        val centroide = if (geoPoints.isNotEmpty()) {
            val latitudPromedio = geoPoints.map { it.latitude }.average()
            val longitudPromedio = geoPoints.map { it.longitude }.average()

            Log.d("GeoPointsInfo", "Cantidad de elementos en geoPoints: ${geoPoints.size}")
            Log.d("GeoPointsInfo", "Latitud promedio: $latitudPromedio")
            Log.d("GeoPointsInfo", "Longitud promedio: $longitudPromedio")


            GeoPoint(latitudPromedio, longitudPromedio)
        } else {
            Log.d("GeoPointsInfo", "geoPoints está vacío, usando valor predeterminado.")
            GeoPoint(0.0, 0.0) // Valor predeterminado si no hay puntos
        }
        val initialLatLng = LatLng(centroide.latitude, centroide.longitude)

        Log.d("GeoPointsInfo", "Contenido de initialLatLng: Lat: ${initialLatLng.latitude}, Lng: ${initialLatLng.longitude}")
        val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

        // Inicializar la camara
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
        }

        // Usar LaunchedEffect para actualizar la posicion de la cámara cuando initialLatLng cambie
        LaunchedEffect(initialLatLng) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
        }

        GoogleMap(
            modifier = Modifier.height(250.dp),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
        ) {
            geoPoints.forEach { geoPoint ->
                val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                Marker(
                    state = MarkerState(position = latLng),
                    title = "Marker",
                    snippet = "Lat: ${geoPoint.latitude}, Lng: ${geoPoint.longitude}"
                )
            }
        }
    }
}