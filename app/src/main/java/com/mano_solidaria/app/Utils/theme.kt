package com.mano_solidaria.app.Utils


import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.bundleOf
import com.mano_solidaria.app.R

// Función para guardar la preferencia de tema
fun saveThemePreference(context: Context, isDarkMode: Boolean) {
    // Guardar la preferencia del tema en SharedPreferences
    val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
    prefs.putBoolean("isDarkMode", isDarkMode)
    prefs.apply()
}

// Función para cambiar el tema según la preferencia guardada
@Composable
fun ThemeManager(context: Context) {
    // Obtener la preferencia de SharedPreferences
    val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE)
    val isDarkMode = prefs.getBoolean("isDarkMode", false)

    // Cambiar el tema según la preferencia
    val colors = if (isDarkMode) {
        darkColorScheme() // Usa un esquema de colores oscuro
    } else {
        lightColorScheme() // Usa un esquema de colores claro
    }

    // Aplicar el tema en Compose
    MaterialTheme(colorScheme = colors) {
        // Contenido de la interfaz de usuario
        // Aquí puedes incluir la UI de tu aplicación
    }
}

@Composable
fun SwitchThemeButton(context: Context) {
    var isDarkMode by remember { mutableStateOf(false) }

    // Detectar el cambio de estado
    Button(onClick = {
        isDarkMode = !isDarkMode
        // Guardar la preferencia al cambiar el tema
        saveThemePreference(context, isDarkMode)
    }) {
        Text(text = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode")
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ThemeManager(context = LocalContext.current)
}

fun applySavedTheme(context: Context) {
    // Obtener la preferencia de SharedPreferences
    val prefs = context.getSharedPreferences(context.getString(R.string.prefs_file), Context.MODE_PRIVATE)
    val isDarkMode = prefs.getBoolean("isDarkMode", false)

    // Aplicar tema a los elementos de la interfaz manualmente (sin afectar todo el contexto global)
    if (isDarkMode) {
        // Cambia solo los recursos de la actividad
        context.setTheme(R.style.AppTheme_Dark)
    } else {
        context.setTheme(R.style.AppTheme_Light)
    }


}
