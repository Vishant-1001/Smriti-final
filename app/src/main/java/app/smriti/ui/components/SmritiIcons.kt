package app.smriti.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Simple, bold, rounded line icons drawn for SMRITI (24-unit grid). Tinted by the caller. */
object SmritiIcons {
    private fun icon(name: String, stroke: Boolean = true, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            if (stroke) path(
                stroke = SolidColor(Color.Black), strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = block,
            ) else path(fill = SolidColor(Color.Black), pathBuilder = block)
        }.build()

    val Back = icon("back") { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
    val Next = icon("next") { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    val Home = icon("home") {
        moveTo(3.5f, 11f); lineTo(12f, 4f); lineTo(20.5f, 11f)
        moveTo(6f, 9.5f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 9.5f)
        moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
    }
    val Settings = icon("settings") {
        moveTo(12f, 8.5f); arcTo(3.5f, 3.5f, 0f, true, true, 11.99f, 8.5f)
        moveTo(12f, 2.5f); lineTo(12f, 5f); moveTo(12f, 19f); lineTo(12f, 21.5f)
        moveTo(2.5f, 12f); lineTo(5f, 12f); moveTo(19f, 12f); lineTo(21.5f, 12f)
        moveTo(5.3f, 5.3f); lineTo(7f, 7f); moveTo(17f, 17f); lineTo(18.7f, 18.7f)
        moveTo(18.7f, 5.3f); lineTo(17f, 7f); moveTo(7f, 17f); lineTo(5.3f, 18.7f)
    }
    val Close = icon("close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    val Exit = icon("exit") {
        moveTo(14f, 4f); lineTo(6f, 4f); lineTo(6f, 20f); lineTo(14f, 20f)
        moveTo(11f, 12f); lineTo(21f, 12f); moveTo(17.5f, 8.5f); lineTo(21f, 12f); lineTo(17.5f, 15.5f)
    }
    val Play = icon("play", stroke = false) { moveTo(7f, 4.5f); lineTo(20f, 12f); lineTo(7f, 19.5f); close() }
    val Pause = icon("pause", stroke = false) {
        moveTo(6f, 4.5f); lineTo(10f, 4.5f); lineTo(10f, 19.5f); lineTo(6f, 19.5f); close()
        moveTo(14f, 4.5f); lineTo(18f, 4.5f); lineTo(18f, 19.5f); lineTo(14f, 19.5f); close()
    }
    val Stop = icon("stop", stroke = false) { moveTo(5.5f, 5.5f); lineTo(18.5f, 5.5f); lineTo(18.5f, 18.5f); lineTo(5.5f, 18.5f); close() }
    val SkipNext = icon("skip_next", stroke = false) {
        moveTo(5f, 5f); lineTo(15f, 12f); lineTo(5f, 19f); close()
        moveTo(16.5f, 5f); lineTo(19.5f, 5f); lineTo(19.5f, 19f); lineTo(16.5f, 19f); close()
    }
    val NotNow = icon("not_now") {
        moveTo(12f, 3.5f); arcTo(8.5f, 8.5f, 0f, true, true, 11.99f, 3.5f)
        moveTo(6f, 18f); lineTo(18f, 6f)
    }
    val Speaker = icon("speaker") {
        moveTo(4f, 9f); lineTo(8f, 9f); lineTo(13f, 5f); lineTo(13f, 19f); lineTo(8f, 15f); lineTo(4f, 15f); close()
        moveTo(16.5f, 9f); quadTo(18.5f, 12f, 16.5f, 15f)
        moveTo(19f, 6.5f); quadTo(23f, 12f, 19f, 17.5f)
    }
    val SpeakerOff = icon("speaker_off") {
        moveTo(4f, 9f); lineTo(8f, 9f); lineTo(13f, 5f); lineTo(13f, 19f); lineTo(8f, 15f); lineTo(4f, 15f); close()
        moveTo(16f, 9f); lineTo(22f, 15f); moveTo(22f, 9f); lineTo(16f, 15f)
    }
    val Replay = icon("replay") {
        moveTo(5f, 12f); arcTo(7f, 7f, 0f, true, false, 8f, 6.3f)
        moveTo(8f, 2.5f); lineTo(8f, 6.5f); lineTo(4f, 6.5f)
    }
    val Help = icon("help") {
        moveTo(12f, 3f); arcTo(9f, 9f, 0f, true, true, 11.99f, 3f)
        moveTo(9.3f, 9.5f); quadTo(9.5f, 7f, 12f, 7f); quadTo(14.7f, 7f, 14.7f, 9.5f); quadTo(14.7f, 11.2f, 12f, 12.3f); lineTo(12f, 14f)
        moveTo(12f, 17f); lineTo(12f, 17.2f)
    }
    val Eye = icon("eye") {
        moveTo(2.5f, 12f); quadTo(12f, 2.5f, 21.5f, 12f); quadTo(12f, 21.5f, 2.5f, 12f); close()
        moveTo(12f, 9f); arcTo(3f, 3f, 0f, true, true, 11.99f, 9f)
    }
    val List = icon("list") {
        moveTo(8f, 6f); lineTo(20f, 6f); moveTo(8f, 12f); lineTo(20f, 12f); moveTo(8f, 18f); lineTo(20f, 18f)
        moveTo(4f, 6f); lineTo(4.2f, 6f); moveTo(4f, 12f); lineTo(4.2f, 12f); moveTo(4f, 18f); lineTo(4.2f, 18f)
    }
    val Door = icon("door") {
        moveTo(6f, 21f); lineTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f)
        moveTo(3.5f, 21f); lineTo(20.5f, 21f); moveTo(14.5f, 12f); lineTo(14.6f, 12f)
    }
    val Add = icon("add") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    val Delete = icon("delete") {
        moveTo(4f, 7f); lineTo(20f, 7f); moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
        moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f)
    }
    val Edit = icon("edit") {
        moveTo(4f, 20f); lineTo(8f, 19f); lineTo(19f, 8f); lineTo(16f, 5f); lineTo(5f, 16f); close()
    }
    val Music = icon("music") {
        moveTo(9f, 18f); lineTo(9f, 5f); lineTo(19f, 3f); lineTo(19f, 16f)
        moveTo(9f, 18f); arcTo(2.5f, 2.5f, 0f, true, true, 8.99f, 18f)
        moveTo(19f, 16f); arcTo(2.5f, 2.5f, 0f, true, true, 18.99f, 16f)
    }
}
