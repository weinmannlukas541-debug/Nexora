package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexoraBlue
import com.example.ui.theme.NexoraBlueBright
import com.example.ui.theme.NexoraBlueLight
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraGreen
import com.example.ui.theme.NexoraGreenLight
import com.example.ui.theme.NexoraIndigo
import com.example.ui.theme.NexoraLine
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraOrange
import com.example.ui.theme.NexoraOrangeLight
import com.example.ui.theme.NexoraRed
import com.example.ui.theme.NexoraRedLight
import com.example.ui.theme.NexoraText

@Composable
fun NexoraLogoBadge(size: Dp = 32.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Brush.linearGradient(listOf(NexoraBlue, NexoraIndigo))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = "Nexora Logo",
            tint = Color.White,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

@Composable
fun NexoraCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NexoraCard,
    borderColor: Color = NexoraLine,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .clip(shape)
        .background(containerColor)
        .border(1.dp, borderColor, shape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    Column(modifier = base.padding(contentPadding), content = content)
}

@Composable
fun NexoraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = NexoraLine,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(
                    if (enabled) Brush.linearGradient(listOf(NexoraBlue, NexoraIndigo))
                    else Brush.linearGradient(listOf(NexoraLine, NexoraLine)),
                    shape = shape
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) Color.White else NexoraMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NexoraSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, NexoraLine),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NexoraCardElevated.copy(alpha = 0.7f),
            contentColor = NexoraText
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .heightIn(min = 48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it }
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun NexoraDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NexoraRed.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = NexoraRed.copy(alpha = 0.16f),
            contentColor = NexoraRedLight
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun NexoraNoticeBox(
    text: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    val bg = if (isError) NexoraRed.copy(alpha = 0.12f) else NexoraOrange.copy(alpha = 0.12f)
    val border = if (isError) NexoraRed.copy(alpha = 0.35f) else NexoraOrange.copy(alpha = 0.28f)
    val tint = if (isError) NexoraRedLight else NexoraOrangeLight
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

enum class BadgeTone { RED, GREEN, ORANGE, BLUE }

@Composable
fun NexoraBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (tone) {
        BadgeTone.RED -> NexoraRed.copy(alpha = 0.2f) to NexoraRedLight
        BadgeTone.GREEN -> NexoraGreen.copy(alpha = 0.2f) to NexoraGreenLight
        BadgeTone.ORANGE -> NexoraOrange.copy(alpha = 0.2f) to NexoraOrangeLight
        BadgeTone.BLUE -> NexoraBlueBright.copy(alpha = 0.2f) to NexoraBlueLight
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
fun nexoraTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NexoraBlueBright,
    unfocusedBorderColor = NexoraLine,
    focusedContainerColor = Color(0x99090D16),
    unfocusedContainerColor = Color(0x99090D16),
    focusedTextColor = NexoraText,
    unfocusedTextColor = NexoraText,
    focusedLabelColor = NexoraBlueLight,
    unfocusedLabelColor = NexoraMuted,
    cursorColor = NexoraBlueLight
)

@Composable
fun NexoraFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = NexoraMuted,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
fun <T> NexoraDropdownSelector(
    label: String,
    selectedText: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        if (label.isNotBlank()) {
            NexoraFieldLabel(label)
        }
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .minimumInteractiveComponentSize()
                    .let { if (testTag != null) it.testTag(testTag) else it }
                    .clickable(enabled = items.isNotEmpty()) { expanded = true },
                color = Color(0x99090D16),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NexoraLine)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedText.ifBlank { "Auswählen..." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedText.isBlank()) NexoraMuted else NexoraText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Auswahl öffnen",
                        tint = NexoraMuted
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(NexoraCard)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = itemLabel(item),
                                style = MaterialTheme.typography.bodyMedium,
                                color = NexoraText
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NexoraConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Löschen",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        titleContentColor = NexoraText,
        textContentColor = NexoraMuted,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            NexoraDangerButton(text = confirmLabel, onClick = onConfirm)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = NexoraMuted)
            }
        }
    )
}

@Composable
fun NexoraPageHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NexoraMuted
                )
            }
        }
        if (action != null) {
            Spacer(Modifier.width(12.dp))
            action()
        }
    }
}

@Composable
fun NexoraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        if (label.isNotBlank()) NexoraFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotBlank()) Text(placeholder, color = NexoraMuted) },
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            colors = nexoraTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
