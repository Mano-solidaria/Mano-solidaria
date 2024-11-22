package com.mano_solidaria.app

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.mano_solidaria.app.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mano_solidaria.app.Utils.saveThemePreference

// Función para obtener el modo del tema desde SharedPreferences
fun getThemePreference(context: Context): Boolean {
    val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE)
    return prefs.getBoolean("isDarkMode", false)
}

// Función para guardar la preferencia de modo de tema en SharedPreferences

@Composable
fun AppBarWithDrawer(
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope,
    title: String, // Parámetro para el título de la AppBar
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerWidthFraction = 0.4f // Representa 2/5 del ancho de la pantalla
    val topBarHeight = 56.dp // Altura estándar del TopAppBar
    val context = LocalContext.current // Obtener el contexto

    // Obtención del tema (modo oscuro o claro)
    var isDarkMode by remember { mutableStateOf(getThemePreference(context)) }

    // Cambio de tema
    val colors = if (isDarkMode) {
        darkColors(
        )
    } else {
        lightColors(
        )
    }

    // ModalDrawer con el contenido del Drawer
    ModalDrawer(
        drawerState = scaffoldState.drawerState,
        gesturesEnabled = scaffoldState.drawerState.isOpen, // Habilitar gestos solo si está abierto
        drawerContent = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(LocalConfiguration.current.screenWidthDp.dp * drawerWidthFraction) // Ancho del drawer
                    .padding(top = topBarHeight) // Desplazar el contenido hacia abajo
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .width(LocalConfiguration.current.screenWidthDp.dp * drawerWidthFraction) // Ancho del drawer
                ) {
                    Text("Contenido del Drawer", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Opción 1", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Opción 2", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Opción 3", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón para cambiar entre modo claro y oscuro
                    Button(
                        onClick = {
                            isDarkMode = !isDarkMode
                            saveThemePreference(context, isDarkMode) // Guardar la preferencia
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDarkMode) "Modo Claro" else "Modo Oscuro")
                    }

                    // Botón para cerrar sesión
                    Button(
                        onClick = {
                            performLogout(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar sesión")
                    }
                }
            }
        },
        content = {
            // Aplicamos el tema dinámico
            MaterialTheme(colors = colors) {
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = {
                        TopAppBar(
                            title = { Text(title) }, // Usar el título pasado como parámetro
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        scaffoldState.drawerState.open()
                                    }
                                }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Abrir menú")
                                }
                            }
                        )
                    },
                    content = { paddingValues ->
                        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            content(paddingValues)
                        }
                    }
                )
            }
        }
    )
}

/**
 * Función para cerrar sesión del usuario.
 * @param context Contexto de la aplicación necesario para navegar.
 */
fun performLogout(context: Context) {
    // Borrado de datos en SharedPreferences
    val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
    prefs.clear()
    prefs.apply()

    // Cerrar sesión en Firebase
    FirebaseAuth.getInstance().signOut()

    // Redirigir al usuario a la pantalla de inicio de sesión
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}
