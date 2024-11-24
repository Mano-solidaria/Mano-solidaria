package com.mano_solidaria.app.donadores

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
import android.util.Log
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Date

data class Donacion(
    val id: String,
    val pesoAlimento: String,
    val tiempoRestante: String,
    val imagenUrl: String,
    val descripcion: String,
    val pesoReservado: Int,
    val pesoEntregado: Int,
    val pesoTotal: Int,
    val estado: String,
    val tipoAlimento: String,  // Nuevo campo
    val requiereRefrigeracion: String,  // Nuevo campo
    val esPedecedero: String  // Nuevo campo
)


data class Reserva(
    val id: String,
    val donacionId: String,
    val palabraClave: String,
    val pesoReservado: Int,
    val usuarioReservadorId: String,
    val estado: String
)

object Repository {
    private val db = FirebaseFirestore.getInstance()

    fun currentUser(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getDonaciones(): List<Donacion> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val snapshots = db.collection("donaciones")
                .whereEqualTo("donanteId", db.collection("users").document(userId))
                .get()
                .await()
            snapshots.documents.map { it.toDonacion() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDonacionById(id: String): Donacion? {
        return try {
            Log.d("Repository", "Iniciando consulta para obtener donación con ID: $id")
            val document = db.collection("donaciones").document(id).get().await()

            // Log para inspeccionar el documento obtenido
            Log.d("Repository", "Documento obtenido: ${document.data}")

            val donacion = document.toDonacion()

            // Log para verificar el objeto Donacion convertido
            Log.d("Repository", "Donación convertida: $donacion")

            donacion
        } catch (e: Exception) {
            // Log para capturar excepciones
            Log.e("Repository", "Error al obtener donación con ID: $id", e)
            null
        }
    }

    fun obtenerReservasPorDonacion(donacionId: String, onResult: (List<Reserva>) -> Unit) {
        val donacionRef = db.collection("donaciones").document(donacionId)

        db.collection("reservas")
            .whereEqualTo("donacionId", donacionRef)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val reservas = querySnapshot.documents.map { it.toReserva() }
                onResult(reservas)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    // Función para modificar los campos pesoEntregado y pesoReservado de una donación
    suspend fun modificarPesoDonacion(donacionId: String, pesoReservado: Int) {
        try {
            val donacionRef = db.collection("donaciones").document(donacionId)

            // Actualizar los valores en la donación
            db.runTransaction { transaction ->
                val snapshot = transaction.get(donacionRef)
                val pesoEntregadoActual = snapshot.getLong("pesoEntregado")?.toInt() ?: 0
                val pesoReservadoActual = snapshot.getLong("pesoReservado")?.toInt() ?: 0
                val pesoTotal = snapshot.getLong("pesoTotal")?.toInt() ?: 0

                // Validar si el peso reservado es suficiente
                if (pesoReservado > pesoReservadoActual) {
                    throw IllegalStateException("El peso reservado excede el disponible en la donación.")
                }

                // Calcular los nuevos valores
                val nuevoPesoEntregado = pesoEntregadoActual + pesoReservado
                val nuevoPesoReservado = pesoReservadoActual - pesoReservado

                // Preparar actualizaciones
                val actualizaciones = mutableMapOf<String, Any>(
                    "pesoEntregado" to nuevoPesoEntregado,
                    "pesoReservado" to nuevoPesoReservado
                )

                // Cambiar el estado si el peso entregado alcanza el total
                if (nuevoPesoEntregado == pesoTotal) {
                    actualizaciones["estado"] = "finalizada"
                }

                // Realizar las actualizaciones
                transaction.update(donacionRef, actualizaciones)
            }.await()

            Log.d("modificarPesoDonacion", "Donación actualizada correctamente.")
        } catch (e: Exception) {
            Log.e("modificarPesoDonacion", "Error al modificar la donación.", e)
            throw e
        }
    }
    suspend fun terminarDonacion(donacionId: String) {
        try {
            val donacionRef = db.collection("donaciones").document(donacionId)

            // Actualizar el estado de la donación a "finalizada"
            db.runTransaction { transaction ->
                val snapshot = transaction.get(donacionRef)
                val estadoActual = snapshot.getString("estado") ?: ""

                // Si la donación ya está finalizada, no hacemos nada
                if (estadoActual == "finalizada") {
                    return@runTransaction
                }

                // Realizar la actualización del estado
                val actualizaciones = mutableMapOf<String, Any>(
                    "estado" to "finalizada"
                )

                // Realizar la actualización
                transaction.update(donacionRef, actualizaciones)
            }.await()

            Log.d("terminarDonacion", "Donación finalizada correctamente.")
        } catch (e: Exception) {
            Log.e("terminarDonacion", "Error al finalizar la donación.", e)
            throw e
        }
    }


    // Función para confirmar la entrega
    // Función para confirmar la entrega
    suspend fun confirmarEntrega(reservaId: String) {
        try {
            // Obtener referencia a la reserva
            val reservaRef = db.collection("reservas").document(reservaId)

            // Obtener datos de la reserva
            val reservaSnapshot = reservaRef.get().await()

            // Obtener el campo 'donacionId' como un DocumentReference
            val donacionRef = reservaSnapshot.getDocumentReference("donacionId")
                ?: throw IllegalStateException("Donación ID no encontrada en la reserva.")

            // Obtener el ID del documento de la donación
            val donacionId = donacionRef.id

            // Obtener el peso reservado de la reserva
            val pesoReservado = reservaSnapshot.getLong("pesoReservado")?.toInt()
                ?: throw IllegalStateException("Peso reservado no encontrado en la reserva.")

            // Actualizar el estado de la reserva
            reservaRef.update("estado", "entregado").await()

            // Modificar la donación asociada
            modificarPesoDonacion(donacionId, pesoReservado)

            Log.d("confirmarEntrega", "Entrega confirmada para la reserva $reservaId.")
        } catch (e: Exception) {
            Log.e("confirmarEntrega", "Error al confirmar entrega para la reserva $reservaId.", e)
            throw e
        }
    }


    suspend fun actualizarFechaFin(donacionId: String, numeroDias: Int) {
        try {
            val donacionRef = db.collection("donaciones").document(donacionId)
            val donacionSnapshot = donacionRef.get().await()

            donacionSnapshot.getTimestamp("fechaFin")?.toDate()?.let {
                val calendar = Calendar.getInstance().apply {
                    time = it
                    add(Calendar.DAY_OF_YEAR, numeroDias)
                }
                donacionRef.update("fechaFin", calendar.time).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun registrarDonacion(
        donanteId: String,
        alimento: String,
        pesoTotal: Double,
        duracionDias: Int,
        descripcion: String,
        tipoAlimento: String,  // Nuevo parámetro
        requiereRefrigeracion: String,  // Nuevo parámetro
        esPedecedero: String,  // Nuevo parámetro
        imageUri: Uri,
        context: Context
    ): String {
        val fechaInicio = Timestamp.now()
        val fechaFin = Timestamp(Date(fechaInicio.toDate().time + duracionDias * 86400000L))

        val imagenURL = uploadImage(context, imageUri)
        if (imagenURL.isEmpty()) return "Error al subir la imagen. Intente de nuevo."

        val donacionData = hashMapOf(
            "alimento" to alimento,
            "descripcion" to descripcion,
            "donanteId" to db.document("users/$donanteId"),
            "estado" to "activo",
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "pesoEntregado" to 0,
            "pesoReservado" to 0,
            "pesoTotal" to pesoTotal,
            "imagenURL" to imagenURL,
            "notiRecibida" to false,
            "tipoAlimento" to tipoAlimento,  // Almacenamos el nuevo campo
            "requiereRefrigeracion" to requiereRefrigeracion,  // Almacenamos el nuevo campo
            "esPedecedero" to esPedecedero  // Almacenamos el nuevo campo
        )

        return try {
            db.collection("donaciones").add(donacionData).await()
            "Donación registrada exitosamente."
        } catch (e: Exception) {
            "Error al registrar donación: ${e.message}"
        }
    }

    suspend fun uploadImage(context: Context, uri: Uri): String {
        return try {
            val file = getFileFromUri(context, uri) ?: return ""
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

            val retrofit = Retrofit.Builder()
                .baseUrl("https://marcelomp3.pythonanywhere.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val response = apiService.uploadImage(body)
            if (response.isSuccessful) response.body()?.get("location") ?: "" else ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    @SuppressLint("Range")
    fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)) else null
        } ?: return null

        val tempFile = File(context.cacheDir, fileName).apply { createNewFile() }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream -> inputStream.copyTo(outputStream) }
        }
        return tempFile
    }

    private fun DocumentSnapshot.toReserva(): Reserva {
        val id = this.id
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""
        val palabraClave = this.getString("palabraClave") ?: ""
        val pesoReservado = (this.get("pesoReservado") as? Long ?: 0).toInt()
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""
        val estado = this.getString("estado") ?: "pendiente"
        return Reserva(id, donacionId, palabraClave, pesoReservado, usuarioReservadorId, estado)
    }

    private fun DocumentSnapshot.toDonacion(): Donacion {
        val id = this.id
        val alimento = this.getString("alimento") ?: "Desconocido"
        val tipoAlimento = this.getString("tipoAlimento") ?: ""  // Nuevo campo, valor vacío si no existe
        val requiereRefrigeracion = this.getString("requiereRefrigeracion") ?: ""  // Nuevo campo, valor vacío si no existe
        val esPedecedero = this.getString("esPedecedero") ?: ""  // Nuevo campo, valor vacío si no existe

        val pesoTotal = (this.get("pesoTotal") as? Number)?.toInt() ?: 0
        val pesoReservado = (this.get("pesoReservado") as? Number)?.toInt() ?: 0
        val pesoEntregado = (this.get("pesoEntregado") as? Number)?.toInt() ?: 0
        val descripcion = this.getString("descripcion") ?: "No disponible"
        val imagenURL = this.getString("imagenURL") ?: ""
        val fechaInicio = this.getTimestamp("fechaInicio")?.toDate()
        val fechaFin = this.getTimestamp("fechaFin")?.toDate()
        val duracion = calcularDuracion(fechaInicio, fechaFin)
        val estado = this.getString("estado") ?: "Desconocido"

        return Donacion(
            id,
            "$alimento $pesoTotal kg",
            duracion,
            imagenURL,
            descripcion,
            pesoReservado,
            pesoEntregado,
            pesoTotal,
            estado,
            tipoAlimento,
            requiereRefrigeracion,
            esPedecedero
        )
    }

}
