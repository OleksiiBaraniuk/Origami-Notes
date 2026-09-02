package com.origaminotes.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.origaminotes.app.ui.OrigamiNotesApp
import com.origaminotes.app.widget.EXTRA_NOTE_ID
import com.origaminotes.app.widget.EXTRA_PICK_KIND
import com.origaminotes.app.widget.EXTRA_WIDGET_ID
import com.origaminotes.app.widget.WidgetPickKind

class MainActivity : ComponentActivity() {

    /**
     * Note a widget asked us to open. Held in state rather than read once, so a tap that arrives
     * while the activity is already alive (singleTop → onNewIntent) still navigates.
     */
    private var pendingNoteId by mutableStateOf<Long?>(null)

    /** Set when a One-* widget asked the app to choose its target. */
    private var pendingPick by mutableStateOf<WidgetPickRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readIntent(intent)
        setContent {
            OrigamiNotesApp(
                openNoteId = pendingNoteId,
                onOpenNoteHandled = { pendingNoteId = null },
                widgetPick = pendingPick,
                onWidgetPickHandled = { pendingPick = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent) {
        pendingNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L).takeIf { it > 0L }

        val kind = intent.getStringExtra(EXTRA_PICK_KIND)
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        pendingPick = if (kind != null && widgetId > 0) {
            runCatching { WidgetPickKind.valueOf(kind) }
                .getOrNull()
                ?.let { WidgetPickRequest(it, widgetId) }
        } else null
    }
}

/** A widget waiting to be told what to show. */
data class WidgetPickRequest(val kind: WidgetPickKind, val appWidgetId: Int)
