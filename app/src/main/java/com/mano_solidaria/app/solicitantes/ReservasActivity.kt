package com.mano_solidaria.app.solicitantes

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import com.mano_solidaria.app.solicitantes.ReservasRepository

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
                ReservaDetailScreen(backStackEntry.arguments?.getString("itemId"))
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservasListScreen(navController: NavController) {
        val reservas = remember { mutableStateListOf<Reserva>() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val reservasList = ReservasRepository.getReservas()
                reservas.clear()
                reservas.addAll(reservasList)
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Reservas activas") }) }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

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
//               Text(reserva.id)
            }
            Column(modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Donante: ${reserva.nombreDonante}")
                Text("Tiempo restante: ${reserva.tiempoRestante}")
            }
        }
        Divider()
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun ReservaDetailScreen(itemId: String?) {
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
                        AsyncImage(
                            model = it.imagenURL,
                            contentDescription = "Imagen de reserva",
                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp),
                            contentScale = ContentScale.Crop
                        )
                        ReservaDetails(reserva!!)
                    }
                } ?: run {
                    Text("Cargando detalles...")
                }
            }
        }
    }

    @Composable
    fun ReservaDetails(reserva: Reserva) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila 1: Alimento y Reservado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Alimento: ${reserva.alimento}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
                Text("Peso reservado: ${reserva.pesoReservado}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
                Text("", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Fila 2: Distancia, Duración y Donante
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Distancia: ${reserva.distancia}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
                Text("Duración: ${reserva.tiempoRestante}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
                Text("Donante: ${reserva.nombreDonante}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Fila 3: Descripción
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Descripción: ${reserva.descripcion}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Fila 4: Tiempo restante
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Tiempo restante para retirar: ${reserva.tiempoRestante}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Fila 5: Palabra clave
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Palabra clave: ${reserva.palabraClave}", modifier = Modifier
                    .weight(1f)
                    .padding(8.dp))
            }
        }
    }

}