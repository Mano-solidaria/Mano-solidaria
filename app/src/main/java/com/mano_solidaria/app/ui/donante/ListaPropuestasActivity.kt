package com.mano_solidaria.app.ui.donante

import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mano_solidaria.app.R
import com.mano_solidaria.app.ui.donante.placeholder.PlaceholderContent

import android.content.Intent
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ListaPropuestasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_propuestas)

        // Configuración de los márgenes para las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener referencia al botón y configurar el listener
        val registerDonationButton: Button = findViewById(R.id.registerDonationButton)
        registerDonationButton.setOnClickListener {
            // Iniciar la actividad RegistrarDonacionActivity
            val intent = Intent(this, RegistrarDonacionActivity::class.java)
            startActivity(intent)
        }

        // Configurar RecyclerView para mostrar la lista de propuestas
        val recyclerView: RecyclerView = findViewById(R.id.propuestaListRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PropuestaListFragment.SimpleItemRecyclerViewAdapter(PlaceholderContent.ITEMS, null)
    }
}
