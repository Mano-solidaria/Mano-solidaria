package com.mano_solidaria.app

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.mano_solidaria.app.ui.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mano_solidaria.app.Utils.saveThemePreference
import java.util.Locale


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

    // Variable para gestionar el idioma actual
    var currentLanguage by remember {
        mutableStateOf(getSavedLanguagePreference(context))
    }

    // Cambio de tema
    val colors = if (isDarkMode) {
        darkColors()
    } else {
        lightColors()
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
                    Text(stringResource(id = R.string.drawer_option_1), style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(id = R.string.drawer_option_2), style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(id = R.string.drawer_option_3), style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Botón para cambiar entre modo claro y oscuro
                    Button(
                        onClick = {
                            isDarkMode = !isDarkMode
                            saveThemePreference(context, isDarkMode) // Guardar la preferencia
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isDarkMode) stringResource(id = R.string.light_mode) else stringResource(id = R.string.dark_mode))
                    }

                    // Botón para cerrar sesión
                    Button(
                        onClick = {
                            performLogout(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.logout))
                    }

                    // Botón para cambiar el idioma
                    Button(
                        onClick = {
                            val newLanguage = if (currentLanguage == "en") "es" else "en"
                            currentLanguage = newLanguage
                            saveLanguagePreference(context, newLanguage) // Guardar la preferencia
                            changeLanguage(context, newLanguage)
                            // Recargar la actividad
                            val intent = (context as Activity).intent
                            context.finish()
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(id = R.string.change_language)) // Usa el recurso traducido
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
                                    Icon(Icons.Filled.Menu, contentDescription = stringResource(id = R.string.menu))
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
 * Función para guardar la preferencia de idioma.
 */
fun saveLanguagePreference(context: Context, languageCode: String) {
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("language", languageCode).apply()
}

/**
 * Función para obtener el idioma guardado.
 */
fun getSavedLanguagePreference(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return sharedPreferences.getString("language", Locale.getDefault().language) ?: "en"
}

/**
 * Cambiar el idioma de la aplicación.
 */
fun changeLanguage(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = Configuration()
    config.setLocale(locale)

    context.resources.updateConfiguration(config, context.resources.displayMetrics)
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
