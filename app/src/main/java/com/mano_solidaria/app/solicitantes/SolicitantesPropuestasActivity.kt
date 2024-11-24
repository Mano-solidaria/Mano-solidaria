package com.mano_solidaria.app.solicitantes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.imageLoader
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mano_solidaria.app.AppBarWithDrawer
import com.mano_solidaria.app.donadores.DonacionRoko
import com.mano_solidaria.app.donadores.ReservaRoko
import com.mano_solidaria.app.donadores.SolicitantesPropuestasRepository
import com.mano_solidaria.app.donadores.UsuarioRoko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.internal.wait
import kotlin.random.Random


private lateinit var _usuarioCurrent: UsuarioRoko

class SolicitantesPropuestasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, NotificationServiceSolicitante::class.java)
        startService(intent)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "reservas"
        ) {
            composable("reservas") {
                ViewContainer(navController)
            }
            composable(
                "detalle/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                DetalleReservaScreen(idDonacion = id ?: "Sin ID")
            }
        }
    }

    @Composable
    fun ViewContainer(
        navController: NavHostController,
        viewModel: SolicitantesPropuestasRepository = SolicitantesPropuestasRepository
    ) {
        val donadores = remember { mutableStateListOf<DonacionRoko>() }
        val scope = rememberCoroutineScope()

        val stateUsuarioActual = viewModel.usuario.collectAsState()

        LaunchedEffect(true) {
            scope.launch {
                val donacionesList = viewModel.getAllDonaciones()
                donadores.clear()
                donadores.addAll(donacionesList)
                viewModel.getUserById(viewModel.currentUser())
            }
        }

        _usuarioCurrent = stateUsuarioActual.value
        val scaffoldState = rememberScaffoldState()  // Agregar ScaffoldState

        // Usamos AppBarWithDrawer en lugar de Scaffold directamente
        AppBarWithDrawer(
            scaffoldState = scaffoldState,
            coroutineScope = scope,
            title = "Solicitantes Propuestas",
            content = { innerPadding ->
                Content(
                    innerPadding,
                    donadores,
                    navController,
                    stateUsuarioActual.value.usuarioUbicacion
                )
            }
        )
    }

    @Composable
    fun ToolBar() {
        TopAppBar(
            title = { Text("Top app bar") }
        )
    }

    @Composable
    fun Content(
        paddingValues: PaddingValues,
        donadores: SnapshotStateList<DonacionRoko>,
        navController: NavController,
        usuarioUbicacion: GeoPoint
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item (_usuarioCurrent.usuarioUbicacion.toString()) {
                MyGoogleMaps(usuarioUbicacion)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item (_usuarioCurrent.imagenUrl) {
                MyReservaRealizadas(_usuarioCurrent)
                Spacer(modifier = Modifier.height(16.dp))
            }
            // Generar dinámicamente los elementos según la lista
            items(donadores) { donacion ->
                MyReservaDisponibles(donacion, navController)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }


    @Composable
    fun MyGoogleMaps(geoPoint: GeoPoint) {
        val address = LatLng(geoPoint.latitude, geoPoint.longitude)
        val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        val mapHeight = screenHeight / 3

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(address, 15f)
        }
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight),
            cameraPositionState = cameraPositionState,
            uiSettings = uiSettings,
        ) {
            Marker(
                state = MarkerState(position = address),
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

            GeoPoint(latitudPromedio, longitudPromedio)
        } else {
            GeoPoint(0.0, 0.0)
        }
        val initialLatLng = LatLng(centroide.latitude, centroide.longitude)

        val uiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true)) }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialLatLng, 12f)
        }

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


    @Composable
    fun MyReservaRealizadas(usuarioRoko: UsuarioRoko) {
        val context = LocalContext.current
        Button(
            onClick = {
                val intent = Intent(context, ReservasActivity::class.java)
                context.startActivity(intent)
            },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter =
                    rememberAsyncImagePainter(
                        model =
                        _usuarioCurrent.imagenUrl
                            ?: "https://i.pinimg.com/originals/9a/dd/74/9add7496fe5ec85b9dd52a0066873f62.jpg"
                    ), // URL de la foto
                    contentDescription = "Foto del usuario",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                Text("Ver Reservas Realizadas")
            }
        }
    }


    @Composable
    fun MyReservaDisponibles(donacion: DonacionRoko, navController: NavController) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, color = Color.Red)
                .height(90.dp)
                .clickable {
                    navController.navigate("detalle/${donacion.id.id}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = donacion.imagenUrl,
                    contentDescription = "Imagen de reserva",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Alimento: ${donacion.descripcion} ${donacion.pesoTotal}",)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Caduca en: ${donacion.tiempoRestante}",)
                }
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "Coto")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "7 km",)
                }
            }
        }
    }



    @Composable
    fun DetalleReservaScreen(
        idDonacion: String,
        viewModel: SolicitantesPropuestasRepository = SolicitantesPropuestasRepository
    ) {
        // Estado para controlar el trigger de recomposición
        var reloadTrigger by remember { mutableStateOf(0) }

        // Estado para almacenar el peso restante
        var pesoRestante by remember { mutableStateOf(0) }

        val stateDonaciones = viewModel.donaciones.collectAsState()
        val stateUsuarioActual = viewModel.usuario.collectAsState()
        val donaciones = stateDonaciones.value

        val donacion: DonacionRoko? = donaciones.find { it.id.id == idDonacion }

        var value by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var isSuscriptor by remember { mutableStateOf(true) }
        var donador by remember { mutableStateOf(UsuarioRoko()) }
        var botonText by remember { mutableStateOf("suscribirse") }
        var buttonColor by remember { mutableStateOf("MaterialTheme.colors.primary") }

        var suscriptorRefEncontrada: DocumentReference?


        /*LaunchedEffect(true) {
            lifecycleScope.launch {
                donador = viewModel.getDonadorByRef(donacion!!.donanteId)!!
                donacion?.let {
                    donador = viewModel.getDonadorByRef(it.donanteId) ?: UsuarioRoko()
                }
            }
        }

        stateUsuarioActual.value.usuarioDocumentRef*/

        suscriptorRefEncontrada = donador.suscriptores.find { suscriptor ->
            Log.d("Donador", donador.usuarioDocumentRef.toString())
            Log.d("esSuscriptor", stateUsuarioActual.value.usuarioDocumentRef.toString())
            suscriptor == stateUsuarioActual.value.usuarioDocumentRef
        }

        Log.d("Donador", donador.usuarioDocumentRef.toString())
        Log.d("RefUsuario", stateUsuarioActual.value.usuarioDocumentRef.toString())


        LaunchedEffect(reloadTrigger) {
            // Actualiza el donador y el estado de la suscripción cada vez que cambia reloadTrigger
            donador = donacion?.donanteId?.let { viewModel.getDonadorByRef(it) } ?: UsuarioRoko()
            val suscriptorRefEncontrada = donador.suscriptores.find {
                it == stateUsuarioActual.value.usuarioDocumentRef
            }
            isSuscriptor = suscriptorRefEncontrada != null
            botonText = if (isSuscriptor) "desuscribirse del donador" else "suscribirse al donador"
        }


        val imagenReserva = donacion?.imagenUrl
            ?: "https://peruretail.sfo3.cdn.digitaloceanspaces.com/wp-content/uploads/Pollo-a-al-abrasa.jpg"
        /*pesoRestante =
            try {
                val total: Int = donacion?.pesoTotal ?: 0
                val reservado: Int = donacion?.pesoReservado ?: 0
                val entregado: Int = donacion?.pesoEntregado ?: 0
                maxOf((total - reservado - entregado), 0)
            } catch (e: Exception) {
                0
            }*/

        LaunchedEffect(reloadTrigger) {
            // Actualiza el peso restante cada vez que reloadTrigger cambia
            pesoRestante =
                try {
                    val total: Int = donacion?.pesoTotal ?: 0
                    val reservado: Int = donacion?.pesoReservado ?: 0
                    val entregado: Int = donacion?.pesoEntregado ?: 0
                    maxOf((total - reservado - entregado), 0)
                } catch (e: Exception) {
                    0
                }
        }

        // Efecto lanzado para cargar el donador y comprobar la suscripción
        LaunchedEffect(donacion) {
            if (donacion != null) {
                val fetchedDonador = viewModel.getDonadorByRef(donacion.donanteId)
                fetchedDonador?.let { donadorData ->
                    donador = donadorData

                    val suscriptorRefEncontrada = donadorData.suscriptores.find {
                        it == stateUsuarioActual.value.usuarioDocumentRef
                    }

                    if (suscriptorRefEncontrada == null) {
                        botonText = "suscribirse al donador"
                        isSuscriptor = false
                    } else {
                        botonText = "desuscribirse del donador"
                        isSuscriptor = true
                    }
                }
            }
        }

        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()

        ToolBar()
        AppBarWithDrawer(
            scaffoldState = scaffoldState,
            coroutineScope = coroutineScope,
            title = "Detalle de Reserva",
            content = { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .heightIn(max = 300.dp),
                        ) {
                            AsyncImage(
                                model = imagenReserva,
                                contentDescription = "Imagen de reserva",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 8.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    item (pesoRestante) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(text = "Alimento:")
                                Text(
                                    text = donacion?.alimentoNombre ?: "null",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.wrapContentWidth().padding(4.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Peso restante:")
                                Text(
                                    text = pesoRestante.toString(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.wrapContentWidth().padding(4.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Peso total:")
                                Text(
                                    text = donacion?.pesoTotal.toString(),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.wrapContentWidth().padding(4.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    item (isSuscriptor) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "Donante:")
                                Text(
                                    text = donador.usuarioNombre ?: "Nombre no encontrado",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.wrapContentWidth().padding(4.dp),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = {
                                    if (suscriptorRefEncontrada == null) {
                                        viewModel.suscribirseAlDonador(
                                            donador,
                                            stateUsuarioActual.value.usuarioDocumentRef
                                        )
                                    } else {
                                        viewModel.desuscribirseAlDonador(
                                            donador,
                                            stateUsuarioActual.value.usuarioDocumentRef
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(IntrinsicSize.Min)
                                    .padding(start = 16.dp)
                            ) {
                                Text(botonText)
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 15.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = "${donacion?.descripcion}")
                                Text(text = "- ${donacion?.estado}")
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(text = "kilos a reservar")
                                TextField(
                                    value = value,
                                    onValueChange = { newValue ->
                                        val filteredValue =
                                            newValue.filter { it.isDigit() }.trimStart { it == '0' }
                                        value = filteredValue.ifEmpty { "" }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = isError
                                )
                                if (isError) {
                                    Text(
                                        text = "cantidad invalida",
                                        style = MaterialTheme.typography.subtitle1
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val numericValue = value.toIntOrNull() ?: 0
                                if ((numericValue > (donacion!!.pesoTotal - donacion!!.pesoReservado - donacion!!.pesoEntregado)) or (numericValue == 0)) {
                                    isError = true
                                } else {
                                    isError = false
                                    val reserva = ReservaRoko(
                                        dispararNoti = true,
                                        donacionId = donacion!!.id,
                                        donanteId = donacion.donanteId,
                                        estado = "pendiente",
                                        notiRecibida = false,
                                        palabraClave = generateRandomWord(8),
                                        pesoReservado = value.toInt(),
                                        usuarioReservador = viewModel.usuario.value.usuarioDocumentRef!!
                                    )
                                    CoroutineScope(Dispatchers.Main).launch {
                                        reloadTrigger++
                                        viewModel.addReservaInDb(reserva, donacion.id)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            Text("Reservar")
                        }
                    }
                }
            })
    }
}


fun generateRandomWord(length: Int): String {
    val chars = ('a'..'z') + ('A'..'Z') // Letras mayúsculas y minúsculas
    return (1..length)
        .map { Random.nextInt(chars.size) } // Genera índices aleatorios
        .map(chars::get) // Obtiene el carácter correspondiente
        .joinToString("") // Une los caracteres en una cadena
}



