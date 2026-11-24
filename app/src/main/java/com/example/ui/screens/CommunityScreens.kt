package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.NexoraData
import com.example.data.NexoraHelpers
import com.example.data.UserRole
import com.example.data.UserSession
import com.example.ui.components.BadgeTone
import com.example.ui.components.NexoraBadge
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraConfirmDialog
import com.example.ui.components.NexoraDangerButton
import com.example.ui.components.NexoraDropdownSelector
import com.example.ui.components.NexoraFieldLabel
import com.example.ui.components.NexoraNoticeBox
import com.example.ui.components.NexoraPageHeader
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.components.NexoraSecondaryButton
import com.example.ui.components.nexoraTextFieldColors
import com.example.ui.theme.NexoraBlueLight
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraText

@Composable
fun HomeworkScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveHomework: (title: String, text: String, dueDate: String, classId: String, onDone: () -> Unit) -> Unit,
    onDeleteHomework: (id: String) -> Unit
) {
    val isAdmin = session.type == UserRole.ADMIN
    var showCreate by remember { mutableStateOf(false) }
    LaunchedEffect(showCreate) { onModalOpenChanged(showCreate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Hausaufgaben",
            subtitle = if (isAdmin) "Hausaufgaben für Klassen aufgeben." else "Übersicht aller Hausaufgaben.",
            action = if (isAdmin) {
                {
                    NexoraPrimaryButton(
                        text = "+ Hausaufgabe",
                        onClick = { showCreate = true },
                        modifier = Modifier.width(150.dp),
                        testTag = "add_homework_button"
                    )
                }
            } else null
        )

        if (data.homework.isEmpty()) {
            NexoraNoticeBox("Keine Hausaufgaben vorhanden.")
        } else {
            data.homework.forEach { hw ->
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(hw.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        if (hw.date.isNotBlank()) {
                            Text(hw.date, style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                        }
                    }
                    if (hw.text.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(hw.text, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Klasse: ${hw.className}", style = MaterialTheme.typography.bodySmall, color = NexoraBlueLight)
                    if (isAdmin) {
                        Spacer(Modifier.height(10.dp))
                        NexoraDangerButton("Löschen", onClick = { onDeleteHomework(hw.id) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        val firstSchoolClasses = data.schools.firstOrNull()?.classes.orEmpty()
        var title by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("") }
        var classId by remember { mutableStateOf(firstSchoolClasses.firstOrNull()?.id.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = NexoraCard,
            title = { Text("Hausaufgabe aufgeben", color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    NexoraFieldLabel("Titel / Fach")
                    OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("z.B. Mathematik Hausaufgabe", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Beschreibung")
                    OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 3, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Fällig am (YYYY-MM-DD)")
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, placeholder = { Text("2026-11-30", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraDropdownSelector(
                        label = "Klasse",
                        selectedText = firstSchoolClasses.find { it.id == classId }?.name.orEmpty(),
                        items = firstSchoolClasses,
                        itemLabel = { it.name },
                        onSelect = { classId = it.id }
                    )
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Veröffentlichen",
                    enabled = title.isNotBlank() && classId.isNotBlank(),
                    onClick = { onSaveHomework(title, text, dueDate, classId) { showCreate = false } },
                    modifier = Modifier.width(150.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }
}

@Composable
fun MessagesScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveMessage: (title: String, text: String, onDone: () -> Unit) -> Unit,
    onDeleteMessage: (id: String) -> Unit
) {
    val isAdmin = session.type == UserRole.ADMIN
    var showCreate by remember { mutableStateOf(false) }
    LaunchedEffect(showCreate) { onModalOpenChanged(showCreate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Nachrichten",
            subtitle = if (isAdmin) "Mitteilungen an die Schulgemeinschaft." else "Wichtige Mitteilungen.",
            action = if (isAdmin) {
                {
                    NexoraPrimaryButton(
                        text = "+ Nachricht",
                        onClick = { showCreate = true },
                        modifier = Modifier.width(140.dp),
                        testTag = "add_message_button"
                    )
                }
            } else null
        )

        if (data.messages.isEmpty()) {
            NexoraNoticeBox("Keine Nachrichten.")
        } else {
            data.messages.forEach { msg ->
                NexoraCard {
                    Text(msg.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                    if (msg.date.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(NexoraHelpers.formatDateTime(msg.date), style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                    }
                    if (isAdmin) {
                        Spacer(Modifier.height(10.dp))
                        NexoraDangerButton("Löschen", onClick = { onDeleteMessage(msg.id) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        var title by remember { mutableStateOf("") }
        var text by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = NexoraCard,
            title = { Text("Nachricht erstellen", color = Color.White) },
            text = {
                Column {
                    NexoraFieldLabel("Titel")
                    OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Text")
                    OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 3, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Senden",
                    enabled = title.isNotBlank() && text.isNotBlank(),
                    onClick = { onSaveMessage(title, text) { showCreate = false } },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }
}

@Composable
fun CalendarScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveEvent: (schoolId: String, title: String, date: String, text: String, onDone: () -> Unit) -> Unit,
    onDeleteEvent: (id: String) -> Unit
) {
    val isDirector = session.type == UserRole.DIRECTOR
    val events = remember(data.calendar, session) {
        if (isDirector) data.calendar.filter { it.schoolId == session.schoolId } else data.calendar
    }
    var showCreate by remember { mutableStateOf(false) }
    LaunchedEffect(showCreate) { onModalOpenChanged(showCreate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = if (isDirector) "Veranstaltungen" else "Schulveranstaltungen",
            subtitle = if (isDirector) "Termine für deine Schule." else "Ausflüge, Projekttage & Termine im Stundenplan.",
            action = {
                NexoraPrimaryButton(
                    text = "+ Termin",
                    onClick = { showCreate = true },
                    modifier = Modifier.width(120.dp),
                    testTag = "add_calendar_button"
                )
            }
        )

        if (events.isEmpty()) {
            NexoraNoticeBox("Noch keine Schulveranstaltungen eingetragen.")
        } else {
            events.forEach { ev ->
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ev.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        Text(ev.date, style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                    }
                    if (ev.text.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(ev.text, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                    }
                    Spacer(Modifier.height(10.dp))
                    NexoraDangerButton("Löschen", onClick = { onDeleteEvent(ev.id) })
                }
            }
        }
    }

    if (showCreate) {
        val eventTypes = listOf(
            "event" to "🎓 Schulveranstaltung",
            "trip" to "🚌 Ausflug",
            "sports" to "🏅 Sportveranstaltung",
            "exam" to "📚 Prüfung / Projekttag"
        )
        var schoolId by remember {
            mutableStateOf(if (isDirector) session.schoolId else data.schools.firstOrNull()?.id.orEmpty())
        }
        var typeKey by remember { mutableStateOf("event") }
        var rawTitle by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(NexoraHelpers.todayIso()) }
        var text by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = NexoraCard,
            title = { Text("Schulveranstaltung eintragen", color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!isDirector) {
                        NexoraDropdownSelector(
                            label = "Schule",
                            selectedText = data.schools.find { it.id == schoolId }?.name.orEmpty(),
                            items = data.schools,
                            itemLabel = { it.name },
                            onSelect = { schoolId = it.id }
                        )
                        NexoraDropdownSelector(
                            label = "Art der Veranstaltung",
                            selectedText = eventTypes.find { it.first == typeKey }?.second ?: "🎓 Schulveranstaltung",
                            items = eventTypes,
                            itemLabel = { it.second },
                            onSelect = { typeKey = it.first }
                        )
                    }
                    NexoraFieldLabel("Titel")
                    OutlinedTextField(value = rawTitle, onValueChange = { rawTitle = it }, placeholder = { Text("z. B. Ausflug ins Museum", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Datum (YYYY-MM-DD)")
                    OutlinedTextField(value = date, onValueChange = { date = it }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Beschreibung / Hinweise")
                    OutlinedTextField(value = text, onValueChange = { text = it }, minLines = 2, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Speichern",
                    enabled = schoolId.isNotBlank() && rawTitle.isNotBlank() && date.isNotBlank(),
                    onClick = {
                        val finalTitle = if (isDirector) rawTitle.trim() else {
                            val prefix = eventTypes.find { it.first == typeKey }?.second ?: "🎓 Schulveranstaltung"
                            "$prefix: ${rawTitle.trim()}"
                        }
                        onSaveEvent(schoolId, finalTitle, date, text) { showCreate = false }
                    },
                    modifier = Modifier.width(130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }
}

@Composable
fun AnnouncementsScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onAddAnnouncement: (schoolId: String, text: String, onDone: () -> Unit) -> Unit,
    onDeleteAnnouncement: (schoolId: String, index: Int) -> Unit
) {
    val isAdmin = session.type == UserRole.ADMIN
    var showCreate by remember { mutableStateOf(false) }
    LaunchedEffect(showCreate) { onModalOpenChanged(showCreate) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Ankündigungen",
            subtitle = if (isAdmin) "Schulweite Infos eintragen und verwalten." else "Schulweite Infos.",
            action = if (isAdmin) {
                {
                    NexoraPrimaryButton(
                        text = "+ Ankündigung",
                        onClick = { showCreate = true },
                        modifier = Modifier.width(150.dp),
                        testTag = "add_announcement_button"
                    )
                }
            } else null
        )

        if (isAdmin) {
            val allAnns = data.schools.flatMap { s -> s.announcements.mapIndexed { idx, ann -> Triple(s, idx, ann) } }
            if (allAnns.isEmpty()) {
                NexoraNoticeBox("Keine Ankündigungen vorhanden.")
            } else {
                allAnns.forEach { (s, idx, ann) ->
                    NexoraCard {
                        Text(s.name, style = MaterialTheme.typography.titleSmall, color = NexoraBlueLight)
                        Spacer(Modifier.height(4.dp))
                        Text(ann, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                        Spacer(Modifier.height(10.dp))
                        NexoraDangerButton("Löschen", onClick = { onDeleteAnnouncement(s.id, idx) })
                    }
                }
            }
        } else {
            val s = data.schools.find { it.id == session.schoolId }
            val anns = s?.announcements.orEmpty()
            if (anns.isEmpty()) {
                NexoraNoticeBox("Keine Ankündigungen.")
            } else {
                anns.forEach { ann ->
                    NexoraCard {
                        Text(ann, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                    }
                }
            }
        }
    }

    if (showCreate) {
        var schoolId by remember { mutableStateOf(data.schools.firstOrNull()?.id.orEmpty()) }
        var text by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = NexoraCard,
            title = { Text("Ankündigung hinzufügen", color = Color.White) },
            text = {
                Column {
                    NexoraDropdownSelector(
                        label = "Schule",
                        selectedText = data.schools.find { it.id == schoolId }?.name.orEmpty(),
                        items = data.schools,
                        itemLabel = { it.name },
                        onSelect = { schoolId = it.id }
                    )
                    NexoraFieldLabel("Ankündigung Text")
                    OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("z.B. Morgen entfällt die 1. Stunde...", color = NexoraMuted) }, minLines = 3, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Veröffentlichen",
                    enabled = schoolId.isNotBlank() && text.isNotBlank(),
                    onClick = { onAddAnnouncement(schoolId, text) { showCreate = false } },
                    modifier = Modifier.width(150.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }
}

@Composable
fun ReportProblemScreen(
    onSubmitProblem: (title: String, description: String, onDone: () -> Unit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Problem melden",
            subtitle = "Melde einen Fehler oder ein Problem mit Nexora."
        )
        NexoraCard {
            NexoraFieldLabel("Betreff")
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 160) title = it },
                placeholder = { Text("z. B. Stundenplan wird falsch angezeigt", color = NexoraMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoraTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("problem_title_input")
            )
            NexoraFieldLabel("Beschreibung")
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 5000) description = it },
                placeholder = { Text("Was ist passiert? Was hast du erwartet?", color = NexoraMuted) },
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = nexoraTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("problem_desc_input")
            )
            Spacer(Modifier.height(14.dp))
            NexoraPrimaryButton(
                text = "Problem absenden",
                enabled = title.isNotBlank() && description.isNotBlank(),
                onClick = {
                    onSubmitProblem(title, description) {
                        title = ""
                        description = ""
                    }
                },
                testTag = "submit_problem_button"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Bitte keine Passwörter oder unnötigen persönlichen Daten eintragen.",
                style = MaterialTheme.typography.bodySmall,
                color = NexoraMuted
            )
        }
    }
}

@Composable
fun ReportedProblemsScreen(
    data: NexoraData,
    error: String,
    onRefresh: () -> Unit,
    onDeleteReport: (id: String) -> Unit
) {
    var deletingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Gemeldete Probleme",
            subtitle = "Meldungen von Schülern, Lehrkräften und Administratoren.",
            action = {
                NexoraSecondaryButton("Aktualisieren", onClick = onRefresh)
            }
        )

        if (error.isNotBlank()) {
            NexoraNoticeBox(error, isError = true)
        } else if (data.problemReports.isEmpty()) {
            NexoraNoticeBox("Noch keine Probleme gemeldet.")
        } else {
            data.problemReports.forEach { r ->
                val school = data.schools.find { it.id == r.schoolId }
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        Text(NexoraHelpers.formatDateTime(r.createdAt), style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                    }
                    Text(
                        buildString {
                            append("Gemeldet von: ${r.reporterType}")
                            if (r.reporterName.isNotBlank()) append(" · ${r.reporterName}")
                            if (school != null) append(" · ${school.name}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NexoraMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(r.description, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NexoraBadge("Status: ${r.status}", BadgeTone.BLUE)
                        NexoraDangerButton("Meldung löschen", onClick = { deletingId = r.id })
                    }
                }
            }
        }
    }

    deletingId?.let { id ->
        NexoraConfirmDialog(
            title = "Meldung löschen?",
            message = "Diese Problemmeldung wirklich löschen?",
            onConfirm = {
                deletingId = null
                onDeleteReport(id)
            },
            onDismiss = { deletingId = null }
        )
    }
}

@Composable
fun UpdatesScreen(
    data: NexoraData,
    isAdminManage: Boolean,
    onPublishUpdate: (title: String, description: String, onDone: () -> Unit) -> Unit,
    onDeleteUpdate: (id: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deletingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = if (isAdminManage) "Updates verwalten" else "Update",
            subtitle = if (isAdminManage) "Diese Updates werden auf der Anmeldeseite und für alle Nutzer angezeigt." else "Aktuelle Neuigkeiten und Änderungen an Nexora."
        )

        if (isAdminManage) {
            NexoraCard {
                NexoraFieldLabel("Titel")
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 180) title = it },
                    placeholder = { Text("z. B. Neue Funktionen in Nexora", color = NexoraMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                NexoraFieldLabel("Beschreibung")
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 8000) description = it },
                    placeholder = { Text("Beschreibe die Neuerung oder wichtige Information …", color = NexoraMuted) },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                NexoraPrimaryButton(
                    text = "Update veröffentlichen",
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    onClick = {
                        onPublishUpdate(title, description) {
                            title = ""
                            description = ""
                        }
                    },
                    testTag = "publish_update_button"
                )
            }
        }

        if (data.updates.isEmpty()) {
            NexoraNoticeBox("Noch keine Updates veröffentlicht.")
        } else {
            data.updates.forEach { u ->
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(u.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                        if (isAdminManage) {
                            NexoraDangerButton("Löschen", onClick = { deletingId = u.id })
                        }
                    }
                    if (u.createdAt.isNotBlank()) {
                        Text(NexoraHelpers.formatDateTime(u.createdAt), style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(u.description, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                }
            }
        }
    }

    deletingId?.let { id ->
        NexoraConfirmDialog(
            title = "Update löschen?",
            message = "Dieses Update wirklich löschen?",
            onConfirm = {
                deletingId = null
                onDeleteUpdate(id)
            },
            onDismiss = { deletingId = null }
        )
    }
}
