package com.origaminotes.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.origaminotes.app.R
import com.origaminotes.app.ui.theme.OrigamiAccents
import com.origaminotes.app.ui.theme.StitchGreen

/** One headline feature and the sentence explaining what to do with it. */
private data class ReleaseItem(val title: String, val detail: String)

private val NewFunctions = listOf(
    ReleaseItem(
        "Text formatting v1",
        "Change size and colour, make text bold, italic, underlined or struck through."
    ),
    ReleaseItem(
        "Web links",
        "Attach a link to selected text, or paste one straight in — it becomes a link by itself. " +
            "Tap it to open the page."
    ),
    ReleaseItem(
        "Note links",
        "Attach a link to text or pick a note to insert. Tap it to jump to the note it points at."
    ),
    ReleaseItem(
        "Task lists",
        "Hold the centre button to open the note menu and pick a task list. Arrange tasks freely, " +
            "or on a time table if you turn one on."
    ),
    ReleaseItem(
        "Widgets",
        "Keep what matters on your home screen: all notes, all tasks, or a single note, task list " +
            "or folder you choose."
    )
)

private val Fixes = listOf(
    "Colour corrections across the dark and light themes",
    "Folder and note grouping now shows what it should",
    "Branches list scrolls all the way to the last item"
)

/**
 * Shown once per version. Not a route: it covers the app on first launch and is dismissed for
 * good, so it never needs a back stack entry of its own.
 */
@Composable
fun WhatsNewScreen(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(40.dp))
                Text(
                    "What's new",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Version ${stringResource(R.string.app_version)}",
                    color = StitchGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(28.dp))
                SectionHeading(Icons.Default.AutoAwesome, "New")
                Spacer(Modifier.height(12.dp))
                NewFunctions.forEachIndexed { index, item ->
                    FeatureCard(index + 1, item)
                    Spacer(Modifier.height(10.dp))
                }

                Spacer(Modifier.height(20.dp))
                SectionHeading(Icons.Default.BuildCircle, "Fixed")
                Spacer(Modifier.height(12.dp))
                Fixes.forEach { fix ->
                    FixRow(fix)
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(24.dp))
            }

            // Outside the scroll: the way out must not depend on reaching the bottom.
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchGreen,
                    contentColor = OrigamiAccents.onAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .height(52.dp)
            ) {
                Text("Got it", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeading(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StitchGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun FeatureCard(number: Int, item: ReleaseItem) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(StitchGreen.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    number.toString(),
                    color = StitchGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun FixRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(StitchGreen, CircleShape)
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
