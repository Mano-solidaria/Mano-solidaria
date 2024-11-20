package com.mano_solidaria.app.ui.login

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mano_solidaria.app.R
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import org.mindrot.jbcrypt.BCrypt


class LoginActivity : AppCompatActivity() {

    private lateinit var registro : Button
    private lateinit var inicio_sesion : Button
    private lateinit var google_boton : Button
    private lateinit var mail: EditText
    private lateinit var contrasena: EditText
    private lateinit var container:LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var db: FirebaseFirestore
    private lateinit var usersCollectionRef: CollectionReference
    private lateinit var alovelaceDocumentRef: DocumentReference

    private val oneTapResultLauncher = registerForActivityResult( //Solo se ejecuta cuando se lo llama (Solo inicia sesion)
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                loginByGoogle()
                                //showHome(user!!.email.toString(), ProviderType.Google)
                            } else {
                                val message= loginFallido(task.exception)
                                showAlert(message)
                            }
                        }
                } else {
                    Log.d(TAG, "No se recibió un ID token.")
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Error al obtener credenciales: ${e.message}")
            }
        } else {
            Log.e(TAG, "Inicio de sesión cancelado o fallido.")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_template)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        usersCollectionRef = db.collection("users")
        alovelaceDocumentRef = db.collection("users").document("alovelace")
        auth = Firebase.auth
        setup()
        oneTapClient = Identity.getSignInClient(this) //Added by GPT
        session()
    }

//    override fun onStart() { //Se ejecuta luego del oncreate
//        super.onStart()
////        container.visibility = View.VISIBLE
////        val currentUser = auth.currentUser //Obtiene el usuario actualmente autenticado en Firebase
////        showHome(currentUser!!.email.toString(), ProviderType.Google) //Ver después cuando se inicia la sesion de google que se estaría iniciando 2 veces este método
//    }

    private fun session(){ //Cuando ya se encuentra loggeado un usuario, se cargan los datos.

        container = findViewById(R.id.container)
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)
        val currentUser = auth.currentUser //Obtiene el usuario actualmente autenticado en Firebase (Fijate boludo que no lo estas usando xd)

        if (email != null && provider != null){
            container.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup(){

        title = "Autenticación"

        inicio_sesion = findViewById(R.id.login)
        registro = findViewById(R.id.sign_in)
        mail = findViewById(R.id.email)
        contrasena = findViewById(R.id.password)
        google_boton = findViewById(R.id.login_google)

        registro.setOnClickListener{
            //Se debe de inflar la actividad Form
            showForm()
        }

        inicio_sesion.setOnClickListener{
            if (mail.text.isNotEmpty() && contrasena.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    mail.text.toString(), contrasena.text.toString()).addOnCompleteListener{
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.Basic)
                    }else{
                        val errorMessage = loginFallido(it.exception)
                        showAlert(errorMessage)
                    }
                }
            } else {
                showAlert("Por favor, completa todos los campos")
            }
        }

        google_boton.setOnClickListener{
            startGoogle()
        }
    }

//    fun validatePassword(contrasena: String, hash: String): Boolean {
//        return BCrypt.checkpw(contrasena, hash)
//    }

    private fun startGoogle(){
        //configuración
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .build()

        oneTapClient.beginSignIn(signInRequest) //Iniciamos el inicio de sesion
            .addOnSuccessListener(this) { result ->
                try {
                    oneTapResultLauncher.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error al iniciar la intención: ${e.message}")
                }
            }
            .addOnFailureListener(this) { e ->
                Log.e(TAG, "Error al iniciar el inicio de sesión: ${e.message}")
            }
    }

    private fun showAlert(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }//Debería hacer una version para el login

    private fun showHome(email:String, provider: ProviderType){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider.name)
        prefs.apply()
        val homeIntent = Intent(this, HomeActivity::class.java)
        startActivity(homeIntent)
    }

    private fun showForm(){
        val formIntent = Intent(this, FormActivity::class.java)
        startActivity(formIntent)
    }

    private fun loginByGoogle() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val uid = user.uid //Se busca si a pesar de estar registrado en Firebase se encuentran sus datos en la base de datos
            val userRef = db.collection("users").document(uid)

            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Usuario registrado: redirige a la actividad de usuario registrado
                    showHome(user!!.email.toString(), ProviderType.Google)
                } else {
                    // Usuario no registrado: redirige a la actividad de registro
                    showForm()
                }
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error al verificar el usuario", e)
            }
        }
    }
}

private fun loginFallido(exception: Exception?): String {
    return when (exception) {
        is FirebaseAuthInvalidCredentialsException -> {
            when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Formato de correo no válido."
                "ERROR_USER_DISABLED" -> "La cuenta ha sido deshabilitada."
                "ERROR_INVALID_CREDENTIAL" -> "El correo electrónico o la contraseña que ingresaste no son correctos. Por favor, revisa tus datos e inténtalo nuevamente"
                else -> "Credenciales no válidas. Verifica tus datos."
            }
        }
        is FirebaseAuthInvalidUserException -> {
            when (exception.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "El usuario no existe. Verifica el correo o regístrate."
                "ERROR_USER_DISABLED" -> "La cuenta ha sido deshabilitada."
                else -> "Usuario no encontrado o cuenta desactivada."
            }
        }
        is FirebaseAuthUserCollisionException -> {
            "Conflicto de usuarios: ${exception.message}"
        }
        is FirebaseAuthException -> {
            "Error de autenticación de Firebase: ${exception.message}"
        }
        null -> {
            "Error desconocido de inicio de sesión."
        }
        else -> {
            "Error inesperado: ${exception.message}"
        }
    }
}

