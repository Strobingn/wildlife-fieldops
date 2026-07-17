package com.strobingn.wildlife.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Animated counter that counts up from 0 to [target] over [durationMillis].
 * Used for stat cards on the dashboard.
 */
@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    durationMillis: Int = 800
) {
    var displayed by remember { mutableIntStateOf(0) }

    LaunchedEffect(target) {
        displayed = 0
        val step = (target / (durationMillis / 16)).coerceAtLeast(1)
        val steps = (target / step)
        repeat(steps) {
            delay(16)
            displayed = (displayed + step).coerceAtMost(target)
        }
        displayed = target
    }

    Text(
        text = displayed.toString(),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * Fade + slide in animation for list items.
 * Each item appears with a staggered delay.
 */
@Composable
fun FadeSlideIn(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Scale-in animation for cards and buttons.
 */
@Composable
fun ScaleIn(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(tween(300)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Pulsing dot indicator for live/sync status.
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size(8.dp)
            .scale(scale)
            .alpha(alpha)
            .background(color, shape = androidx.compose.foundation.shape.CircleShape)
    )
}

/**
 * Empty state illustration with icon and message.
 * Shows a friendly message when there's no data.
 */
@Composable
fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Badge with count for notification indicators.
 */
@Composable
fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return
    Box(
        modifier = modifier
            .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
            .background(
                com.strobingn.wildlifefieldops.ui.theme.ErrorRed,
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .padding(horizontal = 4.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = count.coerceAtMost(99).toString(),
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
