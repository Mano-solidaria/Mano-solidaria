package com.mano_solidaria.app.Utils



import java.util.Calendar
import java.util.Date
import java.util.TimeZone

fun calcularDuracion(fechaFin: Date?): String {
    return if (fechaFin != null) {
        // Obtener la fecha actual en UTC-3
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT-3"))
        val fechaActual = calendar.time

        // Calcular la duración entre la fecha actual y fechaFin
        val duracion = fechaFin.time - fechaActual.time
        "${(duracion / (1000 * 60 * 60 * 24)).toInt()}"
    } else {
        "-"
    }
}

