package com.mano_solidaria.app.donadores

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mano_solidaria.app.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class RegistrarDonacionActivity : AppCompatActivity() {
    private lateinit var alimentoEditText: EditText
    private lateinit var pesoEditText: EditText
    private lateinit var duracionEditText: EditText
    private lateinit var infoAdicionalEditText: EditText
    private lateinit var imgFoto: ImageView
    private lateinit var btnRegistrar: Button
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
        initUI()
        db = FirebaseFirestore.getInstance()

        findViewById<Button>(R.id.btnElegirFoto).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        btnRegistrar.setOnClickListener {
            registrarDonacion()
        }
    }

    private fun initUI() {
        alimentoEditText = findViewById(R.id.alimentoEditText)
        pesoEditText = findViewById(R.id.pesoEditText)
        duracionEditText = findViewById(R.id.duracionEditText)
        infoAdicionalEditText = findViewById(R.id.infoAdicionalEditText)
        imgFoto = findViewById(R.id.imgFoto)
        btnRegistrar = findViewById(R.id.btnRegistrar)
    }

    private fun registrarDonacion() {
        val alimento = alimentoEditText.text.toString().trim()
        val pesoTotal = pesoEditText.text.toString().toDoubleOrNull()
        val duracionDias = duracionEditText.text.toString().toIntOrNull()
        val descripcion = infoAdicionalEditText.text.toString().trim()
        val donanteId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fechaInicio = Timestamp.now()
        val fechaFin = duracionDias?.let { Timestamp(Date(fechaInicio.toDate().time + it * 86400000L)) }

        if (alimento.isEmpty() || pesoTotal == null || duracionDias == null || descripcion.isEmpty()) {
            showToast("Por favor, complete todos los campos correctamente.")
            return
        }

        imageUri?.let {
            uploadImage(it) { imagenURL ->
                if (imagenURL.isNotEmpty()) {
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

                    db.collection("donaciones").add(donacionData)
                        .addOnSuccessListener {
                            showToast("Donación registrada exitosamente.")
                            finish()  // Cierra la actividad después de mostrar el mensaje de éxito
                        }
                        .addOnFailureListener { e ->
                            showToast("Error al registrar donación: ${e.message}")
                        }
                    loadImage(imagenURL)
                } else {
                    showToast("Error al subir la imagen. Intente de nuevo.")
                }
            }
        } ?: showToast("Por favor seleccione una imagen.")
    }

    private fun uploadImage(uri: Uri, callback: (String) -> Unit) {
        val file = getFileFromUri(uri) ?: run {
            showToast("No se pudo obtener el archivo de imagen")
            return
        }

        val requestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

        Retrofit.Builder()
            .baseUrl("https://marcelomp3.pythonanywhere.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
            .uploadImage(body)
            .enqueue(object : Callback<Map<String, String>> {
                override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                    callback(response.body()?.get("location") ?: "")
                }

                override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                    showToast("Error de red: ${t.message}")
                }
            })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val fileName = contentResolver.query(uri, null, null, null, null)?.use {
            it.takeIf { it.moveToFirst() }?.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        } ?: return null

        val tempFile = File(cacheDir, fileName).apply { createNewFile() }
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        return tempFile
    }

    private fun loadImage(imagenUrl: String) {
        Glide.with(this).load(imagenUrl).into(imgFoto)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
