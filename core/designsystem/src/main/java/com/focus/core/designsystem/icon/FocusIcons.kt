package com.focus.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Thin-stroke icons (1.6), rounded caps and joins, 24x24 grid.
 * Drawn locally: sober, consistent look with no extra dependencies.
 */
object FocusIcons {
    val Play by lazy { icon("Play", "M7,4.5v15l12,-7.5z") }
    val Pause by lazy { icon("Pause", "M8.5,5v14", "M15.5,5v14") }
    val Stop by lazy { icon("Stop", "M7.5,6.5h9a1,1 0 0 1 1,1v9a1,1 0 0 1 -1,1h-9a1,1 0 0 1 -1,-1v-9a1,1 0 0 1 1,-1z") }
    val Skip by lazy { icon("Skip", "M6,5.5l9,6.5l-9,6.5z", "M18.5,5.5v13") }
    val Timer by lazy {
        icon("Timer", "M12,14m-7.5,0a7.5,7.5 0 1 0 15,0a7.5,7.5 0 1 0 -15,0", "M12,14V10.5", "M10,2.5h4", "M18,6.5l1.5,-1.5")
    }
    val Chart by lazy { icon("Chart", "M4,4v16h16", "M8.5,16v-4", "M12.5,16V8", "M16.5,16v-6") }
    val LineChart by lazy { icon("LineChart", "M4,4v16h16", "M7.5,14.5l3.5,-4l3,2.5l4.5,-5.5") }
    val Tune by lazy {
        icon("Tune", "M4,6h9", "M17,6h3", "M15,4v4", "M4,12h3", "M11,12h9", "M9,10v4", "M4,18h11", "M19,18h1", "M17,16v4")
    }
    val Compare by lazy { icon("Compare", "M7.5,4l-3.5,3.5l3.5,3.5", "M4,7.5h15", "M16.5,13l3.5,3.5l-3.5,3.5", "M20,16.5H5") }
    val Minus by lazy { icon("Minus", "M6,12h12") }
    val Plus by lazy { icon("Plus", "M12,6v12", "M6,12h12") }
    val Check by lazy { icon("Check", "M5,12.5l4.5,4.5l9.5,-10") }
    val Upload by lazy { icon("Upload", "M20,15v3a2,2 0 0 1 -2,2H6a2,2 0 0 1 -2,-2v-3", "M16.5,8.5l-4.5,-4.5l-4.5,4.5", "M12,4v11") }
    val Download by lazy { icon("Download", "M20,15v3a2,2 0 0 1 -2,2H6a2,2 0 0 1 -2,-2v-3", "M7.5,10.5l4.5,4.5l4.5,-4.5", "M12,15V4") }
    val Globe by lazy {
        icon(
            "Globe",
            "M12,12m-8.5,0a8.5,8.5 0 1 0 17,0a8.5,8.5 0 1 0 -17,0",
            "M3.5,12h17",
            "M12,3.5c2.2,2.3 3.4,5.3 3.4,8.5s-1.2,6.2 -3.4,8.5c-2.2,-2.3 -3.4,-5.3 -3.4,-8.5s1.2,-6.2 3.4,-8.5z",
        )
    }
    val Edit by lazy { icon("Edit", "M4,20h4l10.5,-10.5a2.1,2.1 0 0 0 -3,-3L5,17v3z", "M13.5,7.5l3,3") }
    val Trash by lazy { icon("Trash", "M4,7h16", "M10,11v6", "M14,11v6", "M6,7l1,12a2,2 0 0 0 2,2h6a2,2 0 0 0 2,-2l1,-12", "M9,7V4h6v3") }
    val Book by lazy { icon("Book", "M4,5.5a1.5,1.5 0 0 1 1.5,-1.5H19v16H5.5A1.5,1.5 0 0 1 4,18.5z", "M4,18.5A1.5,1.5 0 0 1 5.5,17H19", "M8,8h7") }
    val Trophy by lazy {
        icon("Trophy", "M8,4h8v5a4,4 0 0 1 -8,0z", "M8,6H5v1.5a3,3 0 0 0 3,3", "M16,6h3v1.5a3,3 0 0 1 -3,3", "M12,13v4", "M8.5,20h7", "M10,17h4v3h-4z")
    }
    val Lock by lazy { icon("Lock", "M6.5,11h11a1,1 0 0 1 1,1v7a1,1 0 0 1 -1,1h-11a1,1 0 0 1 -1,-1v-7a1,1 0 0 1 1,-1z", "M8.5,11V8a3.5,3.5 0 0 1 7,0v3") }
    val Box by lazy { icon("Box", "M6,4.5h12a1.5,1.5 0 0 1 1.5,1.5v12a1.5,1.5 0 0 1 -1.5,1.5H6A1.5,1.5 0 0 1 4.5,18V6A1.5,1.5 0 0 1 6,4.5z") }
    val BoxChecked by lazy { icon("BoxChecked", "M6,4.5h12a1.5,1.5 0 0 1 1.5,1.5v12a1.5,1.5 0 0 1 -1.5,1.5H6A1.5,1.5 0 0 1 4.5,18V6A1.5,1.5 0 0 1 6,4.5z", "M8.5,12.2l2.4,2.4l4.6,-5") }
    val ChevronDown by lazy { icon("ChevronDown", "M7,10l5,5l5,-5") }
    val ArrowLeft by lazy { icon("ArrowLeft", "M19,12H5", "M11,6l-6,6l6,6") }
    val ChevronRight by lazy { icon("ChevronRight", "M10,7l5,5l-5,5") }
    val Close by lazy { icon("Close", "M6.5,6.5l11,11", "M17.5,6.5l-11,11") }

