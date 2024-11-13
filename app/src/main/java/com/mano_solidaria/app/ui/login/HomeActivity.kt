package com.mano_solidaria.app.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mano_solidaria.app.R
import com.mano_solidaria.app.databinding.ActivityHomeBinding
import com.mano_solidaria.app.donadores.MainDonadoresActivity
import com.mano_solidaria.app.solicitantes.MainSolicitantesActivity  // Importa la actividad para solicitantes

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)

        setup(email ?: "", provider ?: "")
    }

    private fun setup(email: String, provider: String) {
        title = "Inicio"
        binding.emailTextView.text = email
        binding.passwordTextView.text = provider
        binding.logOutButton.setOnClickListener {
            // Borrado de datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Botón para verificar el rol del usuario y navegar a la actividad correspondiente
        binding.siguienteButton.setOnClickListener {
            val userId = auth.currentUser?.uid

            if (userId != null) {
                db.collection("users").document(userId).get().addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userRole = document.getString("UsuarioRol") ?: "Desconocido"

                        if (userRole == "donante") {
                            val intent = Intent(this, MainDonadoresActivity::class.java)
                            startActivity(intent)
                        } else if (userRole == "solicitante") {
                            val intent = Intent(this, MainSolicitantesActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Rol de usuario no reconocido.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No se encontró el rol del usuario.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al obtener el rol de usuario.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
