package net.nexus.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.launch
import net.nexus.app.data.DesignStyle
import kotlin.math.abs

/**
 * Jelly glass (Liquid Glass style only): a barely-there, soft response, like iOS glass.
 *  - finger down: the object swells very slightly;
 *  - finger moves: it leans a touch towards the motion (tiny stretch, tiny follow), always easing
 *    towards the finger on a soft spring instead of jumping with it;
 *  - finger up: it eases back with a calm, almost invisible wobble.
 * Touches are only observed, never consumed: clicks, scrolling and drags keep working. If a parent
 * scrolls (or a child drags), the jelly lets go instead of travelling with the list.
 *
 * @param amount 1 = buttons and cards, smaller for big surfaces (the tab bar).
 */
fun Modifier.liquidJelly(amount: Float = 1f): Modifier = composed {
    if (NX.style != DesignStyle.LIQUID_GLASS) return@composed this
    val scope = rememberCoroutineScope()
    val sx = remember { Animatable(1f) }
    val sy = remember { Animatable(1f) }
    val tx = remember { Animatable(0f) }
    val ty = remember { Animatable(0f) }
    // Soft, slow springs: retargeting a running spring keeps its velocity, so following the finger is smooth
    val follow = remember { spring<Float>(dampingRatio = 0.9f, stiffness = 140f) }
    val settle = remember { spring<Float>(dampingRatio = 0.62f, stiffness = 120f) }

    fun ease(a: Animatable<Float, *>, target: Float, spec: androidx.compose.animation.core.AnimationSpec<Float>) {
        scope.launch { a.animateTo(target, spec) }
    }

    this
        .pointerInput(amount) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)
                val swell = 1f + 0.015f * amount
                ease(sx, swell, follow)
                ease(sy, swell, follow)
                var moved = 0f
                while (true) {
                    // Final pass: we see whether a scroll/drag consumed the move
                    val ev = awaitPointerEvent(PointerEventPass.Final)
                    val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!ch.pressed) break
                    moved += abs(ch.positionChange().x) + abs(ch.positionChange().y)
                    if (ch.isConsumed && moved > 2f) break // someone else is scrolling: let go
                    val d = ch.position - down.position
                    val nx = (d.x / w).coerceIn(-0.5f, 0.5f)
                    val ny = (d.y / h).coerceIn(-0.5f, 0.5f)
                    val stretchX = abs(nx) * 0.04f * amount
                    val stretchY = abs(ny) * 0.04f * amount
                    ease(sx, swell + stretchX - stretchY * 0.5f, follow)
                    ease(sy, swell + stretchY - stretchX * 0.5f, follow)
                    ease(tx, nx * w * 0.015f * amount, follow)
                    ease(ty, ny * h * 0.015f * amount, follow)
                }
                ease(sx, 1f, settle)
                ease(sy, 1f, settle)
                ease(tx, 0f, settle)
                ease(ty, 0f, settle)
            }
        }
        .graphicsLayer {
            scaleX = sx.value
            scaleY = sy.value
            translationX = tx.value
            translationY = ty.value
        }
}
