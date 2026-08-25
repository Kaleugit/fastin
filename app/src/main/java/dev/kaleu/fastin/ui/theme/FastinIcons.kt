package dev.kaleu.fastin.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ícones do app (design-system.md §7).
 *
 * Material Icons são proibidos: o traço grosso e o preenchimento sólido brigam com o traço
 * fino e preciso das referências. São poucos ícones — desenhá-los custa menos que a
 * dependência de `material-icons-extended` inteira.
 *
 * Todos: traço 1.5dp, cap e join arredondados, sem preenchimento, viewport 24.
 */
private inline fun icon(name: String, block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit) =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private fun androidx.compose.ui.graphics.vector.ImageVector.Builder.stroke(
    pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
) = path(
    fill = null,
    stroke = SolidColor(androidx.compose.ui.graphics.Color.Black), // tingido por `tint` no uso
    strokeLineWidth = 1.5f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = pathData,
)

object FastinIcons {

    val ArrowLeft: ImageVector by lazy {
        icon("ArrowLeft") {
            stroke {
                moveTo(15f, 5f); lineTo(9f, 12f); lineTo(15f, 19f)
            }
        }
    }

    val ArrowRight: ImageVector by lazy {
        icon("ArrowRight") {
            stroke {
                moveTo(9f, 5f); lineTo(15f, 12f); lineTo(9f, 19f)
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            stroke {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            stroke {
                moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 6.5f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            stroke {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        icon("Clock") {
            stroke {
                moveTo(12f, 3f)
                arcToRelative(9f, 9f, 0f, true, true, -0.01f, 0f)
                close()
                moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15.5f, 14f)
            }
        }
    }

    val Chart: ImageVector by lazy {
        icon("Chart") {
            stroke {
                moveTo(4f, 16f); lineTo(9f, 10f); lineTo(13f, 13.5f); lineTo(20f, 6f)
            }
        }
    }

    /** Reticências, como o canto do card "Distance (km)" da img-ref01. */
    val More: ImageVector by lazy {
        icon("More") {
            stroke {
                moveTo(6f, 12f); lineTo(6.01f, 12f)
                moveTo(12f, 12f); lineTo(12.01f, 12f)
                moveTo(18f, 12f); lineTo(18.01f, 12f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            stroke {
                moveTo(5f, 6.5f)
                lineTo(19f, 6.5f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
                moveTo(8.5f, 3.5f); lineTo(8.5f, 8f)
                moveTo(15.5f, 3.5f); lineTo(15.5f, 8f)
                moveTo(5f, 11f); lineTo(19f, 11f)
            }
        }
    }
}
