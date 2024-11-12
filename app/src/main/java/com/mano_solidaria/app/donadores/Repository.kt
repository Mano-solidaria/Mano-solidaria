package com.mano_solidaria.app.donadores

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.DocumentSnapshot
import com.mano_solidaria.app.Utils.calcularDuracion
import kotlinx.coroutines.tasks.await

data class Donacion(
    val id: String,
    val pesoAlimento: String,
    val tiempoRestante: String,
    val imagenUrl: String,
    val descripcion: String,
    val pesoReservado: Int,
    val pesoEntregado: Int,
    val pesoTotal: Int
)

data class Reserva(
    val id: String,  // Agregar el id de la reserva
    val donacionId: String,
    val palabraClave: String,
    val pesoReservado: Int,
    val usuarioReservadorId: String,
    val estado: String  // Nuevo campo agregado
)

object DonacionRepository {
    private val db = FirebaseFirestore.getInstance()

    // Función para obtener todas las donaciones
    suspend fun getDonaciones(): List<Donacion> {
        return try {
            // Obtener el UID del usuario logueado
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()

            // Filtrar las donaciones por el donanteId que coincida con el UID del usuario logueado
            val snapshots = db.collection("donaciones")
                .whereEqualTo("donanteId", db.collection("users").document(userId)) // Filtrar por donanteId
                .get()
                .await()

            // Convertir los documentos a objetos Donacion
            snapshots.documents.map { it.toDonacion() }
        } catch (e: Exception) {
            emptyList()  // En caso de error, devuelve una lista vacía
        }
    }

    // Función para obtener una donación por su ID
    suspend fun getDonacionById(id: String): Donacion? {
        return try {
            val document = db.collection("donaciones").document(id).get().await()
            document.toDonacion()
        } catch (e: Exception) {
            null  // En caso de error, devuelve null
        }
    }

    // Función para obtener las reservas por ID de la donación
    fun obtenerReservasPorDonacion(donacionId: String, onResult: (List<Reserva>) -> Unit) {
        // Obtener referencia al documento de la donación
        val donacionRef = db.collection("donaciones").document(donacionId)

        db.collection("reservas")
            .whereEqualTo("donacionId", donacionRef)  // Filtrar por la referencia de la donación
            .get()
            .addOnSuccessListener { querySnapshot ->
                val reservas = mutableListOf<Reserva>()
                for (document in querySnapshot.documents) {
                    // Asegúrate de que 'toReserva' maneje correctamente los campos, incluyendo referencias y fechas
                    val reserva = document.toReserva()
                    reservas.add(reserva)
                }
                onResult(reservas)  // Devuelve la lista de reservas
            }
            .addOnFailureListener {
                onResult(emptyList())  // En caso de error, devuelve una lista vacía
            }
    }
    suspend fun confirmarEntrega(reservaId: String) {
        try {
            // Obtener referencia al documento de la reserva por su ID
            val reservaRef = db.collection("reservas").document(reservaId)

            // Actualizar el campo "estado" a "entregado"
            reservaRef.update("estado", "entregado").await()  // Espera a que la operación termine

        } catch (e: Exception) {
            // Manejo de errores si la actualización falla
            e.printStackTrace()
        }
    }

    // Función para convertir un DocumentSnapshot en un objeto Reserva
    private fun DocumentSnapshot.toReserva(): Reserva {
        val id = this.id
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""  // Usamos la referencia del documento
        val palabraClave = this.getString("palabraClave") ?: ""
        val pesoReservado = (this.get("pesoReservado") as? Long ?: 0).toInt()
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""  // Usamos la referencia del documento
        val estado = this.getString("estado") ?: "pendiente"  // Nuevamente, "pendiente" por defecto

        return Reserva(
            id = id,
            donacionId = donacionId,
            palabraClave = palabraClave,
            pesoReservado = pesoReservado,
            usuarioReservadorId = usuarioReservadorId,
            estado = estado  // Asignamos el estado
        )
    }

    // Función para convertir un DocumentSnapshot en un objeto Donacion
    private fun DocumentSnapshot.toDonacion(): Donacion {
        val id = this.id
        val alimento = this.getString("alimento") ?: "Desconocido"
        val pesoTotal = (this.get("pesoTotal") as? Double ?: 0.0).toInt()
        val pesoReservado = (this.get("pesoReservado") as? Double ?: 0.0).toInt()
        val pesoEntregado = (this.get("pesoEntregado") as? Double ?: 0.0).toInt()
        val descripcion = this.getString("descripcion") ?: "No disponible"
        val imageURL = this.getString("imageURL") ?: ""
        val fechaInicio = this.getTimestamp("fechaInicio")?.toDate()
        val fechaFin = this.getTimestamp("fechaFin")?.toDate()
        val duracion = calcularDuracion(fechaInicio, fechaFin)

        return Donacion(
            id = id,
            pesoAlimento = "$pesoTotal kg de $alimento",
            tiempoRestante = duracion,
            imagenUrl = imageURL,
            descripcion = descripcion,
            pesoReservado = pesoReservado,
            pesoEntregado = pesoEntregado,
            pesoTotal = pesoTotal
        )
    }
}