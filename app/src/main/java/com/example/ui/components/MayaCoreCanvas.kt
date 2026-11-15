package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AiCoreState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FaGlyphs
import com.example.ui.theme.FaIcon
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceBlueDeep
import com.example.ui.theme.PearlWhite
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SoftOrchid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MayaCore3DCanvas(
    state: AiCoreState,
    amplitude: Float,
    onTapCore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "maya_core")
    val speedMult = when (state) {
        AiCoreState.IDLE -> 1f
        AiCoreState.LISTENING -> 2.1f
        AiCoreState.THINKING -> 2.8f
        AiCoreState.SPEAKING -> 1.8f
    }
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (11000 / speedMult).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == AiCoreState.IDLE) 2200 else 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )
    val ampBoost by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f) * 0.16f,
        animationSpec = spring(),
        label = "amp_boost"
    )

    Box(
        modifier = modifier
            .size(250.dp)
            .testTag("maya_3d_core")
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTapCore() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseR = size.minDimension * 0.31f * (pulse + ampBoost)

            // Outer ambient luxury light halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        IceBlue.copy(alpha = 0.28f),
                        ElectricViolet.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.50f
                ),
                radius = size.minDimension * 0.50f,
                center = Offset(cx, cy)
            )

            // 3D Gyroscopic Ring 1
            rotate(degrees = rotation, pivot = Offset(cx, cy)) {
                drawOval(
                    brush = Brush.sweepGradient(
                        listOf(IceBlue, ElectricViolet, RoyalPurple.copy(alpha = 0.2f), IceBlue)
                    ),
                    topLeft = Offset(cx - baseR * 1.48f, cy - baseR * 0.96f),
                    size = Size(baseR * 2.96f, baseR * 1.92f),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3D Gyroscopic Ring 2 (Counter-rotating)
            rotate(degrees = -rotation * 1.35f + 52f, pivot = Offset(cx, cy)) {
                drawOval(
                    brush = Brush.sweepGradient(
                        listOf(ElectricViolet, SoftOrchid, IceBlueDeep.copy(alpha = 0.25f), ElectricViolet)
                    ),
                    topLeft = Offset(cx - baseR * 1.32f, cy - baseR * 0.80f),
                    size = Size(baseR * 2.64f, baseR * 1.60f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3D Gyroscopic Ring 3 (Vertical axis tilt)
            rotate(degrees = rotation * 0.8f - 62f, pivot = Offset(cx, cy)) {
                drawOval(
                    color = IceBlueDeep.copy(alpha = 0.50f),
                    topLeft = Offset(cx - baseR * 1.18f, cy - baseR * 1.12f),
                    size = Size(baseR * 2.36f, baseR * 2.24f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Orbiting energy particles
            val particleCount = 12
            for (i in 0 until particleCount) {
                val angleRad = ((rotation * (if (i % 2 == 0) 1f else -1f)) + i * (360f / particleCount)) * (PI / 180.0)
                val orbitRx = baseR * (1.28f + (i % 3) * 0.14f)
                val orbitRy = baseR * (0.95f + (i % 2) * 0.22f)
                val px = cx + (cos(angleRad) * orbitRx).toFloat()
                val py = cy + (sin(angleRad) * orbitRy).toFloat()
                val pColor = if (i % 2 == 0) IceBlue else ElectricViolet
                drawCircle(
                    color = pColor.copy(alpha = 0.78f),
                    radius = (3.2f + (i % 3) * 1.2f).dp.toPx() * 0.5f,
                    center = Offset(px, py)
                )
            }

            // Central 3D Crystalline Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PearlWhite,
                        IceBlue.copy(alpha = 0.82f),
                        ElectricViolet.copy(alpha = 0.92f),
                        RoyalPurple
                    ),
                    center = Offset(cx - baseR * 0.28f, cy - baseR * 0.28f),
                    radius = baseR * 1.35f
                ),
                radius = baseR,
                center = Offset(cx, cy)
            )

            // Specular pearl glass highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PearlWhite.copy(alpha = 0.92f), Color.Transparent),
                    center = Offset(cx - baseR * 0.34f, cy - baseR * 0.36f),
                    radius = baseR * 0.58f
                ),
                radius = baseR * 0.58f,
                center = Offset(cx - baseR * 0.28f, cy - baseR * 0.30f)
            )
        }
    }
}

