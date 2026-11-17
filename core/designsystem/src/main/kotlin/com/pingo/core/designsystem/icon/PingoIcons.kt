package com.pingo.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun pathOf(path: String) = PathParser().parsePathString(path).toNodes()

private fun strokeIcon(name: String, vararg paths: String): ImageVector = strokeIcon(
    name = name,
    paths = paths.toList(),
    filledPaths = emptyList(),
)

private fun strokeIcon(name: String, paths: List<String>, filledPaths: List<String>): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    for (path in filledPaths) {
        builder.addPath(
            pathData = pathOf(path),
            fill = SolidColor(Color.Black),
            stroke = null,
        )
    }

    for (path in paths) {
        builder.addPath(
            pathData = pathOf(path),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }

    return builder.build()
}

object PingoIcons {
    val ArrowDownLeft: ImageVector by lazy {
        strokeIcon("ArrowDownLeft", "M17 7 7 17", "M17 17H7V7")
    }

    val ArrowLeft: ImageVector by lazy {
        strokeIcon("ArrowLeft", "m12 19-7-7 7-7", "M19 12H5")
    }

    val ArrowLeftRight: ImageVector by lazy {
        strokeIcon("ArrowLeftRight", "M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")
    }

    val ArrowUpRight: ImageVector by lazy {
        strokeIcon("ArrowUpRight", "M7 7h10v10", "M7 17 17 7")
    }

    val Banknote: ImageVector by lazy {
        strokeIcon("Banknote", "M2 6h20v12H2Z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z", "M6 12h.01", "M18 12h.01")
    }

    val BatteryCharging: ImageVector by lazy {
        strokeIcon(
            name = "BatteryCharging",
            "M15 7h1a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2h-2",
            "M6 7H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h1",
            "m11 7-3 5h4l-3 5",
            "M22 11L22 13",
        )
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell", "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")
    }

    val BellOff: ImageVector by lazy {
        strokeIcon(
            name = "BellOff",
            "M8.7 3A6 6 0 0 1 18 8a21.3 21.3 0 0 0 .6 5",
            "M17 17H3s3-2 3-9a4.67 4.67 0 0 1 .3-1.7",
            "M10.3 21a1.94 1.94 0 0 0 3.4 0",
            "m2 2 20 20",
        )
    }

    val Bus: ImageVector by lazy {
        strokeIcon(
            name = "Bus",
            "M8 6v6",
            "M15 6v6",
            "M2 12h19.6",
            "M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2 0-.4-.1-.8-.2-1.2l-1.4-5C20.1 6.8 19.1 6 18 6H4a2 2 0 0 0-2 2v10h3",
            "M5 18A2 2 0 1 0 9 18A2 2 0 1 0 5 18Z",
            "M9 18h5",
            "M14 18A2 2 0 1 0 18 18A2 2 0 1 0 14 18Z",
        )
    }

    val Calendar: ImageVector by lazy {
        strokeIcon(
            name = "Calendar",
            "M8 2v4",
            "M16 2v4",
            "M5 4H19A2 2 0 0 1 21 6V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V6A2 2 0 0 1 5 4Z",
            "M3 10h18",
        )
    }

    val CalendarDays: ImageVector by lazy {
        strokeIcon(
            name = "CalendarDays",
            "M8 2v4",
            "M16 2v4",
            "M5 4H19A2 2 0 0 1 21 6V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V6A2 2 0 0 1 5 4Z",
            "M3 10h18",
            "M8 14h.01",
            "M12 14h.01",
            "M16 14h.01",
            "M8 18h.01",
            "M12 18h.01",
            "M16 18h.01",
        )
    }

    val Car: ImageVector by lazy {
        strokeIcon(
            name = "Car",
            (
                "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-" +
                    "1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2"
                ),
            "M5 17A2 2 0 1 0 9 17A2 2 0 1 0 5 17Z",
            "M9 17h6",
            "M15 17A2 2 0 1 0 19 17A2 2 0 1 0 15 17Z",
        )
    }

