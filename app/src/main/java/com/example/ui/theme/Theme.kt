package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LuxuryWhiteColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = PearlWhite,
    primaryContainer = LavenderGlow,
    onPrimaryContainer = RoyalPurple,
    secondary = IceBlueDeep,
    onSecondary = PearlWhite,
    secondaryContainer = IceBlueSoft,
    onSecondaryContainer = ObsidianSlate,
    tertiary = RoyalPurple,
    onTertiary = PearlWhite,
    tertiaryContainer = MistSurface,
    onTertiaryContainer = ObsidianSlate,
    background = AlabasterCanvas,
    onBackground = ObsidianSlate,
    surface = PearlWhite,
    onSurface = ObsidianSlate,
    surfaceVariant = MistSurface,
    onSurfaceVariant = IndigoMuted,
    outline = FrostBorder,
    outlineVariant = HairlineDivider,
    error = CrimsonAlert,
    onError = PearlWhite,
    errorContainer = CrimsonSoft,
    onErrorContainer = CrimsonAlert
)

@Composable
fun NishaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryWhiteColorScheme,
        typography = NishaTypography,
        content = content
    )
}

val NishaAccentGradient = Brush.linearGradient(
    colors = listOf(IceBlueDeep, ElectricViolet, RoyalPurple)
)

val NishaSoftAuraGradient = Brush.verticalGradient(
    colors = listOf(AlabasterCanvas, PearlWhite, MistSurface)
)

@Composable
fun NishaGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 6.dp,
    borderColor: Color = FrostBorder,
    containerColor: Color = FrostGlass,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val shadowMod = modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ElectricViolet.copy(alpha = 0.10f),
        spotColor = IceBlueDeep.copy(alpha = 0.14f)
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = shadowMod,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(content = content)
        }
    } else {
        Surface(
            modifier = shadowMod,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(content = content)
        }
    }
}

@Composable
fun NishaSectionHeader(
    title: String,
    subtitle: String? = null,
    iconGlyph: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    brush = Brush.linearGradient(listOf(IceBlueSoft, LavenderGlow)),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            FaIcon(
                glyph = iconGlyph,
                contentDescription = title,
                color = ElectricViolet,
                size = 16.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ObsidianSlate
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IndigoMuted
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun NishaStatusPill(
    label: String,
    iconGlyph: String,
    tint: Color = ElectricViolet,
    background: Color = LavenderGlow,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = background,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FaIcon(glyph = iconGlyph, contentDescription = null, color = tint, size = 11.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
