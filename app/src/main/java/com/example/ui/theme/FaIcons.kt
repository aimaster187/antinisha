package com.example.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class FaStyle {
    SOLID, REGULAR, BRANDS
}

@Composable
fun FaIcon(
    glyph: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    size: TextUnit = 18.sp,
    style: FaStyle = FaStyle.SOLID
) {
    val family = when (style) {
        FaStyle.SOLID -> FaSolidFontFamily
        FaStyle.REGULAR -> FaRegularFontFamily
        FaStyle.BRANDS -> FaBrandsFontFamily
    }
    val semanticsMod = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }
    Text(
        text = glyph,
        fontFamily = family,
        fontSize = size,
        lineHeight = size,
        color = color,
        textAlign = TextAlign.Center,
        modifier = semanticsMod
    )
}

object FaGlyphs {
    const val HOUSE = "\uF015"
    const val COMMENTS = "\uF086"
    const val COMMENT_DOTS = "\uF4AD"
    const val MICROPHONE = "\uF130"
    const val MICROPHONE_SLASH = "\uF131"
    const val ROBOT = "\uF544"
    const val BRAIN = "\uF5DC"
    const val ATOM = "\uF5D2"
    const val CODE = "\uF121"
    const val TERMINAL = "\uF120"
    const val LAPTOP_CODE = "\uF5FC"
    const val BARS = "\uF0C9"
    const val BELL = "\uF0F3"
    const val USER = "\uF007"
    const val USER_SHIELD = "\uF505"
    const val USERS = "\uF0C0"
    const val GEAR = "\uF013"
    const val SLIDERS = "\uF1DE"
    const val KEY = "\uF084"
    const val LOCK = "\uF023"
    const val SHIELD_HALVED = "\uF3ED"
    const val BOLT = "\uF0E7"
    const val SPARKLES = "\uE2CA"
    const val SUN = "\uF185"
    const val CLOUD_SUN = "\uF6C4"
    const val BATTERY_THREE_QUARTERS = "\uF241"
    const val BATTERY_FULL = "\uF240"
    const val CLOCK = "\uF017"
    const val BOOK = "\uF02D"
    const val BOOK_OPEN = "\uF518"
    const val NOTE_STICKY = "\uF249"
    const val PEN_TO_SQUARE = "\uF044"
    const val MUSIC = "\uF001"
    const val PLAY = "\uF04B"
    const val PAUSE = "\uF04C"
    const val STOP = "\uF04D"
    const val VOLUME_HIGH = "\uF028"
    const val IMAGE = "\uF03E"
    const val CAMERA = "\uF030"
    const val FILE_CODE = "\uF1C9"
    const val FILE_LINES = "\uF15C"
    const val FOLDER_OPEN = "\uF07C"
    const val PAPER_PLANE = "\uF1D8"
    const val ROTATE_RIGHT = "\uF2F9"
    const val COPY = "\uF0C5"
    const val SHARE_NODES = "\uF1E0"
    const val TRASH = "\uF1F8"
    const val PLUS = "\uF067"
    const val CHECK = "\uF00C"
    const val XMARK = "\uF00D"
    const val CHEVRON_RIGHT = "\uF054"
    const val ARROW_LEFT = "\uF060"
    const val GLOBE = "\uF0AC"
    const val SEARCH = "\uF002"
    const val CHART_LINE = "\uF201"
    const val BULLHORN = "\uF0A1"
    const val CROWN = "\uF521"
    const val GEM = "\uF3A5"
    const val DATABASE = "\uF1C0"
    const val SERVER = "\uF233"
    const val PHONE = "\uF095"
    const val COMMENT_SMS = "\uF7CD"
    const val HEART_PULSE = "\uF21E"
    const val CALENDAR_CHECK = "\uF274"
    const val HOURGLASS_HALF = "\uF252"
    const val LAYER_GROUP = "\uF5FD"
    const val CIRCLE_NODES = "\uE4E2"
    const val EYE = "\uF06E"
    const val DOWNLOAD = "\uF019"
    const val UPLOAD = "\uF093"
    const val CIRCLE_INFO = "\uF05A"
    const val TRIANGLE_EXCLAMATION = "\uF071"
    const val RIGHT_FROM_BRACKET = "\uF2F5"
    const val STORE = "\uF54E"
    const val DIAGRAM_PROJECT = "\uF542"
    const val DESKTOP = "\uF390"
    const val CIRCLE_CHECK = "\uF058"
    const val WAND_MAGIC = "\uF0D0"
    const val TOOLBOX = "\uF552"
    const val SATELLITE_DISH = "\uF7C0"
    const val COMPASS = "\uF14E"

    // Brand glyphs (use with FaStyle.BRANDS)
    const val BRAND_GOOGLE = "\uF1A0"
    const val BRAND_WHATSAPP = "\uF232"
    const val BRAND_GITHUB = "\uF09B"

    fun byName(name: String): String = when (name) {
        "atom" -> ATOM
        "code" -> CODE
        "globe" -> GLOBE
        "eye" -> EYE
        "brain" -> BRAIN
        "bolt" -> BOLT
        "terminal" -> TERMINAL
        "shield" -> SHIELD_HALVED
        "crown" -> CROWN
        "sparkles" -> SPARKLES
        "diagram" -> DIAGRAM_PROJECT
        else -> ROBOT
    }
}