    val ChartPie: ImageVector by lazy {
        strokeIcon(
            name = "ChartPie",
            "M21 12c.552 0 1.005-.449.95-.998a10 10 0 0 0-8.953-8.951c-.55-.055-.998.398-.998.95v8a1 1 0 0 0 1 1z",
            "M21.21 15.89A10 10 0 1 1 8 2.83",
        )
    }

    val Check: ImageVector by lazy { strokeIcon("Check", "M20 6 9 17l-5-5") }

    val ChevronDown: ImageVector by lazy { strokeIcon("ChevronDown", "m6 9 6 6 6-6") }

    val ChevronLeft: ImageVector by lazy { strokeIcon("ChevronLeft", "m15 18-6-6 6-6") }

    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight", "m9 18 6-6-6-6") }

    val ChevronUp: ImageVector by lazy { strokeIcon("ChevronUp", "m18 15-6-6-6 6") }

    val CircleAlert: ImageVector by lazy {
        strokeIcon("CircleAlert", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20Z", "M12 8v4", "M12 16h.01")
    }

    val CircleCheck: ImageVector by lazy {
        strokeIcon("CircleCheck", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "m9 12 2 2 4-4")
    }

    val CircleDollarSign: ImageVector by lazy {
        strokeIcon("CircleDollarSign", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8", "M12 18V6")
    }

    val CircleHelp: ImageVector by lazy {
        strokeIcon("CircleHelp", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.01")
    }

    val CircleX: ImageVector by lazy {
        strokeIcon("CircleX", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "m15 9-6 6", "m9 9 6 6")
    }

    val Clock: ImageVector by lazy {
        strokeIcon("Clock", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "M12 6 L12 12 L16 14")
    }

    val CreditCard: ImageVector by lazy {
        strokeIcon("CreditCard", "M4 5H20A2 2 0 0 1 22 7V17A2 2 0 0 1 20 19H4A2 2 0 0 1 2 17V7A2 2 0 0 1 4 5Z", "M2 10L22 10")
    }

    val Delete: ImageVector by lazy {
        strokeIcon(
            name = "Delete",
            "M10 5a2 2 0 0 0-1.344.519l-6.328 5.74a1 1 0 0 0 0 1.481l6.328 5.741A2 2 0 0 0 10 19h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2z",
            "m12 9 6 6",
            "m18 9-6 6",
        )
    }

    val Download: ImageVector by lazy {
        strokeIcon("Download", "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10 L12 15 L17 10", "M12 15L12 3")
    }

    val Droplet: ImageVector by lazy {
        strokeIcon("Droplet", "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z")
    }

    val EllipsisVertical: ImageVector by lazy {
        strokeIcon(
            name = "EllipsisVertical",
            "M11 12A1 1 0 1 0 13 12A1 1 0 1 0 11 12Z",
            "M11 5A1 1 0 1 0 13 5A1 1 0 1 0 11 5Z",
            "M11 19A1 1 0 1 0 13 19A1 1 0 1 0 11 19Z",
        )
    }

    val Eye: ImageVector by lazy {
        strokeIcon(
            name = "Eye",
            "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0",
            "M9 12A3 3 0 1 0 15 12A3 3 0 1 0 9 12Z",
        )
    }

    val EyeOff: ImageVector by lazy {
        strokeIcon(
            name = "EyeOff",
            "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49",
            "M14.084 14.158a3 3 0 0 1-4.242-4.242",
            "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143",
            "m2 2 20 20",
        )
    }

    val FileText: ImageVector by lazy {
        strokeIcon(
            name = "FileText",
            "M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z",
            "M14 2v4a2 2 0 0 0 2 2h4",
            "M10 9H8",
            "M16 13H8",
            "M16 17H8",
        )
    }

    val Filter: ImageVector by lazy { strokeIcon("Filter", "M22 3 L2 3 L10 12.46 L10 19 L14 21 L14 12.46 L22 3 Z") }

    val Fingerprint: ImageVector by lazy {
        strokeIcon(
            name = "Fingerprint",
            "M12 10a2 2 0 0 0-2 2c0 1.02-.1 2.51-.26 4",
            "M14 13.12c0 2.38 0 6.38-1 8.88",
            "M17.29 21.02c.12-.6.43-2.3.5-3.02",
            "M2 12a10 10 0 0 1 18-6",
            "M2 16h.01",
            "M21.8 16c.2-2 .131-5.354 0-6",
            "M5 19.5C5.5 18 6 15 6 12a6 6 0 0 1 .34-2",
            "M8.65 22c.21-.66.45-1.32.57-2",
            "M9 6.8a6 6 0 0 1 9 5.2v2",
        )
    }

    val Fuel: ImageVector by lazy {
        strokeIcon(
            name = "Fuel",
            "M3 22L15 22",
            "M4 9L14 9",
            "M14 22V4a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v18",
            "M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 2 2a2 2 0 0 0 2-2V9.83a2 2 0 0 0-.59-1.42L18 5",
        )
    }

    val Gamepad2: ImageVector by lazy {
        strokeIcon(
            name = "Gamepad2",
            "M6 11L10 11",
            "M8 9L8 13",
            "M15 12L15.01 12",
            "M18 10L18.01 10",
            (
                "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2" +
                    "-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.6" +
                    "85-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z"
                ),
        )
    }

    val Gift: ImageVector by lazy {
        strokeIcon(
            name = "Gift",
            "M4 8H20A1 1 0 0 1 21 9V11A1 1 0 0 1 20 12H4A1 1 0 0 1 3 11V9A1 1 0 0 1 4 8Z",
            "M12 8v13",
            "M19 12v7a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-7",
            "M7.5 8a2.5 2.5 0 0 1 0-5A4.8 8 0 0 1 12 8a4.8 8 0 0 1 4.5-5 2.5 2.5 0 0 1 0 5",
        )
    }

    val GraduationCap: ImageVector by lazy {
        strokeIcon(
            name = "GraduationCap",
            "M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z",
            "M22 10v6",
            "M6 12.5V16a6 3 0 0 0 12 0v-3.5",
        )
    }

    val HeartPulse: ImageVector by lazy {
        strokeIcon(
            name = "HeartPulse",
            (
                "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2" +
                    ".3 1.5 4.05 3 5.5l7 7Z"
                ),
            "M3.22 12H9.5l.5-1 2 4.5 2-7 1.5 3.5h5.27",
        )
    }

    val House: ImageVector by lazy {
        strokeIcon(
            name = "House",
            "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8",
            "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        )
    }

    val HousePlug: ImageVector by lazy {
        strokeIcon(
            name = "HousePlug",
            "M10 12V8.964",
            "M14 12V8.964",
            "M15 12a1 1 0 0 1 1 1v2a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-2a1 1 0 0 1 1-1z",
            (
                "M8.5 21H5a2 2 0 0 1-2-2v-9a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2h" +
                    "-5a2 2 0 0 1-2-2v-2"
                ),
        )
    }

    val Info: ImageVector by lazy {
        strokeIcon("Info", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "M12 16v-4", "M12 8h.01")
    }

    val Landmark: ImageVector by lazy {
        strokeIcon("Landmark", "M3 22L21 22", "M6 18L6 11", "M10 18L10 11", "M14 18L14 11", "M18 18L18 11", "M12 2 L20 7 L4 7 Z")
    }

    val List: ImageVector by lazy {
        strokeIcon("List", "M3 12h.01", "M3 18h.01", "M3 6h.01", "M8 12h13", "M8 18h13", "M8 6h13")
    }

    val LoaderCircle: ImageVector by lazy { strokeIcon("LoaderCircle", "M21 12a9 9 0 1 1-6.219-8.56") }

    val Lock: ImageVector by lazy {
        strokeIcon("Lock", "M5 11H19A2 2 0 0 1 21 13V20A2 2 0 0 1 19 22H5A2 2 0 0 1 3 20V13A2 2 0 0 1 5 11Z", "M7 11V7a5 5 0 0 1 10 0v4")
    }

    val LogOut: ImageVector by lazy {
        strokeIcon("LogOut", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17 L21 12 L16 7", "M21 12L9 12")
    }

    val Mail: ImageVector by lazy {
        strokeIcon(
            name = "Mail",
            "M4 4H20A2 2 0 0 1 22 6V18A2 2 0 0 1 20 20H4A2 2 0 0 1 2 18V6A2 2 0 0 1 4 4Z",
            "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7",
        )
    }

    val Minus: ImageVector by lazy { strokeIcon("Minus", "M5 12h14") }

    val Moon: ImageVector by lazy { strokeIcon("Moon", "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z") }

    val Pencil: ImageVector by lazy {
        strokeIcon(
            name = "Pencil",
            (
                "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0" +
                    " 0 0 .83-.497z"
                ),
            "m15 5 4 4",
        )
    }

    val PiggyBank: ImageVector by lazy {
        strokeIcon(
            name = "PiggyBank",
            "M19 5c-1.5 0-2.8 1.4-3 2-3.5-1.5-11-.3-11 5 0 1.8 0 3 2 4.5V20h4v-2h3v2h4v-4c1-.5 1.7-1 2-2h2v-4h-2c0-1-.5-1.5-1-2V5z",
            "M2 9v1c0 1.1.9 2 2 2h1",
            "M16 11h.01",
        )
    }

    val Pill: ImageVector by lazy {
        strokeIcon("Pill", "m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7")
    }

    val Plane: ImageVector by lazy {
        strokeIcon(
            name = "Plane",
            (
                "M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3" +
                    "L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"
                ),
        )
    }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus", "M5 12h14", "M12 5v14")
    }

    val PlusCircle: ImageVector by lazy {
        strokeIcon("PlusCircle", "M2 12A10 10 0 1 0 22 12A10 10 0 1 0 2 12Z", "M8 12h8", "M12 8v8")
    }

    val Receipt: ImageVector by lazy {
        strokeIcon(
            name = "Receipt",
            "M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z",
            "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8",
            "M12 17.5v-11",
        )
    }

    val RefreshCw: ImageVector by lazy {
        strokeIcon(
            name = "RefreshCw",
            "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
            "M21 3v5h-5",
            "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
            "M8 16H3v5",
        )
    }

    val Repeat: ImageVector by lazy {
        strokeIcon("Repeat", "m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")
    }

    val RotateCcw: ImageVector by lazy {
        strokeIcon("RotateCcw", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5")
    }

    val Search: ImageVector by lazy {
        strokeIcon("Search", "M3 11A8 8 0 1 0 19 11A8 8 0 1 0 3 11Z", "m21 21-4.3-4.3")
    }

    val Settings: ImageVector by lazy {
        strokeIcon(
            name = "Settings",
            (
                "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 " +
                    "0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08" +
                    "a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 " +
                    "2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 " +
                    "2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"
                ),
            "M9 12A3 3 0 1 0 15 12A3 3 0 1 0 9 12Z",
        )
    }

    val ShieldCheck: ImageVector by lazy {
        strokeIcon(
            name = "ShieldCheck",
            (
                "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 " +
                    "0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"
                ),
            "m9 12 2 2 4-4",
        )
    }

    val Shirt: ImageVector by lazy {
        strokeIcon(
            name = "Shirt",
            (
                "M20.38 3.46 16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 " +
                    "0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"
                ),
        )
    }

    val ShoppingBag: ImageVector by lazy {
        strokeIcon("ShoppingBag", "M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z", "M3 6h18", "M16 10a4 4 0 0 1-8 0")
    }

    val ShoppingCart: ImageVector by lazy {
        strokeIcon(
            name = "ShoppingCart",
            "M7 21A1 1 0 1 0 9 21A1 1 0 1 0 7 21Z",
            "M18 21A1 1 0 1 0 20 21A1 1 0 1 0 18 21Z",
            "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12",
        )
    }

    val SlidersHorizontal: ImageVector by lazy {
        strokeIcon(
            name = "SlidersHorizontal",
            "M21 4L14 4",
            "M10 4L3 4",
            "M21 12L12 12",
            "M8 12L3 12",
            "M21 20L16 20",
            "M12 20L3 20",
            "M14 2L14 6",
            "M8 10L8 14",
            "M16 18L16 22",
        )
    }

    val Smartphone: ImageVector by lazy {
        strokeIcon("Smartphone", "M7 2H17A2 2 0 0 1 19 4V20A2 2 0 0 1 17 22H7A2 2 0 0 1 5 20V4A2 2 0 0 1 7 2Z", "M12 18h.01")
    }

    val Sparkles: ImageVector by lazy {
        strokeIcon(
            name = "Sparkles",
            (
                "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0" +
                    " 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6" +
                    ".135a.5.5 0 0 1-.963 0z"
                ),
            "M20 3v4",
            "M22 5h-4",
            "M4 17v2",
            "M5 18H3",
        )
    }

    val Store: ImageVector by lazy {
        strokeIcon(
            name = "Store",
            "m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7",
            "M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8",
            "M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4",
            "M2 7h20",
            (
                "M22 7v3a2 2 0 0 1-2 2a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-" +
                    ".82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0" +
                    " 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7"
                ),
        )
    }

    val Sun: ImageVector by lazy {
        strokeIcon(
            name = "Sun",
            "M8 12A4 4 0 1 0 16 12A4 4 0 1 0 8 12Z",
            "M12 2v2",
            "M12 20v2",
            "m4.93 4.93 1.41 1.41",
            "m17.66 17.66 1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "m6.34 17.66-1.41 1.41",
            "m19.07 4.93-1.41 1.41",
        )
    }

    val Tag: ImageVector by lazy {
        strokeIcon(
            name = "Tag",
            paths = listOf(
                (
                    "M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42" +
                        " 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z"
                    ),
                "M7 7.5A0.5 0.5 0 1 0 8 7.5A0.5 0.5 0 1 0 7 7.5Z",
            ),
            filledPaths = listOf(
                "M7 7.5A0.5 0.5 0 1 0 8 7.5A0.5 0.5 0 1 0 7 7.5Z",
            ),
        )
    }

    val Ticket: ImageVector by lazy {
        strokeIcon(
            name = "Ticket",
            "M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z",
            "M13 5v2",
            "M13 17v2",
            "M13 11v2",
        )
    }

    val Trash2: ImageVector by lazy {
        strokeIcon(
            name = "Trash2",
            "M3 6h18",
            "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6",
            "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2",
            "M10 11L10 17",
            "M14 11L14 17",
        )
    }

    val TrendingDown: ImageVector by lazy {
        strokeIcon("TrendingDown", "M22 17 L13.5 8.5 L8.5 13.5 L2 7", "M16 17 L22 17 L22 11")
    }

    val TrendingUp: ImageVector by lazy {
        strokeIcon("TrendingUp", "M22 7 L13.5 15.5 L8.5 10.5 L2 17", "M16 7 L22 7 L22 13")
    }

    val TriangleAlert: ImageVector by lazy {
        strokeIcon("TriangleAlert", "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01")
    }

    val Undo2: ImageVector by lazy {
        strokeIcon("Undo2", "M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5a5.5 5.5 0 0 1-5.5 5.5H11")
    }

    val User: ImageVector by lazy {
        strokeIcon("User", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M8 7A4 4 0 1 0 16 7A4 4 0 1 0 8 7Z")
    }

    val Utensils: ImageVector by lazy {
        strokeIcon("Utensils", "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7")
    }

    val Wallet: ImageVector by lazy {
        strokeIcon(
            name = "Wallet",
            "M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1",
            "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4",
        )
    }

    val Wifi: ImageVector by lazy {
        strokeIcon("Wifi", "M12 20h.01", "M2 8.82a15 15 0 0 1 20 0", "M5 12.859a10 10 0 0 1 14 0", "M8.5 16.429a5 5 0 0 1 7 0")
    }

    val X: ImageVector by lazy {
        strokeIcon("X", "M18 6 6 18", "m6 6 12 12")
    }

    val Zap: ImageVector by lazy {
        strokeIcon(
            name = "Zap",
            (
                "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5" +
                    " 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"
                ),
        )
    }

    val ArrowRightLeft: ImageVector get() = ArrowLeftRight
    val Backspace: ImageVector get() = Delete
    val CarFront: ImageVector get() = Car
    val CheckCircle: ImageVector get() = CircleCheck
    val CirclePlus: ImageVector get() = PlusCircle
    val Gamepad: ImageVector get() = Gamepad2
    val Home: ImageVector get() = House
    val PieChart: ImageVector get() = ChartPie
    val Sliders: ImageVector get() = SlidersHorizontal
    val Undo: ImageVector get() = Undo2
}
