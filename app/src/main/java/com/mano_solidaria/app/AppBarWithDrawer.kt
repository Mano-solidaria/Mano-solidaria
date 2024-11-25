package com.mano_solidaria.app


import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clip
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.mano_solidaria.app.data.UserRepository.UserData
import com.mano_solidaria.app.data.UserRepository.UserRepository
import kotlinx.coroutines.*

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
    val topBarHeight = 56.dp // Altura estándar del TopAppBar
    val context = LocalContext.current // Obtener el contexto

    // Variables para obtener los datos del usuario
    var userData by remember { mutableStateOf<UserData?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Llamamos a la función para obtener los datos del usuario
    LaunchedEffect(true) {
        userData = UserRepository().getUserData() // Asumimos que getUserData retorna directamente los datos del usuario
        loading = false
    }

    // Obtención del tema (modo oscuro o claro)
    var isDarkMode by remember { mutableStateOf(getThemePreference(context)) }

    // Variable para gestionar el idioma actual
    var currentLanguage by remember {
        mutableStateOf(getSavedLanguagePreference(context)).also {
            // Agregar un log para mostrar el idioma actual cuando se inicializa
            Log.d("LanguagePreference", "Idioma actual: $it")
        }
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
                    .padding(top = topBarHeight) // Desplazar el contenido hacia abajo
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()) // Permite desplazar el contenido del Drawer
                ) {
                    // Mostrar datos del usuario si están disponibles
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        userData?.let {
                            // Imagen de usuario centrada
                            Image(
                                painter = rememberImagePainter(it.UsuarioImagen),
                                contentDescription = "User Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(bottom = 8.dp)
                                    .align(Alignment.CenterHorizontally)
                            )

                            // Nombre del usuario debajo de la imagen
                            Text(
                                text = it.UsuarioNombre,
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 4.dp)
                            )

                            // Rol del usuario debajo del nombre
                            Text(
                                text = it.UsuarioRol,
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } ?: run {
                            // Si no hay datos de usuario, mostramos un mensaje predeterminado
                            Text(
                                text = "No user data available",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

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
                            val newLanguage = if (currentLanguage == "es") "en" else "es"
                            currentLanguage = newLanguage
                            saveLanguagePreference(context, newLanguage) // Guardar la preferencia
                            changeLanguage(context, newLanguage)
                            // Recargar la actividad
                            (context as Activity).recreate()
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
    return sharedPreferences.getString("language", Locale.getDefault().language) ?: "es"
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
