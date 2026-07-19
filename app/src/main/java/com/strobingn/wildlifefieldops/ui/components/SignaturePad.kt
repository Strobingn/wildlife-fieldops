package com.strobingn.wildlifefieldops.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.ui.theme.BackgroundCard
import com.strobingn.wildlifefieldops.ui.theme.TextPrimary

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onSignatureChanged: (List<Offset>) -> Unit = {},
    strokeColor: Color = TextPrimary,
    strokeWidth: Float = 4f,
    backgroundColor: Color = BackgroundCard
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    Card(
        modifier = modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (currentPath.isNotEmpty()) {
                                paths.add(currentPath.toList())
                                currentPath.clear()
                            }
                            currentPath.add(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath.add(change.position)
                            onSignatureChanged(paths.flatten() + currentPath)
                        }
                    )
                }
        ) {
            (paths + listOf(currentPath)).forEach { path ->
                if (path.size > 1) {
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            path.forEachIndexed { index, offset ->
                                if (index == 0) moveTo(offset.x, offset.y)
                                else lineTo(offset.x, offset.y)
                            }
                        },
                        color = strokeColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun SignaturePadWithControls(
    modifier: Modifier = Modifier,
    onSignatureCaptured: (List<Offset>) -> Unit = {},
    onClear: () -> Unit = {}
) {
    val paths = remember { mutableStateListOf<List<Offset>>() }
    val currentPath = remember { mutableStateListOf<Offset>() }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundCard)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (currentPath.isNotEmpty()) {
                                    paths.add(currentPath.toList())
                                    currentPath.clear()
                                }
                                currentPath.add(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath.add(change.position)
                            }
                        )
                    }
            ) {
                (paths + listOf(currentPath)).forEach { path ->
                    if (path.size > 1) {
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                path.forEachIndexed { index, offset ->
                                    if (index == 0) moveTo(offset.x, offset.y)
                                    else lineTo(offset.x, offset.y)
                                }
                            },
                            color = TextPrimary,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPath.clear()
                    onClear()
                }
            ) {
                Text("Clear")
            }
            Button(
                onClick = { onSignatureCaptured(paths.flatten() + currentPath) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Capture Signature")
            }
        }
    }
}
