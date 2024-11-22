package com.mano_solidaria.app.solicitantes

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import android.location.Location
import kotlin.math.round

data class Reserva(
    val id: String,
    val donacionId: String,
    val palabraClave: String,
    var pesoReservado: String, // por ahora string
    val usuarioReservadorId: String,
    val estado: String,
    val nombreDonante: String,
    val imagenURL: String,
    val distancia: Float?,
    val tiempoInicial: String,
    val alimento: String,
    val descripcion: String,
    val ubicacionDonante: GeoPoint,
    val rangoReserva: Long

)

object ReservasRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getReservas(): List<Reserva> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val snapshots = db.collection("reservas")
                .whereEqualTo("usuarioReservador", db.collection("users").document(userId))
                .whereIn("estado", listOf("reservado", "entregado", "cancelada"))
                .get()
                .await()

            Log.d("getReservas", "Snapshots de reservas: ${snapshots.documents.size}")

            snapshots.documents
                .map { it.toReserva() }
                .sortedWith(
                    compareBy<Reserva> { reserva ->
                        when (reserva.estado) {
                            "reservado" -> 1
                            "entregado" -> 2
                            "cancelada" -> 3
                            else -> Int.MAX_VALUE
                        }
                    }
                        .thenBy { reserva -> reserva.distancia } // Ordena por distancia después del estado
                )
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun cancelarReserva(id: String) {
        // primero, obtener la referencia de la reserva
        val reservaRef = db.collection("reservas").document(id)

        // obtener la reserva
        reservaRef.get()
            .addOnSuccessListener { reserva ->
                // obtener el id de la donacion desde la reserva
                val donacionRef = reserva.getDocumentReference("donacionId")
                val pesoReservado = reserva.getDouble("pesoReservado") ?: 0.0

                // verificar que la referencia de la donacion no sea nula
                if (donacionRef != null) {
                    // actualizar el campo pesoReservado en el documento de la donacion
                    donacionRef.update("pesoReservado", FieldValue.increment(-pesoReservado))
                        .addOnSuccessListener {
                            Log.d("Reserva", "Peso actualizado correctamente en la donación.")
                        }
                        .addOnFailureListener { e ->
                            Log.d("Reserva", "Error al actualizar el peso en la donación: ${e.message}")
                        }
                } else {
                    Log.d("Reserva", "No se encontró la referencia de la donación.")
                }

                // actualizar la reserva para marcarla como cancelada
                reservaRef.update("estado", "cancelada")
                    .addOnSuccessListener {
                        Log.d("Reserva", "Reserva cancelada con éxito.")
                    }
                    .addOnFailureListener { e ->
                        Log.d("Reserva", "Error al cancelar la reserva: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.d("Reserva", "Error al obtener la reserva: ${e.message}")
            }
    }

    fun modificarReserva(id: String, anteriorPesoReservado: Long, nuevoPesoReservado: Long) {
        // primero, obtener la referencia de la reserva
        val reservaRef = db.collection("reservas").document(id)

        // obtener la reserva
        reservaRef.get()
            .addOnSuccessListener { reserva ->
                // obtener el id de la donacion desde la reserva
                val donacionRef = reserva.getDocumentReference("donacionId")

                // verificar que la referencia de la donacion no sea nula
                if (donacionRef != null) {
                    // actualizar el campo pesoReservado en el documento de la donacion
                    donacionRef.update("pesoReservado", FieldValue.increment(-anteriorPesoReservado + nuevoPesoReservado))
                        .addOnSuccessListener {
                            Log.d("Reserva", "Peso actualizado correctamente en la donación.")
                        }
                        .addOnFailureListener { e ->
                            Log.d("Reserva", "Error al actualizar el peso en la donación: ${e.message}")
                        }
                } else {
                    Log.d("Reserva", "No se encontró la referencia de la donación.")
                }

                // actualizar el peso reservado de la reserva
                reservaRef.update("pesoReservado", nuevoPesoReservado)
                    .addOnSuccessListener {
                        Log.d("Reserva", "Reserva modificada con éxito.")
                    }
                    .addOnFailureListener { e ->
                        Log.d("Reserva", "Error al modificar la reserva: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.d("Reserva", "Error al obtener la reserva: ${e.message}")
            }
    }

    fun calcularDistanciaEnKm(geoPoint1: GeoPoint, geoPoint2: GeoPoint): Float {
        val location1 = Location("").apply {
            latitude = geoPoint1.latitude
            longitude = geoPoint1.longitude
        }

        val location2 = Location("").apply {
            latitude = geoPoint2.latitude
            longitude = geoPoint2.longitude
        }

        // distancia en metros
        val distanciaEnMetros = location1.distanceTo(location2)

        // convertir a kilometros y redondear a 1 decimal
        return round((distanciaEnMetros / 1000) * 10) / 10
    }

    // convertir el string con formato "lat/lng: (latitud,longitud)" a un GeoPoint
    fun stringToGeoPoint(ubicacion: String): GeoPoint {
        if (ubicacion.startsWith("lat/lng: (") && ubicacion.endsWith(")")) {
            val coordenadas = ubicacion
                .removePrefix("lat/lng: (")
                .removeSuffix(")")
                .split(",")

            if (coordenadas.size == 2) {
                val latitud = coordenadas[0].toDoubleOrNull()
                val longitud = coordenadas[1].toDoubleOrNull()

                if (latitud != null && longitud != null) {
                    return GeoPoint(latitud, longitud)
                }
            }
        }
        return GeoPoint(0.0,0.0)
    }

    private suspend fun DocumentSnapshot.toReserva(): Reserva {
        val id = this.id
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""  // obtener el id de la donacion
        val palabraClave = this.getString("palabraClave") ?: ""  // obtener la palabra clave
        val pesoReservado = this.get("pesoReservado") as? Long ?: 0  // obtener el peso reservado
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""  // Obtener el id del reservador
        val estado = this.getString("estado") ?: "pendiente"  // obtener el estado (pendiente o no)
        val donacionIdRef = this.getDocumentReference("donacionId") // referencia al documento de la colección donaciones

        // obtener el documento de la donacion
        val donacionSnapshot = donacionIdRef?.get()?.await()

        // obtener la fecha de inicio de la donacion
        val donacionFechaHorarioInicio = donacionSnapshot?.getTimestamp("fechaInicio")
        val donacionFecha = donacionFechaHorarioInicio?.toDate()
        val donacionFechaFormateada = java.text.SimpleDateFormat("dd/MM/yyyy").format(donacionFecha)

        // obtener el donanteId de la donacion
        val donanteId = donacionSnapshot?.getDocumentReference("donanteId")?.id ?: ""

        // obtener el nombre del donante usando el donanteId
        val nombreDonante = if (donanteId.isNotEmpty()) {
            val userSnapshot = db.collection("users").document(donanteId).get().await()
            userSnapshot.getString("UsuarioNombre") ?: "Desconocido"
        } else {
            "Desconocido"
        }

        // obtener la URL de la imagen desde el documento de la donacion
        val imagenURL = donacionSnapshot?.getString("imagenURL") ?: "Sin URL"

        // obtener el nombre del alimento desde el documento de la donación
        val alimento = donacionSnapshot?.getString("alimento") ?: "Sin nombre de alimento"

        // obtener la descripcion desde el documento de la donacion
        val descripcion = donacionSnapshot?.getString("descripcion") ?: "Sin descripcion"

        // obtener pesos de la donacion
        val pesoTotalDonacion = donacionSnapshot?.getLong("pesoTotal") ?: 0
        val pesoReservadoDonacion = donacionSnapshot?.getLong("pesoReservado") ?: 0
        val pesoEntregadoDonacion = donacionSnapshot?.getLong("pesoEntregado") ?: 0
        // ver hasta cuando se puede modificar la reserva
        val rangoDeReserva = pesoTotalDonacion - pesoEntregadoDonacion - pesoReservadoDonacion + pesoReservado

        val geoPointSolicitante = if (usuarioReservadorId.isNotEmpty()) {
            val userSnapshot = db.collection("users").document(usuarioReservadorId).get().await()
            userSnapshot.getString("Usuarioubicacion") ?: "lat/lng: (0,0)"
        } else {
            "lat/lng: (0,0)"
        }
        Log.d("GeoPointLog", "GeoPoint obtenido: ${geoPointSolicitante}")

        // obtener el GeoPoint del donante usando el donanteId
        val geoPointDonante = if (donanteId.isNotEmpty()) {
            val userSnapshot = db.collection("users").document(donanteId).get().await()
            userSnapshot.getString("Usuarioubicacion") ?: "lat/lng: (0,0)"
        } else {
            "lat/lng: (0,0)"
        }
        Log.d("GeoPointLog", "GeoPoint obtenido: ${geoPointDonante}")

        val distancia =
            stringToGeoPoint(geoPointSolicitante)?.let {
                stringToGeoPoint(geoPointDonante)?.let { it1 ->
                    calcularDistanciaEnKm(
                        it,
                        it1
                    )
                }
            }

        // crear y retornar un objeto de tipo Reserva
        return Reserva(id, donacionId, palabraClave, pesoReservado.toString(),
            usuarioReservadorId, estado, nombreDonante, imagenURL,
            distancia, donacionFechaFormateada, alimento, descripcion, stringToGeoPoint(geoPointDonante), rangoDeReserva)
    }
}