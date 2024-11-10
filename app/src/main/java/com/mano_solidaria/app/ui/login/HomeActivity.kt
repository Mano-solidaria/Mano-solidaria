package com.mano_solidaria.app.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.mano_solidaria.app.R
import com.mano_solidaria.app.databinding.ActivityHomeBinding
import com.mano_solidaria.app.ui.donante.ListaPropuestasActivity // Importa la actividad ListaPropuestasActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtenemos los valores de las preferencias
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        setup(email ?: "", provider ?: "")

        // Configuramos el botón "Siguiente"
        binding.nextButton.setOnClickListener {
            // Creamos un Intent para navegar a ListaPropuestasActivity
            val intent = Intent(this, ListaPropuestasActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setup(email: String, provider: String){
        title = "Inicio"
        binding.emailTextView.text = email
        binding.passwordTextView.text = provider
        binding.logOutButton.setOnClickListener{
            // Borramos los datos guardados y cerramos sesión
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
