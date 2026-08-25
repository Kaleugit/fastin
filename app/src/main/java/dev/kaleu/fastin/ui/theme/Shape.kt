package dev.kaleu.fastin.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.pow

/**
 * Espaçamento base 4dp (design-system.md §3).
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 48.dp

    /** Alvo de toque mínimo, mesmo quando o desenho parece menor. */
    val touchTarget = 48.dp
}

/**
 * Squircle: superelipse contínua, não arco de círculo.
 *
 * `RoundedCornerShape` do Compose usa arco circular. Num raio grande isso produz a quebra
 * visível entre reta e curva que denuncia o canto. A superelipse aproxima a curva contínua
 * das referências (e do iOS) — é a diferença entre "arredondado" e "usinado".
 *
 * Aproximação por Bézier cúbica com handles em `r * K`, onde K vem do expoente da
 * superelipse. n = 4 dá a curva do iOS.
 */
class SquircleShape(private val radius: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = min(
            with(density) { radius.toPx() },
            min(size.width, size.height) / 2f,
        )
        if (r <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))

        // Handle da Bézier que aproxima a superelipse de expoente 4.
        val k = r * (1f - (0.5f).pow(1f / 4f)) * 2.2f
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - k, 0f, w, k, w, r)
            lineTo(w, h - r)
            cubicTo(w, h - k, w - k, h, w - r, h)
            lineTo(r, h)
            cubicTo(k, h, 0f, h - k, 0f, h - r)
            lineTo(0f, r)
            cubicTo(0f, k, k, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Raios do sistema. **Regra concêntrica**: raio interno = raio externo − padding.
 * Card de 28dp com padding 8dp ⇒ filho de 20dp. Curvas paralelas.
 */
object FastinShapes {
    val card = SquircleShape(28.dp)
    val inner = SquircleShape(20.dp)
    val control = SquircleShape(16.dp)
    val small = SquircleShape(12.dp)

    /** Pill. Chips e botões redondos. */
    val chip = SquircleShape(999.dp)

    fun squircle(radius: Dp): Shape = SquircleShape(radius)

    /** Filho concêntrico dentro de um pai de [parentRadius] com [padding]. */
    fun concentric(parentRadius: Dp, padding: Dp): Shape =
        SquircleShape((parentRadius - padding).coerceAtLeast(0.dp))
}
