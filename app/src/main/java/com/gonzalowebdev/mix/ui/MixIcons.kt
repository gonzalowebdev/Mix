package com.gonzalowebdev.mix.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

private fun ImageVector.Builder.line(d: String, width: Float = 1.8f, filled: Boolean = false): ImageVector.Builder =
    addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        fill = if (filled) SolidColor(Color.Black) else null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )

private fun rr(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}H${x + w - r}A$r $r 0 0 1 ${x + w} ${y + r}V${y + h - r}" +
        "A$r $r 0 0 1 ${x + w - r} ${y + h}H${x + r}A$r $r 0 0 1 $x ${y + h - r}" +
        "V${y + r}A$r $r 0 0 1 ${x + r} ${y}Z"

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx + r} ${cy}A$r $r 0 1 1 ${cx - r} ${cy}A$r $r 0 1 1 ${cx + r} ${cy}Z"

/** Set de íconos propio: trazo uniforme, puntas redondeadas. */
object MixIcons {
    val Logo: ImageVector by lazy {
        icon("Logo") {
            line("M5 17V6L9 10Q10.5 12 12 10L16 6V17", width = 2.6f)
            line("M13.6 14.6L16 17L18.4 14.6", width = 2.6f)
        }
    }
    val Mosaic: ImageVector by lazy {
        icon("Mosaic") {
            line(rr(4f, 4f, 7f, 10f, 2f))
            line(rr(13f, 4f, 7f, 6f, 2f))
            line(rr(13f, 12f, 7f, 8f, 2f))
            line(rr(4f, 16f, 7f, 4f, 1.5f))
        }
    }
    val Search: ImageVector by lazy {
        icon("Search") {
            line(circle(11f, 11f, 6f))
            line("M15.5 15.5L20 20")
        }
    }
    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            line("M3 7H6C9 7 11 9 12 12C13 15 15 17 18 17H21")
            line("M18 14L21 17L18 20")
            line("M3 17H6C7.8 17 9.1 16.2 10 15M14 9C14.9 7.8 16.2 7 18 7H21")
            line("M18 4L21 7L18 10")
        }
    }
    val MoreVert: ImageVector by lazy {
        icon("MoreVert") { line("M12 5.5V5.6M12 12V12.1M12 18.4V18.5", width = 2.6f) }
    }
    val Close: ImageVector by lazy { icon("Close") { line("M6 6L18 18M18 6L6 18") } }
    val Clear: ImageVector get() = Close
    val ArrowBack: ImageVector by lazy { icon("ArrowBack") { line("M19 12H5M11 6L5 12L11 18") } }
    val Add: ImageVector by lazy { icon("Add") { line("M12 5V19M5 12H19") } }
    val SelectAll: ImageVector by lazy {
        icon("SelectAll") {
            line(rr(4f, 4f, 16f, 16f, 4f))
            line("M8.5 12.3L11 14.8L15.8 9.5")
        }
    }
    val PlayArrow: ImageVector by lazy { icon("PlayArrow") { line("M8 5.8V18.2L18.5 12Z") } }
    val Pause: ImageVector by lazy { icon("Pause") { line("M9 6V18M15 6V18", width = 2.4f) } }
    val SkipNext: ImageVector by lazy {
        icon("SkipNext") {
            line("M6 6L15 12L6 18Z")
            line("M18.5 6V18")
        }
    }
    val SkipPrevious: ImageVector by lazy {
        icon("SkipPrevious") {
            line("M18 6L9 12L18 18Z")
            line("M5.5 6V18")
        }
    }
    val Replay: ImageVector by lazy {
        icon("Replay") {
            line("M5.5 12A6.5 6.5 0 1 0 8 6.9")
            line("M5 3.5V8H9.5")
        }
    }
    val Repeat: ImageVector by lazy {
        icon("Repeat") {
            line("M17 3.5L20.5 7L17 10.5M3.5 11V10C3.5 8.3 4.8 7 6.5 7H20.5")
            line("M7 20.5L3.5 17L7 13.5M20.5 13V14C20.5 15.7 19.2 17 17.5 17H3.5")
        }
    }
    val RepeatOne: ImageVector by lazy {
        icon("RepeatOne") {
            line("M17 3.5L20.5 7L17 10.5M3.5 11V10C3.5 8.3 4.8 7 6.5 7H20.5")
            line("M7 20.5L3.5 17L7 13.5M20.5 13V14C20.5 15.7 19.2 17 17.5 17H3.5")
            line("M11 10.4L12.5 9.4V14.6", width = 1.6f)
        }
    }
    val PlaylistAdd: ImageVector by lazy {
        icon("PlaylistAdd") {
            line("M4 7H15M4 12H11M4 17H11")
            line("M17 13V21M13 17H21")
        }
    }
    val PlaylistPlay: ImageVector by lazy {
        icon("PlaylistPlay") {
            line("M4 7H15M4 12H11M4 17H10")
            line("M15 13.5V20.5L21 17Z")
        }
    }
    val QueueMusic: ImageVector get() = PlaylistPlay
    val Folder: ImageVector by lazy {
        icon("Folder") {
            line("M3 7.5C3 6.7 3.7 6 4.5 6H9L11 8.5H19.5C20.3 8.5 21 9.2 21 10V17.5C21 18.3 20.3 19 19.5 19H4.5C3.7 19 3 18.3 3 17.5Z")
        }
    }
    val Favorite: ImageVector by lazy {
        icon("Favorite") {
            line("M12 20S4 15 4 9.5C4 7 6 5.5 8 5.5C9.8 5.5 11.2 6.5 12 8C12.8 6.5 14.2 5.5 16 5.5C18 5.5 20 7 20 9.5C20 15 12 20 12 20Z")
        }
    }
    val FavoriteFilled: ImageVector by lazy {
        icon("FavoriteFilled") {
            line(
                "M12 20S4 15 4 9.5C4 7 6 5.5 8 5.5C9.8 5.5 11.2 6.5 12 8C12.8 6.5 14.2 5.5 16 5.5C18 5.5 20 7 20 9.5C20 15 12 20 12 20Z",
                filled = true
            )
        }
    }
    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            line(circle(12f, 12f, 9f))
            line("M8 12.3L10.8 15L16 9.3")
        }
    }
    val RadioButtonUnchecked: ImageVector by lazy { icon("RadioButtonUnchecked") { line(circle(12f, 12f, 9f)) } }
    val SwapVert: ImageVector by lazy {
        icon("SwapVert") { line("M8 4V20M5 17L8 20L11 17M16 20V4M13 7L16 4L19 7") }
    }
    val AutoAwesome: ImageVector by lazy {
        icon("AutoAwesome") {
            line("M11 3.5L12.9 9.1L18.5 11L12.9 12.9L11 18.5L9.1 12.9L3.5 11L9.1 9.1Z")
            line("M19 3V7M17 5H21")
        }
    }
    val Delete: ImageVector by lazy {
        icon("Delete") {
            line("M5 7H19M10 7V5H14V7M7 7L8 19H16L17 7M10.5 10.5V16M13.5 10.5V16")
        }
    }
    val Info: ImageVector by lazy {
        icon("Info") {
            line(circle(12f, 12f, 9f))
            line("M12 11V16.5")
            line("M12 7.8V7.9", width = 2.4f)
        }
    }
    val ScreenRotation: ImageVector by lazy {
        icon("ScreenRotation") {
            line(rr(4f, 10f, 16f, 10f, 2f))
            line("M8.5 6.4C9.5 5 11 4.2 12.6 4.2C15 4.2 17.1 5.8 17.9 8")
            line("M18 4.4V8.2H14.2")
        }
    }
    val DragHandle: ImageVector by lazy { icon("DragHandle") { line("M5 9H19M5 15H19") } }
    val Tune: ImageVector by lazy {
        icon("Tune") {
            line("M4 8H6.8M11.2 8H20M4 16H12.8M17.2 16H20")
            line(circle(9f, 8f, 2.2f))
            line(circle(15f, 16f, 2.2f))
        }
    }
    val Share: ImageVector by lazy {
        icon("Share") {
            line("M12 15V4M8 8L12 4L16 8M5 13V18.5C5 19.3 5.7 20 6.5 20H17.5C18.3 20 19 19.3 19 18.5V13")
        }
    }
    val PictureInPictureAlt: ImageVector by lazy {
        icon("PictureInPictureAlt") {
            line(rr(3f, 5f, 18f, 14f, 2.5f))
            line(rr(12f, 12f, 7f, 5f, 1.2f))
        }
    }
    val VolumeUp: ImageVector by lazy {
        icon("VolumeUp") {
            line("M4 10V14H8L13 18V6L8 10Z")
            line("M16.5 9C17.5 10.2 17.5 13.8 16.5 15M19 7C21 9.6 21 14.4 19 17")
        }
    }
    val VolumeOff: ImageVector by lazy {
        icon("VolumeOff") {
            line("M4 10V14H8L13 18V6L8 10Z")
            line("M16 9.5L21 14.5M21 9.5L16 14.5")
        }
    }
}
