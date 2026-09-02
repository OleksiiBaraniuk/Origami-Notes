package com.origaminotes.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.origaminotes.app.data.Folder
import com.origaminotes.app.data.Note
import com.origaminotes.app.data.NoteBlocks
import com.origaminotes.app.data.NoteType
import java.time.LocalTime
import com.origaminotes.app.ui.theme.BottomBarInset
import com.origaminotes.app.ui.theme.OrigamiAccents
import com.origaminotes.app.ui.theme.StitchGreen
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────────
// NoteGrid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteGrid(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onContextMenu: (note: Note, anchor: Offset, bounds: Rect) -> Unit = { _, _, _ -> },
) {
    val pinned = remember(notes) { notes.filter { it.isPinned } }
    val others = remember(notes) { notes.filter { !it.isPinned } }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description, contentDescription = null,
                        modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        "No notes yet", fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            // ─── PINNED SECTION ───────────────────────────────────────────
            if (pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    SectionHeader(
                        label = "PINNED",
                        icon = {
                            Icon(Icons.Default.PushPin, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(
                    items = pinned,
                    key = { it.id },
                    span = { note -> if (note.isFullWidth) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane }
                ) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onLongPress = { anchor, bounds -> onContextMenu(note, anchor, bounds) }
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(8.dp)) }
            }

            // ─── OTHERS SECTION ───────────────────────────────────────────
            if (others.isNotEmpty()) {
                if (pinned.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SectionHeader(label = "OTHERS")
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(
                    items = others,
                    key = { it.id },
                    span = { note -> if (note.isFullWidth) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane }
                ) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onLongPress = { anchor, bounds -> onContextMenu(note, anchor, bounds) }
                    )
                }
            }

            // Keeps the last row reachable above the floating island bar.
            item(span = StaggeredGridItemSpan.FullLine) { Spacer(Modifier.height(BottomBarInset)) }
        }

    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Size Picker — appears below the note card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteResizePicker(
    isFullWidth: Boolean,
    cardBounds: Rect,
    onSelectSize: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val yPx = with(density) { (cardBounds.bottom + 14.dp.toPx()).roundToInt() }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(150)
    )

    Row(
        modifier = Modifier
            .offset { IntOffset(0, yPx) }
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .scale(scale),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        // Size 1 — Normal (two per row, default)
        SizeOptionPanel(
            icon       = Icons.Default.ViewModule,
            label      = "Normal",
            isSelected = !isFullWidth,
            onClick    = { onSelectSize(false) },
            modifier   = Modifier.weight(1f)
        )
        // Size 2 — Full (single note fills whole row)
        SizeOptionPanel(
            icon       = Icons.Default.ViewStream,
            label      = "Full",
            isSelected = isFullWidth,
            onClick    = { onSelectSize(true) },
            modifier   = Modifier.weight(1f)
        )
    }
}

