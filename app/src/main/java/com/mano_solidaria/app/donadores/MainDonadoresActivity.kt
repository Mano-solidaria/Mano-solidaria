package com.mano_solidaria.app.donadores

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mano_solidaria.app.AppBarWithDrawer
import com.mano_solidaria.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mano_solidaria.app.Utils.cambioEstado
import kotlinx.coroutines.CoroutineScope


class MainDonadoresActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, NotificationServiceDonador::class.java)
        startService(intent)
        cambioEstado()
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
        val donaciones = remember { mutableStateListOf<Donacion>() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(true) {
            scope.launch {
                val donacionesList = Repository.getDonaciones()
                donaciones.clear()
                donaciones.addAll(donacionesList)
            }
        }

        // Estados de expansión de las listas
        val isActiveExpanded = remember { mutableStateOf(true) }
        val isPendingExpanded = remember { mutableStateOf(true) }
        val isFinalizedExpanded = remember { mutableStateOf(false) }

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

                // Verificar si todas las listas están vacías
                val donacionesActivas = donaciones.filter { it.estado == "activo" }
                val donacionesPendientes = donaciones.filter { it.estado == "pendiente" }
                val donacionesFinalizadas = donaciones.filter { it.estado == "finalizada" }

                if (donacionesActivas.isEmpty() && donacionesPendientes.isEmpty() && donacionesFinalizadas.isEmpty()) {
                    // Mostrar el mensaje si no hay donaciones
                    Text(
                        text = stringResource(id = R.string.registra_primera_propuesta),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    // Si hay donaciones, mostrar las listas
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        // Título y lista de donaciones activas (expandida por defecto)
                        if (donacionesActivas.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isActiveExpanded.value = !isActiveExpanded.value }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.donaciones_activas),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isActiveExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expandir/Contraer",
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            // Mostrar las donaciones activas solo si está expandida
                            if (isActiveExpanded.value) {
                                items(donacionesActivas) { donacion ->
                                    DonadorItem(donacion) {
                                        navController.navigate("detail/${donacion.id}")
                                    }
                                }
                            }
                        }

                        // Título y lista de donaciones pendientes (expandida por defecto)
                        if (donacionesPendientes.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPendingExpanded.value = !isPendingExpanded.value }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.donaciones_pendientes),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isPendingExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expandir/Contraer",
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            // Mostrar las donaciones pendientes solo si está expandida
                            if (isPendingExpanded.value) {
                                items(donacionesPendientes) { donacion ->
                                    DonadorItem(donacion) {
                                        navController.navigate("detail/${donacion.id}")
                                    }
                                }
                            }
                        }

                        // Título y lista de donaciones finalizadas (plegada por defecto)
                        if (donacionesFinalizadas.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isFinalizedExpanded.value = !isFinalizedExpanded.value }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.donaciones_finalizadas),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isFinalizedExpanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expandir/Contraer",
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            // Mostrar las donaciones finalizadas solo si está expandida
                            if (isFinalizedExpanded.value) {
                                items(donacionesFinalizadas) { donacion ->
                                    DonadorItem(donacion) {
                                        navController.navigate("detail/${donacion.id}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun DonadorItem(donacion: Donacion, onClick: () -> Unit) {
        val diasTexto = stringResource(id = R.string.dias) // Obtener el texto de "días" desde strings.xml
        val terminaTexto = stringResource(id = R.string.termina)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp)
        ) {
            AsyncImage(
                model = donacion.imagenUrl,
                contentDescription = "Imagen de donación",
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 8.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = donacion.pesoAlimento,
                    fontWeight = FontWeight.Bold, // Negrita
                    fontSize = 20.sp // Tamaño más grande
                )

                // Solo mostrar el tiempo restante si la donación no está finalizada
                if (donacion.estado != "finalizada") {
                    Text("$terminaTexto ${donacion.tiempoRestante} $diasTexto")
                }
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
                donacion?.let { donacion ->
                    // Comprobamos si el estado de la donación es "finalizada"
                    val isFinalizada = donacion.estado == "finalizada"

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // Título "Alimento" encima de la imagen
                            Text(
                                text = donacion.pesoAlimento,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Imagen relacionada con la donación
                            AsyncImage(
                                model = donacion.imagenUrl,
                                contentDescription = "Imagen de donación",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .padding(bottom = 16.dp),
                                contentScale = ContentScale.Crop
                            )

                            // Muestra los detalles de la donación con los nuevos campos
                            DonacionDetails(donacion)

                            // Tabla con los valores de Disponible, Reservado, Entregado
                            val disponible = donacion.pesoTotal - donacion.pesoReservado - donacion.pesoEntregado
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x99999999))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Disponible", fontWeight = FontWeight.Bold)
                                    Text(text = "${disponible} kg")
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Reservado", fontWeight = FontWeight.Bold)
                                    Text(text = "${donacion.pesoReservado} kg")
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = "Entregado", fontWeight = FontWeight.Bold)
                                    Text(text = "${donacion.pesoEntregado} kg")
                                }
                            }

                            // Botón para extender duración, solo si no está "finalizada"
                            if (!isFinalizada) {
                                ExtenderDuracionButton(
                                    donacion = donacion,
                                    diasRestantes = diasRestantes,
                                    onDiasRestantesChange = { diasRestantes = it },
                                    onDurationExtended = {
                                        Toast.makeText(
                                            context,
                                            R.string.extended_duration,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Después de extender, volvemos a cargar los datos actualizados
                                        scope.launch {
                                            cargarDatosSuspendido()
                                        }
                                    }
                                )
                            }
                        }

                        // Verificar si el estado "pendiente" es 0
                        val reservasPendientes = reservas.filter { it.estado == "pendiente" }

                        // Botón para terminar la donación, solo si no está "finalizada"
                        item {
                            if (!isFinalizada) {
                                TerminarDonacionButton(
                                    donacion = donacion,
                                    scope = scope,
                                    context = context,
                                    reservasPendientes = reservasPendientes
                                )
                            }
                        }

                        // Mostrar las reservas, si hay alguna
                        if (reservas.isNotEmpty()) {
                            items(count = reservas.size, key = { index -> reservas[index].id }) { index ->
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
    fun TerminarDonacionButton(
        donacion: Donacion?,
        scope: CoroutineScope,
        context: Context,
        reservasPendientes: List<Reserva>
    ) {
        Button(
            onClick = {
                // Llamar a la función para terminar la donación
                scope.launch {
                    try {
                        donacion?.id?.let {
                            // Llamamos a la función del repositorio
                            Repository.terminarDonacion(it)

                            // Mostrar mensaje de éxito
                            Toast.makeText(context, "Donación Terminada", Toast.LENGTH_SHORT).show()

                            // Volver a la pantalla anterior
                            (context as? Activity)?.onBackPressed()  // Regresa a la pantalla anterior
                        }
                    } catch (e: Exception) {
                        // Manejar cualquier error
                        Toast.makeText(context, "Error al terminar la donación", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = reservasPendientes.isEmpty(),  // El botón estará habilitado solo si no hay reservas pendientes
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Terminador Donacion")
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
                // Palabra clave en negrita
                Text(
                    text = stringResource(id = R.string.donador_palabra_clave, updatedReserva.palabraClave),
                    fontWeight = FontWeight.Bold // Aplicamos la negrita aquí
                )
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
        var pesoDisponible = donacion.pesoTotal - donacion.pesoReservado - donacion.pesoEntregado
        Column {
            Text(stringResource(id = R.string.donador_duracion_restante, donacion.tiempoRestante))
            Spacer(modifier = Modifier.height(8.dp))

            /*Divider() // Línea separadora
            Text(stringResource(id = R.string.donador_disponible, pesoDisponible))
            Text(stringResource(id = R.string.donador_reservado, donacion.pesoReservado))
            Text(stringResource(id = R.string.donador_entregado, donacion.pesoEntregado))
            Spacer(modifier = Modifier.height(8.dp))*/

            Divider() // Línea separadora
            Text(stringResource(id = R.string.tipo_alimento, donacion.tipoAlimento))
            Text(stringResource(id = R.string.requiere_refrigeracion, donacion.requiereRefrigeracion))
            Text(stringResource(id = R.string.es_perecedero, donacion.esPedecedero))
            Spacer(modifier = Modifier.height(8.dp))

            Divider() // Línea separadora

            Text(stringResource(id = R.string.donador_descripcion, donacion.descripcion))




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
