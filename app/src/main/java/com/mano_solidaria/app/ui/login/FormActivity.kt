package com.mano_solidaria.app.ui.login

import ApiService
import ApiServiceLogin
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.mano_solidaria.app.BuildConfig
import com.mano_solidaria.app.R
import com.mano_solidaria.app.donadores.Repository.getFileFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


class FormActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var usuarioRolSwitch : Switch
    private lateinit var horarioApertura : TimePicker
    private lateinit var horarioCierre : TimePicker
    private lateinit var sendLogin : Button
    private lateinit var user : EditText
    private lateinit var email : EditText
    private lateinit var password : EditText
    private lateinit var address : EditText
    private var userType: String = ""
    private var userName: String = ""
    private var userEmail: String = ""
    private var userPassword: String = ""
    private var userAddress: String = ""
    private var ubicacion: String = ""
    private lateinit var horariosInicio: Horarios
    private lateinit var horariosUsuario: Horarios
    private lateinit var db: FirebaseFirestore
    private lateinit var usersCollectionRef: CollectionReference
    private lateinit var text_horainicio: TextView
    private lateinit var text_horaFinal: TextView
    private var userFirebase = FirebaseAuth.getInstance().currentUser
    private var provider: String = ""
    private var LogByGoogle: Boolean = false
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var autocomplete: AutocompleteSupportFragment
    // carga imagen
    private lateinit var imgFoto: ImageView
    private lateinit var btnElegirFoto: Button
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        val apiKey = BuildConfig.MAPS_API_KEY

        // Construct a PlacesClient
        Places.initialize(applicationContext, apiKey)

        // Log an error if apiKey is not set.
        if (apiKey.isEmpty() || apiKey == "DEFAULT_API_KEY") {
            Log.e("Places test", "No api key")
            finish()
            return
        }


        autocomplete = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
            as AutocompleteSupportFragment

        autocomplete.setTypesFilter(listOf("landmark", "restaurant", "store",
                                            "supermarket", "drugstore",
                                            "convenience_store", "meal_takeaway",
                                            "meal_delivery", "bakery"
                                            ))
        autocomplete.setTypesFilter(listOf(PlaceTypes.ADDRESS))
        autocomplete.setCountries("AR")
        autocomplete.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))

        autocomplete.setOnPlaceSelectedListener(object :PlaceSelectionListener{
            override fun onError(p0: Status) {
                Toast.makeText(this@FormActivity, "busqueda cancelada", Toast.LENGTH_SHORT).show()
            }
            override fun onPlaceSelected(p0: Place) {
                val add = p0.address
                val id = p0.id
                val latLng = p0.latLng!!
                val name = p0.name
                setAddress(add, latLng)
                zoomOnMap(latLng)
            }
        })

        //Hace los inputs y esas cosas y qsyo, tengo una paja. Se hace mañana. Exitos Franco del lunes (ahre que ya es Lunes)

        db = Firebase.firestore

        usersCollectionRef = db.collection("users")

        user = findViewById(R.id.usuario_nombre) //Comprobar que no es null (lugar adecuado? -> Al presionar registrarme)

        email = findViewById(R.id.usuario_email) //Comprobar que no es null

        password = findViewById(R.id.usuario_contrasenia) //Comprobar que no es null

        if (userFirebase != null){
            LogByGoogle = true //No es muy escalable ya que si se desea hacer login con por ejemplo facebook no se puede pero me tiene los huevos lleno ya. Solo voy a comentar ProviderData y ProviderId (Buscalo) // chupala estoy con otras cosas
            fillOutEmail()
            hidePassword()
        }

        address = findViewById(R.id.usuario_direccion) //Comprobar que no es null

        usuarioRolSwitch = findViewById(R.id.usuario_rol_switch) //Que pinte el fondo del text escogido xd.

        text_horainicio = findViewById(R.id.horario_atencion_inicio_text)

        text_horaFinal = findViewById(R.id.horario_atencion_fin_text)

        horarioApertura = findViewById(R.id.horario_atencion_inicio)
        horarioApertura.setIs24HourView(true) //Si quieren que se vea el AM/PM modifiquenlo ustedes, me da paja jajajaja (Solo tienen que sacar esto y corregir la cuenta en la funcion que verifica que fin>inicio)

        horarioCierre = findViewById(R.id.horario_atencion_fin)
        horarioCierre.setIs24HourView(true)

        horariosInicio = Horarios(horarioApertura.hour, horarioApertura.minute, horarioCierre.hour, horarioCierre.minute )

        visibility(usuarioRolSwitch.isChecked)

