package com.origaminotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origaminotes.app.data.NoteBlock
import com.origaminotes.app.data.NoteBlocks
import com.origaminotes.app.ui.theme.OrigamiAccents
import com.origaminotes.app.ui.theme.StitchGreen

/** Height of one hour on the grid. Everything else is derived from it. */
private val HourHeight = 64.dp
private val GutterWidth = 52.dp

/** Snapping tasks to quarter hours keeps the grid readable and the maths exact. */
private const val SNAP_MINUTES = 15

/**
 * Day grid for a task-list note — hours down the side, tasks placed at their time.
 *
 * Tapping an empty hour adds a task there; tapping a task edits it. No drag gestures: dragging a
 * block inside a vertically scrolling note is exactly the kind of undiscoverable interaction the
 * project rules out, and the time fields in the dialog do the same job.
 */
@Composable
fun TaskDayView(
    tasks: List<NoteBlock.Task>,
    onTasksChange: (List<NoteBlock.Task>) -> Unit,
    timeTable: Boolean,
    onTimeTableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<NoteBlock.Task?>(null) }
    var isNew by remember { mutableStateOf(false) }

    fun toggleDone(task: NoteBlock.Task) {
        onTasksChange(tasks.map { if (it.id == task.id) it.copy(done = !it.done) else it })
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Kept above the list rather than in a menu: it changes the whole shape of the note, so
        // it belongs where you can see the result change.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onTimeTableChange(!timeTable) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = timeTable,
                onCheckedChange = onTimeTableChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = StitchGreen,
                    checkmarkColor = OrigamiAccents.onAccent
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Add time table",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (timeTable) {
                Box(modifier = Modifier.fillMaxWidth().height(HourHeight * 24)) {

                    // ── Hour rows ───────────────────────────────────────────
                    Column(modifier = Modifier.fillMaxSize()) {
                        repeat(24) { hour ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(HourHeight)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        editing = NoteBlock.Task(startMinute = hour * 60)
                                        isNew = true
                                    }
                            ) {
                                Text(
                                    text = "%02d:00".format(hour),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .width(GutterWidth)
                                        .padding(top = 2.dp, end = 8.dp),
                                    textAlign = TextAlign.End
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Tasks laid over the grid ────────────────────────────
                    tasks.forEach { task ->
                        val top = HourHeight * (task.startMinute / 60f)
                        val height = HourHeight * (task.durationMinutes / 60f)
                        TaskPanel(
                            task = task,
                            modifier = Modifier
                                .offset(x = GutterWidth, y = top)
                                .padding(end = 8.dp)
                                // A very short task still needs a tappable body.
                                .height(height.coerceAtLeast(28.dp)),
                            onToggleDone = { toggleDone(task) },
                            onClick = { editing = task; isNew = false }
                        )
                    }
                }
            } else {
                // Plain checklist: same panels, separated by rules, no clock involved.
                tasks.forEach { task ->
                    TaskPanel(
                        task = task,
                        showTime = false,
                        modifier = Modifier.height(44.dp),
                        onToggleDone = { toggleDone(task) },
                        onClick = { editing = task; isNew = false }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
                // Without the hour grid there is nothing to tap to create a task.
                Text(
                    "+ Add task",
                    color = StitchGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = NoteBlock.Task(); isNew = true }
                        .padding(vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editing?.let { task ->
        TaskEditDialog(
            task = task,
            isNew = isNew,
            showTime = timeTable,
            onConfirm = { updated ->
                onTasksChange(
                    if (isNew) tasks + updated
                    else tasks.map { if (it.id == updated.id) updated else it }
                )
                editing = null
            },
            onDelete = {
                onTasksChange(tasks.filterNot { it.id == task.id })
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

/**
 * One task rendered as a panel. Shared by the day grid and the Dashboard preview so a task looks
 * the same wherever it appears.
 *
 * Both callbacks are optional: with neither, the panel is a read-only display (the checkbox stays
 * fully coloured rather than greyed, since it is showing state, not offering a disabled control).
 */
@Composable
fun TaskPanel(
    task: NoteBlock.Task,
    modifier: Modifier = Modifier,
    /** Off for task lists without a time table — the times exist but mean nothing there. */
    showTime: Boolean = true,
    onToggleDone: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (task.done) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            StitchGreen.copy(alpha = 0.20f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = onToggleDone?.let { toggle -> { toggle() } },
                colors = CheckboxDefaults.colors(
                    checkedColor = StitchGreen,
                    checkmarkColor = OrigamiAccents.onAccent
                ),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.text.ifBlank { "(Untitled task)" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showTime) {
                    Text(
                        text = "${NoteBlocks.formatTime(task.startMinute)}–${NoteBlocks.formatTime(task.endMinute)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskEditDialog(
    task: NoteBlock.Task,
    isNew: Boolean,
    showTime: Boolean,
    onConfirm: (NoteBlock.Task) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(task.id) { mutableStateOf(task.text) }
    var start by remember(task.id) { mutableStateOf(task.startMinute) }
    var duration by remember(task.id) { mutableStateOf(task.durationMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                if (isNew) "New task" else "Edit task",
                color = StitchGreen,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text("What needs doing?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = StitchGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                        cursorColor          = StitchGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                // Without a time table the steppers would be editing numbers nothing displays.
                if (showTime) {
                    Spacer(Modifier.height(16.dp))

                    StepperRow(
                        label = "Starts",
                        value = NoteBlocks.formatTime(start),
                        onDecrease = { start = (start - SNAP_MINUTES).coerceAtLeast(0) },
                        onIncrease = {
                            start = (start + SNAP_MINUTES).coerceAtMost(NoteBlock.DAY_MINUTES - SNAP_MINUTES)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    StepperRow(
                        label = "Lasts",
                        value = durationLabel(duration),
                        onDecrease = { duration = (duration - SNAP_MINUTES).coerceAtLeast(SNAP_MINUTES) },
                        onIncrease = {
                            duration = (duration + SNAP_MINUTES).coerceAtMost(NoteBlock.DAY_MINUTES - start)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        task.copy(
                            text = text.trim(),
                            startMinute = start,
                            // Never let a task run past midnight.
                            durationMinutes = duration.coerceAtMost(NoteBlock.DAY_MINUTES - start)
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchGreen,
                    contentColor = OrigamiAccents.onAccent
                )
            ) { Text(if (isNew) "Add" else "Save") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete, null,
                            tint = Color_Danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = Color_Danger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}

private val Color_Danger = androidx.compose.ui.graphics.Color(0xFFDC2626)

private fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton("−", onDecrease)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(76.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            StepperButton("+", onIncrease)
        }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(StitchGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = StitchGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
