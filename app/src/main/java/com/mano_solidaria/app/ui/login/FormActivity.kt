package com.mano_solidaria.app.ui.login

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.mano_solidaria.app.R


class FormActivity : AppCompatActivity() {

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
    private lateinit var horariosInicio: Horarios
    private lateinit var horariosUsuario: Horarios
    private lateinit var db: FirebaseFirestore
    private lateinit var usersCollectionRef: CollectionReference
    private lateinit var text_horainicio: TextView
    private lateinit var text_horaFinal: TextView
    private var userFirebase = FirebaseAuth.getInstance().currentUser
    private var provider: String = ""
    private var LogByGoogle: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        //Hace los inputs y esas cosas y qsyo, tengo una paja. Se hace mañana. Exitos Franco del lunes (ahre que ya es Lunes)

        db = Firebase.firestore

        usersCollectionRef = db.collection("users")

        user = findViewById(R.id.usuario_nombre) //Comprobar que no es null (lugar adecuado? -> Al presionar registrarme)

        email = findViewById(R.id.usuario_email) //Comprobar que no es null

        password = findViewById(R.id.usuario_contrasenia) //Comprobar que no es null

        if (userFirebase != null){
            LogByGoogle = true //No es muy escalable ya que si se desea hacer login con por ejemplo facebook no se puede pero me tiene los huevos lleno ya. Solo voy a comentar ProviderData y ProviderId (Buscalo)
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

        eventListeners()

    }

    private fun eventListeners(){
        usuarioRolSwitch.setOnCheckedChangeListener { _, isChecked -> //Evento para que aparezcan los horarios o no, acorde al rol escogido.
            visibility(isChecked)
        }

        sendLogin.setOnClickListener{
            validateData()
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
        userAddress = address.text.toString()
        val userData = mutableMapOf( //Este solo se utiliza para comprobar que los campos tengan valores
            "Nombre" to userName,
            "Email" to userEmail,
            "Contraseña" to userPassword,
            "Direccion" to userAddress
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
//                else {
//                    validateDate()
//                } //Era una validacion bastante mala del horario (Mejorar)
            }
            initLogin()
        } catch (e: IllegalArgumentException){
            validateFields()
        } catch (e: InconsistenciaHorariaException) {
            Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception){
            sendLogin.setError("${e.message}")
        }
    }

    private fun initLogin() {
        provider = ProviderType.Google.toString()
        if (userFirebase == null){
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                userEmail, userPassword).addOnCompleteListener{
                if (it.isSuccessful) {
                    userFirebase = FirebaseAuth.getInstance().currentUser
                    provider = ProviderType.Basic.toString()
                    val userData = createUser()
                    WriteInDB(userFirebase!!.uid, userData)
                    showHome(userEmail, provider)
                }else{
                    showAlert()
                }
            }
        }else {
            val userData = createUser()
            WriteInDB(userFirebase!!.uid, userData)
            showHome(userEmail, provider)
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

    private fun validateDate(){
        val apertura = horariosUsuario.aperturaHora * 60 + horariosUsuario.aperturaMinuto
        val cierre = horariosUsuario.cierreHora * 60 + horariosUsuario.aperturaMinuto
        if (cierre <= apertura) {
            throw InconsistenciaHorariaException("El horario de cierre debe ser mayor al horario de apertura")
        }
    }

    // Función para verificar si el horario ha sido modificado
    private fun isHorarioModificado(): Boolean {
        return (horariosUsuario.aperturaHora != horariosInicio.aperturaHora || horariosUsuario.aperturaMinuto != horariosInicio.aperturaMinuto)
                || (horariosUsuario.cierreHora != horariosInicio.cierreHora || horariosUsuario.aperturaMinuto != horariosInicio.cierreMinuto)
    }

    private fun createUser(): MutableMap<String, String> {
        val user = mutableMapOf(
                "UsuarioRol" to userType.lowercase(),
                "UsuarioNombre" to userName,
                "UsuarioMail" to userEmail,
                "UsuarioDireccion" to userAddress,
            )

        if (userType.lowercase() == "donante"){
            val horarioAtencionInicio = String.format("%02d:%02d", horariosUsuario.aperturaHora, horariosUsuario.aperturaMinuto)
            val horarioAtencionCierre = String.format("%02d:%02d", horariosUsuario.cierreHora, horariosUsuario.cierreMinuto)
            user["HorarioAtencionInicio"] = horarioAtencionInicio
            user["HorarioAtencionFin"] = horarioAtencionCierre
        }

        if (!LogByGoogle){ //Ver que dice cuando es con mail y password
            user["UsuarioContraseña"] = userPassword
        }

        return user
    }

    private fun WriteInDB(userUID: String, user: MutableMap<String, String>) {
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

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
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
}