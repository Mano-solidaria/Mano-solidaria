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
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Date

data class DonacionRoko(
    val id: String,
    val pesoAlimento: String,
    val tiempoRestante: String,
    val imagenUrl: String,
    val descripcion: String,
    val pesoReservado: Int,
    val pesoEntregado: Int,
    val pesoTotal: Int
)

data class ReservaRoko(
    val id: String,
    val donacionId: String,
    val palabraClave: String,
    val pesoReservado: Int,
    val usuarioReservadorId: String,
    val estado: String
)

object SolicitantesPropuestasRepository {
    private val db = FirebaseFirestore.getInstance()

    fun currentUser(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getDonaciones(): List<DonacionRoko> {
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

    suspend fun getAllDonaciones(): List<DonacionRoko> {
        return try {
            val snapshots = db.collection("donaciones")
                .get()
                .await()
            snapshots.documents.map { it.toDonacion() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDonacionById(id: String): DonacionRoko? {
        return try {
            val document = db.collection("donaciones").document(id).get().await()
            document.toDonacion()
        } catch (e: Exception) {
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

    suspend fun confirmarEntrega(reservaId: String) {
        try {
            val reservaRef = db.collection("reservas").document(reservaId)
            reservaRef.update("estado", "entregado").await()
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun registrarDonacion(donanteId: String, alimento: String, pesoTotal: Double, duracionDias: Int, descripcion: String, imageUri: Uri, context: Context): String {
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
            "imagenURL" to imagenURL
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

    private fun DocumentSnapshot.toDonacion(): DonacionRoko {
        val id = this.id
        val alimento = this.getString("alimento") ?: "Desconocido"
        val pesoTotal = (this.get("pesoTotal") as? Double ?: 0.0).toInt()
        val pesoReservado = (this.get("pesoReservado") as? Double ?: 0.0).toInt()
        val pesoEntregado = (this.get("pesoEntregado") as? Double ?: 0.0).toInt()
        val descripcion = this.getString("descripcion") ?: "No disponible"
        val imagenURL = this.getString("imagenURL") ?: ""
        val fechaInicio = this.getTimestamp("fechaInicio")?.toDate()
        val fechaFin = this.getTimestamp("fechaFin")?.toDate()
        val duracion = calcularDuracion(fechaInicio, fechaFin)
        return DonacionRoko(id, "$pesoTotal kg de $alimento", duracion, imagenURL, descripcion, pesoReservado, pesoEntregado, pesoTotal)
    }
}