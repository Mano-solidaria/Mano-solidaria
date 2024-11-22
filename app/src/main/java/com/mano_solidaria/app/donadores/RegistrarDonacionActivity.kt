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


        if (alimento.isEmpty() || pesoTotal == null || duracionDias == null || descripcion.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos correctamente.", Toast.LENGTH_SHORT).show()
            return
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
                    pesoTotal,
                    duracionDias,
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
