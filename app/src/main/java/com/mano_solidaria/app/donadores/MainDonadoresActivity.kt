package com.mano_solidaria.app.donadores

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
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


class MainDonadoresActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, NotificationService::class.java)
        startService(intent)
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
                val donacionesList = Repository.getDonaciones()
                donadores.clear()
                donadores.addAll(donacionesList)
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Donaciones Activas") }) }
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
        var diasRestantes by remember { mutableIntStateOf(1) }
        val context = LocalContext.current

        LaunchedEffect(itemId) {
            itemId?.let {
                donacion = Repository.getDonacionById(it)
                Repository.obtenerReservasPorDonacion(it) { reservasList ->
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
                        DonacionDetails(donacion!!)
                        ExtenderDuracionButton(
                            donacion = donacion!!,
                            diasRestantes = diasRestantes,
                            onDiasRestantesChange = { diasRestantes = it },
                            onDurationExtended = {
                                Toast.makeText(context, "Duración extendida", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (reservas.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(reservas.size) { index -> ReservaItem(reservas[index]) }
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
    fun DonacionDetails(donacion: Donacion) {
        Column {
            Text("Alimento: ${donacion.pesoAlimento}")
            Text("Duración Restante: ${donacion.tiempoRestante}")
            Text("Disponible: ${donacion.pesoTotal} kg")
            Text("Reservado: ${donacion.pesoReservado} kg")
            Text("Estado: ${donacion.estado}")
            Text("Entregado: ${donacion.pesoEntregado} kg")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Descripción: ${donacion.descripcion}")
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    @Composable
    fun ExtenderDuracionButton(
        donacion: Donacion,
        diasRestantes: Int,
        onDiasRestantesChange: (Int) -> Unit,
        onDurationExtended: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val donacionState = remember { mutableStateOf(donacion) }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    scope.launch {
                        Repository.actualizarFechaFin(donacionState.value.id, diasRestantes)
                        val tiempoRestanteActual = donacionState.value.tiempoRestante.split(" ")[0].toIntOrNull() ?: 0
                        val nuevoTiempoRestante = tiempoRestanteActual + diasRestantes
                        donacionState.value = donacionState.value.copy(tiempoRestante = "$nuevoTiempoRestante días")
                        onDurationExtended()
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("Extender duración")
            }

            TextField(
                value = diasRestantes.toString(),
                onValueChange = { newValue -> onDiasRestantesChange(newValue.toIntOrNull() ?: 0) },
                label = { Text("Días") },
                modifier = Modifier.width(80.dp).padding(start = 8.dp).height(56.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
        }
    }

    @Composable
    fun ReservaItem(reserva: Reserva) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
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
                Text("Estado: ${updatedReserva.estado}")
            }

            if (updatedReserva.estado == "pendiente") {
                Button(onClick = {
                    scope.launch {
                        Repository.confirmarEntrega(updatedReserva.id)
                        updatedReserva = updatedReserva.copy(estado = "entregada")
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
