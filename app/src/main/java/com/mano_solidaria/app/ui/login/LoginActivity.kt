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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.mano_solidaria.app.R
import com.mano_solidaria.app.ui.login.ProviderType
import android.util.Log.d
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseUser


class LoginActivity : AppCompatActivity() {

    private lateinit var registro : Button
    private lateinit var inicio_sesion : Button
    private lateinit var google_boton : Button
    private lateinit var mail: EditText
    private lateinit var contrasena: EditText
    private lateinit var container:LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient

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
                                val user = auth.currentUser
                                showHome(user!!.email.toString(), ProviderType.Google)
                            } else {
                                showAlert()
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
        Log.d(TAG, "onCreate: Iniciando LoginActivity")

        setContentView(R.layout.activity_login)
        Log.d(TAG, "Layout activity_login establecido")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_template)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth //Había otra clase, puede ser que le hayas errado en la importacion
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
        val currentUser = auth.currentUser //Obtiene el usuario actualmente autenticado en Firebase

        if (email != null && provider != null){
            container.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup(){

        title = "Autenticación"

        inicio_sesion = findViewById(R.id.sign_in)
        registro = findViewById(R.id.login)
        mail = findViewById(R.id.email)
        contrasena = findViewById(R.id.password)
        google_boton = findViewById(R.id.login_google)

        registro.setOnClickListener{
            if (mail.text.isNotEmpty() && contrasena.text.isNotEmpty()){
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                    mail.text.toString(), contrasena.text.toString()).addOnCompleteListener{
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.Basic)
                    }else{
                        showAlert()
                    }
                }
            }
        }

        inicio_sesion.setOnClickListener{
            if (mail.text.isNotEmpty() && contrasena.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(
                    mail.text.toString(), contrasena.text.toString()).addOnCompleteListener{
                    if (it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.Basic)
                    }else{
                        showAlert()
                    }
                }
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

    private fun showAlert(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error autenticando al usuario")
        builder.setPositiveButton("Aceptar",null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showHome(email:String, provider: ProviderType){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider.name)
        prefs.apply()
        val homeIntent = Intent(this, HomeActivity::class.java)
        startActivity(homeIntent)
    }

    }

