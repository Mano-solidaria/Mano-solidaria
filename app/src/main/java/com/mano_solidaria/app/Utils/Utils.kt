package com.mano_solidaria.app.Utils



import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import java.util.TimeZone


val db = FirebaseFirestore.getInstance()

fun calcularDuracion(fechaFin: Date?): String {
    return if (fechaFin != null) {
        // Obtener la fecha actual en UTC-3
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-3"))
        val fechaActual = calendar.time

        // Calcular la duración entre la fecha actual y fechaFin
        val duracion = fechaFin.time - fechaActual.time
        "${((duracion / (1000 * 60 * 60 * 24))+1).toInt()}"
    } else {
        "-"
    }
}

fun cambioEstado(){
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser!!.uid
    val collections = db.collection("donaciones")
        .whereEqualTo("donanteId",db.collection("users").document(userId))
        .whereEqualTo("estado", "activo")
    collections.get().addOnSuccessListener { colecciones ->
        for (donacion in colecciones) {
            val fechaFin = donacion.getTimestamp("fechaFin")?.toDate()
            val tiempoRestante = calcularDuracion(fechaFin).toDouble()
            if (tiempoRestante <= 0){
                updateStatus(donacion.id)
            }
        }
    }.addOnFailureListener { e ->
        println("Error al obtener las donaciones: ${e.message}")
    }
}

private fun updateStatus(id: String) {
    val donacion = db.collection("donaciones").document(id)
    val reservas = db.collection("reservas").whereEqualTo("donacionId",donacion)
    reservas.get()
        .addOnSuccessListener { reserva ->
            if (reserva.isEmpty) {
                donacion.update("estado", "finalizada")
                    .addOnSuccessListener {
                        println("Estado actualizado a 'finalizado'")
                    }
                    .addOnFailureListener { e ->
                        println("Error al actualizar el estado: ${e.message}")
                    }
            } else {
                donacion.update("estado", "pendiente")
                    .addOnSuccessListener {
                        println("Estado actualizado a 'pendiente'")
                    }
                    .addOnFailureListener { e ->
                        println("Error al actualizar el estado: ${e.message}")
                    }
            }
        }
        .addOnFailureListener { e ->
            println("Error al verificar reservas: ${e.message}")
        }
}


