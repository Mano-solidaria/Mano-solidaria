package com.mano_solidaria.app.solicitantes

import ApiService
import android.annotation.SuppressLint
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.mano_solidaria.app.Utils.calcularDuracion
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import android.net.Uri
import android.provider.OpenableColumns
import com.google.firebase.Timestamp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import okhttp3.MultipartBody
import android.content.Context
import com.mano_solidaria.app.donadores.Donacion
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Date

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import android.location.Location
import kotlin.math.round

data class Reserva(
    val id: String,
    val donacionId: String,
    val palabraClave: String,
    val pesoReservado: String, // por ahora string
    val usuarioReservadorId: String,
    val estado: String,
    val nombreDonante: String,
    val imagenURL: String,
    val distancia: String,
    val tiempoInicial: String,
    val alimento: String,
    val descripcion: String,
    val ubicacionDonante: GeoPoint
)

object ReservasRepository {
    private val db = FirebaseFirestore.getInstance()
    fun currentUser(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getReservas(): List<Reserva> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val snapshots = db.collection("reservas")
                .whereEqualTo("usuarioReservador", db.collection("users").document(userId))
                //.whereNotEqualTo("estado", "cancelada") // Filtro para que el estado no sea "cancelada" (no esta funcionando)
                .whereIn("estado", listOf("entregado", "retirado", "reservado")) // este funciona bien
                .get()
                .await()

            Log.d("getReservas", "Snapshots de reservas: ${snapshots.documents.size}")
            snapshots.documents.map { it.toReserva() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReservaById(id: String): Reserva? {
        return try {
            val document = db.collection("reservas").document(id).get().await()
            document.toReserva()
        } catch (e: Exception) {
            null
        }
    }

    fun cancelarReserva(id: String) {
        // Primero, obtener la referencia de la reserva
        val reservaRef = db.collection("reservas").document(id)

        // Obtener la reserva
        reservaRef.get()
            .addOnSuccessListener { reserva ->
                // Obtener el id de la donacion desde la reserva
                val donacionRef = reserva.getDocumentReference("donacionId")
                val pesoReservado = reserva.getDouble("pesoReservado") ?: 0.0

                // Verificar que la referencia de la donacion no sea nula
                if (donacionRef != null) {
                    // Actualizar el campo pesoReservado en el documento de la donacion
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

                // Actualizar la reserva para marcarla como cancelada
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


    private suspend fun getUserNameById(userId: String): String {
        val userSnapshot = db.collection("users").document(userId).get().await()
        return userSnapshot.getString("UsuarioNombre") ?: "Nombre desconocido"
    }

    suspend fun getUserLatLngById(userId: String): GeoPoint {
        val userSnapshot = db.collection("users").document(userId).get().await()
        return userSnapshot.getGeoPoint("Usuarioubicacion") ?: GeoPoint(37.7749, -122.4194)
    }

    private suspend fun getUsuarioDonadorIdByDonacionId(donacionId: String): String {
        var donacionSnapshot = db.collection("donaciones").document(donacionId).get().await()
        return donacionSnapshot.getString("donanteId") ?: "Donante desconocido"
    }

    private suspend fun getNombreDeDonadorByDonacionId(donacionId: String): String {
        var usuarioDonador = getUsuarioDonadorIdByDonacionId(donacionId)
        var nombreDonador = getUserNameById(usuarioDonador)
        return nombreDonador
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

        // Distancia en metros
        val distanciaEnMetros = location1.distanceTo(location2)

        // Convertir a kilómetros y redondear a 1 decimal
        return round((distanciaEnMetros / 1000) * 10) / 10
    }

    // Método para convertir el string con formato "lat/lng: (latitud,longitud)" a un GeoPoint
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
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""  // Obtener el ID de la donación
        val palabraClave = this.getString("palabraClave") ?: ""  // Obtener la palabra clave
        val pesoReservado = (this.get("pesoReservado") as? Long ?: 0).toString()  // Obtener el peso reservado (por ahora string)
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""  // Obtener el ID del reservador
        val estado = this.getString("estado") ?: "pendiente"  // Obtener el estado (si está pendiente o no)
        val donacionIdRef = this.getDocumentReference("donacionId") // Referencia al documento de la colección "donaciones"

        // Obtener el documento de la donacion
        val donacionSnapshot = donacionIdRef?.get()?.await()

        // obtener la fecha de inicio de la donacion
        val donacionFechaHorarioInicio = donacionSnapshot?.getTimestamp("fechaInicio")
        val donacionFecha = donacionFechaHorarioInicio?.toDate()
        val donacionFechaFormateada = java.text.SimpleDateFormat("dd/MM/yyyy").format(donacionFecha)

        // Obtener el donanteId de la donacion
        val donanteId = donacionSnapshot?.getDocumentReference("donanteId")?.id ?: ""

        // Obtener el nombre del donante usando el donanteId
        val nombreDonante = if (donanteId.isNotEmpty()) {
            val userSnapshot = db.collection("users").document(donanteId).get().await()
            userSnapshot.getString("UsuarioNombre") ?: "Desconocido"
        } else {
            "Desconocido"
        }

        // Obtener la URL de la imagen desde el documento de la donación
        val imagenURL = donacionSnapshot?.getString("imagenURL") ?: "Sin URL"

        // Obtener el nombre del alimento desde el documento de la donación
        val alimento = donacionSnapshot?.getString("alimento") ?: "Sin nombre de alimento"

        // Obtener la descripcion desde el documento de la donación
        val descripcion = donacionSnapshot?.getString("descripcion") ?: "Sin descripcion"

        // Mdistancia
        //val distancia = "mock distancia"

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

        // Mock tiempo restante
        val tiempoRestante = "mock tiempo restante"

        // Crear y retornar un objeto de tipo "Reserva"
        return Reserva(id, donacionId, palabraClave, pesoReservado,
            usuarioReservadorId, estado, nombreDonante, imagenURL,
            distancia.toString(), donacionFechaFormateada, alimento, descripcion, stringToGeoPoint(geoPointDonante))
    }
}