@Composable
fun SizeOptionPanel(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor  = if (isSelected) StitchGreen else MaterialTheme.colorScheme.surface
    val fgColor  = if (isSelected) Color.Black  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val border   = if (isSelected) BorderStroke(0.dp, Color.Transparent)
                   else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))

    Card(
        modifier  = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = border,
        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = fgColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = fgColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spotlight overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpotlightOverlay(cardBounds: Rect, onDismiss: () -> Unit) {
    val density  = LocalDensity.current
    val cornerPx = with(density) { 12.dp.toPx() }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.72f else 0f,
        animationSpec = tween(200)
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        drawRect(Color.Black.copy(alpha = alpha))
        if (cardBounds != Rect.Zero) {
            drawRoundRect(
                color        = Color.Transparent,
                topLeft      = cardBounds.topLeft,
                size         = cardBounds.size,
                cornerRadius = CornerRadius(cornerPx),
                blendMode    = BlendMode.Clear
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Radial Menu
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteRadialMenu(
    anchor: Offset,
    isPinned: Boolean,
    isFullWidth: Boolean,
    onDelete: () -> Unit,
    onFolder: () -> Unit,
    onPin: () -> Unit,
    onResize: () -> Unit,
    onDismiss: () -> Unit
) {
    val density  = LocalDensity.current
    val radiusPx = with(density) { 90.dp.toPx() }
    val halfBtn  = with(density) { 28.dp.toPx() }

    fun radialOffset(angleDeg: Double): IntOffset {
        val rad = Math.toRadians(angleDeg)
        return IntOffset(
            x = (anchor.x + radiusPx * cos(rad) - halfBtn).roundToInt(),
            y = (anchor.y + radiusPx * sin(rad) - halfBtn).roundToInt()
        )
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(160)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RadialButton(Icons.Default.Delete, "Delete", Color(0xFFDC2626), Color.White, scale, radialOffset(225.0), onDelete)
        RadialButton(
            icon     = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
            label    = if (isPinned) "Unpin" else "Pin",
            bgColor  = Color(0xFFF59E0B), tint = Color.White,
            scale    = scale, offsetPx = radialOffset(315.0), onClick = onPin
        )
        RadialButton(Icons.Default.Folder, "Folder", OrigamiAccents.radialContainer, OrigamiAccents.onRadialContainer, scale, radialOffset(135.0), onFolder)
        RadialButton(Icons.Default.AspectRatio, "Resize", OrigamiAccents.radialContainer, Color.White, scale, radialOffset(45.0), onResize)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Radial button
// ─────────────────────────────────────────────────────────────────────────────

/** [label] is no longer drawn — it stays as the icon's contentDescription for accessibility. */
@Composable
fun RadialButton(
    icon: ImageVector, label: String,
    bgColor: Color, tint: Color,
    scale: Float, offsetPx: IntOffset,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .offset { offsetPx }
            .scale(scale)
            .size(60.dp)
            .background(bgColor, CircleShape)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Assign Folder dialog — type to filter, tap a suggestion to move the note
// ─────────────────────────────────────────────────────────────────────────────

/** Builds "Parent / Child / Leaf" for a folder. Bounded so a malformed parent chain can't hang. */
private fun folderPath(folder: Folder, all: List<Folder>): String {
    val segments = ArrayDeque(listOf(folder.name))
    var parentId = folder.parentId
    var hops = 0
    while (parentId != null && hops++ <= all.size) {
        val parent = all.firstOrNull { it.id == parentId } ?: break
        segments.addFirst(parent.name)
        parentId = parent.parentId
    }
    return segments.joinToString(" / ")
}

@Composable
fun AssignFolderDialog(
    note: Note,
    folders: List<Folder>,
    onAssign: (Folder?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val paths = remember(folders) { folders.associate { it.id to folderPath(it, folders) } }
    // Matching the whole path, not just the leaf name, lets "Food" surface "Food / Meat".
    val matches = remember(folders, paths, query) {
        val q = query.trim().lowercase()
        folders
            .filter { q.isEmpty() || paths[it.id].orEmpty().lowercase().contains(q) }
            .sortedBy { paths[it.id] }
    }
    val homeMatches = query.isBlank() || "home".contains(query.trim().lowercase())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Move to Folder", color = StitchGreen, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Folder name…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = StitchGreen) },
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
                Spacer(Modifier.height(8.dp))

                if (!homeMatches && matches.isEmpty()) {
                    Text(
                        "No folder matches \"${query.trim()}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (homeMatches) {
                            SuggestionRow(
                                icon = Icons.Default.Folder,
                                label = "Home",
                                subtitle = "No folder",
                                isCurrent = note.folderId == null,
                                onClick = { onAssign(null) }
                            )
                        }
                        matches.forEach { folder ->
                            val path = paths[folder.id].orEmpty()
                            SuggestionRow(
                                icon = Icons.Default.Folder,
                                label = folder.name,
                                subtitle = path.takeIf { it != folder.name },
                                isCurrent = note.folderId == folder.id,
                                onClick = { onAssign(folder) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

/** Shared row for the folder and note pickers. */
@Composable
private fun SuggestionRow(
    icon: ImageVector,
    label: String,
    subtitle: String?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, null,
            tint = StitchGreen.copy(alpha = if (isCurrent) 1f else 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isCurrent) {
            Icon(Icons.Default.Check, "Current folder", tint = StitchGreen, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Task list preview — the task that is coming up, drawn as it is inside the note
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TaskListPreview(note: Note) {
    val tasks = remember(note.blocks) { NoteBlocks.tasksOf(note.blocks) }
    // Recomputed per composition rather than kept in state: the card only needs to be right
    // when it is drawn, and a ticking clock across the whole grid would not earn its cost.
    val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
    val next = remember(tasks, nowMinute) { NoteBlocks.nextTask(tasks, nowMinute) }

    if (next == null) {
        Text(
            "No tasks yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    } else {
        // Read-only: no callbacks, so the panel neither ticks off nor navigates from here.
        TaskPanel(
            task = next,
            modifier = Modifier.height(46.dp),
            showTime = NoteBlocks.timeTableOf(note.blocks)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Select Note dialog — same shape as AssignFolderDialog, searching notes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Type-to-filter picker for linking to another note. Deliberately mirrors [AssignFolderDialog]
 * so the two feel like one interaction.
 *
 * [excludeNoteId] drops the note being edited — a note linking to itself is never useful.
 */
@Composable
fun SelectNoteDialog(
    notes: List<Note>,
    excludeNoteId: Long?,
    onSelect: (Note) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matches = remember(notes, query, excludeNoteId) {
        val q = query.trim().lowercase()
        notes
            .filter { it.id != excludeNoteId }
            .filter {
                q.isEmpty() ||
                    it.title.lowercase().contains(q) ||
                    it.content.lowercase().contains(q)
            }
            .sortedByDescending { it.modifiedAt }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Link to Note", color = StitchGreen, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Note title…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = StitchGreen) },
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
                Spacer(Modifier.height(8.dp))

                if (matches.isEmpty()) {
                    Text(
                        if (query.isBlank()) "No other notes yet"
                        else "No note matches \"${query.trim()}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        matches.forEach { note ->
                            SuggestionRow(
                                icon = Icons.Default.Description,
                                label = note.title.ifBlank { "(Untitled)" },
                                subtitle = note.content
                                    .replace('\n', ' ')
                                    .trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.take(60),
                                isCurrent = false,
                                onClick = { onSelect(note) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Select Folder dialog — same shape again, for widget targeting
// ─────────────────────────────────────────────────────────────────────────────

/** Type-to-filter folder picker. Unlike [AssignFolderDialog] it has no note and no "Home" row. */
@Composable
fun SelectFolderDialog(
    folders: List<Folder>,
    title: String,
    onSelect: (Folder) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val paths = remember(folders) { folders.associate { it.id to folderPath(it, folders) } }
    val matches = remember(folders, paths, query) {
        val q = query.trim().lowercase()
        folders
            .filter { q.isEmpty() || paths[it.id].orEmpty().lowercase().contains(q) }
            .sortedBy { paths[it.id] }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = StitchGreen, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Folder name…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    leadingIcon = { Icon(Icons.Default.Folder, null, tint = StitchGreen) },
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
                Spacer(Modifier.height(8.dp))

                if (matches.isEmpty()) {
                    Text(
                        if (folders.isEmpty()) "No folders yet"
                        else "No folder matches \"${query.trim()}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        matches.forEach { folder ->
                            val path = paths[folder.id].orEmpty()
                            SuggestionRow(
                                icon = Icons.Default.Folder,
                                label = folder.name,
                                subtitle = path.takeIf { it != folder.name },
                                isCurrent = false,
                                onClick = { onSelect(folder) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Delete Note confirmation dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeleteNoteDialog(note: Note, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val displayTitle = note.title.ifBlank { "Untitled" }.let {
        if (it.length > 40) it.take(40) + "…" else it
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Text("Delete Note", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text("Are you sure you want to delete \"$displayTitle\"?", color = MaterialTheme.colorScheme.onSurface)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(label: String?, icon: (@Composable () -> Unit)? = null) {
    if (label == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        icon?.invoke()
        if (icon != null) Spacer(modifier = Modifier.width(6.dp))
        Text(
            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.8.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Note Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongPress: (touchInRoot: Offset, cardBoundsInRoot: Rect) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surface
    var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                cardBoundsInRoot = Rect(offset = pos, size = Size(coords.size.width.toFloat(), coords.size.height.toFloat()))
            }
            .pointerInput(note.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { localOffset ->
                        onLongPress(cardBoundsInRoot.topLeft + localOffset, cardBoundsInRoot)
                    }
                )
            },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = if (note.isPinned) BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (note.title.isNotEmpty()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (note.isPinned) {
                    Icon(Icons.Default.PushPin, "Pinned", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                }
            }
            if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
            if (NoteType.fromName(note.type) == NoteType.TASK_LIST) {
                TaskListPreview(note)
            } else if (note.content.isNotEmpty()) {
                // Rendered, not stripped: the card should look like the note, so bold, colours
                // and mint links survive into the preview.
                val bodyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                val previewState = remember(note.blocks, note.content) {
                    RichTextState().apply {
                        setHtml(NoteBlocks.previewHtml(note.blocks, note.content))
                        config.linkColor = StitchGreen
                        config.linkTextDecoration = TextDecoration.None
                    }
                }
                RichText(
                    state = previewState,
                    style = MaterialTheme.typography.bodyMedium.copy(color = bodyColor),
                    maxLines = 15,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
