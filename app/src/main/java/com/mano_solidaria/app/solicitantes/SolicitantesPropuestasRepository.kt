package com.mano_solidaria.app.donadores

import android.content.ContentValues.TAG
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.mano_solidaria.app.Utils.calcularDuracion
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DonacionRoko(
    val id: DocumentReference,
    val alimentoNombre: String,
    val donanteId: DocumentReference,
    val tiempoRestante: String,
    val imagenUrl: String,
    val descripcion: String,
    val pesoReservado: Int,
    val pesoEntregado: Int,
    val pesoTotal: Int,
    val estado: String
)

data class ReservaRoko(
    val dispararNoti: Boolean,
    val donacionId: DocumentReference,
    val donanteId: DocumentReference,
    val estado: String,
    val notiRecibida: Boolean,
    val palabraClave: String,
    val pesoReservado: Int,
    val usuarioReservador: DocumentReference,
)

data class UsuarioRoko(
    val imagenUrl: String? = null,
    val usuarioDocumentRef: DocumentReference? = null,
    val usuarioDireccion: String? = null,
    val usuarioMail: String? = null,
    val usuarioNombre: String? = null,
    val usuarioUbicacion: GeoPoint = GeoPoint(0.0, 0.0),
    val suscriptores: List<DocumentReference> = emptyList()
)

object SolicitantesPropuestasRepository {

    private val _donaciones: MutableStateFlow<List<DonacionRoko>> = MutableStateFlow(emptyList())
    val donaciones: StateFlow<List<DonacionRoko>> = _donaciones

    private var _usuario: MutableStateFlow<UsuarioRoko> = MutableStateFlow(UsuarioRoko())
    val usuario : StateFlow<UsuarioRoko> = _usuario

    private val db = FirebaseFirestore.getInstance()

    fun currentUser(): String = FirebaseAuth.getInstance().currentUser!!.uid

    init {
        donaciones()
        user(currentUser())
    }

    fun donaciones(){
        CoroutineScope(Dispatchers.IO).launch{
            getAllDonaciones()
        }
    }

    suspend fun getAllDonaciones(): List<DonacionRoko> {
        return try {
            val snapshots = db.collection("donaciones")
                .whereEqualTo("estado","activo")
                .get()
                .await()
            _donaciones.value = snapshots.documents.map { toDonacion(it) }
            snapshots.documents.map { toDonacion(it)}
        } catch (e: Exception) {
            emptyList<DonacionRoko>()
        }
    }

    fun user(id: String){
        CoroutineScope(Dispatchers.IO).launch{
            getUserById(id)
        }
    }

    suspend fun getUserById(documentId: String){
        try {
            val snapshots = db.collection("users").document(documentId).get().await()
            _usuario.value = snapshots.ToUser()
        } catch (e: Exception) {
            emptyList<DonacionRoko>()
        }
    }

    suspend fun getDonadorByRef(ref: DocumentReference): UsuarioRoko? {
        return try {
            val snapshots = db.collection("users").document(ref.id).get().await()
            snapshots.ToUser()
        } catch (e: Exception) {
            Log.d("ERRORRRR", "error al devolver donador por referencia")
            UsuarioRoko()
        }
    }


    fun addReservaInDb(
        reservaNueva: ReservaRoko,
        donacionRef: DocumentReference,
    ) {
        db.runTransaction { transaction ->
            val snapshot = transaction.get(donacionRef)
            val donacion = toDonacion(snapshot)

            val pesoRestante = donacion.pesoTotal - donacion.pesoEntregado - donacion.pesoReservado

            if (pesoRestante >= reservaNueva.pesoReservado) {
                val nuevoPesoReservado = donacion.pesoReservado + reservaNueva.pesoReservado
                
                transaction.update(donacionRef, "pesoReservado", nuevoPesoReservado)
                transaction.set(db.collection("reservas").document(), reservaNueva)
            }
        }.addOnSuccessListener {
            Log.d("Reservarr", "funciona reserva")
        }.addOnFailureListener { e ->
            Log.d("Reservarr", "falla reserva")
        }
    }

    // convertir el string con formato "lat/lng: (latitud,longitud)" a un GeoPoint
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





    private fun DocumentSnapshot.toReserva(): Reserva {
        val id = this.id
        val donacionId = this.getDocumentReference("donacionId")?.id ?: ""
        val palabraClave = this.getString("palabraClave") ?: ""
        val pesoReservado = (this.get("pesoReservado") as? Long ?: 0).toInt()
        val usuarioReservadorId = this.getDocumentReference("usuarioReservador")?.id ?: ""
        val estado = this.getString("estado") ?: "pendiente"
        return Reserva(id, donacionId, palabraClave, pesoReservado, usuarioReservadorId, estado)
    }

    private fun toDonacion(snap: DocumentSnapshot): DonacionRoko {
        val fechaInicio = snap.getTimestamp("fechaInicio")?.toDate()
        val fechaFin = snap.getTimestamp("fechaFin")?.toDate()
        val duracion = calcularDuracion(fechaInicio, fechaFin)
        return DonacionRoko(
            FirebaseFirestore.getInstance().collection("donaciones").document(snap.id),
            snap.getString("alimento") ?: "Alimento desconocido",
            snap.getDocumentReference("donanteId") ?:
                FirebaseFirestore.getInstance().document("users/desconocido") ,
            duracion,
            snap.getString("imagenURL") ?: "Imagen no encontrada",
            snap.getString("descripcion") ?: "Descripcion no disponible",
            snap.getLong("pesoReservado")?.toInt() ?: 0,
            snap.getLong("pesoEntregado")?.toInt() ?: 0,
            snap.getLong("pesoTotal")?.toInt() ?: 0,
            snap.getString("estado") ?: "Estado no valido")
    }

    private fun DocumentSnapshot.ToUser(): UsuarioRoko{
        val direccionString = this.getString("Usuarioubicacion") ?: "Desconocido"

        // Obtener el array de suscriptores como lista de cadenas
        val suscriptoresPaths = this.get("suscriptores") as? List<String> ?: emptyList()

        // Convertir las rutas de documentos en objetos DocumentReference
        val suscriptoresReferences = suscriptoresPaths.map {
            FirebaseFirestore.getInstance().document(it)
        }

        return UsuarioRoko(
            this.getString("UsuarioImagen") ?: null,
            FirebaseFirestore.getInstance().collection("users").document(this.id),
            this.getString("UsuarioDireccion") ?: "Direccion desconocida",
            this.getString("UsuarioMail") ?: "Desconocido",
            this.getString("UsuarioNombre") ?: "Nombre desconocido",
            stringToGeoPoint(direccionString),
            suscriptores = suscriptoresReferences
        )
    }

    fun suscribirseAlDonador(don: UsuarioRoko, usuarioDocumentRef: DocumentReference?) {
        db.collection("users").document(don.usuarioDocumentRef!!.id)
            .update("suscriptores", FieldValue.arrayUnion(usuarioDocumentRef) )
            .addOnSuccessListener { Log.d(TAG, "Donacion actualizado correctamente!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error actualizando donacion", e) }
    }

    fun desuscribirseAlDonador(don: UsuarioRoko, usuarioDocumentRef: DocumentReference?) {
        db.collection("users").document(don.usuarioDocumentRef!!.id)
            .update("suscriptores", FieldValue.arrayRemove(usuarioDocumentRef))
            .addOnSuccessListener { Log.d(TAG, "Usuario desuscrito correctamente del donador!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error al desuscribir al usuario", e) }
    }
}