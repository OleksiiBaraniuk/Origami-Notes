package com.origaminotes.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origaminotes.app.data.NoteType
import com.origaminotes.app.ui.theme.OrigamiAccents
import com.origaminotes.app.ui.theme.StitchGreen

/**
 * Note kinds offered when the add button is held.
 *
 * Five are planned; the two that exist are listed. [NoteTypeBubbles] lays out however many are
 * here, so adding Whiteboard/Drawing/List later needs no layout change.
 */
private data class BubbleOption(
    val type: NoteType,
    val icon: ImageVector,
    val label: String
)

private val Options = listOf(
    BubbleOption(NoteType.TEXT, Icons.Default.Description, "Note"),
    BubbleOption(NoteType.TASK_LIST, Icons.Default.Checklist, "Task list")
)

/**
 * Panel of type bubbles that rises above the add button on a long press.
 *
 * A plain tap still creates a text note, so the shortcut stays the default and this is an
 * addition rather than a new step.
 */
@Composable
fun NoteTypeBubbles(
    onPick: (NoteType) -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(160),
        label = "bubbles"
    )

    Surface(
        modifier = modifier.scale(progress).alpha(progress),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Options.forEach { option ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onPick(option.type) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(StitchGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            option.icon,
                            contentDescription = option.label,
                            tint = OrigamiAccents.onAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        option.label,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
