package com.mano_solidaria.app.solicitantes

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mano_solidaria.app.R

class NotificationServiceSolicitante : Service() {

    private var listenerRegistrationDonaciones: ListenerRegistration? = null
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    override fun onCreate() {
        super.onCreate()
        donacionesListener()
    }

    private fun donacionesListener() {

        if (userId == null) {
            Log.w("NotificationService", "Usuario no autenticado, no se puede escuchar Firestore")
            stopSelf()
            return
        }

        val donaciones = db.collection("donaciones")

        listenerRegistrationDonaciones = donaciones.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("FirestoreListener", "Error al escuchar cambios en donaciones", error)
                return@addSnapshotListener
            }

            for (change in snapshots?.documentChanges ?: emptyList()) {
                val donacionSnapshot = change.document
                val id = donacionSnapshot.id
                val donadorId = donacionSnapshot.getString("donanteId") ?: ""
                val user = db.collection("users").document(donadorId)
                val alimento = donacionSnapshot.getString("alimento") ?: "Desconocido"
                val pesoTotal = donacionSnapshot.getLong("pesoTotal") ?: 0L
                val pesoEntregado = donacionSnapshot.getLong("pesoEntregado") ?: 0L
                val notiRecibida = donacionSnapshot.getBoolean("notiRecibida") ?: false
                var suscriptores: List<String> = emptyList()
                user.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Extraemos la lista de suscriptores
                            suscriptores = document.get("suscriptores") as? List<String> ?: emptyList()
                        }

                        when (change.type) {
                            DocumentChange.Type.ADDED -> {
                                for (suscriptor in suscriptores) {
                                    println(suscriptor)
                                }
                            }

                            DocumentChange.Type.MODIFIED -> {

                                for (suscriptor in suscriptores) {
                                    println(suscriptor)
                                }
                            }

                            DocumentChange.Type.REMOVED -> {
                                Toast.makeText(this, "Donacion eliminada correctamente", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }}
        }
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistrationDonaciones?.remove()
    }

    @SuppressLint("RemoteViewLayout", "MissingPermission")
    private fun showNotification(nombre: String, alimentoPropuesto: String, alimentoReservado: String, tipoNotificacion: String) {

        val MY_CHANNEL_ID = "Reservas"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MY_CHANNEL_ID,
                "Canal de reservas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal de notificaciones de reservas"
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, MY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(tipoNotificacion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(nombre)
                    .addLine("-------------------------------")
                    .addLine(alimentoPropuesto)
                    .addLine(alimentoReservado)
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), builder)
    }

}