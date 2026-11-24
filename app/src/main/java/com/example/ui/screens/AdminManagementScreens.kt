package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceRecord
import com.example.data.NexoraConstants
import com.example.data.NexoraData
import com.example.data.NexoraHelpers
import com.example.data.School
import com.example.data.SchoolClass
import com.example.data.SchoolDirector
import com.example.data.Teacher
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
import com.example.ui.theme.NexoraBlueBright
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraText

@Composable
fun DashboardScreen(
    session: UserSession,
    data: NexoraData
) {
    val isDirector = session.type == UserRole.DIRECTOR
    val directorSchool = if (isDirector) data.schools.find { it.id == session.schoolId } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isDirector) {
            NexoraPageHeader(
                title = "Schuldirektor-Bereich",
                subtitle = "Du verwaltest ausschließlich die dir zugewiesene Schule."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Schule", directorSchool?.name ?: "Nicht verfügbar", Modifier.weight(1.4f), smallValue = true)
                StatCard("Klassen", "${directorSchool?.classes?.size ?: 0}", Modifier.weight(1f))
                StatCard("Stunden", "${directorSchool?.lessons?.size ?: 0}", Modifier.weight(1f))
            }
            NexoraCard {
                Text("Schulverwaltung", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Verwalte Klassen, Schülerlisten, Stundenplan und Schulveranstaltungen über das Navigationsmenü.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraMuted
                )
            }
        } else {
            NexoraPageHeader(
                title = "Übersicht",
                subtitle = "Zentrale Steuerung für alle Schulen."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Schulen", "${data.schools.size}", Modifier.weight(1f))
                StatCard("Klassen", "${data.schools.sumOf { it.classes.size }}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Lehrer", "${data.teachers.size}", Modifier.weight(1f))
                StatCard("Stunden", "${data.schools.sumOf { it.lessons.size }}", Modifier.weight(1f))
            }
            NexoraCard {
                Text("Bereit", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Alle Funktionen inklusive Klassen-, Lehrer- und Schülerverwaltung sowie Supabase-Sync sind aktiv.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoraMuted
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, smallValue: Boolean = false) {
    NexoraCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = if (smallValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.displayLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SchoolsScreen(
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveSchool: (id: String?, name: String, city: String, onDone: () -> Unit) -> Unit,
    onDeleteSchool: (id: String) -> Unit
) {
    var editingSchool by remember { mutableStateOf<School?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deletingSchool by remember { mutableStateOf<School?>(null) }

    LaunchedEffect(editingSchool, showCreate, deletingSchool) {
        onModalOpenChanged(editingSchool != null || showCreate || deletingSchool != null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Schulen",
            subtitle = "Schulen hinzufügen und bearbeiten.",
            action = {
                NexoraPrimaryButton(
                    text = "+ Schule",
                    onClick = { showCreate = true },
                    modifier = Modifier.width(120.dp),
                    testTag = "add_school_button"
                )
            }
        )

        if (data.schools.isEmpty()) {
            NexoraNoticeBox("Noch keine Schulen vorhanden.")
        } else {
            data.schools.forEach { s ->
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("${s.city.ifBlank { "Kein Ort" }} · ${s.classes.size} Klassen", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NexoraSecondaryButton("Bearbeiten", onClick = { editingSchool = s })
                        NexoraDangerButton("Löschen", onClick = { deletingSchool = s })
                    }
                }
            }
        }
    }

    if (showCreate || editingSchool != null) {
        val target = editingSchool
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var city by remember(target) { mutableStateOf(target?.city.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showCreate = false; editingSchool = null },
            containerColor = NexoraCard,
            title = { Text(if (target != null) "Schule bearbeiten" else "Neue Schule", color = Color.White) },
            text = {
                Column {
                    NexoraFieldLabel("Name")
                    OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Ort")
                    OutlinedTextField(value = city, onValueChange = { city = it }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Speichern",
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSaveSchool(target?.id, name, city) { showCreate = false; editingSchool = null }
                    },
                    modifier = Modifier.width(130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; editingSchool = null }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }

    deletingSchool?.let { s ->
        NexoraConfirmDialog(
            title = "Schule löschen?",
            message = "Schule „${s.name}“ wirklich löschen?",
            onConfirm = {
                deletingSchool = null
                onDeleteSchool(s.id)
            },
            onDismiss = { deletingSchool = null }
        )
    }
}

@Composable
fun ClassesScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveClass: (id: String?, schoolId: String, name: String, username: String, password: String, onDone: () -> Unit) -> Unit,
    onDeleteClass: (id: String) -> Unit,
    onAddStudent: (schoolId: String, classId: String, name: String) -> Unit,
    onDeleteStudent: (studentId: String) -> Unit
) {
    val isDirector = session.type == UserRole.DIRECTOR
    val classPairs = remember(data.schools, session) {
        if (isDirector) {
            val s = data.schools.find { it.id == session.schoolId }
            s?.classes?.map { s to it } ?: emptyList()
        } else {
            data.schools.flatMap { s -> s.classes.map { s to it } }
        }
    }

    var showCreate by remember { mutableStateOf(false) }
    var editingPair by remember { mutableStateOf<Pair<School, SchoolClass>?>(null) }
    var managingStudentsFor by remember { mutableStateOf<Pair<School, SchoolClass>?>(null) }
    var deletingClass by remember { mutableStateOf<SchoolClass?>(null) }

    LaunchedEffect(showCreate, editingPair, managingStudentsFor, deletingClass) {
        onModalOpenChanged(showCreate || editingPair != null || managingStudentsFor != null || deletingClass != null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = if (isDirector) "Klassen & Schüler" else "Klassen",
            subtitle = if (isDirector) "Nur Klassen deiner Schule werden angezeigt." else "Klassen und Schüler zentral verwalten.",
            action = {
                NexoraPrimaryButton(
                    text = "+ Klasse",
                    onClick = { showCreate = true },
                    modifier = Modifier.width(120.dp),
                    testTag = "add_class_button"
                )
            }
        )

        if (classPairs.isEmpty()) {
            NexoraNoticeBox("Noch keine Klassen vorhanden.")
        } else {
            classPairs.forEach { (s, c) ->
                val studentCount = data.students.count { it.classId == c.id }
                NexoraCard {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "${s.name} · Benutzername: ${c.username} · $studentCount Schüler",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexoraMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NexoraSecondaryButton("Schüler ($studentCount)", onClick = { managingStudentsFor = s to c })
                        NexoraSecondaryButton("Bearbeiten", onClick = { editingPair = s to c })
                        NexoraDangerButton("Löschen", onClick = { deletingClass = c })
                    }
                }
            }
        }
    }

    if (showCreate || editingPair != null) {
        val target = editingPair?.second
        var schoolId by remember(editingPair) {
            mutableStateOf(
                if (isDirector) session.schoolId
                else editingPair?.first?.id ?: data.schools.firstOrNull()?.id.orEmpty()
            )
        }
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var username by remember(target) { mutableStateOf(target?.username.orEmpty()) }
        var password by remember(target) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false; editingPair = null },
            containerColor = NexoraCard,
            title = { Text(if (target != null) "Klasse bearbeiten" else "Neue Klasse", color = Color.White) },
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
                    }
                    NexoraFieldLabel("Klassenname")
                    OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("z. B. 8a", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Benutzername")
                    OutlinedTextField(value = username, onValueChange = { username = it }, placeholder = { Text("z. B. 8a", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel(if (target != null) "Neues Passwort (optional)" else "Passwort")
                    OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Speichern",
                    enabled = schoolId.isNotBlank() && name.isNotBlank() && username.isNotBlank() && (target != null || password.isNotBlank()),
                    onClick = {
                        onSaveClass(target?.id, schoolId, name, username, password) { showCreate = false; editingPair = null }
                    },
                    modifier = Modifier.width(130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; editingPair = null }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }

    managingStudentsFor?.let { (school, schoolClass) ->
        val students = data.students.filter { it.classId == schoolClass.id }
        var newStudentName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { managingStudentsFor = null },
            containerColor = NexoraCard,
            title = {
                Column {
                    Text("Schüler verwalten: Klasse ${schoolClass.name}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("Füge Schüler zur Klasse hinzu oder entferne sie.", color = NexoraMuted, style = MaterialTheme.typography.bodySmall)
                }
            },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newStudentName,
                            onValueChange = { newStudentName = it },
                            placeholder = { Text("Vor- und Nachname", color = NexoraMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = nexoraTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        NexoraPrimaryButton(
                            text = "+",
                            enabled = newStudentName.isNotBlank(),
                            onClick = {
                                onAddStudent(school.id, schoolClass.id, newStudentName)
                                newStudentName = ""
                            },
                            modifier = Modifier.width(56.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (students.isEmpty()) {
                        NexoraNoticeBox("Keine Schüler in dieser Klasse eingetragen.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(students, key = { it.id }) { st ->
                                NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(st.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        NexoraDangerButton("Löschen", onClick = { onDeleteStudent(st.id) })
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                NexoraPrimaryButton(text = "Fertig", onClick = { managingStudentsFor = null }, modifier = Modifier.width(110.dp))
            }
        )
    }

    deletingClass?.let { c ->
        NexoraConfirmDialog(
            title = "Klasse löschen?",
            message = "Klasse „${c.name}“ wirklich löschen?",
            onConfirm = {
                deletingClass = null
                onDeleteClass(c.id)
            },
            onDismiss = { deletingClass = null }
        )
    }
}

@Composable
fun TeachersScreen(
    session: UserSession,
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveTeacher: (id: String?, schoolId: String, name: String, username: String, subjects: String, password: String, onDone: () -> Unit) -> Unit,
    onDeleteTeacher: (id: String) -> Unit
) {
    val isDirector = session.type == UserRole.DIRECTOR
    val teachers = remember(data.teachers, session) {
        if (isDirector) data.teachers.filter { it.schoolId == session.schoolId } else data.teachers
    }

    var showCreate by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var deletingTeacher by remember { mutableStateOf<Teacher?>(null) }

    LaunchedEffect(showCreate, editingTeacher, deletingTeacher) {
        onModalOpenChanged(showCreate || editingTeacher != null || deletingTeacher != null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = if (isDirector) "Lehrer konfigurieren" else "Lehrer",
            subtitle = if (isDirector) "Du kannst nur Lehrkräfte deiner Schule erstellen und verwalten." else "Lehrer-Zugänge und deren Fächer verwalten.",
            action = {
                NexoraPrimaryButton(
                    text = "+ Lehrer",
                    onClick = { showCreate = true },
                    modifier = Modifier.width(120.dp),
                    testTag = "add_teacher_button"
                )
            }
        )

        if (teachers.isEmpty()) {
            NexoraNoticeBox("Keine Lehrer vorhanden.")
        } else {
            teachers.forEach { t ->
                val school = data.schools.find { it.id == t.schoolId }
                NexoraCard {
                    Text(t.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        "${school?.name.orEmpty()} · Kürzel: ${t.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexoraMuted
                    )
                    if (t.subject.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Fächer: ${t.subject}", style = MaterialTheme.typography.bodySmall, color = NexoraText)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NexoraSecondaryButton("Bearbeiten", onClick = { editingTeacher = t })
                        NexoraDangerButton("Löschen", onClick = { deletingTeacher = t })
                    }
                }
            }
        }
    }

    if (showCreate || editingTeacher != null) {
        val target = editingTeacher
        var schoolId by remember(target) {
            mutableStateOf(if (isDirector) session.schoolId else target?.schoolId ?: data.schools.firstOrNull()?.id.orEmpty())
        }
        var name by remember(target) { mutableStateOf(target?.name.orEmpty()) }
        var username by remember(target) { mutableStateOf(target?.username.orEmpty()) }
        var password by remember(target) { mutableStateOf("") }
        val selectedSubjects = remember(target) {
            mutableStateListOf<String>().apply {
                target?.subject?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.let { addAll(it) }
            }
        }
        val availableSubjects = remember(schoolId, data.schools) {
            NexoraHelpers.getSchoolSubjects(data.schools.find { it.id == schoolId })
        }

        AlertDialog(
            onDismissRequest = { showCreate = false; editingTeacher = null },
            containerColor = NexoraCard,
            title = { Text(if (target != null) "Lehrer bearbeiten" else "Neuer Lehrer", color = Color.White) },
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
                    }
                    NexoraFieldLabel("Name")
                    OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("z.B. Herr Müller", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Benutzername / Kürzel")
                    OutlinedTextField(value = username, onValueChange = { username = it }, placeholder = { Text("z.B. MUE", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Fächer (Mehrfachauswahl)")
                    NexoraCard(containerColor = NexoraCardElevated.copy(alpha = 0.5f), contentPadding = PaddingValues(8.dp)) {
                        Column(modifier = Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState())) {
                            availableSubjects.forEach { sub ->
                                val checked = selectedSubjects.contains(sub)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) selectedSubjects.remove(sub) else selectedSubjects.add(sub)
                                        }
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (it) { if (!selectedSubjects.contains(sub)) selectedSubjects.add(sub) }
                                            else selectedSubjects.remove(sub)
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = NexoraBlueBright)
                                    )
                                    Text(sub, style = MaterialTheme.typography.bodySmall, color = NexoraText)
                                }
                            }
                        }
                    }
                    NexoraFieldLabel(if (target != null) "Passwort (leer lassen zum Beibehalten)" else "Passwort")
                    OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Speichern",
                    enabled = schoolId.isNotBlank() && name.isNotBlank() && username.isNotBlank() && (target != null || password.isNotBlank()),
                    onClick = {
                        onSaveTeacher(target?.id, schoolId, name, username, selectedSubjects.joinToString(", "), password) {
                            showCreate = false; editingTeacher = null
                        }
                    },
                    modifier = Modifier.width(130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false; editingTeacher = null }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }

    deletingTeacher?.let { t ->
        NexoraConfirmDialog(
            title = "Lehrer löschen?",
            message = "Lehrer „${t.name}“ wirklich löschen?",
            onConfirm = {
                deletingTeacher = null
                onDeleteTeacher(t.id)
            },
            onDismiss = { deletingTeacher = null }
        )
    }
}

@Composable
fun DirectorsScreen(
    data: NexoraData,
    directors: List<SchoolDirector>,
    directorsError: String,
    onModalOpenChanged: (Boolean) -> Unit,
    onCreateDirector: (schoolId: String, name: String, password: String, onDone: () -> Unit) -> Unit,
    onDeleteDirector: (directorId: String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var deletingDirector by remember { mutableStateOf<SchoolDirector?>(null) }

    LaunchedEffect(showCreate, deletingDirector) {
        onModalOpenChanged(showCreate || deletingDirector != null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Schuldirektor",
            subtitle = "Direktor-Zugang für eine ausgewählte Schule erstellen und verwalten.",
            action = {
                NexoraPrimaryButton(
                    text = "+ Direktor",
                    enabled = data.schools.isNotEmpty(),
                    onClick = { showCreate = true },
                    modifier = Modifier.width(130.dp),
                    testTag = "add_director_button"
                )
            }
        )

        if (directorsError.isNotBlank()) {
            NexoraNoticeBox(directorsError)
        } else if (directors.isEmpty()) {
            NexoraNoticeBox("Noch keine Direktor-Konten.")
        } else {
            directors.forEach { d ->
                val schoolName = data.schools.find { it.id == d.schoolId }?.name ?: "Schule gelöscht"
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("$schoolName · ${d.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                        }
                        NexoraDangerButton("Löschen", onClick = { deletingDirector = d })
                    }
                }
            }
        }
    }

    if (showCreate) {
        var schoolId by remember { mutableStateOf(data.schools.firstOrNull()?.id.orEmpty()) }
        var name by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = NexoraCard,
            title = { Text("Schuldirektor erstellen", color = Color.White) },
            text = {
                Column {
                    NexoraDropdownSelector(
                        label = "Schule",
                        selectedText = data.schools.find { it.id == schoolId }?.name.orEmpty(),
                        items = data.schools,
                        itemLabel = { it.name + if (it.city.isNotBlank()) " · ${it.city}" else "" },
                        onSelect = { schoolId = it.id }
                    )
                    NexoraFieldLabel("Name / Anmeldename")
                    OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("z. B. Max Mustermann", color = NexoraMuted) }, singleLine = true, shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                    NexoraFieldLabel("Passwort (min. 8 Zeichen)")
                    OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(12.dp), colors = nexoraTextFieldColors(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                NexoraPrimaryButton(
                    text = "Erstellen",
                    enabled = schoolId.isNotBlank() && name.isNotBlank() && password.length >= 8,
                    onClick = {
                        onCreateDirector(schoolId, name, password) { showCreate = false }
                    },
                    modifier = Modifier.width(130.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Abbrechen", color = NexoraMuted) }
            }
        )
    }

    deletingDirector?.let { d ->
        NexoraConfirmDialog(
            title = "Direktor-Zugang löschen?",
            message = "Direktor-Zugang „${d.displayName}“ wirklich löschen?",
            onConfirm = {
                deletingDirector = null
                onDeleteDirector(d.id)
            },
            onDismiss = { deletingDirector = null }
        )
    }
}

@Composable
fun AttendanceReportsScreen(
    data: NexoraData,
    onModalOpenChanged: (Boolean) -> Unit,
    onUpdateAttendance: (id: String, date: String, timeIndex: Int, status: String, lateMinutes: Int, onDone: () -> Unit) -> Unit,
    onDeleteAttendance: (id: String) -> Unit
) {
    val allClassOptions = remember(data.schools) {
        listOf("" to "Alle Klassen") + data.schools.flatMap { s -> s.classes.map { c -> c.id to "${s.name} – ${c.name}" } }
    }
    var filterClassId by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<AttendanceRecord?>(null) }
    var deleting by remember { mutableStateOf<AttendanceRecord?>(null) }

    LaunchedEffect(editing, deleting) {
        onModalOpenChanged(editing != null || deleting != null)
    }

    val filtered = remember(data.attendance, filterClassId) {
        data.attendance
            .filter { it.status != "present" && (filterClassId.isBlank() || it.classId == filterClassId) }
            .sortedByDescending { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NexoraPageHeader(
            title = "Fehlzeiten & Krankheitsberichte",
            subtitle = "Übersicht aller gemeldeten Fehlzeiten, Krankmeldungen und Verspätungen."
        )

        NexoraDropdownSelector(
            label = "Klasse filtern",
            selectedText = allClassOptions.find { it.first == filterClassId }?.second ?: "Alle Klassen",
            items = allClassOptions,
            itemLabel = { it.second },
            onSelect = { filterClassId = it.first }
        )

        if (filtered.isEmpty()) {
            NexoraNoticeBox("Keine aktuellen Fehlzeiten gemeldet.")
        } else {
            filtered.forEach { a ->
                val cls = data.schools.flatMap { it.classes }.find { it.id == a.classId }
                val (badgeLabel, badgeTone) = when (a.status) {
                    "excused" -> "Entschuldigt (Krank)" to BadgeTone.ORANGE
                    "late" -> "Verspätet · ${a.lateMinutes} Min." to BadgeTone.GREEN
                    else -> "Fehlend" to BadgeTone.RED
                }
                NexoraCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.studentName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "${a.date} · ${NexoraConstants.TIMES.getOrElse(a.timeIndex - 1) { "" }} · Klasse ${cls?.name ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexoraMuted
                            )
                        }
                        NexoraBadge(badgeLabel, badgeTone)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NexoraSecondaryButton("Bearbeiten", onClick = { editing = a })
                        NexoraDangerButton("Löschen", onClick = { deleting = a })
                    }
                }
            }
        }
    }

    editing?.let { rec ->
        EditAttendanceDialog(
            record = rec,
            onDismiss = { editing = null },
            onSave = { date, timeIdx, status, mins ->
                onUpdateAttendance(rec.id, date, timeIdx, status, mins) { editing = null }
            }
        )
    }

    deleting?.let { rec ->
        NexoraConfirmDialog(
            title = "Fehlzeit löschen?",
            message = "Fehlzeit von ${rec.studentName} am ${rec.date} dauerhaft löschen?",
            onConfirm = {
                deleting = null
                onDeleteAttendance(rec.id)
            },
            onDismiss = { deleting = null }
        )
    }
}
