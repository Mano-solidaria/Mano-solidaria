package com.mano_solidaria.app.Utils

import java.util.*

fun calcularDuracion(fechaInicio: Date?, fechaFin: Date?): String {
    return if (fechaInicio != null && fechaFin != null) {
        val duracion = fechaFin.time - fechaInicio.time
        "${(duracion / (1000 * 60 * 60 * 24)).toInt()} días"
    } else {
        "Fecha no disponible"
    }
}