package com.entropy.stage.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.shell.InstalledApp

enum class StageSymbolType {
    APPS,
    BROWSER,
    SEARCH,
    SETTINGS,
    CONTROL,
    WIFI,
    WINDOW,
    FOLDER,
}

@Composable
fun StageGlyph(
    modifier: Modifier = Modifier,
    tint: Color = StagePalette.VioletBright,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.12f
        val left = size.width * 0.18f
        val right = size.width * 0.82f
        val top = size.height * 0.18f
        val middle = size.height * 0.5f
        val bottom = size.height * 0.82f
        val path = Path().apply {
            moveTo(left, middle)
            lineTo(size.width * 0.5f, top)
            lineTo(right, middle)
            lineTo(size.width * 0.5f, bottom)
            close()
        }
        drawPath(path, color = tint.copy(alpha = 0.28f))
        drawPath(path, color = tint, style = Stroke(width = stroke, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawLine(
            color = tint,
            start = Offset(left, middle),
            end = Offset(size.width * 0.5f, middle + size.height * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(right, middle),
            end = Offset(size.width * 0.5f, middle + size.height * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun StageSymbol(
    type: StageSymbolType,
    modifier: Modifier = Modifier,
    tint: Color = StagePalette.TextPrimary,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = (size.minDimension * 0.09f).coerceAtLeast(1.5f)
        when (type) {
            StageSymbolType.APPS -> {
                val cell = size.minDimension * 0.28f
                val gap = size.minDimension * 0.12f
                val origin = (size.minDimension - cell * 2 - gap) / 2f
                repeat(2) { row ->
                    repeat(2) { column ->
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(origin + column * (cell + gap), origin + row * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(cell * 0.28f),
                            style = Stroke(stroke),
                        )
                    }
                }
            }

            StageSymbolType.BROWSER -> {
                val radius = size.minDimension * 0.34f
                drawCircle(
                    color = tint,
                    radius = radius,
                    center = center,
                    style = Stroke(stroke),
                )
                drawOval(
                    color = tint.copy(alpha = 0.82f),
                    topLeft = Offset(center.x - radius * 0.46f, center.y - radius),
                    size = Size(radius * 0.92f, radius * 2f),
                    style = Stroke(stroke * 0.72f),
                )
                drawLine(
                    color = tint.copy(alpha = 0.82f),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y),
                    strokeWidth = stroke * 0.72f,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = StagePalette.VioletBright,
                    radius = stroke * 0.9f,
                    center = Offset(center.x + radius * 0.55f, center.y - radius * 0.48f),
                )
            }

            StageSymbolType.SEARCH -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.27f,
                    center = Offset(size.width * 0.44f, size.height * 0.42f),
                    style = Stroke(stroke),
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.63f, size.height * 0.62f),
                    end = Offset(size.width * 0.82f, size.height * 0.82f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            StageSymbolType.SETTINGS -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.18f,
                    center = center,
                    style = Stroke(stroke),
                )
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    val inner = size.minDimension * 0.3f
                    val outer = size.minDimension * 0.41f
                    drawLine(
                        color = tint,
                        start = Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * inner,
                            center.y + kotlin.math.sin(angle).toFloat() * inner,
                        ),
                        end = Offset(
                            center.x + kotlin.math.cos(angle).toFloat() * outer,
                            center.y + kotlin.math.sin(angle).toFloat() * outer,
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }

            StageSymbolType.CONTROL -> {
                listOf(0.28f to 0.38f, 0.5f to 0.68f, 0.72f to 0.48f).forEach { (y, knob) ->
                    drawLine(
                        tint,
                        Offset(size.width * 0.18f, size.height * y),
                        Offset(size.width * 0.82f, size.height * y),
                        stroke,
                        StrokeCap.Round,
                    )
                    drawCircle(tint, stroke * 1.55f, Offset(size.width * knob, size.height * y))
                }
            }

            StageSymbolType.WIFI -> {
                repeat(3) { index ->
                    val inset = size.minDimension * (0.13f + index * 0.12f)
                    drawArc(
                        color = tint,
                        startAngle = 220f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                drawCircle(tint, stroke * 0.8f, Offset(size.width / 2f, size.height * 0.75f))
            }

            StageSymbolType.WINDOW -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.14f, size.height * 0.18f),
                    size = Size(size.width * 0.72f, size.height * 0.64f),
                    cornerRadius = CornerRadius(size.minDimension * 0.1f),
                    style = Stroke(stroke),
                )
                drawLine(
                    tint,
                    Offset(size.width * 0.14f, size.height * 0.36f),
                    Offset(size.width * 0.86f, size.height * 0.36f),
                    stroke,
                )
            }

            StageSymbolType.FOLDER -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.32f)
                    lineTo(size.width * 0.38f, size.height * 0.32f)
                    lineTo(size.width * 0.48f, size.height * 0.22f)
                    lineTo(size.width * 0.84f, size.height * 0.22f)
                    quadraticTo(size.width * 0.9f, size.height * 0.22f, size.width * 0.9f, size.height * 0.3f)
                    lineTo(size.width * 0.9f, size.height * 0.74f)
                    quadraticTo(size.width * 0.9f, size.height * 0.82f, size.width * 0.82f, size.height * 0.82f)
                    lineTo(size.width * 0.16f, size.height * 0.82f)
                    quadraticTo(size.width * 0.1f, size.height * 0.82f, size.width * 0.1f, size.height * 0.74f)
                    lineTo(size.width * 0.1f, size.height * 0.4f)
                    quadraticTo(size.width * 0.1f, size.height * 0.32f, size.width * 0.12f, size.height * 0.32f)
                    close()
                }
                drawPath(path, tint.copy(alpha = 0.85f))
                drawLine(
                    tint.copy(alpha = 0.55f),
                    Offset(size.width * 0.12f, size.height * 0.43f),
                    Offset(size.width * 0.88f, size.height * 0.43f),
                    stroke * 0.7f,
                )
            }
        }
    }
}

