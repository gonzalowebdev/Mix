package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Selección arrastrando el dedo: una pulsación larga selecciona el archivo y, sin soltar,
 * arrastrar sobre otros los agrega (o los quita, si el primero ya estaba seleccionado).
 * Se evalúa antes que el desplazamiento normal, que sigue funcionando.
 */
fun Modifier.dragSelect(
    keyAt: (Offset) -> String?,
    onStart: (String) -> Boolean,
    onEnter: (String, Boolean) -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var out = false
            while (!out) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                ) {
                    out = true
                }
            }
            true
        }
        // Si el dedo se movió o se levantó antes de tiempo, no es una pulsación larga
        if (early != null) return@awaitEachGesture
        val startKey = keyAt(down.position) ?: return@awaitEachGesture
        val add = onStart(startKey)
        var last = startKey
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
            val pos = event.changes.firstOrNull()?.position
            val key = if (pos != null) keyAt(pos) else null
            if (key != null && key != last) {
                last = key
                onEnter(key, add)
            }
        } while (event.changes.any { it.pressed })
    }
}
