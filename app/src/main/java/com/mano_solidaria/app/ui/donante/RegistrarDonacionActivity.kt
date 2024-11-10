package com.mano_solidaria.app.ui.donante

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mano_solidaria.app.R

class RegistrarDonacionActivity : AppCompatActivity() {
    private lateinit var alimentoEditText: EditText
    private lateinit var pesoEditText: EditText
    private lateinit var duracionEditText: EditText
    private lateinit var infoAdicionalEditText: EditText
    private lateinit var imgFoto: ImageView
    private lateinit var btnRegistrar: Button
    private lateinit var btnElegirFoto: Button

    // Lanzador de actividad para obtener la imagen seleccionada
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar_donacion)

        // Inicialización de vistas
        alimentoEditText = findViewById(R.id.alimentoEditText)
        pesoEditText = findViewById(R.id.pesoEditText)
        duracionEditText = findViewById(R.id.duracionEditText)
        infoAdicionalEditText = findViewById(R.id.infoAdicionalEditText)
        imgFoto = findViewById(R.id.imgFoto)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnElegirFoto = findViewById(R.id.btnElegirFoto)

        // Configuración de márgenes para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar el lanzador para seleccionar una imagen
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val selectedImageUri: Uri? = result.data?.data
                imgFoto.setImageURI(selectedImageUri) // Muestra la imagen seleccionada
            }
        }

        // Listener para el botón "Registrar"
        btnRegistrar.setOnClickListener {
            registrarDonacion()
        }

        // Listener para el botón "Elegir Foto"
        btnElegirFoto.setOnClickListener {
            elegirFoto()
        }
    }

    // Método para registrar la donación
    private fun registrarDonacion() {
        val alimento = alimentoEditText.text.toString()
        val peso = pesoEditText.text.toString()
        val duracion = duracionEditText.text.toString()
        val infoAdicional = infoAdicionalEditText.text.toString()

        if (alimento.isNotEmpty() && peso.isNotEmpty() && duracion.isNotEmpty()) {
            // Aquí puedes agregar el código para guardar la donación (en una base de datos o realizar alguna otra acción)
            // Por ejemplo, mostrar un mensaje de éxito
            // Toast.makeText(this, "Donación registrada exitosamente", Toast.LENGTH_SHORT).show()
        } else {
            // Muestra un error si los campos no están completos
            // Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para elegir una foto desde la galería
    private fun elegirFoto() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
}
