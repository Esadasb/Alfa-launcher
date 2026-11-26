package pl.alfa.launcher.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Własne, konturowe ikony panelu – w tym samym stylu co ikony aplikacji. */
object LineIcons {
    val Phone: ImageVector = line(
        "M5 4h3l2 5-2.5 1.5a11 11 0 0 0 6 6L15 14l5 2v3a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z",
    )
    val Messages: ImageVector = line(
        "M4 5h16v11H9l-5 4z",
        "M8.5 10.5h0.01M12 10.5h0.01M15.5 10.5h0.01",
    )

    private fun line(vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = addPathNodes(data),
                stroke = SolidColor(Ink.White),
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