@Composable
fun Floating10DOrb(
    coreState: AiCoreState,
    amplitude: Float,
    sizeDp: Float,
    compactMode: Boolean,
    offsetX: Float,
    offsetY: Float,
    onDragDelta: (Float, Float) -> Unit,
    onTapOrb: () -> Unit,
    onLongPressOrb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actualSize = (if (compactMode) (sizeDp * 0.78f) else sizeDp).coerceIn(48f, 92f)
    val infinite = rememberInfiniteTransition(label = "orb_10d")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (coreState == AiCoreState.IDLE) 1800 else 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )
    val scaleBoost = 1f + (amplitude * 0.14f)

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size((actualSize * pulse * scaleBoost).dp)
            .shadow(14.dp, CircleShape, ambientColor = ElectricViolet, spotColor = IceBlueDeep)
            .testTag("floating_10d_orb")
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTapOrb() },
                    onLongPress = { onLongPressOrb() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            border = BorderStroke(2.dp, PearlWhite.copy(alpha = 0.92f)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            when (coreState) {
                                AiCoreState.LISTENING -> listOf(IceBlue, IceBlueDeep, ElectricViolet)
                                AiCoreState.THINKING -> listOf(ElectricViolet, RoyalPurple, IceBlue)
                                AiCoreState.SPEAKING -> listOf(SoftOrchid, IceBlue, RoyalPurple)
                                AiCoreState.IDLE -> listOf(IceBlueDeep, ElectricViolet, RoyalPurple)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val glyph = when (coreState) {
                    AiCoreState.LISTENING -> FaGlyphs.MICROPHONE
                    AiCoreState.THINKING -> FaGlyphs.ATOM
                    AiCoreState.SPEAKING -> FaGlyphs.VOLUME_HIGH
                    AiCoreState.IDLE -> FaGlyphs.SPARKLES
                }
                FaIcon(
                    glyph = glyph,
                    contentDescription = "Floating 10D AI Orb (${coreState.label})",
                    color = PearlWhite,
                    size = if (compactMode) 16.sp else 20.sp
                )
            }
        }
    }
}

@Composable
fun FourEdgeLightingFrame(
    enabled: Boolean,
    style: String,
    palette: String,
    brightness: Float,
    speed: Float,
    modifier: Modifier = Modifier
) {
    if (!enabled) return
    val infinite = rememberInfiniteTransition(label = "edge_light")
    val duration = (3600f / speed.coerceIn(0.4f, 2.5f)).toInt()
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edge_phase"
    )
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (duration / 2).coerceAtLeast(600)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_pulse"
    )

    val colors = when (palette) {
        "Pearl Prism" -> listOf(IceBlue, PearlWhite, SoftOrchid, IceBlue)
        "Royal Aurora" -> listOf(RoyalPurple, ElectricViolet, IceBlueDeep, RoyalPurple)
        "Arctic Cyan" -> listOf(IceBlue, IceBlueDeep, Color(0xFF818CF8), IceBlue)
        else -> listOf(IceBlue, ElectricViolet, RoyalPurple, SoftOrchid, IceBlue)
    }

    val effectiveAlpha = (brightness.coerceIn(0.15f, 1.0f) *
        if (style == "Quantum Pulse") pulseAlpha else 0.9f).coerceIn(0.1f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokePx = when (style) {
            "Comet Orbit" -> 5.dp.toPx()
            "Quantum Pulse" -> 7.dp.toPx()
            else -> 6.dp.toPx()
        }
        rotate(degrees = phase, pivot = Offset(size.width / 2f, size.height / 2f)) {
            // Rotating gradient brush centered on viewport
        }
        val sweep = Brush.sweepGradient(
            colors = colors.map { it.copy(alpha = effectiveAlpha) },
            center = Offset(
                size.width / 2f + cos(phase * PI / 180.0).toFloat() * 40f,
                size.height / 2f + sin(phase * PI / 180.0).toFloat() * 40f
            )
        )
        drawRoundRect(
            brush = sweep,
            topLeft = Offset(strokePx / 2f, strokePx / 2f),
            size = Size(size.width - strokePx, size.height - strokePx),
            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
            style = Stroke(width = strokePx)
        )
    }
}