//        horarioApertura.visibility = if (usuarioRolSwitch.isChecked) View.VISIBLE else View.GONE
//        horarioCierre.visibility = if (usuarioRolSwitch.isChecked) View.VISIBLE else View.GONE



        sendLogin = findViewById(R.id.boton_guardar)

        imgFoto = findViewById(R.id.imgFoto)
        btnElegirFoto = findViewById(R.id.btnElegirFoto)


        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_form) as SupportMapFragment
        mapFragment.getMapAsync(this)

        eventListeners()
    }

    private fun eventListeners(){
        usuarioRolSwitch.setOnCheckedChangeListener { _, isChecked -> //Evento para que aparezcan los horarios o no, acorde al rol escogido.
            visibility(isChecked)
        }

        sendLogin.setOnClickListener{
            validateData()
        }

        // boton imagen
        btnElegirFoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            resultLauncher.launch(intent)
        }

    }

    // carga imagen
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            imgFoto.setImageURI(imageUri)
        }
    }

    private  fun hidePassword(){
        val passwordText:TextView  = findViewById(R.id.usuario_contrasenia_text) //Comprobar que no es null
        //Oculta la contraseña
        passwordText.visibility = View.GONE
        password.visibility = View.GONE
    }

    private fun fillOutEmail(){
        //completamos el campo email con el email de firebase imposibilitando el cambio de mail.
        email.setText(userFirebase!!.email)
        email.isEnabled = false
        email.isFocusable = false
        email.isFocusableInTouchMode = false
    }

    private fun visibility(isChecked: Boolean) {
        if (isChecked) {
            // Switch está a la derecha, muestra los TimePickers
            horarioApertura.visibility = View.VISIBLE
            horarioCierre.visibility = View.VISIBLE
            text_horainicio.visibility = View.VISIBLE
            text_horaFinal.visibility = View.VISIBLE
            userType = "Donante"
        } else {
            // Switch está a la izquierda, oculta los TimePickers
            horarioApertura.visibility = View.GONE
            horarioCierre.visibility = View.GONE
            text_horainicio.visibility = View.GONE
            text_horaFinal.visibility = View.GONE
            userType = "Solicitante"
        }
    }

    private fun validateData(){ //Acá validamos los datos y en caso de estar de 10 (Fuaaaaah el diego) se continua con el flujo normalmente.

        if(userFirebase != null){
            //recupero correo de firebase
            userEmail = userFirebase!!.email.toString()
            userPassword = "a" //Solucion fea pero efectiva xd
        }
        else {
            userEmail = email.text.toString()
            userPassword = password.text.toString()
        }
        userName = user.text.toString()
        val userData = mutableMapOf( //Este solo se utiliza para comprobar que los campos tengan valores
            "Nombre" to userName,
            "Email" to userEmail,
            "Contraseña" to userPassword,
            "Direccion" to userAddress,
            "Ubicacion" to  ubicacion
        )
        try {
            validateString(userData)
            if (usuarioRolSwitch.isChecked) { //Dependiendo el rol nos importa o no
                horariosUsuario = Horarios(
                    horarioApertura.hour,
                    horarioApertura.minute,
                    horarioCierre.hour,
                    horarioCierre.minute
                )

                if (!isHorarioModificado()) {
                    //Se deben de modificar los horarios.
                    throw InconsistenciaHorariaException("Seleccione los horarios")
                }
                else {
                    validateTime()
                } //Era una validacion bastante mala del horario (Mejorar)
            }
            validateEmail(userEmail)
            validatePassword(userPassword)
            if (imageUri == null) {
                Toast.makeText(this, "Por favor seleccione una imagen.", Toast.LENGTH_SHORT).show()
                return
            }
            initLogin()
        } catch (e: IllegalArgumentException){
            validateFields()
        } catch (e: InconsistenciaHorariaException) {
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: EmailException){
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: PasswordException){
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception){
            sendLogin.setError("${e.message}")
        }
    }

    private fun initLogin() {
        provider = ProviderType.Google.toString()

        // Llamamos a la función suspendida en un contexto adecuado (lanzamos una corutina)
        GlobalScope.launch(Dispatchers.Main) {
            if (userFirebase == null) {
                try {
                    val result = FirebaseAuth.getInstance().createUserWithEmailAndPassword(userEmail, userPassword).await()
                    if (result.user != null) {
                        userFirebase = result.user
                        provider = ProviderType.Basic.toString()
                        val userData = createUser()  // Ahora podemos llamar a createUser aquí
                        WriteInDB(userFirebase!!.uid, userData)
                        showHome(userEmail, provider)
                    } else {
                        showAlert("Error al registrar el usuario.")
                    }
                } catch (e: Exception) {
                    // Aquí manejamos cualquier error que ocurra durante el registro
                    if (e is FirebaseAuthUserCollisionException) {
                        showAlert("El correo ya está registrado. Por favor, usa otro correo o inicia sesión.")
                    } else {
                        showAlert("Error al registrar el usuario: ${e.message}")
                    }
                }
            } else {
                // Si el usuario ya está autenticado, simplemente lo registramos
                val userData = createUser()
                WriteInDB(userFirebase!!.uid, userData)
                showHome(userEmail, provider)
            }
        }
    }


    private fun validateString(data: MutableMap<String, String>) {
        for ((key, value) in data) {
            if (value.isEmpty()) {
                // Informar al usuario sobre el campo vacío
                throw IllegalArgumentException("El campo ${key} está vacío.")
            }
        }
    }

    private fun validateEmail(email: String){
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!emailRegex.matches(email)){
            throw EmailException("Formato de email no válido")
        }

        val domain = email.substringAfter('@').lowercase()

        when {
            domain.endsWith("gmail.com") -> Unit
            domain.endsWith("yahoo.com") -> Unit
            domain.endsWith("outlook.com") -> Unit
            domain.endsWith("hotmail.com") -> Unit
            else -> throw EmailException("Dominio de email no válido.")
        }
    }

    fun validatePassword(contrasena: String){
        val passwordRegex = "^(?=.*[A-Z])(?=.*\\d).*\$".toRegex()
        if(contrasena.length < 6){
            throw PasswordException("La contraseña debe de tener más que 6 caracteres")
        }
        if (!passwordRegex.matches(contrasena)){
            throw PasswordException("La contraseña debe contener al menos una mayuscula y un valor numerico")
        }
    }

    private fun validateTime() {
        // Minutos pasados desde la medianoche
        val apertura = horariosUsuario.aperturaHora * 60 + horariosUsuario.aperturaMinuto
        val cierre = horariosUsuario.cierreHora * 60 + horariosUsuario.cierreMinuto

        // Le sumamos el tiempo correspondiente por si cambia de día.
        val cierreAjustado = if (cierre < apertura) cierre + 1440 else cierre

        // Sumar 4 horas que es el tiempo mínimo que se encontrará abierto.
        val minCierreRequerido = apertura + 240

        if (cierreAjustado <= minCierreRequerido) {
            throw InconsistenciaHorariaException("El horario de cierre debe ser al menos 4 horas después del horario de apertura")
        }
    }


    // Función para verificar si el horario ha sido modificado
    private fun isHorarioModificado(): Boolean {
        return (horariosUsuario.aperturaHora != horariosInicio.aperturaHora || horariosUsuario.aperturaMinuto != horariosInicio.aperturaMinuto)
                || (horariosUsuario.cierreHora != horariosInicio.cierreHora || horariosUsuario.aperturaMinuto != horariosInicio.cierreMinuto)
    }

    private suspend fun createUser(): MutableMap<String, String?> {

        val imagenURL = imageUri?.let {
            Log.d("CreateUser", "Iniciando la carga de la imagen con URI: $it")
            uploadImage(applicationContext, it) }
        if (imagenURL != null) {
            Log.d("CreateUser", "Imagen subida correctamente. URL: $imagenURL")
            if (imagenURL.isEmpty()) {
                Log.e("CreateUser", "Error: La URL de la imagen está vacía después de la subida.")
                throw Exception("Error al subir la imagen")
            }
        }

        val user = mutableMapOf(
                "UsuarioRol" to userType.lowercase(),
                "UsuarioNombre" to userName,
                "UsuarioMail" to userEmail,
                "UsuarioDireccion" to userAddress,
                "Usuarioubicacion" to ubicacion,
                "UsuarioImagen" to imagenURL
            )

        if (userType.lowercase() == "donante"){
            val horarioAtencionInicio = String.format("%02d:%02d", horariosUsuario.aperturaHora, horariosUsuario.aperturaMinuto)
            val horarioAtencionCierre = String.format("%02d:%02d", horariosUsuario.cierreHora, horariosUsuario.cierreMinuto)
            user["HorarioAtencionInicio"] = horarioAtencionInicio
            user["HorarioAtencionFin"] = horarioAtencionCierre
        }

        return user
    }

    private fun WriteInDB(userUID: String, user: MutableMap<String, String?>) {
        db.collection("users").document(userUID) // Usa el UID como ID del documento
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "Documento creado con éxito!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error al crear el documento", e)
            }
    }

    private fun showHome(email:String, provider: String){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()
        val homeIntent = Intent(this, HomeActivity::class.java)
        startActivity(homeIntent)
    }

    private fun showAlert(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun validateFields(): Boolean {
        var isValid = true

        // Validación para el nombre
        if (userName.isEmpty()) {
            user.setError("Por favor, complete el nombre")
            isValid = false
        } else {
            user.setError(null) // Limpia el mensaje de error si está completo
        }

        // Validación para el email
        if (userEmail.isEmpty()) {
            email.setError("Por favor, complete el email")
            isValid = false
        } else {
            email.setError(null) // Limpia el mensaje de error si está completo
        }

        // Validación para otros campos de texto...
        if (userPassword.isEmpty()) {
            password.setError("Por favor, complete la contraseña")
            isValid = false
        } else {
            password.setError(null)
        }

        if (userAddress.isEmpty()) {
            address.setError("Por favor, complete la dirección")
            isValid = false
        } else {
            address.setError(null)
        }

        return isValid
    }

    private fun zoomOnMap(latLng: LatLng){
        val newLatLng = CameraUpdateFactory.newLatLngZoom(latLng,15f)
        mMap.addMarker(MarkerOptions().position(latLng))
        mMap.animateCamera(
            newLatLng,
            4000,
            null
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Buenos Aires, Argentina
        val initialLocation = LatLng(-34.6037, -58.3816)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 5f))

        val place = LatLng(-34.77459095976608, -58.266914119799154)
        val newLatLng = CameraUpdateFactory.newLatLngZoom(place,15f)

        mMap.addMarker(MarkerOptions().position(place).title("Marker in unaj city "))
        mMap.animateCamera(
            newLatLng,
            5000,
            null
        )
    }

    private fun setAddress(add: String, latLng: LatLng) {
        userAddress = add
        ubicacion = latLng.toString()
        address.setText(add)
    }

    suspend fun uploadImage(context: Context, uri: Uri): String {
        return try {
            Log.d("UploadImage", "Iniciando carga de imagen")

            val file = getFileFromUri(context, uri) ?: run {
                Log.e("UploadImage", "Error: no se pudo obtener el archivo de la URI.")
                return ""
            }
            Log.d("UploadImage", "Archivo obtenido: ${file.name}")

            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestBody)
            Log.d("UploadImage", "Creado el cuerpo de la solicitud Multipart.")

            val retrofit = Retrofit.Builder()
                .baseUrl("https://marcelomp3.pythonanywhere.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            Log.d("UploadImage", "Retrofit configurado")

            val apiServiceLogin = retrofit.create(ApiServiceLogin::class.java)
            Log.d("UploadImage", "Llamando al servicio para subir la imagen.")

            val response = apiServiceLogin.subirImagen(body)
            Log.d("UploadImage", "Respuesta del servidor: ${response.code()}")

            if (response.isSuccessful) {
                val location = response.body()?.get("location")
                Log.d("UploadImage", "Imagen cargada exitosamente. URL: $location")
                location ?: ""
            } else {
                Log.e("UploadImage", "Error al cargar la imagen. Código de respuesta: ${response.code()}")
                ""
            }
        } catch (e: Exception) {
            Log.e("UploadImage", "Excepción al subir imagen", e)
            e.printStackTrace()
            ""
        }
    }

}

