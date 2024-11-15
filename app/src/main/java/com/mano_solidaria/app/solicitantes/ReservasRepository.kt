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

data class Reserva(
    val id: String,
    val donacionId: String,
    val palabraClave: String,
    val pesoReservado: Int,
    val usuarioReservadorId: String,
    val estado: String,
    val nombreDonante: String
)

object ReservasRepository {
    private val db = FirebaseFirestore.getInstance()
    fun currentUser(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getReservas(): List<Reserva> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return emptyList()
            val snapshots = db.collection("reservas")
                .whereEqualTo("usuarioReservador", db.collection("users").document(userId))
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

    private suspend fun getUserNameById(userId: String): String {
        val userSnapshot = db.collection("users").document(userId).get().await()
        return userSnapshot.getString("UsuarioNombre") ?: "Nombre desconocido"
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

    private suspend fun DocumentSnapshot.toReserva(): Reserva {
        val id = this.id
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""  // Obtener el ID de la donación
        val palabraClave = this.getString("palabraClave") ?: ""  // Obtener la palabra clave
        val pesoReservado = (this.get("pesoReservado") as? Long ?: 0).toInt()  // Obtener el peso reservado
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""  // Obtener el ID del reservador
        val estado = this.getString("estado") ?: "pendiente"  // Obtener el estado (si está pendiente o no)

        val donacionIdRef = this.getDocumentReference("donacionId") // Referencia al documento de la colección "donaciones"

        // Obtener el documento de la donación
        val donacionSnapshot = donacionIdRef?.get()?.await()

        // Obtener el donanteId de la donación
        val donanteId = donacionSnapshot?.getDocumentReference("donanteId")?.id ?: ""

        // Obtener el nombre del donante usando el donanteId
        val nombreDonante = if (donanteId.isNotEmpty()) {
            val userSnapshot = db.collection("users").document(donanteId).get().await()
            userSnapshot.getString("UsuarioNombre") ?: "Desconocido"
        } else {
            "Desconocido"
        }

        // Crear y retornar un objeto de tipo "Reserva"
        return Reserva(id, donacionId, palabraClave, pesoReservado, usuarioReservadorId, estado, nombreDonante)
    }
}