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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mano_solidaria.app.AppBarWithDrawer
import com.mano_solidaria.app.R

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

        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        AppBarWithDrawer(
            title = when (currentRoute) {
                "list" -> stringResource(id = R.string.mis_reservas)
                "detail/{itemId}" -> stringResource(id = R.string.detalle_reserva)
                else -> stringResource(id = R.string.mis_reservas)
            },
            scaffoldState = scaffoldState,
            coroutineScope = coroutineScope
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavHost(navController, startDestination = "list") {
                    composable("list") { ReservasListScreen(navController) }
                    composable("detail/{itemId}") { backStackEntry ->
                        val itemId = backStackEntry.arguments?.getString("itemId")
                        val reserva = reservas.find { it.id == itemId }

                        if (reserva != null) {
                            ReservaDetailScreen(reserva = reserva, navController = navController)
                        } else {
                            Text(stringResource(id = R.string.reserva_no_encontrada))
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
        val geoPointsAMostrar = remember { mutableStateListOf<GeoPointWithAlimento>() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val reservasList = ReservasRepository.getReservas()
                reservas.clear()
                reservas.addAll(reservasList)

                geoPointsAMostrar.clear()
                geoPointsAMostrar.addAll(reservasList.mapNotNull { reserva ->
                    reserva.ubicacionDonante.let { geoPoint ->
                        GeoPointWithAlimento(geoPoint, reserva.alimento)
                    }
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
                .border(1.dp, color = MaterialTheme.colors.onBackground),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Columna para la imagen
            Box(
                modifier = Modifier
                    .weight(1f)  // 1/3 de la pantalla
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = reserva.imagenURL,
                    contentDescription = "Imagen de reserva",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp),
            ) {
                Text(
                    stringResource(id = R.string.nombre_alimento, reserva.alimento),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis, // Puntos suspensivos si excede
                )
                Text(
                    stringResource(id = R.string.peso_reservado, reserva.pesoReservado),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis, // Puntos suspensivos si excede
                )
                Text(
                    text = stringResource(id = R.string.distancia_lista, String.format("%.2f", reserva.distancia)),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis, // Puntos suspensivos si excede
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(1.dp),
            ) {
                Text(
                    stringResource(id = R.string.donante, reserva.nombreDonante),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis, // Puntos suspensivos si excede
                )
                Text(
                    stringResource(id = R.string.estado, reserva.estado),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Normal,
                    color = when (reserva.estado) {
                        "entregado" -> Color.Green
                        "cancelada" -> Color.Red
                        else -> MaterialTheme.colors.onBackground
                    },
                    maxLines = 1, // Limitar a 1 línea
                    overflow = TextOverflow.Ellipsis, // Puntos suspensivos si excede
                )
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservaDetailScreen(reserva: Reserva, navController: NavController) {
        Scaffold {
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


        val operacionCanceladaMessage = stringResource(id = R.string.operacion_cancelada)
        val numeroNoValidoMessage = stringResource(id = R.string.numero_no_valido)

        LaunchedEffect(showSnackbar) {
            if (showSnackbar) {
                snackbarHostState.showSnackbar(
                    message = operacionCanceladaMessage,
                    duration = SnackbarDuration.Short
                )
                showSnackbar = false // restablecer el estado despues de mostrar el mensaje
            }
        }

        LaunchedEffect(showSnackbarModif) {
            if (showSnackbarModif) {
                snackbarHostState.showSnackbar(
                    message = numeroNoValidoMessage,
                    duration = SnackbarDuration.Short
                )
                showSnackbarModif = false // restablecer el estado despues de mostrar el mensaje
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
                            stringResource(id = R.string.nombre_alimento, reserva.alimento),
                            modifier = Modifier.weight(1f).padding(8.dp),
                            fontWeight = FontWeight.Bold

                        )
                        Text(
                            stringResource(id = R.string.peso_reservado, reserva.pesoReservado),
                            modifier = Modifier.weight(1f).padding(8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(id = R.string.maxima_reserva, reserva.rangoReserva),
                            modifier = Modifier.weight(1f).padding(8.dp),
                            fontWeight = FontWeight.Bold)
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
                            text = stringResource(id = R.string.distancia, String.format("%.2f", reserva.distancia)),
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            stringResource(id = R.string.fecha_publicacion_donacion, reserva.tiempoInicial),
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                        Text(
                            stringResource(id = R.string.donante, reserva.nombreDonante),
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
                            stringResource(id = R.string.descripcion, reserva.descripcion),
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
                            stringResource(id = R.string.estado, reserva.estado),
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
                            stringResource(id = R.string.palabra_clave, reserva.palabraClave),
                            modifier = Modifier.weight(1f).padding(8.dp)
                        )
                    }
                }

                // Botón de modificar reserva
                if (reserva.estado == "pendiente") {
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
                                Text(stringResource(id = R.string.cancelar_reserva))
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
                    title = { Text(stringResource(id = R.string.modificar_reserva)) },
                    text = {
                        Column {
                            Text(
                                text = stringResource(id = R.string.max_reserva_posible, reserva.rangoReserva)
                            )
                            TextField(
                                value = newReservaValue,
                                onValueChange = { newReservaValue = it },
                                label = { Text(stringResource(id = R.string.nuevo_peso_reservado)) },
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
                            Text(stringResource(id = R.string.confirmar))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showModifyDialog = false }) {
                            Text(stringResource(id = R.string.cancelar))
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
            title = { Text(stringResource(id = R.string.confirmacion)) },
            text = { Text(stringResource(id = R.string.confirmacion_cancelacion) )},
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(id = R.string.si))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.no))
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
                Text(stringResource(id = R.string.modificar_reserva))
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
                snippet = "Lat: ${geoPoint.latitude}, Lng: ${geoPoint.longitude}"
            )
        }
    }

    @Composable
    fun MostrarMapaMultiplesPuntos(geoPoints: List<GeoPointWithAlimento>) {
        // Si geoPoints esta vacio, utilizar un valor predeterminado
        if (geoPoints.isEmpty()) {
            return
        }

        // Calcular latitudes y longitudes maximas y minimas
        val minLat = geoPoints.minOf { it.geoPoint.latitude }
        val maxLat = geoPoints.maxOf { it.geoPoint.latitude }
        val minLng = geoPoints.minOf { it.geoPoint.longitude }
        val maxLng = geoPoints.maxOf { it.geoPoint.longitude }

        // Calcular el centroide (promedio de las latitudes y longitudes)
        val latitudPromedio = geoPoints.map { it.geoPoint.latitude }.average()
        val longitudPromedio = geoPoints.map { it.geoPoint.longitude }.average()
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
                val latLng = LatLng(geoPoint.geoPoint.latitude, geoPoint.geoPoint.longitude)
                Marker(
                    state = MarkerState(position = latLng),
                    title = geoPoint.alimento,
                    snippet = "Lat: ${geoPoint.geoPoint.latitude}, Lng: ${geoPoint.geoPoint.longitude}"
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