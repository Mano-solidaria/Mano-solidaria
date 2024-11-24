package com.mano_solidaria.app.data.UserRepository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    // Función para obtener los datos del usuario (nombre, foto y rol)
    suspend fun getUserData(): UserData? {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: return null // Retorna null si el usuario no está logueado

            val userDoc = db.collection("users").document(userId).get().await()

            return if (userDoc.exists()) {
                userDoc.toObject(UserData::class.java) // Devuelve los datos del usuario si existen
            } else {
                null // Si los datos del usuario no existen
            }
        } catch (e: Exception) {
            return null // Si ocurre un error, retorna null
        }
    }
}

data class UserData(
    val UsuarioNombre: String = "",
    val UsuarioImagen: String = "",
    val UsuarioRol: String = ""
)
