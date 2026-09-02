package com.origaminotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.origaminotes.app.ui.theme.EditorToolbarDark
import com.origaminotes.app.ui.theme.EditorToolbarDivider
import com.origaminotes.app.ui.theme.StitchGreen

/** Text sizes offered by the toolbar's size menu. */
enum class EditorTextSize(val label: String, val sp: Int) {
    SMALL("Small", 13),
    NORMAL("Normal", 16),
    LARGE("Large", 20),
    HUGE("Huge", 26)
}

/**
 * Which formatting marks are active for the current selection / insertion point.
 * Held by the editor screen so the toolbar itself stays stateless.
 */
data class EditorFormatState(
    val size: EditorTextSize = EditorTextSize.NORMAL,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val textColor: Color? = null,
    val highlightColor: Color? = null
)

/**
 * A focusable popup steals focus from the text field, which closes the keyboard and drops the
 * selection the user is trying to format. Non-focusable keeps the IME up; tapping outside still
 * dismisses.
 */
private val KeepKeyboardPopup = PopupProperties(focusable = false)

/** Palette shared by the text-color and highlight pickers. */
private val FormatPalette = listOf(
    Color(0xFF13ECA4), Color(0xFF3B82F6), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFA855F7), Color(0xFF10B981),
    Color(0xFFEC4899), Color(0xFF64748B)
)

/**
 * Formatting bar that sits directly above the keyboard.
 *
 * Scrolls horizontally: nine controls do not fit across a phone at a comfortable touch size,
 * and shrinking them below 40dp would break the minimum tap target.
 */
@Composable
fun EditorToolbar(
    state: EditorFormatState,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onPickSize: (EditorTextSize) -> Unit,
    onPickTextColor: (Color?) -> Unit,
    onPickHighlight: (Color?) -> Unit,
    onLinkNote: () -> Unit,
    onLinkWeb: () -> Unit,
    onInsertTool: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sizeMenuOpen by remember { mutableStateOf(false) }
    var textColorMenuOpen by remember { mutableStateOf(false) }
    var highlightMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EditorToolbarDark)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        // Wide enough that the active-state rounded square never touches its neighbour.
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Size ────────────────────────────────────────────────────────────
        Box {
            ToolbarButton(
                icon = Icons.Default.FormatSize,
                label = "Text size (${state.size.label})",
                active = state.size != EditorTextSize.NORMAL,
                onClick = { sizeMenuOpen = true }
            )
            DropdownMenu(
                expanded = sizeMenuOpen,
                onDismissRequest = { sizeMenuOpen = false },
                containerColor = MaterialTheme.colorScheme.surface,
                properties = KeepKeyboardPopup
            ) {
                EditorTextSize.entries.forEach { size ->
                    val isCurrent = size == state.size
                    DropdownMenuItem(
                        text = {
                            Text(
                                size.label,
                                fontSize = size.sp.sp,
                                color = if (isCurrent) StitchGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = { onPickSize(size); sizeMenuOpen = false }
                    )
                }
            }
        }

        ToolbarDivider()

        // ── Character styles ────────────────────────────────────────────────
        ToolbarButton(Icons.Default.FormatBold, "Bold", state.bold, onClick = onToggleBold)
        ToolbarButton(Icons.Default.FormatItalic, "Italic", state.italic, onClick = onToggleItalic)
        ToolbarButton(Icons.Default.FormatUnderlined, "Underline", state.underline, onClick = onToggleUnderline)
        ToolbarButton(Icons.Default.FormatStrikethrough, "Strikethrough", state.strikethrough, onClick = onToggleStrikethrough)

        ToolbarDivider()

        // ── Colors: text and highlight are separate pickers ─────────────────
        Box {
            ToolbarButton(
                icon = Icons.Default.FormatColorText,
                label = "Text color",
                active = state.textColor != null,
                accent = state.textColor,
                onClick = { textColorMenuOpen = true }
            )
            ColorPickerMenu(
                expanded = textColorMenuOpen,
                selected = state.textColor,
                onDismiss = { textColorMenuOpen = false },
                onPick = { onPickTextColor(it); textColorMenuOpen = false }
            )
        }
        Box {
            ToolbarButton(
                icon = Icons.Default.FormatColorFill,
                label = "Highlight color",
                active = state.highlightColor != null,
                accent = state.highlightColor,
                onClick = { highlightMenuOpen = true }
            )
            ColorPickerMenu(
                expanded = highlightMenuOpen,
                selected = state.highlightColor,
                onDismiss = { highlightMenuOpen = false },
                onPick = { onPickHighlight(it); highlightMenuOpen = false }
            )
        }

        ToolbarDivider()

        // ── Insert ──────────────────────────────────────────────────────────
        ToolbarButton(Icons.Default.AddLink, "Link to note", false, onClick = onLinkNote)
        ToolbarButton(Icons.Default.Language, "Link to web page", false, onClick = onLinkWeb)
        ToolbarButton(Icons.Default.Widgets, "Insert tool", false, onClick = onInsertTool)
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(
                color = if (active) StitchGreen.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Icon(
            icon,
            contentDescription = label,
            // A chosen color previews itself on the icon; otherwise mint when on, muted when off.
            tint = accent ?: if (active) StitchGreen else Color(0xFFBFD9CE),
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(22.dp)
            .background(EditorToolbarDivider)
    )
}

@Composable
private fun ColorPickerMenu(
    expanded: Boolean,
    selected: Color?,
    onDismiss: () -> Unit,
    onPick: (Color?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = KeepKeyboardPopup
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    "None",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            onClick = { onPick(null) }
        )
        // Two rows of four keep the menu compact instead of eight stacked items.
        FormatPalette.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(if (color == selected) 30.dp else 26.dp)
                            .background(color, CircleShape)
                    ) {
                        IconButton(onClick = { onPick(color) }, modifier = Modifier.size(30.dp)) {}
                    }
                }
            }
        }
    }
}
