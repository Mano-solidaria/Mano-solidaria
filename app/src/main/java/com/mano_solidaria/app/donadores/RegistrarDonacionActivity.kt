package com.mano_solidaria.app.donadores

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mano_solidaria.app.R
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import com.mano_solidaria.app.Utils.applySavedTheme

class RegistrarDonacionActivity : AppCompatActivity() {
    private lateinit var alimentoEditText: EditText
    private lateinit var pesoEditText: EditText
    private lateinit var duracionEditText: EditText
    private lateinit var infoAdicionalEditText: EditText
    private lateinit var imgFoto: ImageView
    private lateinit var btnRegistrar: Button
    private lateinit var btnElegirFoto: Button
    private var imageUri: Uri? = null
    val regex = Regex(".*\\d.*")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme(this)
        setContentView(R.layout.activity_registrar_donacion)
        initUI()

        btnRegistrar.setOnClickListener {
            registrarDonacion()
        }

        btnElegirFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            imgFoto.setImageURI(imageUri)
        }
    }

    private fun registrarDonacion() {
        val alimento = alimentoEditText.text.toString().trim()
        val pesoTotal = pesoEditText.text.toString().toDoubleOrNull()
        val duracionDias = duracionEditText.text.toString().toIntOrNull()
        val descripcion = infoAdicionalEditText.text.toString().trim()
        val donanteId = Repository.currentUser() ?: ""

        val donacionData = mutableMapOf(
            "Alimento" to alimento,
            "Peso" to pesoEditText.text.toString(),
            "Duración" to duracionEditText.text.toString(),
            "Informacion adicional" to descripcion,
        )

        try {
            validateString(donacionData)

            if (regex.containsMatchIn(alimentoEditText.text.toString())){
                throw IllegalArgumentException("")
            }

            if (imageUri == null) {
                Toast.makeText(this, "Por favor seleccione una imagen.", Toast.LENGTH_SHORT).show()
                return
            }

            imageUri?.let { uri ->
                lifecycleScope.launch {
                    val resultado = Repository.registrarDonacion(
                        donanteId,
                        alimento,
                        pesoTotal!!,
                        duracionDias!!,
                        descripcion,
                        uri,
                        this@RegistrarDonacionActivity
                    )
                    Toast.makeText(this@RegistrarDonacionActivity, resultado, Toast.LENGTH_SHORT).show()
                    if (resultado == "Donación registrada exitosamente.") { //finish()
                        val intent = Intent(this@RegistrarDonacionActivity, MainDonadoresActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK //PRIMERA SOLUCION HASTA QUE SE VUELVA A HABLAR
                        startActivity(intent)
                    }
                    else Toast.makeText(this@RegistrarDonacionActivity, "Error al registrar donación: $resultado", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IllegalArgumentException){
                validateFields()
        }
    }

    private fun validateString(data: MutableMap<String, String>) {
        for ((key, value) in data) {
            if (value.isEmpty()) {
                throw IllegalArgumentException("El campo ${key} está vacío.")
            }
        }
    }

    private fun validateFields() {

        if (alimentoEditText.text.toString().trim().isEmpty()) {
            alimentoEditText.setError("Por favor, ingrese el nombre del alimento")
        }else if (regex.containsMatchIn(alimentoEditText.text.toString())){
            alimentoEditText.setError("El nombre del alimento no debe contener números")
        } else {
            alimentoEditText.setError(null)
        }

        if (pesoEditText.text.toString().trim().isEmpty()) {
            pesoEditText.setError("Por favor, ingrese la cantidad de peso a donar")
        } else {
            pesoEditText.setError(null)
        }

        if (duracionEditText.text.toString().trim().isEmpty()) {
            duracionEditText.setError("Por favor, ingrese la duracion de la donación")
        } else {
            duracionEditText.setError(null)
        }

        if (infoAdicionalEditText.text.toString().trim().isEmpty()) {
            infoAdicionalEditText.setError("Por favor, ingrese informacion adicional del alimento")
        } else {
            infoAdicionalEditText.setError(null)
        }
    }

    private fun initUI() {
        alimentoEditText = findViewById(R.id.alimentoEditText)
        pesoEditText = findViewById(R.id.pesoEditText)
        duracionEditText = findViewById(R.id.duracionEditText)
        infoAdicionalEditText = findViewById(R.id.infoAdicionalEditText)
        imgFoto = findViewById(R.id.imgFoto)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnElegirFoto = findViewById(R.id.btnElegirFoto)
    }
}
