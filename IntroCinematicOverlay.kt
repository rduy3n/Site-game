package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.audio.LocalSoundPlayer
import com.example.game.audio.SoundEffect
import com.example.game.data.FighterDefinition
import com.example.game.data.FighterVisualStyle
import com.example.game.data.GameMode
import com.example.game.engine.ArenaWorldState
import com.example.game.engine.FighterRuntime
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.UltiGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Smash Bros-style cinematic introduction directly on the fighting map:
 * - No banner cards, no UI boxes.
 * - The fighter physically emerges from their element right on the stage map.
 */
@Composable
fun IntroCinematicOverlay(
    world: ArenaWorldState,
    timeMs: Long,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = world.introDurationMs.coerceAtLeast(2500L)
    val elapsedMs = (totalMs - world.phaseTimerMs).coerceIn(0L, totalMs)
    val p1Focus = elapsedMs < 1150L
    val p2Focus = elapsedMs in 1150L until 2300L
    val vsClash = elapsedMs >= 2300L

    val soundPlayer = LocalSoundPlayer.current
    LaunchedEffect(p1Focus, p2Focus, vsClash) {
        if (p1Focus) soundPlayer.play(SoundEffect.ROUND_START)
        if (p2Focus) soundPlayer.play(SoundEffect.GLITCH_PORTAL)
        if (vsClash) soundPlayer.play(SoundEffect.ULTI_IMPACT)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("intro_cinematic_overlay")
    ) {
        // Arena Stage Backdrop (Fighter emerges directly on the map)
        Image(
            painter = painterResource(id = world.stage.backgroundRes),
            contentDescription = world.stage.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Volumetric lighting vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x7704060D),
                            Color(0x2205070E),
                            Color(0x9904060C)
                        )
                    )
                )
        )

        when {
            p1Focus -> {
                val progress = (elapsedMs / 1150f).coerceIn(0f, 1f)
                StageFighterEmergence(
                    runtime = world.p1,
                    sideLabel = "JOUEUR 1",
                    progress = progress,
                    timeMs = timeMs
                )
            }
            p2Focus -> {
                val progress = ((elapsedMs - 1150L) / 1150f).coerceIn(0f, 1f)
                StageFighterEmergence(
                    runtime = world.p2,
                    sideLabel = "ADVERSAIRE",
                    progress = progress,
                    timeMs = timeMs
                )
            }
            else -> {
                val progress = ((elapsedMs - 2300L) / 200f).coerceIn(0f, 1f)
                SmashBrosVsClashCanvas(
                    p1 = world.p1.def,
                    p2 = world.p2.def,
                    progress = progress
                )
            }
        }

        // Skip Button
        if (world.mode != GameMode.ONLINE && !vsClash) {
            OutlinedButton(
                onClick = onSkip,
                border = BorderStroke(1.2.dp, UltiGold.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 34.dp, end = 16.dp)
                    .height(48.dp)
                    .testTag("skip_intro_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "Passer l'intro",
                    tint = UltiGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "PASSER",
                    style = MaterialTheme.typography.labelLarge,
                    color = UltiGold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StageFighterEmergence(
    runtime: FighterRuntime,
    sideLabel: String,
    progress: Float,
    timeMs: Long
) {
    val fighter = runtime.def
    val emergeScale = 0.40f + progress * 0.60f
    val emergeAlpha = (progress / 0.22f).coerceIn(0f, 1f)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val groundY = h * 0.84f
            val cx = if (runtime.side == 1) w * 0.32f else w * 0.68f

            // Elemental Emergence Surge directly on the map
            when (fighter.visualStyle) {
                FighterVisualStyle.GLITCHERMAN -> {
                    for (ring in 1..4) {
                        val r = h * (0.10f + ring * 0.08f) * emergeScale
                        drawCircle(
                            color = (if (ring % 2 == 0) fighter.accentColor else fighter.primaryColor).copy(alpha = (1f - progress) * 0.8f),
                            radius = r,
                            center = Offset(cx, groundY - h * 0.2f),
                            style = Stroke(width = 4f)
                        )
                    }
                }
                FighterVisualStyle.JARDIN -> {
                    for (v in 0..6) {
                        val vx = cx + (v - 3f) * 35f
                        val vinePath = Path().apply {
                            moveTo(vx, groundY)
                            quadraticTo(vx + sin(progress * 5f + v) * 50f, groundY - h * 0.35f * progress, vx + (v - 3f) * 12f, groundY - h * 0.55f * progress)
                        }
                        drawPath(vinePath, color = Color(0xFF00E676).copy(alpha = 0.85f), style = Stroke(width = 8f, cap = StrokeCap.Round))
                    }
                }
                FighterVisualStyle.CHINOIS_BAGUETTES -> {
                    for (i in 0..10) {
                        val yOffset = (i / 10f) * h * 0.6f
                        val sx = cx + (sin(progress * 6f + i) * w * 0.25f)
                        drawLine(
                            color = Color(0xFFFFD600).copy(alpha = 0.7f),
                            start = Offset(sx - 40f, yOffset + h * 0.2f),
                            end = Offset(sx + 40f, yOffset + h * 0.2f - 20f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                FighterVisualStyle.DARON_BRESILIEN -> {
                    for (wave in 1..4) {
                        val r = h * (0.12f + wave * 0.10f) * emergeScale
                        drawCircle(
                            color = Color(0xFFFF2A85).copy(alpha = (1f - progress) * 0.6f),
                            radius = r,
                            center = Offset(cx, groundY - h * 0.25f),
                            style = Stroke(width = 6f)
                        )
                    }
                }
            }

            // Draw Detailed Fighter Character directly on the map stage
            drawDetailedFighter(
                f = runtime.copy(y = (1f - emergeScale) * 0.25f),
                arenaW = w,
                arenaH = h,
                groundY = groundY,
                timeMs = timeMs
            )
        }

        // Smash Bros Style Fighter Name & Title Overlay (No Banner Card)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .graphicsLayer {
                    alpha = emergeAlpha
                    scaleX = emergeScale
                    scaleY = emergeScale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = sideLabel,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = fighter.primaryColor
            )
            Text(
                text = fighter.name.uppercase(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White
            )
            Text(
                text = fighter.introAbilityTag,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = UltiGold
            )
        }
    }
}

@Composable
private fun SmashBrosVsClashCanvas(
    p1: FighterDefinition,
    p2: FighterDefinition,
    progress: Float
) {
    val zoom = 0.90f + progress * 0.20f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = zoom
                scaleY = zoom
            }
        ) {
            Text(
                text = "PRÊT ?",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                ),
                color = UltiGold
            )
            Text(
                text = "${p1.name.uppercase()}  VS  ${p2.name.uppercase()}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = ElectricCyan
            )
        }
    }
}
