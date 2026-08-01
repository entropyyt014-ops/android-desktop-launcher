package com.entropy.stage.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropy.stage.design.StageGlyph
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.device.DeviceProfile

@Composable
fun StageOnboarding(
    profile: DeviceProfile,
    homeRoleHeld: Boolean,
    homeRoleAvailable: Boolean,
    reduceMotion: Boolean,
    onRequestHomeRole: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val transitionMillis = if (reduceMotion) 0 else 220

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08090E), Color(0xFF10101A), Color(0xFF07080C)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxSize(0.72f)
                .background(
                    Brush.radialGradient(
                        listOf(StagePalette.Violet.copy(alpha = 0.2f), Color.Transparent),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .padding(18.dp)
                .widthIn(max = 520.dp)
                .shadow(
                    36.dp,
                    RoundedCornerShape(24.dp),
                    ambientColor = Color.Black.copy(alpha = 0.65f),
                    spotColor = Color.Black.copy(alpha = 0.9f),
                )
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(24.dp)),
            color = StagePalette.Window,
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color.White.copy(alpha = 0.025f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    repeat(3) { index ->
                        Box(
                            Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(
                                    when (index) {
                                        0 -> StagePalette.Danger.copy(alpha = 0.75f)
                                        1 -> StagePalette.Warning.copy(alpha = 0.75f)
                                        else -> StagePalette.Success.copy(alpha = 0.75f)
                                    },
                                ),
                        )
                    }
                    Text(
                        text = "Stage Setup Assistant",
                        modifier = Modifier.weight(1f),
                        color = StagePalette.TextSecondary,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.size(47.dp))
                }

                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(tween(transitionMillis)) togetherWith fadeOut(tween(transitionMillis))
                    },
                    label = "setup-step",
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep(
                            profile = profile,
                            onContinue = { step = 1 },
                        )

                        else -> HomeRoleStep(
                            homeRoleHeld = homeRoleHeld,
                            homeRoleAvailable = homeRoleAvailable,
                            onRequestHomeRole = onRequestHomeRole,
                            onFinish = onFinish,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    profile: DeviceProfile,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .shadow(24.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF292334), Color(0xFF15131C)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                .padding(19.dp),
            contentAlignment = Alignment.Center,
        ) {
            StageGlyph(Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Welcome to Stage",
            color = StagePalette.TextPrimary,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "A calm desktop workspace built for your phone—without root, risky permissions, or fake system controls.",
            color = StagePalette.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(22.dp))
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = Color.White.copy(alpha = 0.04f),
            border = androidx.compose.foundation.BorderStroke(1.dp, StagePalette.Hairline),
        ) {
            Text(
                text = "Optimized for ${profile.manufacturer} ${profile.model}  •  ${profile.performanceTier.name.lowercase().replaceFirstChar(Char::uppercase)} renderer",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = StagePalette.TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        PrimaryAction(text = "Continue", onClick = onContinue)
        Spacer(Modifier.height(12.dp))
        SetupProgress(activeStep = 0)
    }
}

@Composable
private fun HomeRoleStep(
    homeRoleHeld: Boolean,
    homeRoleAvailable: Boolean,
    onRequestHomeRole: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (homeRoleHeld) {
                        StagePalette.Success.copy(alpha = 0.14f)
                    } else {
                        StagePalette.Violet.copy(alpha = 0.15f)
                    },
                )
                .border(
                    1.dp,
                    if (homeRoleHeld) StagePalette.Success.copy(alpha = 0.45f)
                    else StagePalette.Violet.copy(alpha = 0.45f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (homeRoleHeld) "✓" else "⌂",
                color = if (homeRoleHeld) StagePalette.Success else StagePalette.VioletBright,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = if (homeRoleHeld) "Stage is your Home" else "Use Stage as Home",
            color = StagePalette.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = if (homeRoleHeld) {
                "Pressing Home will now return to your Stage desktop. You can change this anytime in Android Settings."
            } else {
                "Android must ask before Stage can replace your current launcher. Other apps still open normally outside Stage windows."
            },
            color = StagePalette.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(28.dp))
        if (!homeRoleHeld) {
            PrimaryAction(
                text = if (homeRoleAvailable) "Choose Stage as Home" else "Open Home settings",
                onClick = onRequestHomeRole,
            )
            Spacer(Modifier.height(11.dp))
            SecondaryAction(text = "Not now", onClick = onFinish)
        } else {
            PrimaryAction(text = "Enter Stage", onClick = onFinish)
        }
        Spacer(Modifier.height(14.dp))
        SetupProgress(activeStep = 1)
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(StagePalette.VioletBright, StagePalette.Violet),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = StagePalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SetupProgress(activeStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(2) { index ->
            Box(
                Modifier
                    .size(if (index == activeStep) 18.dp else 6.dp, 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == activeStep) StagePalette.VioletBright
                        else Color.White.copy(alpha = 0.18f),
                    ),
            )
        }
    }
}