    val Target by lazy {
        icon("Target", "M12,12m-8.5,0a8.5,8.5 0 1 0 17,0a8.5,8.5 0 1 0 -17,0", "M12,12m-4.5,0a4.5,4.5 0 1 0 9,0a4.5,4.5 0 1 0 -9,0", "M12,12m-0.8,0a0.8,0.8 0 1 0 1.6,0a0.8,0.8 0 1 0 -1.6,0")
    }
    val Calendar by lazy { icon("Calendar", "M5.5,5.5h13a1,1 0 0 1 1,1v12a1,1 0 0 1 -1,1h-13a1,1 0 0 1 -1,-1v-12a1,1 0 0 1 1,-1z", "M4.5,10h15", "M8.5,3.5v4", "M15.5,3.5v4") }
    val Star by lazy { icon("Star", "M12,4l2.4,5l5.4,0.7l-4,3.7l1,5.4l-4.8,-2.7l-4.8,2.7l1,-5.4l-4,-3.7l5.4,-0.7z") }
    val StarFilled by lazy { filled("StarFilled", "M12,4l2.4,5l5.4,0.7l-4,3.7l1,5.4l-4.8,-2.7l-4.8,2.7l1,-5.4l-4,-3.7l5.4,-0.7z") }
    val Shield by lazy { icon("Shield", "M12,3.5l7,2.8v5.2c0,4.3 -2.9,7.6 -7,9c-4.1,-1.4 -7,-4.7 -7,-9V6.3z") }
    val Insight by lazy { icon("Insight", "M9,18h6", "M10,21h4", "M12,3a6,6 0 0 0 -3.5,10.9c0.6,0.5 1,1.2 1,2.1h5c0,-0.9 0.4,-1.6 1,-2.1A6,6 0 0 0 12,3z") }

    val Sound by lazy { icon("Sound", "M4,9.5h3.5l4.5,-4v13l-4.5,-4H4z", "M15.5,9a4,4 0 0 1 0,6", "M18,6.5a7.5,7.5 0 0 1 0,11") }
    val Moon by lazy { icon("Moon", "M19.5,14.5A7.5,7.5 0 0 1 9.5,4.5a7.5,7.5 0 1 0 10,10z") }

    val Bell by lazy { icon("Bell", "M6,16.5V11a6,6 0 0 1 12,0v5.5l1.5,1.5h-15z", "M10,20.5a2,2 0 0 0 4,0") }
    val Repeat by lazy { icon("Repeat", "M4.5,12a7.5,7.5 0 0 1 13,-5", "M17.5,3.5v3.5h-3.5", "M19.5,12a7.5,7.5 0 0 1 -13,5", "M6.5,20.5v-3.5h3.5") }
    val Clock by lazy { icon("Clock", "M12,12m-8.5,0a8.5,8.5 0 1 0 17,0a8.5,8.5 0 1 0 -17,0", "M12,7.5V12l3,2") }

    private fun filled(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            fill = SolidColor(Color.White),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()

    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            b.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }
}
