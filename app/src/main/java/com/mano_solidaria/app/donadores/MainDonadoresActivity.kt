package com.mano_solidaria.app.donadores

import android.annotation.SuppressLint
import android.os.Bundle
import android.content.Intent
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import com.mano_solidaria.app.AppBarWithDrawer
import com.mano_solidaria.app.R


class MainDonadoresActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, NotificationServiceDonador::class.java)
        startService(intent)
        setContent { MainDonadoresApp() }
    }

    @Composable
    fun MainDonadoresApp() {
        val navController = rememberNavController()
        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
        val coroutineScope = rememberCoroutineScope()


        AppBarWithDrawer(
            title = stringResource(id = R.string.active_donations),
            scaffoldState = scaffoldState,
            coroutineScope = coroutineScope
        ) { paddingValues ->
            NavHost(
                navController,
                startDestination = "list",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("list") { DonadoresListScreen(navController) }
                composable("detail/{itemId}") { backStackEntry ->
                    DonadorDetailScreen(backStackEntry.arguments?.getString("itemId"))
                }
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

        Scaffold() {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(navController.context, RegistrarDonacionActivity::class.java)
                        navController.context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.register_donation))
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
        val reservas = remember { mutableStateListOf<Reserva>() }
        var diasRestantes by remember { mutableIntStateOf(1) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope() // Usamos rememberCoroutineScope para lanzar corrutinas

        // Función para cargar los datos desde el servidor de forma suspendida
        suspend fun cargarDatosSuspendido() {
            itemId?.let {
                // Solicitar nuevamente los datos del servidor
                donacion = Repository.getDonacionById(it) // Función suspendida
                Log.d("Donacion", "Donación recibida: $donacion")
                Repository.obtenerReservasPorDonacion(it) { reservasList ->
                    reservas.clear()  // Limpiar reservas antes de añadir las nuevas
                    reservas.addAll(reservasList)
                }
            }
        }

        // Limpiar y cargar los datos cada vez que el itemId cambie
        LaunchedEffect(itemId) {
            cargarDatosSuspendido() // Llamada a la función suspendida
        }

        Scaffold {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                donacion?.let {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            AsyncImage(
                                model = it.imagenUrl,
                                contentDescription = "Imagen de donación",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .padding(bottom = 16.dp),
                                contentScale = ContentScale.Crop
                            )
                            DonacionDetails(donacion!!)
                            ExtenderDuracionButton(
                                donacion = donacion!!,
                                diasRestantes = diasRestantes,
                                onDiasRestantesChange = { diasRestantes = it },
                                onDurationExtended = {
                                    Toast.makeText(context, R.string.extended_duration, Toast.LENGTH_SHORT).show()
                                    // Después de extender, volvemos a cargar los datos actualizados
                                    scope.launch {
                                        cargarDatosSuspendido()
                                    }
                                }
                            )
                        }

                        if (reservas.isNotEmpty()) {
                            items(
                                count = reservas.size,
                                key = { index -> reservas[index].id }
                            ) { index ->
                                val reserva = reservas[index]
                                ReservaItem(
                                    reserva = reserva,
                                    onEstadoChange = { updatedReserva ->
                                        reservas[index] = updatedReserva
                                    },
                                    onEntregaConfirmada = {
                                        // Recargar los datos después de confirmar la entrega
                                        scope.launch {
                                            cargarDatosSuspendido() // Recargar los datos
                                        }
                                    }
                                )
                            }
                        } else {
                            item {
                                Text(stringResource(id = R.string.no_reservations_for_donation))
                            }
                        }
                    }
                } ?: run {
                    Text(stringResource(id = R.string.loading_details))
                }
            }
        }
    }
    @Composable
    fun ReservaItem(
        reserva: Reserva,
        onEstadoChange: (Reserva) -> Unit,
        onEntregaConfirmada: () -> Unit // Callback para notificar cuando se confirme la entrega
    ) {
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
                Text(stringResource(id = R.string.donador_palabra_clave, updatedReserva.palabraClave))
                Text(stringResource(id = R.string.donador_kg_reservados, updatedReserva.pesoReservado))
                Text(stringResource(id = R.string.donador_estado_reserva, updatedReserva.estado))
            }

            if (updatedReserva.estado.lowercase() == "pendiente" || updatedReserva.estado.lowercase() == "confirmada") {
                Button(onClick = {
                    scope.launch {
                        Repository.confirmarEntrega(updatedReserva.id)
                        updatedReserva = updatedReserva.copy(estado = "entregada")
                        onEstadoChange(updatedReserva)
                        Toast.makeText(context, R.string.donador_entrega_confirmada, Toast.LENGTH_SHORT).show()
                        onEntregaConfirmada() // Notificar a la pantalla padre para recargar datos
                    }
                }) {
                    Text(stringResource(id = R.string.donador_confirmar_entrega))
                }
            }
        }
        Divider()
    }



    @Composable
    fun DonacionDetails(donacion: Donacion) {
        var pesoDisponible= donacion.pesoTotal - donacion.pesoReservado - donacion.pesoEntregado
        Column {
            Text(stringResource(id = R.string.donador_alimento, donacion.pesoAlimento)
            )
            Text(stringResource(id = R.string.donador_duracion_restante, donacion.tiempoRestante)
            )
            Text(stringResource(id = R.string.donador_disponible, pesoDisponible)
            )
            Text(stringResource(id = R.string.donador_reservado, donacion.pesoReservado)
            )
            Text(stringResource(id = R.string.donador_estado, donacion.estado)
            )
            Text(stringResource(id = R.string.donador_entregado, donacion.pesoEntregado)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.donador_descripcion, donacion.descripcion)
            )
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

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    scope.launch {
                        // Actualizar la fecha de fin en el repositorio
                        Repository.actualizarFechaFin(donacion.id, diasRestantes)
                        val tiempoRestanteActual = donacion.tiempoRestante.split(" ")[0].toIntOrNull() ?: 0
                        val nuevoTiempoRestante = tiempoRestanteActual + diasRestantes
                        // Al hacer click, se invoca el callback para extender la duración
                        onDurationExtended()
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(stringResource(id = R.string.donador_extender_duracion))
            }

            TextField(
                value = diasRestantes.toString(),
                onValueChange = { newValue -> onDiasRestantesChange(newValue.toIntOrNull() ?: 0) },
                label = { Text(stringResource(id = R.string.donador_dias)) },
                modifier = Modifier.width(80.dp).padding(start = 8.dp).height(56.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
        }
    }
}
