package com.origaminotes.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.origaminotes.app.data.Note
import com.origaminotes.app.data.NoteBlock
import com.origaminotes.app.data.NoteBlocks
import com.origaminotes.app.data.NoteType
import com.origaminotes.app.ui.OrigamiNotesViewModel
import com.origaminotes.app.ui.components.TaskDayView
import com.origaminotes.app.ui.components.EditorFormatState
import com.origaminotes.app.ui.components.EditorTextSize
import com.origaminotes.app.ui.components.EditorToolbar
import com.origaminotes.app.ui.components.ConfirmOpenLinkDialog
import com.origaminotes.app.ui.components.SelectNoteDialog
import com.origaminotes.app.ui.components.UrlDetector
import com.origaminotes.app.ui.components.WebLinkDialog
import com.origaminotes.app.ui.theme.StitchGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Long?,
    viewModel: OrigamiNotesViewModel,
    onNavigateBack: () -> Unit,
    onOpenNote: (Long) -> Unit = {},
    /** Only consulted for a new note; an existing one carries its own type. */
    initialType: NoteType = NoteType.DEFAULT
) {
    var noteType by remember { mutableStateOf(initialType) }
    // New task lists start as a plain checklist; the time table is opt-in.
    var timeTable by remember { mutableStateOf(false) }
    val allNotes by viewModel.allNotes.collectAsState()
    val confirmLinkOpen by viewModel.confirmLinkOpen.collectAsState()
    var showNotePicker by remember { mutableStateOf(false) }
    var showWebLinkDialog by remember { mutableStateOf(false) }
    var pendingLinkUrl by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    // noteId == null means new note — nothing to load, ready immediately
    var isLoaded by remember { mutableStateOf(noteId == null) }
    // Keeps the original note so saveNote() can update without re-querying StateFlow
    var loadedNote by remember { mutableStateOf<Note?>(null) }

    val blocks = remember { mutableStateListOf<NoteBlock>(NoteBlock.Text()) }
    // Every text block's live editor state, keyed by block id. A plain Column keeps all blocks
    // composed, so entries stay valid until the screen leaves.
    val blockStates = remember { mutableStateMapOf<String, RichTextState>() }
    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val focusedState = focusedBlockId?.let { blockStates[it] }

    LaunchedEffect(noteId, viewModel) {
        if (noteId != null) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                // Stamp the same instant locally and in the DB: saveNote() writes loadedNote
                // back verbatim, so a stale copy here would undo the persisted stamp.
                val openedAt = System.currentTimeMillis()
                loadedNote = note.copy(lastOpenedAt = openedAt)
                title = note.title
                isPinned = note.isPinned
                noteType = NoteType.fromName(note.type)
                timeTable = NoteBlocks.timeTableOf(note.blocks)
                blocks.clear()
                blocks.addAll(NoteBlocks.decode(note.blocks, note.content))
                viewModel.markNoteOpened(noteId, openedAt)
            }
        } else if (initialType == NoteType.TASK_LIST) {
            // A new task list starts empty; the default text block belongs to text notes only.
            blocks.clear()
        }
        isLoaded = true
    }

    /** Pulls the current HTML out of every live editor before persisting. */
    fun currentBlocks(): List<NoteBlock> = blocks.map { block ->
        when (block) {
            is NoteBlock.Text -> block.copy(html = blockStates[block.id]?.toHtml() ?: block.html)
            // Task edits are committed straight into `blocks`, so there is no live state to pull.
            is NoteBlock.Task -> block
        }
    }

    /**
     * Plain mirror taken from the editor itself.
     *
     * Not from stripping `toHtml()`: that output entity-encodes every non-ASCII character
     * (`Нотатка` becomes `&Ncy;&ocy;&tcy;...`), and no reasonable amount of regex un-escaping
     * covers the full HTML5 named-entity set. `toText()` is the library's own answer.
     */
    fun currentPlainText(blocksNow: List<NoteBlock>): String = blocksNow.joinToString("\n") { block ->
        when (block) {
            is NoteBlock.Text ->
                blockStates[block.id]?.toText() ?: NoteBlocks.toPlainText(listOf(block))
            // Tasks never went through HTML, so the pure projection is already correct.
            is NoteBlock.Task -> NoteBlocks.toPlainText(listOf(block))
        }
    }.trim()

    fun saveNote() {
        if (!isLoaded) return
        val current = currentBlocks()
        val plain = currentPlainText(current)
        if (title.isBlank() && plain.isBlank()) return
        val json = NoteBlocks.encode(current, timeTable)
        if (noteId == null) {
            viewModel.addNote(title, plain, json, noteType)
        } else {
            val existing = loadedNote ?: return
            val updated = existing.copy(
                title = title, content = plain, blocks = json, isPinned = isPinned
            )
            // Opening a note and backing straight out must not bump modifiedAt — that would
            // corrupt "Last Changed" ordering for every note the user merely reads.
            val changed = updated.title != existing.title ||
                updated.content != existing.content ||
                updated.blocks != existing.blocks ||
                updated.isPinned != existing.isPinned
            if (changed) viewModel.updateNote(updated)
        }
    }

    BackHandler {
        saveNote()
        onNavigateBack()
    }

    val focusManager = LocalFocusManager.current
    val systemUriHandler = LocalUriHandler.current

    // Toolbar reflects the marks under the caret of whichever block has focus.
    val spanStyle = focusedState?.currentSpanStyle
    val formatState = EditorFormatState(
        size = EditorTextSize.entries
            .firstOrNull { spanStyle?.fontSize?.value?.toInt() == it.sp }
            ?: EditorTextSize.NORMAL,
        bold = spanStyle?.fontWeight == FontWeight.Bold,
        italic = spanStyle?.fontStyle == FontStyle.Italic,
        underline = spanStyle?.textDecoration?.contains(TextDecoration.Underline) == true,
        strikethrough = spanStyle?.textDecoration?.contains(TextDecoration.LineThrough) == true,
        textColor = spanStyle?.color?.takeIf { it != Color.Unspecified },
        highlightColor = spanStyle?.background?.takeIf { it != Color.Unspecified }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        saveNote()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (noteId != null) {
                        IconButton(onClick = {
                            loadedNote?.let { viewModel.deleteNote(it) }
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface // Match editor background
                )
            )
        },
        // A task list has no runs of text to format, so it gets no formatting bar.
        // One stable lambda with the branch *inside* it. Swapping two different composable
        // lambdas here instead left Scaffold's bottomBar subcomposition with unbalanced groups
        // and crashed in IntStack.peek2.
        bottomBar = {
            if (noteType != NoteType.TASK_LIST) {
            EditorToolbar(
                state = formatState,
                onToggleBold = { focusedState?.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onToggleItalic = { focusedState?.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onToggleUnderline = {
                    focusedState?.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                },
                onToggleStrikethrough = {
                    focusedState?.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                },
                onPickSize = { size ->
                    focusedState?.toggleSpanStyle(SpanStyle(fontSize = size.sp.sp))
                },
                onPickTextColor = { color ->
                    val state = focusedState ?: return@EditorToolbar
                    if (color == null) {
                        formatState.textColor?.let { state.removeSpanStyle(SpanStyle(color = it)) }
                    } else {
                        state.addSpanStyle(SpanStyle(color = color))
                    }
                },
                onPickHighlight = { color ->
                    val state = focusedState ?: return@EditorToolbar
                    if (color == null) {
                        formatState.highlightColor?.let { state.removeSpanStyle(SpanStyle(background = it)) }
                    } else {
                        state.addSpanStyle(SpanStyle(background = color))
                    }
                },
                onLinkNote = { if (focusedState != null) showNotePicker = true },
                onLinkWeb = { if (focusedState != null) showWebLinkDialog = true },
                onInsertTool = { /* insert tool — next step */ },
                modifier = Modifier.height(52.dp)
            )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                // No bottom padding: the text runs straight into the toolbar, which removes
                // the pale band that used to sit between the note and the bar.
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall, color = Color.Gray) },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // if/else rather than an early `return@Column`: returning out of a @Composable
            // lambda leaves the composer's group stack unbalanced, which is what crashed the
            // content subcomposition with IntStack.peek2 index=-2.
            if (noteType == NoteType.TASK_LIST) {
                TaskDayView(
                    tasks = blocks.filterIsInstance<NoteBlock.Task>().sortedBy { it.startMinute },
                    onTasksChange = { updated ->
                        blocks.clear()
                        blocks.addAll(updated.sortedBy { it.startMinute })
                    },
                    timeTable = timeTable,
                    onTimeTableChange = { timeTable = it },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Every link tap funnels through here: ask first unless the user opted out.
                val onLinkTap: (String) -> Unit = { uri ->
                    if (confirmLinkOpen) {
                        pendingLinkUrl = uri
                    } else {
                        followLink(uri, NoteBlock.noteIdFrom(uri), systemUriHandler, onOpenNote) {
                            saveNote()
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    blocks.forEach { block ->
                        key(block.id) {
                            when (block) {
                                is NoteBlock.Text -> TextBlockEditor(
                                    block = block,
                                    isOnlyBlock = blocks.size == 1,
                                    onRegisterState = { blockStates[block.id] = it },
                                    onFocused = { focusedBlockId = block.id },
                                    onLinkTap = onLinkTap
                                )
                                // Tasks only occur in a task-list note, handled above.
                                is NoteBlock.Task -> Unit
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showNotePicker) {
        SelectNoteDialog(
            notes = allNotes,
            excludeNoteId = noteId,
            onSelect = { target ->
                focusedState?.insertLink(
                    url = NoteBlock.linkTo(target.id),
                    fallbackLabel = target.title.ifBlank { "Untitled" }
                )
                showNotePicker = false
            },
            onDismiss = { showNotePicker = false }
        )
    }

    if (showWebLinkDialog) {
        WebLinkDialog(
            onConfirm = { url ->
                // Bare host reads better in the text than the full URL with scheme.
                focusedState?.insertLink(
                    url = url,
                    fallbackLabel = url.substringAfter("://").removePrefix("www.")
                )
                showWebLinkDialog = false
            },
            onDismiss = { showWebLinkDialog = false }
        )
    }

    pendingLinkUrl?.let { url ->
        val noteTarget = NoteBlock.noteIdFrom(url)
        ConfirmOpenLinkDialog(
            url = url,
            isNoteLink = noteTarget != null,
            onConfirm = { dontAskAgain ->
                if (dontAskAgain) viewModel.setConfirmLinkOpen(false)
                pendingLinkUrl = null
                followLink(url, noteTarget, systemUriHandler, onOpenNote) { saveNote() }
            },
            onDismiss = { pendingLinkUrl = null }
        )
    }
}

/**
 * Turns bare web addresses in [text] into links.
 *
 * Skips any address the caret currently sits inside — that one is probably still being typed, and
 * linking it mid-word would fight the user. Skips ranges that are already links so this can run on
 * every keystroke without looping.
 */
private fun autoLinkUrls(state: RichTextState, text: String, selection: TextRange) {
    if (text.isEmpty()) return
    UrlDetector.detect(text).forEach { found ->
        if (selection.start in found.start..found.endExclusive) return@forEach
        val range = TextRange(found.start, found.endExclusive)
        if (state.getRichSpanStyle(range) is RichSpanStyle.Link) return@forEach
        state.addLinkToTextRange(found.url, range)
    }
}

/** Applies a link to the selection, or inserts [fallbackLabel] as the link text when there is none. */
private fun RichTextState.insertLink(url: String, fallbackLabel: String) {
    if (selection.collapsed) addLink(fallbackLabel, url) else addLinkToSelection(url)
}

private fun followLink(
    url: String,
    noteTarget: Long?,
    systemHandler: UriHandler,
    onOpenNote: (Long) -> Unit,
    onBeforeLeave: () -> Unit
) {
    onBeforeLeave()
    if (noteTarget != null) onOpenNote(noteTarget) else systemHandler.openUri(url)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextBlockEditor(
    block: NoteBlock.Text,
    isOnlyBlock: Boolean,
    onRegisterState: (RichTextState) -> Unit,
    onFocused: () -> Unit,
    onLinkTap: (String) -> Unit
) {
    val state = rememberRichTextState()
    val scope = rememberCoroutineScope()

    // setHtml only on first bind: re-running it on every recomposition would fight the user's
    // typing and reset the caret.
    LaunchedEffect(state) {
        state.setHtml(block.html)
        // Note links read as mint pills, not as underlined web links.
        state.config.linkColor = StitchGreen
        state.config.linkTextDecoration = TextDecoration.None
        onRegisterState(state)
    }

    // Auto-link: runs on text *and* caret changes, so a pasted address links immediately while
    // one still being typed is left alone until the caret leaves it.
    LaunchedEffect(state) {
        snapshotFlow { state.annotatedString.text to state.selection }
            .collect { (text, selection) -> autoLinkUrls(state, text, selection) }
    }

    RichTextEditor(
        state = state,
        placeholder = if (isOnlyBlock) {
            { Text("Start typing...", style = MaterialTheme.typography.bodyLarge, color = Color.Gray) }
        } else null,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = RichTextEditorDefaults.richTextEditorColors(
            containerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() }
            // richeditor-compose never opens links itself — it has no UriHandler anywhere in the
            // library — so the tap has to be spotted here. Observed in the Initial pass and never
            // consumed, so the editor still places the caret exactly as before; once it has, the
            // caret sits inside the link span and `isLink` answers whether one was hit.
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var event: PointerEvent
                    do {
                        event = awaitPointerEvent(PointerEventPass.Final)
                    } while (event.changes.any { it.pressed })

                    scope.launch {
                        // Two frames: one for the editor to commit the new selection, one to be
                        // safe against it landing a frame later.
                        withFrameNanos {}
                        withFrameNanos {}
                        if (state.isLink) state.selectedLinkUrl?.let(onLinkTap)
                    }
                }
            }
    )
}
