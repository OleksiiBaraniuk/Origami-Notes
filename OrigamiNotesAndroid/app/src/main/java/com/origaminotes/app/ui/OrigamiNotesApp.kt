package com.origaminotes.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.origaminotes.app.R
import com.origaminotes.app.WidgetPickRequest
import com.origaminotes.app.data.NoteType
import com.origaminotes.app.ui.components.SelectFolderDialog
import com.origaminotes.app.ui.components.SelectNoteDialog
import com.origaminotes.app.widget.WidgetPickKind
import com.origaminotes.app.widget.setWidgetTarget
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.origaminotes.app.ui.screens.EditorScreen
import com.origaminotes.app.ui.screens.SettingsScreen
import com.origaminotes.app.ui.screens.StitchPrototypeScreen
import com.origaminotes.app.ui.screens.WhatsNewScreen
import com.origaminotes.app.ui.theme.OrigamiNotesTheme

@Composable
fun OrigamiNotesApp(
    viewModel: OrigamiNotesViewModel = viewModel(factory = OrigamiNotesViewModel.Factory),
    /** Set when a widget tap asked for a specific note. */
    openNoteId: Long? = null,
    onOpenNoteHandled: () -> Unit = {},
    /** Set when a One-* widget needs its target chosen. */
    widgetPick: WidgetPickRequest? = null,
    onWidgetPickHandled: () -> Unit = {}
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(openNoteId) {
        val id = openNoteId ?: return@LaunchedEffect
        navController.navigate("editor?noteId=$id")
        // Cleared immediately so returning to the list does not bounce straight back in.
        onOpenNoteHandled()
    }

    OrigamiNotesTheme(darkTheme = isDarkTheme) {
        val appVersion = stringResource(R.string.app_version)
        val seenVersion by viewModel.whatsNewSeenVersion.collectAsState()
        // Covers everything while it is up, so the app is not usable behind it.
        if (seenVersion != PREFS_LOADING && seenVersion != appVersion) {
            WhatsNewScreen(onDismiss = { viewModel.markWhatsNewSeen(appVersion) })
            return@OrigamiNotesTheme
        }

        // Rendered over whatever screen is showing: a widget tap can land at any moment, and the
        // picker is a decision about the widget, not about the current screen.
        widgetPick?.let { request ->
            val allNotes by viewModel.allNotes.collectAsState()
            val allFolders by viewModel.allFolders.collectAsState()

            fun commit(targetId: Long) {
                scope.launch { setWidgetTarget(context, request.appWidgetId, targetId) }
                onWidgetPickHandled()
            }

            when (request.kind) {
                WidgetPickKind.NOTE -> SelectNoteDialog(
                    notes = allNotes.filter { NoteType.fromName(it.type) == NoteType.TEXT },
                    excludeNoteId = null,
                    onSelect = { commit(it.id) },
                    onDismiss = onWidgetPickHandled
                )
                WidgetPickKind.TASK_LIST -> SelectNoteDialog(
                    notes = allNotes.filter { NoteType.fromName(it.type) == NoteType.TASK_LIST },
                    excludeNoteId = null,
                    onSelect = { commit(it.id) },
                    onDismiss = onWidgetPickHandled
                )
                WidgetPickKind.FOLDER -> SelectFolderDialog(
                    folders = allFolders,
                    title = "Folder for widget",
                    onSelect = { commit(it.id) },
                    onDismiss = onWidgetPickHandled
                )
            }
        }

        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                StitchPrototypeScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        navController.navigate("editor?noteId=$noteId")
                    },
                    onAddNoteClick = { type ->
                        navController.navigate("editor?type=${type.name}")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }
            composable(
                route = "editor?noteId={noteId}&type={type}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = NoteType.DEFAULT.name
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L
                EditorScreen(
                    noteId = if (noteId == -1L) null else noteId,
                    initialType = NoteType.fromName(backStackEntry.arguments?.getString("type")),
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    // Following a note link opens the target on top, so Back returns to the
                    // note that linked out.
                    onOpenNote = { targetId -> navController.navigate("editor?noteId=$targetId") }
                )
            }
            composable("settings") {
                val confirmLinkOpen by viewModel.confirmLinkOpen.collectAsState()
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { viewModel.setDarkTheme(it) },
                    confirmLinkOpen = confirmLinkOpen,
                    onResetLinkConfirm = { viewModel.setConfirmLinkOpen(true) },
                    onExport = { context, uri -> viewModel.exportData(context, uri) },
                    onImport = { context, uri -> viewModel.importData(context, uri) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
