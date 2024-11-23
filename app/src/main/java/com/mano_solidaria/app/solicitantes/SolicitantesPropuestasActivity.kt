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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.common.server.converter.StringToIntConverter
import com.mano_solidaria.app.donadores.Donacion
import com.mano_solidaria.app.donadores.DonacionRoko
import com.mano_solidaria.app.donadores.SolicitantesPropuestasRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async


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
fun ViewContainer(navController: NavHostController) {
    val donadores = remember { mutableStateListOf<DonacionRoko>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        scope.launch {
            val donacionesList = SolicitantesPropuestasRepository.getAllDonaciones()
            donadores.clear()
            donadores.addAll(donacionesList)
        }
    }
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
                model = "https://peruretail.sfo3.cdn.digitaloceanspaces.com/wp-content/uploads/Pollo-a-al-abrasa.jpg",
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
fun DetalleReservaScreen(id: String) {
    val donacion : DonacionRoko? = null
    val scope = rememberCoroutineScope()
    var response : DonacionRoko? = null

    LaunchedEffect(true) {
        scope.launch {
            val deferred : Deferred<DonacionRoko?> = async { SolicitantesPropuestasRepository.getDonacionById(id) }
            response = deferred.await()
        }
    }

    DetalleReservaScreen2(response)


}



@Composable
fun DetalleReservaScreen2(donacion: DonacionRoko?) {
    ToolBar()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            AsyncImage(
                model = "https://peruretail.sfo3.cdn.digitaloceanspaces.com/wp-content/uploads/Pollo-a-al-abrasa.jpg",
                contentDescription = "Imagen de reserva",
                modifier = Modifier
                    .padding(8.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        // First row of 3 fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Alimento: ${donacion?.descripcion ?: "null"}")
            Text(text = "Peso restante:")
            Text(text = "Peso total: ${donacion?.pesoAlimento ?: "null"}")
        }

        // Second row of 3 fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Coto ",)
            Text(text = "Coto ",)
            Text(text = "Coto ",)
        }

        // Button
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Reservar")
        }
    }
}

