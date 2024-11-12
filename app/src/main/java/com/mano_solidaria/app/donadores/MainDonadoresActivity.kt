// MainDonadoresActivity.kt
package com.mano_solidaria.app.donadores
import androidx.compose.ui.platform.LocalContext

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.delay


class MainDonadoresActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MainDonadoresApp() }
    }

    @Composable
    fun MainDonadoresApp() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = "list") {
            composable("list") { DonadoresListScreen(navController) }
            composable("detail/{itemId}") { backStackEntry ->
                DonadorDetailScreen(backStackEntry.arguments?.getString("itemId"))
            }
        }
    }

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun DonadoresListScreen(navController: NavController) {
        val donadores = remember { mutableStateListOf<Donacion>() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val donacionesList = DonacionRepository.getDonaciones()
                donadores.clear()
                donadores.addAll(donacionesList)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Donaciones Activas") })
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(navController.context, RegistrarDonacionActivity::class.java)
                        navController.context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar Donación")
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(donadores.size) { index ->
                        DonadorItem(donadores[index]) {
                            navController.navigate("detail/${donadores[index].id}")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DonadorItem(donador: Donacion, onClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp)
        ) {
            AsyncImage(
                model = donador.imagenUrl,
                contentDescription = "Imagen de donación",
                modifier = Modifier.size(80.dp).padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(donador.pesoAlimento)
                Text(donador.tiempoRestante)
            }
        }
        Divider()
    }
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    @Composable
    fun DonadorDetailScreen(itemId: String?) {
        var donacion by remember { mutableStateOf<Donacion?>(null) }
        var reservas by remember { mutableStateOf<List<Reserva>>(emptyList()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(itemId) {
            itemId?.let {
                // Cargar la donación por ID
                donacion = DonacionRepository.getDonacionById(it)
                // Cargar las reservas para esta donación
                DonacionRepository.obtenerReservasPorDonacion(it) { reservasList ->
                    reservas = reservasList
                }
            }
        }

        Scaffold(topBar = { TopAppBar(title = { Text("Detalle de la donación") }) }) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                donacion?.let {
                    Column {
                        AsyncImage(
                            model = it.imagenUrl,
                            contentDescription = "Imagen de donación",
                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(bottom = 16.dp),
                            contentScale = ContentScale.Crop
                        )
                        Text("Alimento: ${it.pesoAlimento}")
                        Text("Duración Restante: ${it.tiempoRestante}")
                        Text("Disponible: ${it.pesoTotal} kg")
                        Text("Reservado: ${it.pesoReservado} kg")
                        Text("Entregado: ${it.pesoEntregado} kg")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Descripción: ${it.descripcion}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Extender duración") }

                        // Mostrar reservas
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Reservas:", style = MaterialTheme.typography.h6)
                        // Solo mostrar reservas si hay alguna
                        if (reservas.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(reservas.size) { index ->
                                    ReservaItem(reservas[index])
                                }
                            }
                        } else {
                            Text("No hay reservas para esta donación.")
                        }
                    }
                } ?: run {
                    Text("Cargando detalles...")
                }
            }
        }
    }

    @Composable
    fun ReservaItem(reserva: Reserva) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current // Contexto necesario para el Toast

        // Creamos un estado mutable para seguir el estado de la reserva
        var updatedReserva by remember { mutableStateOf(reserva) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Palabra clave: ${updatedReserva.palabraClave}")
                Text("Kg reservados: ${updatedReserva.pesoReservado}")
                Text("Estado: ${updatedReserva.estado}")  // Agregamos el campo 'estado'
            }

            // Mostrar el botón solo si el estado es "pendiente"
            if (updatedReserva.estado == "pendiente") {
                Button(onClick = {
                    // Llamamos a la función confirmarEntrega
                    scope.launch {
                        DonacionRepository.confirmarEntrega(updatedReserva.id)

                        // Actualizamos el estado local para reflejar el cambio en el UI
                        updatedReserva = updatedReserva.copy(estado = "entregada") // O el estado que corresponda

                        // Muestra el Toast cuando se confirma la entrega
                        Toast.makeText(context, "Entrega confirmada", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Confirmar entrega")
                }
            }
        }

        Divider()
    }




}
