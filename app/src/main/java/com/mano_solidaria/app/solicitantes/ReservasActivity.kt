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
        NavHost(navController, startDestination = "list") {
            composable("list") { ReservasListScreen(navController) }
            composable("detail/{itemId}") { backStackEntry ->
                ReservaDetailScreen(backStackEntry.arguments?.getString("itemId"),
                    navController = navController)
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservasListScreen(navController: NavController) {
        val reservas = remember { mutableStateListOf<Reserva>() }
        val geoPointsAMostrar = remember { mutableStateListOf<GeoPoint>() } // Estado para los GeoPoints
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val reservasList = ReservasRepository.getReservas()
                reservas.clear()
                reservas.addAll(reservasList)

                // Extraer los GeoPoints desde la lista de reservas
                geoPointsAMostrar.clear()
                geoPointsAMostrar.addAll(reservasList.mapNotNull { reserva ->
                    reserva.ubicacionDonante // Suponiendo que es de tipo GeoPoint
                })
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Reservas activas") }) }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                MostrarMapaMultiplesPuntos(geoPointsAMostrar) // Ahora es accesible
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(reservas.size) { index ->
                        ReservaItem(reservas[index]) {
                            navController.navigate("detail/${reservas[index].id}")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ReservaItem(reserva: Reserva, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                Text("Peso reservado: ${reserva.pesoReservado}")
                Text("Distancia: ${reserva.distancia}")
            }
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Donante: ${reserva.nombreDonante}")
                Text("Inicio de donación: ${reserva.tiempoInicial}")
            }
        }
        //Divider()
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservaDetailScreen(itemId: String?, navController: NavController) {
        var navController = navController
        var reserva by remember { mutableStateOf<Reserva?>(null) }
        //var diasRestantes by remember { mutableIntStateOf(1) }
        val context = LocalContext.current

        LaunchedEffect(itemId) {
            itemId?.let {
                reserva = ReservasRepository.getReservaById(it)
            }
        }

        Scaffold(topBar = { TopAppBar(title = { Text("Detalle de la reserva") }) }) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                reserva?.let {
                    Column {
                        MostrarMapaUnPunto(reserva!!.ubicacionDonante)
                        AsyncImage(
                            model = it.imagenURL,
                            contentDescription = "Imagen de reserva",
                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp),
                            contentScale = ContentScale.Crop
                        )
                        ReservaDetails(reserva!!, navController)
                    }
                } ?: run {
                    Text("Cargando detalles...")
                }
            }
        }
    }

    @Composable
    fun ReservaDetails(reserva: Reserva, navController: NavController) {
        var showDialog by remember { mutableStateOf(false) } // Estado para mostrar el diálogo
        var showSnackbar by remember { mutableStateOf(false) } // Estado para mostrar el Snackbar
        var showSnackbarModif by remember { mutableStateOf(false) } // Estado para mostrar el Snackbar de modificacion
        val snackbarHostState = remember { SnackbarHostState() } // Host para el Snackbar
        var showModifyDialog by remember { mutableStateOf(false) } // Estado para mostrar el diálogo de modificar reserva
        var newReservaValue by remember { mutableStateOf("") } // Estado para manejar el nuevo valor de la reserva


        LaunchedEffect(showSnackbar) {
            if (showSnackbar) {
                snackbarHostState.showSnackbar(
                    message = "Operación cancelada",
                    duration = SnackbarDuration.Short // Duración extendida
                )
                showSnackbar = false // Restablecer el estado después de mostrar el mensaje
            }
        }

        LaunchedEffect(showSnackbarModif) {
            if (showSnackbarModif) {
                snackbarHostState.showSnackbar(
                    message = "Número no válido",
                    duration = SnackbarDuration.Short
                )
                showSnackbar = false // Restablecer el estado después de mostrar el mensaje
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado entre los elementos
            ) {
                // Fila 1: Alimento y Reservado
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Alimento: ${reserva.alimento}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Peso reservado: ${reserva.pesoReservado}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text("Rango reserva: ${reserva.rangoReserva}", modifier = Modifier.weight(1f).padding(8.dp))
                    }
                }

                // Fila 2: Distancia, inicio de la donación y donante
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Distancia: ${reserva.distancia}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Inicio de donación: ${reserva.tiempoInicial}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            "Donante: ${reserva.nombreDonante}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // Fila 3: Descripción
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

                // Fila 5: Palabra clave
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = Color.Black)
                    ) {
                        Text(
                            "Palabra clave: ${reserva.palabraClave}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // Fila 6: Botón de Cancelar Reserva
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                showDialog = true // Mostrar el diálogo cuando se presiona el botón
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar reserva")
                        }
                    }
                }

                // Fila 7: Botón de Modificar Reserva
                item {
                    BotonModificarReserva(
                        onModifyClick = {
                            showModifyDialog = true // Mostrar el diálogo de modificar reserva
                        }
                    )
                }
            }

            // Mostrar el diálogo de confirmación si showDialog es true
            if (showDialog) {
                ConfirmacionCancelarReservaDialog(
                    onConfirm = {
                        // Lógica para cancelar la reserva
                        ReservasRepository.cancelarReserva(reserva.id)

                        // Mostrar el Snackbar
                        showSnackbar = true

                        // Regresar a la pantalla anterior
                        navController.navigate("list") {
                            popUpTo("detail/{itemId}") { inclusive = true }
                        }

                        showDialog = false // Cerrar el diálogo después de la confirmación
                    },
                    onDismiss = {
                        showDialog = false // Cerrar el diálogo si el usuario presiona "No"
                    }
                )
            }

            // Mostrar el diálogo para modificar reserva
            if (showModifyDialog) {
                AlertDialog(
                    onDismissRequest = { showModifyDialog = false },
                    title = { Text("Modificar reserva") },
                    text = {
                        Column {
                            Text("Rango de reserva: ${reserva.rangoReserva} kg")
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
                                    // Si el valor es válido, modificar la reserva
                                    reserva.pesoReservado.toLongOrNull()?.let {
                                        ReservasRepository.modificarReserva(
                                            reserva.id,
                                            it,
                                            newValue
                                        )
                                    }
                                    showModifyDialog = false
                                    navController.navigate("list") {
                                        popUpTo("detail/{itemId}") { inclusive = true }
                                    }
                                } else {
                                    // Si el valor es inválido, mostrar un mensaje
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
            text = { Text("¿Estás seguro de que deseas cancelar la reserva?") },
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
            modifier = Modifier.height(250.dp).padding(16.dp),
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

        // Inicializar la cámara
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
        }

        // Usar LaunchedEffect para actualizar la posición de la cámara cuando initialLatLng cambie
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