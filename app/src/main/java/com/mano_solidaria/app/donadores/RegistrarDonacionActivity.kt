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
import android.widget.ArrayAdapter
import android.widget.Spinner
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

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            imgFoto.setImageURI(imageUri)
        }
    }



    private lateinit var tipoAlimentoSpinner: Spinner
    private lateinit var requiereRefrigeracionSpinner: Spinner
    private lateinit var esPedecederoSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme(this)
        setContentView(R.layout.activity_registrar_donacion)
        initUI()



        // Cargar las opciones en los Spinners
        cargarOpcionesSpinner()

        btnRegistrar.setOnClickListener {
            registrarDonacion()
        }

        btnElegirFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }


    }

    private fun registrarDonacion() {
        val alimento = alimentoEditText.text.toString().trim()
        val pesoTotal = pesoEditText.text.toString().toDoubleOrNull()
        val duracionDias = duracionEditText.text.toString().toIntOrNull()
        val descripcion = infoAdicionalEditText.text.toString().trim()
        val donanteId = Repository.currentUser() ?: ""

        // Obtenemos los nuevos valores de los Spinners
        val tipoAlimento = tipoAlimentoSpinner.selectedItem.toString()
        val requiereRefrigeracion = requiereRefrigeracionSpinner.selectedItem.toString()
        val esPedecedero = esPedecederoSpinner.selectedItem.toString()

        val donacionData = mutableMapOf(
            "Alimento" to alimento,
            "Peso" to pesoTotal.toString(),
            "Duración" to duracionDias.toString(),
            "Informacion adicional" to descripcion,
            "TipoAlimento" to tipoAlimento,  // Nuevo campo
            "RequiereRefrigeracion" to requiereRefrigeracion,  // Nuevo campo
            "EsPerecedero" to esPedecedero  // Nuevo campo
        )

        try {
            validateString(donacionData)

            if (regex.containsMatchIn(alimentoEditText.text.toString()) || descripcion.length > 500){
                throw IllegalArgumentException("")
            }

            if (imageUri == null) {
                Toast.makeText(this, R.string.donador_seleccionar_imagen, Toast.LENGTH_SHORT).show()
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
                        tipoAlimento,    // Pasamos el nuevo campo
                        requiereRefrigeracion,  // Pasamos el nuevo campo
                        esPedecedero,    // Pasamos el nuevo campo
                        uri,
                        this@RegistrarDonacionActivity
                    )
                    Toast.makeText(this@RegistrarDonacionActivity, resultado, Toast.LENGTH_SHORT).show()
                    if (resultado == "Donación registrada exitosamente.") {
                        val intent = Intent(this@RegistrarDonacionActivity, MainDonadoresActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    else Toast.makeText(this@RegistrarDonacionActivity, R.string.donador_error_registrar_donacion, Toast.LENGTH_SHORT).show()
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
        }else {
            pesoEditText.setError(null)
        }

        if (duracionEditText.text.toString().trim().isEmpty()) {
            duracionEditText.setError("Por favor, ingrese la duracion de la donación")
        } else {
            duracionEditText.setError(null)
        }

        if (infoAdicionalEditText.text.toString().trim().isEmpty()) {
            infoAdicionalEditText.setError("Por favor, ingrese informacion adicional del alimento")
        } else if (infoAdicionalEditText.text.toString().trim().length > 500) {
            infoAdicionalEditText.setError("Por favor, ingrese menos de 500 caracteres")
        } else {
            infoAdicionalEditText.setError(null)
        }
    }
    private fun cargarOpcionesSpinner() {
        val tipoAlimentoAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.tipo_alimento_options,
            android.R.layout.simple_spinner_item
        )
        tipoAlimentoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tipoAlimentoSpinner.adapter = tipoAlimentoAdapter

        val requiereRefrigeracionAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.requiere_refrigeracion_options,
            android.R.layout.simple_spinner_item
        )
        requiereRefrigeracionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        requiereRefrigeracionSpinner.adapter = requiereRefrigeracionAdapter

        val esPedecederoAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.es_perecedero_options,
            android.R.layout.simple_spinner_item
        )
        esPedecederoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        esPedecederoSpinner.adapter = esPedecederoAdapter
    }

    private fun initUI() {
        alimentoEditText = findViewById(R.id.alimentoEditText)
        pesoEditText = findViewById(R.id.pesoEditText)
        duracionEditText = findViewById(R.id.duracionEditText)
        infoAdicionalEditText = findViewById(R.id.infoAdicionalEditText)
        tipoAlimentoSpinner = findViewById(R.id.tipoAlimentoSpinner)
        requiereRefrigeracionSpinner = findViewById(R.id.requiereRefrigeracionSpinner)
        esPedecederoSpinner = findViewById(R.id.esPedecederoSpinner)
        imgFoto = findViewById(R.id.imgFoto)
        btnRegistrar = findViewById(R.id.btnRegistrar)
        btnElegirFoto = findViewById(R.id.btnElegirFoto)
    }
}
