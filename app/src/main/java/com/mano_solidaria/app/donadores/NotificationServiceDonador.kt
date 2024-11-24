package com.mano_solidaria.app.donadores

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

class NotificationServiceDonador : Service() {

    private var listenerRegistrationReservas: ListenerRegistration? = null
    private var listenerRegistrationDonaciones: ListenerRegistration? = null
    var dispararNoti = true
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    override fun onCreate() {
        super.onCreate()
        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        reservasListener()
        donacionesListener()
    }

    private fun reservasListener(){

        if (userId == null) {
            Log.w("NotificationService", "Usuario no autenticado, no se puede escuchar Firestore")
            stopSelf()
            return
        }

        val collectionRef = db.collection("reservas").whereEqualTo("donanteId", db.collection("users").document(userId))

        listenerRegistrationReservas = collectionRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("FirestoreListener", "Error al escuchar cambios", error)
                return@addSnapshotListener
            }

            for (change in snapshots?.documentChanges ?: emptyList()) {
                val reservaSnapshot = change.document
                val id = reservaSnapshot.id
                val docRefDonacion = reservaSnapshot.getDocumentReference("donacionId")
                val docRefSolicitante = reservaSnapshot.getDocumentReference("usuarioReservador")
                var pesoReser = reservaSnapshot.getLong("pesoReservado") ?: 0L
                var notiRecibida = reservaSnapshot.getBoolean("notiRecibida") ?: false
                dispararNoti = reservaSnapshot.getBoolean("dispararNoti") ?: true
                docRefDonacion?.get()?.addOnSuccessListener { donacion ->
                    var pesoTotal = donacion.getLong("pesoTotal") ?: 0L
                    var alimento = donacion.getString("alimento") ?: "Sin alimento"
                    docRefSolicitante?.get()?.addOnSuccessListener { solicitante ->
                        var nombre = solicitante.getString("UsuarioNombre") ?: "UserContento"
                        when (change.type) {
                            DocumentChange.Type.ADDED ->
                                if (!notiRecibida) {
                                    actualizarEstadoNotiRese(id)
                                    showNotification(
                                        nombre!!,
                                        "Propuesta: $pesoTotal KG $alimento",
                                        "Reserva: $pesoReser KG",
                                        "Tiene una nueva reserva"
                                    )
                                    dispararNoti = reservaSnapshot.getBoolean("dispararNoti") ?: false
                                }

                            DocumentChange.Type.MODIFIED -> //De momento no se es posible cambiar el estado
                                if (dispararNoti) {
                                    showNotification(
                                        nombre!!,
                                        "Propuesta: $pesoTotal KG $alimento",
                                        "Reserva: $pesoReser KG",
                                        "Se ha modificado una reserva"
                                    )
                                }

                            DocumentChange.Type.REMOVED -> showNotification(
                                nombre!!,
                                "Propuesta: $pesoTotal KG $alimento",
                                "Reserva: $pesoReser KG",
                                "Se ha eliminado una reserva"
                            )
                        }
                    }?.addOnFailureListener { e ->
                        Log.w("Firestore", "Error al obtener los datos del donador", e)
                    }
                }?.addOnFailureListener { e ->
                    Log.w("Firestore", "Error al obtener los datos de la donacion", e)
                }
            }
        }
    }

    private fun donacionesListener() {

        if (userId == null) {
            Log.w("NotificationService", "Usuario no autenticado, no se puede escuchar Firestore")
            stopSelf()
            return
        }
        val user = db.collection("users").document(userId)
        val collectionRefDonaciones = db.collection("donaciones").whereEqualTo("donanteId", db.collection("users").document(userId))


        listenerRegistrationDonaciones = collectionRefDonaciones.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("FirestoreListener", "Error al escuchar cambios en donaciones", error)
                return@addSnapshotListener
            }

            for (change in snapshots?.documentChanges ?: emptyList()) {
                val donacionSnapshot = change.document
                val id = donacionSnapshot.id
                val alimento = donacionSnapshot.getString("alimento") ?: "Desconocido"
                val pesoTotal = donacionSnapshot.getLong("pesoTotal") ?: 0L
                val pesoEntregado = donacionSnapshot.getLong("pesoEntregado") ?: 0L
                val notiRecibida = donacionSnapshot.getBoolean("notiRecibida") ?: false
                when (change.type) {
                    DocumentChange.Type.ADDED -> {
                        if (donacionSnapshot.metadata.hasPendingWrites()) {
                            Toast.makeText(
                                this,
                                "Donacion agregada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    DocumentChange.Type.MODIFIED -> {
                        val estado = donacionSnapshot.getString("estado") ?: "activo"
                        if(!notiRecibida && (estado.lowercase() == "finalizado" || pesoEntregado == pesoTotal)){
                            actualizarEstadoNotiDona(id)
                            showNotification(
                                "Estado: Finalizado",
                                "Propuesta: $pesoTotal KG $alimento",
                                "",
                                "Una propuesta ha finalizado"
                            )
                        }
                    }

                    DocumentChange.Type.REMOVED -> {
                        Toast.makeText(this, "Donacion eliminada correctamente", Toast.LENGTH_SHORT).show()
                    }
                }
            }
//            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistrationReservas?.remove() // Detener el listener cuando el servicio se destruya
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

    private fun actualizarEstadoNotiRese(id: String) {
        db.collection("reservas").document(id)
            .update(
                "notiRecibida", true,
                "dispararNoti", false,
            )
            .addOnSuccessListener {
                Log.d("Firestore", "Campo actualizado correctamente")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al actualizar el campo", e)
            }
    }

    private fun actualizarEstadoNotiDona(id: String) {
        db.collection("donaciones").document(id)
            .update(
                "notiRecibida", true,
            )
            .addOnSuccessListener {
                Log.d("Firestore", "Campo actualizado correctamente")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error al actualizar el campo", e)
            }
    }

}
