package com.mano_solidaria.app.solicitantes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mano_solidaria.app.donadores.DonacionRoko
import com.mano_solidaria.app.donadores.SolicitantesPropuestasRepository
import kotlinx.coroutines.launch


class SolicitantesPropuestasActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
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
            DetalleReservaScreen(id = id ?: "Sin ID")
        }
    }
}


@Composable
fun ViewContainer(navController: NavHostController, viewModel: SolicitantesPropuestasRepository = SolicitantesPropuestasRepository) {
    val donadores = remember { mutableStateListOf<DonacionRoko>() }
    val scope = rememberCoroutineScope()

    val stateUsuarioActual = viewModel.usuario.collectAsState()

    LaunchedEffect(true) {
        scope.launch {
            val donacionesList = SolicitantesPropuestasRepository.getAllDonaciones()
            donadores.clear()
            donadores.addAll(donacionesList)
            SolicitantesPropuestasRepository.getUserById(SolicitantesPropuestasRepository.currentUser())
        }
    }

    val usuarioActual = stateUsuarioActual.value

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {ToolBar()}
    ){innerPadding ->
        Content(innerPadding, donadores, navController)
    }
}


@Preview
@Composable
fun ToolBar(){
    TopAppBar (
        title = {
            Text("Top app bar")
        }
    )
}


@Composable
fun Content(paddingValues: PaddingValues,
            donadores: SnapshotStateList<DonacionRoko>,
            navController: NavController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ){
        item {
            MyGoogleMaps()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        item {
            MyReservaRealizadas()
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
        println(donadores)
        // Generar dinámicamente los elementos según la lista
        items(donadores) { donacion ->
            MyReservaDisponibles(donacion, navController) // Tu componente para representar una donación
            Spacer(modifier = Modifier.height(16.dp)) // Espacio entre ítems
        }
    }
}


@Composable
fun MyGoogleMaps() {
    val address = LatLng(0.0, 0.0)
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
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}


@Composable
fun MyReservaRealizadas(){
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, ReservasActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Text("Ver Reservas Realizadas")
    }
}




@Composable
fun MyReservaDisponibles(donacion: DonacionRoko,
                         navController: NavController // Recibe el NavController para navegar
    ){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color = Color.Red)
            .clickable {
                navController.navigate("detalle/${donacion.id}")
            },
        verticalAlignment = Alignment.CenterVertically
    ){
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ){
            AsyncImage(
                model = donacion.imagenUrl,
                contentDescription = "Imagen de reserva",
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
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
                Text(
                    text = "Alimento: ${donacion.descripcion} ${donacion.pesoAlimento}",
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Caduca en: ${donacion.tiempoRestante}",
                )
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
                Text(
                    text = "Coto"
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "7 km",
                )
            }
        }
    }
}










@Composable
fun DetalleReservaScreen(id: String, viewModel: SolicitantesPropuestasRepository = SolicitantesPropuestasRepository) {

    val stateDonaciones = viewModel.donaciones.collectAsState()
    val donaciones = stateDonaciones.value
    val donacion = donaciones.find { it.id == id }

    var value by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val imagenReserva = donacion?.imagenUrl ?: "https://peruretail.sfo3.cdn.digitaloceanspaces.com/wp-content/uploads/Pollo-a-al-abrasa.jpg"
    val alimento = ""
    val pesoRestante = ""
    val pesoTotal = ""
    val distDireccion = ""
    val duracionRestante = ""
    val donante = ""

    ToolBar()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .border(1.dp, color = Color.Red),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            AsyncImage(
                model = imagenReserva,
                contentDescription = "Imagen de reserva",
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        // First row of 3 fields
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, color = Color.Red),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Alimento:")
                Text(text = donacion?.descripcion ?: "null",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp), // Ajusta el ancho del texto y añade un pequeño padding
                    maxLines = 3, // Permite hasta 3 líneas (puedes ajustarlo según el diseño)
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Peso restante:")
                // Aquí puedes agregar un valor real si está disponible
                Text(text = "N/A",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp), // Ajusta el ancho del texto y añade un pequeño padding
                    maxLines = 3, // Permite hasta 3 líneas (puedes ajustarlo según el diseño)
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Peso total:")
                Text(text = donacion?.pesoAlimento ?: "null",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp), // Ajusta el ancho del texto y añade un pequeño padding
                    maxLines = 3, // Permite hasta 3 líneas (puedes ajustarlo según el diseño)
                    overflow = TextOverflow.Ellipsis
                )
            }
        }


        // Second row of 3 fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Distancia direccion:")
                Text(text = donacion?.descripcion ?: "null",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp), // Ajusta el ancho del texto y añade un pequeño padding
                    maxLines = 3, // Permite hasta 3 líneas (puedes ajustarlo según el diseño)
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Duracion restante:")
                Text(text = "N/A",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Donante:")
                Text(text = donacion?.pesoAlimento ?: "null",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(4.dp), // Ajusta el ancho del texto y añade un pequeño padding
                    maxLines = 3, // Permite hasta 3 líneas (puedes ajustarlo según el diseño)
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ){
            Text(text = "Descripcion: (vencimiento, estado, detalles):")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        )   {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ){
                Text(text = "kilos a reservar")
                TextField(
                    value = value,
                    onValueChange = {
                        /*value = it*/
                            newValue ->
                        // Permite solo números y elimina ceros a la izquierda
                        val filteredValue = newValue.filter { it.isDigit() }.trimStart { it == '0' }
                        value = filteredValue.ifEmpty { "" }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError
                )
                if (isError) {
                    Text(
                        text = "errorMessage",
                        color = Color.Red,
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }
        }
        // Button
        Button(
            onClick = {
                val numericValue = value.toIntOrNull() ?: 0
                if (numericValue > 15) {
                    isError = true
                    println("Numero mayor a 15")
                } else {
                    isError = false
                    println("Numero menoooooooor a 15")
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Reservar")
        }
    }
}



