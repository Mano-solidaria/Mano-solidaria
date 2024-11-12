package com.mano_solidaria.app.donadores

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mano_solidaria.app.R
import java.util.*

class RegistrarDonacionActivity : AppCompatActivity() {
    private lateinit var alimentoEditText: EditText
    private lateinit var pesoEditText: EditText
    private lateinit var duracionEditText: EditText
    private lateinit var infoAdicionalEditText: EditText
    private lateinit var imgFoto: ImageView
    private lateinit var btnRegistrar: Button
    private lateinit var btnElegirFoto: Button
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                imgFoto.setImageURI(imageUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_donacion)

        alimentoEditText = findViewById(R.id.alimentoEditText)
        pesoEditText = findViewById(R.id.pesoEditText)
        duracionEditText = findViewById(R.id.duracionEditText)
        infoAdicionalEditText = findViewById(R.id.infoAdicionalEditText)
        imgFoto = findViewById(R.id.imgFoto)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnElegirFoto = findViewById(R.id.btnElegirFoto)

        db = FirebaseFirestore.getInstance()

        btnElegirFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        btnRegistrar.setOnClickListener {
            registrarDonacion()
        }
    }

    private fun registrarDonacion() {
        val alimento = alimentoEditText.text.toString().trim()
        val pesoTotal = pesoEditText.text.toString().toDoubleOrNull()
        val duracionDias = duracionEditText.text.toString().toIntOrNull()
        val descripcion = infoAdicionalEditText.text.toString().trim()
        val donanteId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fechaInicio = Timestamp.now()
        val fechaFin = if (duracionDias != null) {
            Timestamp(Date(fechaInicio.toDate().time + duracionDias * 24 * 60 * 60 * 1000L))
        } else {
            null
        }

        if (alimento.isEmpty() || pesoTotal == null || duracionDias == null || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos correctamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // URL de la imagen
        val imagenURL = "https://imagenes.elpais.com/resizer/v2/4HWUG3I7PVA7VKAWLQVCBUL4E4.jpg?auth=114b5a92f5b098e2c67d9642883a4e7a3010b6020bac133f60f1c766f565b78f&width=1200"

        // Datos de la donación, incluyendo la URL de la imagen
        val donacionData = hashMapOf(
            "alimento" to alimento,
            "descripcion" to descripcion,
            "donanteId" to db.document("users/$donanteId"),
            "estado" to "activo",
            "fechaInicio" to fechaInicio,
            "fechaFin" to fechaFin,
            "pesoEntregado" to 0,
            "pesoReservado" to 0,
            "pesoTotal" to pesoTotal, // Almacenado como número
            "imagenURL" to imagenURL // Añadido el campo de la URL de la imagen
        )

        db.collection("donaciones")
            .add(donacionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Donación registrada exitosamente.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al registrar donación: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
