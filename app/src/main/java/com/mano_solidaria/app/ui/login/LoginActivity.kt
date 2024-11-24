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
import android.widget.TextView
import android.widget.Toast
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
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.mano_solidaria.app.donadores.MainDonadoresActivity
import com.mano_solidaria.app.solicitantes.SolicitantesPropuestasActivity
import org.mindrot.jbcrypt.BCrypt

class LoginActivity : AppCompatActivity() {

    private lateinit var registro : TextView
    private lateinit var inicio_sesion : Button
    private lateinit var google_boton : Button
    private lateinit var mail: EditText
    private lateinit var contrasena: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var db: FirebaseFirestore
    private lateinit var usersCollectionRef: CollectionReference

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
        db = Firebase.firestore
        auth = Firebase.auth
        // Configurar persistencia de Firestore
//        val settings = FirebaseFirestoreSettings.Builder()
//            .setPersistenceEnabled(true) // Habilita la persistencia local
//            .build()
//
//        FirebaseFirestore.getInstance().firestoreSettings = settings
        if (auth.currentUser != null){
            showHome()
        }else {
            setup()
        }
    }

//    override fun onStart() {
//        super.onStart()
//        FirebaseAuth.getInstance().signOut() // Cerrar sesión al iniciar LoginActivity
//    }

    private fun setup(){
        auth.signOut()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_template)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        usersCollectionRef = db.collection("users")
        oneTapClient = Identity.getSignInClient(this)

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
                        showHome()
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
    }

    private fun showHome(){
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userRole = document.getString("UsuarioRol") ?: "Desconocido"

                    if (userRole == "donante") {
                        val intent = Intent(this, MainDonadoresActivity::class.java)
                        startActivity(intent)
                    } else if (userRole == "solicitante") {
                        val intent = Intent(this, SolicitantesPropuestasActivity::class.java)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, getString(R.string.rol_usuario_no_reconocido), Toast.LENGTH_SHORT).show()
                        setup()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.rol_usuario_no_encontrado), Toast.LENGTH_SHORT).show()
                    setup()
                }
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_obtener_rol_usuario), Toast.LENGTH_SHORT).show()
                setup()
            }
        }
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
                    showHome()
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

