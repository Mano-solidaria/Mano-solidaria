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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mano_solidaria.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationService : Service() {

    private var listenerRegistration: ListenerRegistration? = null
    var alimento = "Sin alimento"
    var pesoTotal :Long = 0
    var pesoReser:Long = 0

    override fun onCreate() {
        super.onCreate()
        startFirestoreListener()
    }

    private fun startFirestoreListener() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (userId == null) {
            Log.w("NotificationService", "Usuario no autenticado, no se puede escuchar Firestore")
            stopSelf()
            return
        }

        val collectionRef = db.collection("reservas").whereEqualTo("donadorId", db.collection("users").document(userId))

        listenerRegistration = collectionRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.w("FirestoreListener", "Error al escuchar cambios", error)
                return@addSnapshotListener
            }

            for (change in snapshots?.documentChanges ?: emptyList()) {
                val reservaSnapshot = change.document
                val docRefDonacion = reservaSnapshot.getDocumentReference("donacionId")
                docRefDonacion?.get()?.addOnSuccessListener { snapshot ->
                    pesoTotal = snapshot.getLong("pesoTotal") ?: 0L
                    alimento = snapshot.getString("alimento") ?: "Sin alimento"
                    pesoReser = reservaSnapshot.getLong("pesoReservado") ?: 0L

                    when (change.type) {
                        DocumentChange.Type.ADDED -> showNotification("Propuesta: $pesoTotal KG $alimento", "Reserva: $pesoReser KG","Tiene una nueva reserva")

                        DocumentChange.Type.MODIFIED -> showNotification("Propuesta: $pesoTotal KG $alimento", "Reserva: $pesoReser KG","Se ha modificado una reserva")

                        DocumentChange.Type.REMOVED -> showNotification("Propuesta: $pesoTotal KG $alimento", "Reserva: $pesoReser KG","Se ha eliminado una reserva")
                    }

                }?.addOnFailureListener { e ->
                    Log.w("Firestore", "Error al obtener el campo 'alimento'", e)
                }
            }
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove() // Detener el listener cuando el servicio se destruya
    }

    @SuppressLint("RemoteViewLayout", "MissingPermission")
    private fun showNotification(alimentoPropuesto: String, alimentoReservado: String, tipoNotificacion: String) {

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

        val customView = RemoteViews(packageName, R.layout.notification).apply {
            setTextViewText(R.id.tituloNotificacion, tipoNotificacion)
            setTextViewText(R.id.detalleNotificacion, alimentoPropuesto)
            setTextViewText(R.id.reservaNotificacion, alimentoReservado)
            setImageViewResource(R.id.iconoApp, R.drawable.ic_launcher_foreground)
        }

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val currentDate: String = dateFormat.format(calendar.time)

        val builder = NotificationCompat.Builder(this, MY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(tipoNotificacion)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine(currentDate)
                    .addLine("-------------------------------")
                    .addLine(alimentoPropuesto)
                    .addLine(alimentoReservado)
            )
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1, builder)
    }

}