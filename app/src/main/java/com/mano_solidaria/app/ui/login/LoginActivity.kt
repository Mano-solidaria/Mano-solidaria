package com.mano_solidaria.app.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.mano_solidaria.app.R
import com.mano_solidaria.app.ui.login.ProviderType

class LoginActivity : AppCompatActivity() {

    private lateinit var registro : Button
    private lateinit var inicio_sesion : Button
    private lateinit var google_boton : Button
    private lateinit var mail: EditText
    private lateinit var contrasena: EditText
    private lateinit var container:LinearLayout
    private val google_Sign_In = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_template)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setup()

        session()
    }

    override fun onStart() {
        super.onStart()
        container.visibility = View.VISIBLE
    }

    private fun session(){

        container = findViewById(R.id.container)
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        if (email != null && provider !=null){
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
            //configuración

            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleClient =  GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, google_Sign_In)

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
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("provider", provider.name)
            putExtra("email", email)
        }
        startActivity(homeIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == google_Sign_In){

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                if (account != null){

                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showHome(account.email ?: "", ProviderType.Google)
                            } else {
                                showAlert()
                            }
                        }

                }
            } catch (e: ApiException){
                showAlert()
            }

        }
    }

}