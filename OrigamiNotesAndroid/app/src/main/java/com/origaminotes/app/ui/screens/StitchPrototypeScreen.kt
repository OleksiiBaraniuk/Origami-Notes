package com.origaminotes.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origaminotes.app.data.Folder
import com.origaminotes.app.data.Note
import com.origaminotes.app.data.NoteSortMode
import com.origaminotes.app.data.NoteType
import com.origaminotes.app.ui.components.NoteTypeBubbles
import com.origaminotes.app.ui.OrigamiNotesViewModel
import com.origaminotes.app.ui.components.AssignFolderDialog
import com.origaminotes.app.ui.components.DeleteFolderDialog
import com.origaminotes.app.ui.components.DeleteNoteDialog
import com.origaminotes.app.ui.components.FolderRadialMenu
import com.origaminotes.app.ui.components.FolderTreeComponent
import com.origaminotes.app.ui.components.NoteGrid
import com.origaminotes.app.ui.components.NoteRadialMenu
import com.origaminotes.app.ui.components.NoteResizePicker
import com.origaminotes.app.ui.components.RenameFolderDialog
import com.origaminotes.app.ui.components.RenameNoteDialog
import com.origaminotes.app.ui.components.SpotlightOverlay
import com.origaminotes.app.ui.theme.OrigamiAccents
import com.origaminotes.app.ui.theme.StitchGreen

// Context menu target — determines which overlay/menu renders at screen level
private sealed class ContextTarget {
    abstract val anchor: Offset
    abstract val bounds: Rect

    data class DashNote(val note: Note, override val anchor: Offset, override val bounds: Rect) : ContextTarget()
    data class TreeFolder(val folder: Folder, override val anchor: Offset, override val bounds: Rect) : ContextTarget()
    data class TreeNote(val note: Note, override val anchor: Offset, override val bounds: Rect) : ContextTarget()
}

