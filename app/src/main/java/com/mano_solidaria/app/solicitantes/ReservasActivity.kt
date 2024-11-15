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
//            AsyncImage(
//                model = reserva.imagenUrl,
//                contentDescription = "Imagen de reserva",
//                modifier = Modifier.size(80.dp).padding(end = 8.dp),
//                contentScale = ContentScale.Crop
//            )
            Column(modifier = Modifier.weight(1f)) {
//                Text(reserva.pesoReservado)
//                Text("Distancia")
                  Text(reserva.id)
                  Text(reserva.nombreDonante)
            }
            Column(modifier = Modifier.weight(1f)) {
//                Text("Nombre donante")
//                Text("Tiempo restante")
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
//                        AsyncImage(
//                            model = it.imagenUrl,
//                            contentDescription = "Imagen de reserva",
//                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp),
//                            contentScale = ContentScale.Crop
//                        )
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
        Column {
            Text("Alimento: Nombre")
            Text("Reservado: ${reserva.pesoReservado}")
            Text("Distancia: (distancia)")
            Text("Duracion: (duracion)")
            Text("Donante: (donante)")
            Text("Descripcion: (descripcion) ")
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}