package com.mono.launcher.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mono.launcher.ui.theme.Mono

/** Primary action: 52dp pill, inverted (text-colour fill). */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (enabled) Mono.palette.accent else Mono.palette.accent.copy(alpha = 0.38f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Mono.palette.onAccent)
    }
}

/** Secondary action: plain text, 48dp tall. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 15.sp,
            color = if (emphasized) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Uppercase 11sp section heading with 0.12em tracking. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (11 * 0.12).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() },
    )
}

/** Rounded 20dp group on the surface colour. */
@Composable
fun SettingGroup(modifier: Modifier = Modifier, contentPadding: Int = 4, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = contentPadding.dp),
        content = content,
    )
}

/** 44×26 switch: on = text-colour track with background-colour thumb; off = track grey. */
@Composable
fun MonoSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val p = Mono.palette
    val track by animateColorAsState(if (checked) p.accent else p.track, tween(150), label = "track")
    val thumb by animateColorAsState(if (checked) p.onAccent else p.tertiaryText, tween(150), label = "thumb")
    val x by animateDpAsState(if (checked) 21.dp else 3.dp, tween(150), label = "thumbX")
    Box(modifier.size(44.dp, 26.dp).clip(CircleShape).background(track)) {
        Box(Modifier.offset(x = x, y = 3.dp).size(20.dp).clip(CircleShape).background(thumb))
    }
}

/** Segmented pill; the selected option is inverted. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.background)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Mono.palette.accent else c.background.copy(alpha = 0f))
                    .clickable(role = Role.RadioButton) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o),
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) Mono.palette.onAccent else c.onSurfaceVariant,
                )
            }
        }
    }
}

/** Selection tile: container colour, 2dp text-colour border when selected. */
@Composable
fun Tile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Int = 14,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(c.surfaceContainer)
            .border(2.dp, if (selected) Mono.palette.accent else c.surfaceContainer, RoundedCornerShape(radius.dp))
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

/** Outlined "work" tag. */
@Composable
fun WorkTag(modifier: Modifier = Modifier) {
    Box(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text("work", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Inverted count badge ("2 new"), or a dot for a single notification. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    if (count > 1) {
        Box(modifier.clip(CircleShape).background(Mono.palette.accent).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("$count new", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Mono.palette.onAccent)
        }
    } else {
        Box(modifier.size(8.dp).clip(CircleShape).background(Mono.palette.accent))
    }
}

/** Row spacing helper used by lists in sheets and settings. */
@Composable
fun RowScope.Gap(dp: Int) = androidx.compose.foundation.layout.Spacer(Modifier.width(dp.dp))