@Composable
fun StitchPrototypeScreen(
    viewModel: OrigamiNotesViewModel,
    onNoteClick: (Long) -> Unit,
    onAddNoteClick: (NoteType) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var showTypeBubbles by remember { mutableStateOf(false) }

    var isAssignMode by remember { mutableStateOf(false) }
    var workingFolders by remember { mutableStateOf<List<Folder>>(emptyList()) }
    val workingNoteAssignments = remember { mutableStateMapOf<Long, Long?>() }
    val workingNoteSortOrders = remember { mutableStateMapOf<Long, Long>() }
    var noteDraggingOverBar by remember { mutableStateOf(false) }
    val allFolders by viewModel.allFolders.collectAsState()

    // ── Full-screen context menu state ──────────────────────────────────────────
    var contextTarget by remember { mutableStateOf<ContextTarget?>(null) }
    var showResizePicker by remember { mutableStateOf(false) }
    var pendingDeleteNote by remember { mutableStateOf<Note?>(null) }
    var pendingDeleteFolder by remember { mutableStateOf<Folder?>(null) }
    var pendingRenameFolder by remember { mutableStateOf<Folder?>(null) }
    var pendingRenameNote by remember { mutableStateOf<Note?>(null) }
    var pendingAssignFolderNote by remember { mutableStateOf<Note?>(null) }

    if (showAddBranchDialog) {
        AddBranchDialog(
            onConfirm = { name ->
                viewModel.createFolder(name, parentId = null)
                showAddBranchDialog = false
            },
            onDismiss = { showAddBranchDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                val uiState by viewModel.uiState.collectAsState()
                val sortMode by viewModel.sortMode.collectAsState()
                StitchTopBar(
                    folders = allFolders,
                    selectedFolder = uiState.selectedFolder,
                    onFolderSelect = { viewModel.selectFolder(it) },
                    searchQuery = uiState.searchQuery,
                    isSearchActive = uiState.isSearchActive,
                    onSearchToggle = { viewModel.toggleSearch() },
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    selectedTab = selectedTab,
                    sortMode = sortMode,
                    onSortModeChange = { viewModel.setSortMode(it) },
                    onSettingsClick = onSettingsClick
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedTab) {
                    0 -> StitchDashboardScreen(
                        viewModel = viewModel,
                        onNoteClick = onNoteClick,
                        onContextMenu = { note, anchor, bounds ->
                            contextTarget = ContextTarget.DashNote(note, anchor, bounds)
                            showResizePicker = false
                        }
                    )
                    1 -> StitchTreeScreen(
                        folders = if (isAssignMode) workingFolders else allFolders,
                        viewModel = viewModel,
                        isAssignMode = isAssignMode,
                        onEnterAssignMode = {
                            workingFolders = allFolders.toList()
                            workingNoteAssignments.clear()
                            workingNoteSortOrders.clear()
                            isAssignMode = true
                        },
                        onWorkingFoldersChange = { workingFolders = it },
                        onFolderClick = { folder ->
                            if (folder != null) viewModel.selectFolder(folder)
                            else viewModel.selectUncategorized()
                            selectedTab = 0
                        },
                        onNoteClick = onNoteClick,
                        noteAssignments = workingNoteAssignments,
                        onNoteAssign = { noteId, folderId -> workingNoteAssignments[noteId] = folderId },
                        onDraggingOverBar = { noteDraggingOverBar = it },
                        onNoteSortOrdersChange = { workingNoteSortOrders.putAll(it) },
                        onContextFolder = { folder, anchor, bounds ->
                            contextTarget = ContextTarget.TreeFolder(folder, anchor, bounds)
                        },
                        onContextNote = { note, anchor, bounds ->
                            contextTarget = ContextTarget.TreeNote(note, anchor, bounds)
                        }
                    )
                }
            }
        }

        // Dismiss scrim, declared before the bar so the bubbles stay above it and clickable.
        if (showTypeBubbles) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showTypeBubbles = false }
            )
        }

        // Floating island bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isAssignMode) {
                AssignModeBottomBar(
                    onAccept = {
                        val filteredNoteSortOrders = workingNoteSortOrders
                            .filter { (noteId, _) -> !workingNoteAssignments.containsKey(noteId) }
                        viewModel.saveAssignModeResult(
                            workingFolders,
                            filteredNoteSortOrders,
                            workingNoteAssignments.toMap()
                        )
                        workingNoteAssignments.clear()
                        workingNoteSortOrders.clear()
                        noteDraggingOverBar = false
                        isAssignMode = false
                    },
                    onCancel = {
                        workingNoteAssignments.clear()
                        workingNoteSortOrders.clear()
                        noteDraggingOverBar = false
                        isAssignMode = false
                    }
                )
            } else {
                IslandBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onAddClick = if (selectedTab == 1) {
                        { showAddBranchDialog = true }
                    } else {
                        { onAddNoteClick(NoteType.TEXT) }
                    },
                    onAddLongPress = { if (selectedTab == 0) showTypeBubbles = true }
                )
            }
        }


        // Rendered at screen level, like the radial menus: a child placed outside its parent's
        // bounds is not reliably hit-tested, and inside the island bar these would sit above it.
        if (showTypeBubbles) {
            NoteTypeBubbles(
                onPick = { type ->
                    showTypeBubbles = false
                    onAddNoteClick(type)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Clears the bar: 24dp margin + 90dp bar + 14dp gap.
                    .padding(bottom = 128.dp)
            )
        }

        // ── Full-screen spotlight + radial menu ─────────────────────────────────
        // Rendered here so the dim covers the bottom bar and top bar too.
        contextTarget?.let { target ->
            val dismiss = { contextTarget = null; showResizePicker = false }
            SpotlightOverlay(cardBounds = target.bounds, onDismiss = dismiss)

            when (target) {
                is ContextTarget.DashNote -> {
                    if (!showResizePicker) {
                        NoteRadialMenu(
                            anchor      = target.anchor,
                            isPinned    = target.note.isPinned,
                            isFullWidth = target.note.isFullWidth,
                            onDelete    = { pendingDeleteNote = target.note; dismiss() },
                            onFolder    = { pendingAssignFolderNote = target.note; dismiss() },
                            onPin       = { viewModel.togglePin(target.note.id); dismiss() },
                            onResize    = { showResizePicker = true },
                            onDismiss   = dismiss
                        )
                    } else {
                        NoteResizePicker(
                            isFullWidth  = target.note.isFullWidth,
                            cardBounds   = target.bounds,
                            onSelectSize = { isFullWidth ->
                                viewModel.setNoteFullWidth(target.note.id, isFullWidth)
                                dismiss()
                            },
                            onDismiss = dismiss
                        )
                    }
                }
                is ContextTarget.TreeFolder -> {
                    FolderRadialMenu(
                        anchor   = target.anchor,
                        onRename = { pendingRenameFolder = target.folder; dismiss() },
                        onDelete = { pendingDeleteFolder = target.folder; dismiss() },
                        onDismiss = dismiss
                    )
                }
                is ContextTarget.TreeNote -> {
                    FolderRadialMenu(
                        anchor   = target.anchor,
                        onRename = { pendingRenameNote = target.note; dismiss() },
                        onDelete = { pendingDeleteNote = target.note; dismiss() },
                        onDismiss = dismiss
                    )
                }
            }
        }

        // ── Confirmation dialogs ────────────────────────────────────────────────
        pendingDeleteNote?.let { note ->
            DeleteNoteDialog(
                note      = note,
                onConfirm = { viewModel.deleteNote(note); pendingDeleteNote = null },
                onDismiss = { pendingDeleteNote = null }
            )
        }
        pendingDeleteFolder?.let { folder ->
            DeleteFolderDialog(
                folder    = folder,
                onConfirm = { viewModel.deleteFolder(folder); pendingDeleteFolder = null },
                onDismiss = { pendingDeleteFolder = null }
            )
        }
        pendingRenameFolder?.let { folder ->
            RenameFolderDialog(
                folder    = folder,
                onConfirm = { name -> viewModel.renameFolder(folder, name); pendingRenameFolder = null },
                onDismiss = { pendingRenameFolder = null }
            )
        }
        pendingRenameNote?.let { note ->
            RenameNoteDialog(
                note      = note,
                onConfirm = { name -> viewModel.renameNote(note, name); pendingRenameNote = null },
                onDismiss = { pendingRenameNote = null }
            )
        }
        pendingAssignFolderNote?.let { note ->
            AssignFolderDialog(
                note      = note,
                folders   = allFolders,
                onAssign  = { folder ->
                    viewModel.setNoteFolder(note.id, folder?.id)
                    pendingAssignFolderNote = null
                },
                onDismiss = { pendingAssignFolderNote = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StitchTopBar(
    folders: List<Folder>,
    selectedFolder: Folder?,
    onFolderSelect: (Folder?) -> Unit,
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    selectedTab: Int,
    sortMode: NoteSortMode,
    onSortModeChange: (NoteSortMode) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val currentLevelFolders = remember(folders, selectedFolder) {
        folders.filter { it.parentId == selectedFolder?.parentId }.sortedBy { it.sortOrder }
    }
    val childFolders = remember(folders, selectedFolder) {
        if (selectedFolder != null) folders.filter { it.parentId == selectedFolder.id }.sortedBy { it.sortOrder }
        else emptyList()
    }
    val parentFolder = remember(folders, selectedFolder) {
        folders.firstOrNull { it.id == selectedFolder?.parentId }
    }
    val showAllChip = selectedFolder?.parentId == null

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        if (isSearchActive) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onSearchToggle(); onSearchQueryChanged("") },
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                }
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search notes...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = StitchGreen) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Search, null, tint = StitchGreen, modifier = Modifier.size(20.dp))
                }
                if (selectedTab == 0 && selectedFolder != null) {
                    IconButton(
                        onClick = { onFolderSelect(parentFolder) },
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = StitchGreen, modifier = Modifier.size(20.dp))
                    }
                }

                if (selectedTab == 0) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        if (showAllChip) {
                            item {
                                FolderChip(
                                    label = "Home",
                                    selected = selectedFolder == null,
                                    onClick = { onFolderSelect(null) }
                                )
                            }
                        }
                        items(currentLevelFolders, key = { it.id }) { folder ->
                            FolderChip(
                                label = folder.name,
                                selected = selectedFolder?.id == folder.id,
                                onClick = { onFolderSelect(folder) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Settings, null, tint = StitchGreen, modifier = Modifier.size(20.dp))
                }
            }

            // Child chips and the sort control share one row: on their own each would cost a
            // full line, and the pair of them was eating too much of the screen.
            if (selectedTab == 0) {
                val hasChildren = childFolders.isNotEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasChildren) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(childFolders, key = { it.id }) { child ->
                                FolderChip(label = child.name, selected = false, onClick = { onFolderSelect(child) })
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    SortSelector(
                        sortMode = sortMode,
                        onSortModeChange = onSortModeChange,
                        compact = hasChildren
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor   = if (selected) StitchGreen else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun AssignModeBottomBar(
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.height(90.dp).width(350.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchGreen, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Done, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IslandBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit,
    onAddLongPress: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.height(90.dp).width(350.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { onTabSelected(0) }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Main",
                    tint = if (selectedTab == 0) StitchGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Deliberately a Surface, not a FloatingActionButton: the FAB owns an internal
            // clickable that consumes the gesture, and Material3 offers no onLongClick on it,
            // so a long press could never reach an outer modifier. Same look, own gestures.
            Surface(
                shape = CircleShape,
                color = StitchGreen,
                contentColor = OrigamiAccents.onAccent,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .size(70.dp)
                    .combinedClickable(
                        onClick = onAddClick,
                        onLongClick = onAddLongPress,
                        role = Role.Button
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (selectedTab == 1) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder", modifier = Modifier.size(35.dp))
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(35.dp))
                    }
                }
            }

            IconButton(onClick = { onTabSelected(1) }) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "Tree",
                    tint = if (selectedTab == 1) StitchGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun StitchDashboardScreen(
    viewModel: OrigamiNotesViewModel,
    onNoteClick: (Long) -> Unit,
    onContextMenu: (Note, Offset, Rect) -> Unit = { _, _, _ -> }
) {
    val notes by viewModel.notes.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        NoteGrid(
            notes = notes,
            onNoteClick = onNoteClick,
            onContextMenu = onContextMenu
        )
    }
}

/**
 * Sort control living under the Settings button.
 *
 * [compact] collapses it to a filter icon — used when child-folder chips share the row, so the
 * two together cost one line instead of two.
 */
@Composable
private fun SortSelector(
    sortMode: NoteSortMode,
    onSortModeChange: (NoteSortMode) -> Unit,
    compact: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(180),
        label = "sortChevron"
    )

    // Wraps the control tightly so the menu drops directly beneath it rather than
    // beneath a full-width parent.
    Box {
        if (compact) {
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Sorted by ${sortMode.label}",
                    tint = StitchGreen,
                    modifier = Modifier.size(19.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Sorted by ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Text(
                    text = sortMode.label,
                    color = StitchGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Change sort order",
                    tint = StitchGreen,
                    modifier = Modifier.size(18.dp).rotate(chevronAngle)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NoteSortMode.entries.forEach { mode ->
                val isCurrent = mode == sortMode
                DropdownMenuItem(
                    text = {
                        Text(
                            mode.label,
                            color = if (isCurrent) StitchGreen else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingIcon = if (isCurrent) {
                        { Icon(Icons.Default.Check, null, tint = StitchGreen, modifier = Modifier.size(18.dp)) }
                    } else null,
                    onClick = { onSortModeChange(mode); expanded = false }
                )
            }
        }
    }
}

@Composable
fun StitchTreeScreen(
    folders: List<Folder>,
    viewModel: OrigamiNotesViewModel,
    isAssignMode: Boolean,
    onEnterAssignMode: () -> Unit,
    onWorkingFoldersChange: (List<Folder>) -> Unit,
    onFolderClick: (Folder?) -> Unit,
    onNoteClick: (Long) -> Unit,
    noteAssignments: Map<Long, Long?> = emptyMap(),
    onNoteAssign: (Long, Long?) -> Unit = { _, _ -> },
    onDraggingOverBar: (Boolean) -> Unit = {},
    onNoteSortOrdersChange: (Map<Long, Long>) -> Unit = {},
    onContextFolder: (Folder, Offset, Rect) -> Unit = { _, _, _ -> },
    onContextNote: (Note, Offset, Rect) -> Unit = { _, _, _ -> }
) {
    val allNotes by viewModel.allNotes.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        FolderTreeComponent(
            folders = folders,
            notes = allNotes,
            isAssignMode = isAssignMode,
            onEnterAssignMode = onEnterAssignMode,
            onFolderDelete = { viewModel.deleteFolder(it) },
            onFolderRename = { folder, name -> viewModel.renameFolder(folder, name) },
            onFoldersChange = onWorkingFoldersChange,
            onFolderClick = onFolderClick,
            onNoteClick = onNoteClick,
            onNoteFolderChange = { noteId, folderId -> viewModel.setNoteFolder(noteId, folderId) },
            onNoteDelete = { viewModel.deleteNote(it) },
            onNoteRename = { note, title -> viewModel.renameNote(note, title) },
            noteAssignments = noteAssignments,
            onNoteAssign = onNoteAssign,
            onDraggingOverBar = onDraggingOverBar,
            onNoteSortOrdersChange = onNoteSortOrdersChange,
            onContextFolder = onContextFolder,
            onContextNote = onContextNote
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add Branch Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddBranchDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("New Folder", color = StitchGreen, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Folder name…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
        },
        confirmButton = {
            val isEmpty = name.isBlank()
            Button(
                onClick = { if (!isEmpty) onConfirm(name.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEmpty) MaterialTheme.colorScheme.surfaceVariant else StitchGreen,
                    contentColor   = if (isEmpty) StitchGreen else Color.Black
                ),
                border = if (isEmpty) BorderStroke(2.dp, StitchGreen) else null
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