@Composable
fun StageAppIcon(
    app: InstalledApp,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(app.id, app.icon) {
        app.icon?.runCatching { toBitmap(width = 128, height = 128).asImageBitmap() }?.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(24))
                .background(
                    Color(
                        red = 0.28f + (app.label.hashCode().ushr(8) and 0x1F) / 255f,
                        green = 0.22f,
                        blue = 0.44f + (app.label.hashCode() and 0x1F) / 255f,
                        alpha = 1f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
fun StageDockButton(
    label: String,
    selected: Boolean,
    magnification: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val targetScale = if (magnification && (pressed || hovered)) 1.12f else 1f
    val scale by animateFloatAsState(targetValue = targetScale, label = "dock-scale")

    Box(
        modifier = modifier
            .size(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = if (targetScale > 1f) -4.dp.toPx() else 0f
            }
            .semantics {
                contentDescription = label
            }
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = if (selected) 10.dp else 5.dp,
                    shape = RoundedCornerShape(13.dp),
                    ambientColor = StagePalette.Violet.copy(alpha = 0.28f),
                    spotColor = Color.Black.copy(alpha = 0.7f),
                )
                .clip(RoundedCornerShape(13.dp))
                .background(
                    if (selected) StagePalette.Elevated else StagePalette.Graphite,
                )
                .border(
                    1.dp,
                    if (selected) StagePalette.Violet.copy(alpha = 0.48f) else StagePalette.Hairline,
                    RoundedCornerShape(13.dp),
                )
                .padding(9.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(StagePalette.TextPrimary),
            )
        }
    }
}

@Composable
fun WindowTrafficLights(
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TrafficLight(StagePalette.Danger, "Close", onClose)
        Spacer(Modifier.width(8.dp))
        TrafficLight(StagePalette.Warning, "Minimize", onMinimize)
        Spacer(Modifier.width(8.dp))
        TrafficLight(StagePalette.Success, "Maximize", onMaximize)
    }
}

@Composable
private fun TrafficLight(
    color: Color,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(13.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
fun StageToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val knobPosition by animateFloatAsState(if (checked) 1f else 0f, label = "toggle")
    Box(
        modifier = modifier
            .width(42.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(if (checked) StagePalette.Violet else Color.White.copy(alpha = 0.12f))
            .border(1.dp, StagePalette.Hairline, CircleShape)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = 18.dp.toPx() * knobPosition }
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
