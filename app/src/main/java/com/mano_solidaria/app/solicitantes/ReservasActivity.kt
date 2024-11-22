package com.mano_solidaria.app.solicitantes

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mano_solidaria.app.AppBarWithDrawer

class ReservasActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VerReservasApp() }
    }

    @Composable
    fun VerReservasApp() {
        val navController = rememberNavController()
        val reservas = remember { mutableStateListOf<Reserva>() }
        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
        val coroutineScope = rememberCoroutineScope()

        // Cargar las reservas antes de navegar, o usar un estado de cargado
        LaunchedEffect(true) {
            val reservasList = ReservasRepository.getReservas()
            reservas.clear()
            reservas.addAll(reservasList)
        }

        AppBarWithDrawer(
            title = "Mis reservas",
            scaffoldState = scaffoldState,
            coroutineScope = coroutineScope
        ) { paddingValues ->
            // Asegúrate de que el contenido tenga padding para evitar que quede cubierto por el drawer
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(navController, startDestination = "list") {
                    composable("list") { ReservasListScreen(navController) }
                    composable("detail/{itemId}") { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId")
                        val reserva = reservas.find { it.id == itemId }

                        if (reserva != null) {
                            ReservaDetailScreen(reserva = reserva, navController = navController)
                        } else {
                            Text("Reserva no encontrada")
                        }
                    }
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
            //topBar = { TopAppBar(title = { Text("Reservas activas") }) }
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
                .padding(vertical = 8.dp) // Relleno horizontal y vertical
                .border(1.dp, color = MaterialTheme.colors.onBackground) // Bordes
                //.background(Color.White) // Fondo blanco
        ) {
            // Columna para la imagen
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp),
                verticalArrangement = Arrangement.Center, // Centrado verticalmente
                //horizontalAlignment = Alignment.CenterHorizontally // Centrado horizontalmente
            ) {
                AsyncImage(
                    model = reserva.imagenURL,
                    contentDescription = "Imagen de reserva",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Columna para la información de la reserva (peso, distancia, estado)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp)
            ) {
                Text(
                    text = "Peso reservado: ${reserva.pesoReservado} kg",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Distancia: ${reserva.distancia} km",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "Estado: ${reserva.estado}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Normal,
                    color = when (reserva.estado) {
                        "entregado" -> Color.Green
                        "cancelada" -> Color.Red
                        else -> MaterialTheme.colors.onBackground
                    }
                )
            }

            // Columna para la información del donante y la fecha de publicación
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp)
            ) {
                Text(
                    text = "Donante: ${reserva.nombreDonante}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis // Puntos suspensivos si excede
                )
                Text(
                    text = "Fecha de publicación: ${reserva.tiempoInicial}",
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservaDetailScreen(reserva: Reserva, navController: NavController) {
        Scaffold(topBar = { TopAppBar(title = { Text("Detalle de la reserva") }) }) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                            .border(1.dp, color = MaterialTheme.colors.onBackground),
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
                            .border(1.dp, color = MaterialTheme.colors.onBackground),
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
                            .border(1.dp, color = MaterialTheme.colors.onBackground)
                    ) {
                        Text(
                            "Descripción: ${reserva.descripcion}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // fila estado
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = MaterialTheme.colors.onBackground)
                    ) {
                        Text(
                            "Estado: ${reserva.estado}",
                            modifier = Modifier.weight(1f).padding(8.dp),
                            color = when (reserva.estado) {
                                "entregado" -> Color.Green
                                "cancelada" -> Color.Red
                                else -> MaterialTheme.colors.onBackground
                            }
                        )
                    }
                }

                // fila 5: palabra(s) clave
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, color = MaterialTheme.colors.onBackground)
                    ) {
                        Text(
                            "Palabra(s) clave: ${reserva.palabraClave}",
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // Botón de modificar reserva
                if (reserva.estado == "reservado") {
                    item {
                        BotonModificarReserva(
                            onModifyClick = {
                                showModifyDialog = true // mostrar el diálogo de modificar reserva
                            }
                        )
                    }

                    // Botón de cancelar reserva
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
        // Si geoPoints esta vacio, utilizar un valor predeterminado
        if (geoPoints.isEmpty()) {
            return
        }

        // Calcular latitudes y longitudes maximas y minimas
        val minLat = geoPoints.minOf { it.latitude }
        val maxLat = geoPoints.maxOf { it.latitude }
        val minLng = geoPoints.minOf { it.longitude }
        val maxLng = geoPoints.maxOf { it.longitude }

        // Calcular el centroide (promedio de las latitudes y longitudes)
        val latitudPromedio = geoPoints.map { it.latitude }.average()
        val longitudPromedio = geoPoints.map { it.longitude }.average()
        val centroide = GeoPoint(latitudPromedio, longitudPromedio)

        // Calcular el nivel de zoom basado en la distancia
        val latitudDiff = maxLat - minLat
        val longitudDiff = maxLng - minLng

        // Usamos una firmula simple para estimar el zoom segun la distancia
        val maxDiff = maxOf(latitudDiff, longitudDiff)
        val zoomLevel = calculateZoomLevel(maxDiff)

        // Inicializar el LatLng para la camara
        val initialLatLng = LatLng(centroide.latitude, centroide.longitude)

        // Configuracion de la cámara
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialLatLng, zoomLevel)
        }

        // Usar LaunchedEffect para actualizar la camara cuando se cambien los puntos
        LaunchedEffect(initialLatLng) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLatLng, zoomLevel)
        }

        GoogleMap(
            modifier = Modifier.height(250.dp),
            cameraPositionState = cameraPositionState,
        ) {
            // Agregar los marcadores al mapa
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

    // Funcion para calcular el nivel de zoom basado en la diferencia maxima de latitud o longitud
    fun calculateZoomLevel(diff: Double): Float {
        return when {
            diff < 0.01 -> 16f // Muy cerca
            diff < 0.05 -> 14f // Distancia media
            diff < 0.1 -> 12f  // Distancia mas lejana
            else -> 10f      // Muy distante
        }
    }

}