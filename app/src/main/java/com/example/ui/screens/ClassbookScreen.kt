package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceRecord
import com.example.data.ClassbookEntry
import com.example.data.NexoraConstants
import com.example.data.NexoraData
import com.example.data.SchoolClass
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
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraLine
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraText

private val ATTENDANCE_STATUS_OPTIONS = listOf(
    "present" to "Anwesend",
    "unexcused" to "Fehlend",
    "excused" to "Krank / entschuldigt",
    "late" to "Verspätet"
)

private val ENTRY_TYPES = listOf(
    "incident" to "Vorfall / Regelverstoß",
    "positive" to "Positive Beobachtung",
    "note" to "Sonstige Notiz"
)

@Composable
fun ClassbookScreen(
    session: UserSession,
    data: NexoraData,
    selectedClassId: String,
    selectedDate: String,
    classbookLoadError: String,
    onSelectClass: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onModalOpenChanged: (Boolean) -> Unit,
    onToast: (String) -> Unit,
    onSaveAttendance: (classId: String, date: String, timeIndex: Int, rows: List<Triple<String, String, Int>>) -> Unit,
    onUpdateAttendance: (id: String, date: String, timeIndex: Int, status: String, lateMinutes: Int, onDone: () -> Unit) -> Unit,
    onDeleteAttendance: (id: String) -> Unit,
    onSaveClassbookEntry: (studentId: String, entryType: String, title: String, description: String, date: String, onDone: () -> Unit) -> Unit,
    onUpdateClassbookEntry: (id: String, title: String, description: String, entryType: String, onDone: () -> Unit) -> Unit,
    onDeleteClassbookEntry: (id: String) -> Unit,
    onSaveGrade: (classId: String, studentName: String, subject: String, gradeValue: String, onDone: () -> Unit) -> Unit,
    onDeleteGrade: (id: String) -> Unit
) {
    val availableClasses = remember(session, data.schools) {
        if (session.type == UserRole.ADMIN) {
            data.schools.flatMap { sc -> sc.classes.map { c -> c to "${sc.name} · ${c.name}" } }
        } else {
            val s = data.schools.find { it.id == session.schoolId }
            s?.classes?.map { c -> c to c.name } ?: emptyList()
        }
    }

    val activeClassId = remember(availableClasses, selectedClassId) {
        availableClasses.find { it.first.id == selectedClassId }?.first?.id
            ?: availableClasses.firstOrNull()?.first?.id.orEmpty()
    }
    val activeClassLabel = availableClasses.find { it.first.id == activeClassId }?.second.orEmpty()

    val classStudents = remember(data.students, activeClassId, session) {
        data.students.filter {
            it.classId == activeClassId && (session.type == UserRole.ADMIN || it.schoolId == session.schoolId)
        }
    }
    val schoolStudents = remember(data.students, availableClasses, session) {
        val classMap = availableClasses.associate { it.first.id to it.second }
        data.students.filter { st ->
            classMap.containsKey(st.classId) && (session.type == UserRole.ADMIN || st.schoolId == session.schoolId)
        }.map { st -> st to "${classMap[st.classId]} · ${st.name}" }
    }

    var attTimeIndex by remember { mutableIntStateOf(1) }
    val statusMap = remember(activeClassId, selectedDate, data.attendance) {
        mutableStateMapOf<String, String>().apply {
            val todays = data.attendance.filter { it.classId == activeClassId && it.date == selectedDate }
            classStudents.forEach { st ->
                put(st.name, todays.find { it.studentName == st.name }?.status ?: "present")
            }
        }
    }
    val minutesMap = remember(activeClassId, selectedDate, data.attendance) {
        mutableStateMapOf<String, String>().apply {
            val todays = data.attendance.filter { it.classId == activeClassId && it.date == selectedDate }
            classStudents.forEach { st ->
                val mins = todays.find { it.studentName == st.name }?.lateMinutes ?: 0
                put(st.name, if (mins > 0) mins.toString() else "")
            }
        }
    }

    // Classbook entry form state
    var entryStudentId by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("incident") }
    var entryTitle by remember { mutableStateOf("") }
    var entryDescription by remember { mutableStateOf("") }

    // Grade form state
    var gradeStudent by remember { mutableStateOf("") }
    var gradeSubject by remember { mutableStateOf("") }
    var gradeValue by remember { mutableStateOf("") }

    var editingEntry by remember { mutableStateOf<ClassbookEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<ClassbookEntry?>(null) }
    var editingAttendance by remember { mutableStateOf<AttendanceRecord?>(null) }
    var deletingAttendance by remember { mutableStateOf<AttendanceRecord?>(null) }

    LaunchedEffect(editingEntry, deletingEntry, editingAttendance, deletingAttendance) {
        onModalOpenChanged(editingEntry != null || deletingEntry != null || editingAttendance != null || deletingAttendance != null)
    }

    val canManageRecord: (String) -> Boolean = { schoolId ->
        session.type == UserRole.ADMIN || (session.type == UserRole.DIRECTOR && session.schoolId == schoolId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        NexoraPageHeader(
            title = "Digitales Klassenbuch",
            subtitle = when (session.type) {
                UserRole.ADMIN -> "Administrator · Anwesenheit und Noten für alle Klassen verwalten."
                UserRole.DIRECTOR -> "Klassen, Anwesenheit, Einträge und Noten deiner Schule."
                else -> "Anwesenheit dokumentieren, Einträge & Noten verwalten."
            }
        )

        // Class & Date Selectors
        NexoraCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NexoraDropdownSelector(
                    label = "Klasse",
                    selectedText = activeClassLabel,
                    items = availableClasses,
                    itemLabel = { it.second },
                    onSelect = { onSelectClass(it.first.id) },
                    modifier = Modifier.weight(1f),
                    testTag = "cb_class_selector"
                )
                Column(modifier = Modifier.weight(1f)) {
                    NexoraFieldLabel("Datum (YYYY-MM-DD)")
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = onDateChange,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().testTag("cb_date_input")
                    )
                }
            }
        }

        // Attendance Checklist
        NexoraCard {
            Text("Anwesenheitsliste", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Spacer(Modifier.height(6.dp))
            NexoraDropdownSelector(
                label = "Unterrichtsstunde",
                selectedText = "$attTimeIndex. Stunde · ${NexoraConstants.TIMES.getOrElse(attTimeIndex - 1) { "" }}",
                items = NexoraConstants.TIMES.indices.map { it + 1 },
                itemLabel = { "$it. Stunde · ${NexoraConstants.TIMES[it - 1]}" },
                onSelect = { attTimeIndex = it }
            )
            Spacer(Modifier.height(10.dp))
            if (classStudents.isEmpty()) {
                NexoraNoticeBox("Keine Schüler in dieser Klasse hinterlegt. Der Administrator oder Direktor muss zuerst Schüler hinzufügen.")
            } else {
                Text(
                    "Für jeden Schüler Status auswählen. Bei „Verspätet“ die Minuten eintragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NexoraMuted
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    classStudents.forEach { st ->
                        val stStatus = statusMap[st.name] ?: "present"
                        val stMins = minutesMap[st.name] ?: ""
                        NexoraCard(containerColor = NexoraCardElevated.copy(alpha = 0.55f), contentPadding = PaddingValues(12.dp)) {
                            Text(st.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NexoraDropdownSelector(
                                    label = "",
                                    selectedText = ATTENDANCE_STATUS_OPTIONS.find { it.first == stStatus }?.second ?: "Anwesend",
                                    items = ATTENDANCE_STATUS_OPTIONS,
                                    itemLabel = { it.second },
                                    onSelect = { statusMap[st.name] = it.first },
                                    modifier = Modifier.weight(1.4f)
                                )
                                if (stStatus == "late") {
                                    OutlinedTextField(
                                        value = stMins,
                                        onValueChange = { minutesMap[st.name] = it },
                                        placeholder = { Text("Min.", color = NexoraMuted) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = nexoraTextFieldColors(),
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                NexoraPrimaryButton(
                    text = "Anwesenheit speichern",
                    onClick = {
                        val rows = mutableListOf<Triple<String, String, Int>>()
                        for (st in classStudents) {
                            val status = statusMap[st.name] ?: "present"
                            val mins = if (status == "late") {
                                val parsed = (minutesMap[st.name] ?: "").trim().toIntOrNull()
                                if (parsed == null || parsed < 1 || parsed > 600) {
                                    onToast("Bitte für ${st.name} eine Verspätung von 1 bis 600 Minuten eintragen.")
                                    return@NexoraPrimaryButton
                                }
                                parsed
                            } else 0
                            rows.add(Triple(st.name, status, mins))
                        }
                        onSaveAttendance(activeClassId, selectedDate, attTimeIndex, rows)
                    },
                    testTag = "save_attendance_button"
                )
            }
        }

        // Saved Absences & Lates (Teacher & Director)
        if (session.type == UserRole.TEACHER || session.type == UserRole.DIRECTOR) {
            val savedAbsences = remember(data.attendance, session) {
                data.attendance
                    .filter { (session.type == UserRole.ADMIN || it.schoolId == session.schoolId) && it.status != "present" }
                    .sortedByDescending { it.date }
            }
            NexoraCard {
                Text("Gespeicherte Fehlzeiten & Verspätungen", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Einträge deiner Schule verwalten und alte Datensätze entfernen.", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                Spacer(Modifier.height(12.dp))
                if (savedAbsences.isEmpty()) {
                    NexoraNoticeBox("Keine Fehlzeiten oder Verspätungen gespeichert.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedAbsences.forEach { a ->
                            val cls = data.schools.flatMap { it.classes }.find { it.id == a.classId }
                            val statusText = when (a.status) {
                                "late" -> "Verspätet · ${a.lateMinutes} Min."
                                "excused" -> "Krank / entschuldigt"
                                else -> "Fehlend"
                            }
                            NexoraCard(containerColor = NexoraCardElevated.copy(alpha = 0.55f), contentPadding = PaddingValues(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(a.studentName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        Text(
                                            "${a.date} · ${NexoraConstants.TIMES.getOrElse(a.timeIndex - 1) { "" }} · Klasse ${cls?.name ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NexoraMuted
                                        )
                                    }
                                    NexoraBadge(
                                        text = statusText,
                                        tone = when (a.status) {
                                            "late" -> BadgeTone.GREEN
                                            "excused" -> BadgeTone.ORANGE
                                            else -> BadgeTone.RED
                                        }
                                    )
                                }
                                if (canManageRecord(a.schoolId)) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        NexoraSecondaryButton("Bearbeiten", onClick = { editingAttendance = a })
                                        NexoraDangerButton("Löschen", onClick = { deletingAttendance = a })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Classbook Entry Form & List
        NexoraCard {
            Text("Eintrag ins digitale Klassenbuch", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "Dokumentiere Vorfälle, Regelverstöße oder wichtige Beobachtungen dauerhaft in Supabase.",
                style = MaterialTheme.typography.bodySmall,
                color = NexoraMuted
            )

            NexoraDropdownSelector(
                label = "Schüler",
                selectedText = schoolStudents.find { it.first.id == entryStudentId }?.second ?: "Schüler auswählen",
                items = schoolStudents,
                itemLabel = { it.second },
                onSelect = { entryStudentId = it.first.id }
            )
            NexoraDropdownSelector(
                label = "Art des Eintrags",
                selectedText = ENTRY_TYPES.find { it.first == entryType }?.second ?: "Vorfall / Regelverstoß",
                items = ENTRY_TYPES,
                itemLabel = { it.second },
                onSelect = { entryType = it.first }
            )
            NexoraFieldLabel("Titel")
            OutlinedTextField(
                value = entryTitle,
                onValueChange = { entryTitle = it },
                placeholder = { Text("z. B. Störung des Unterrichts", color = NexoraMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoraTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("cb_entry_title_input")
            )
            NexoraFieldLabel("Beschreibung")
            OutlinedTextField(
                value = entryDescription,
                onValueChange = { entryDescription = it },
                placeholder = { Text("Sachlich beschreiben, was passiert ist …", color = NexoraMuted) },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = nexoraTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("cb_entry_desc_input")
            )
            Spacer(Modifier.height(14.dp))
            NexoraPrimaryButton(
                text = "Eintrag dauerhaft speichern",
                enabled = entryStudentId.isNotBlank() && entryTitle.isNotBlank() && entryDescription.isNotBlank(),
                onClick = {
                    onSaveClassbookEntry(entryStudentId, entryType, entryTitle, entryDescription, selectedDate) {
                        entryTitle = ""
                        entryDescription = ""
                    }
                },
                testTag = "save_cb_entry_button"
            )

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = NexoraLine)
            Spacer(Modifier.height(14.dp))

            if (classbookLoadError.isNotBlank()) {
                NexoraNoticeBox("Klassenbucheinträge konnten nicht aus Supabase geladen werden: $classbookLoadError", isError = true)
                Spacer(Modifier.height(10.dp))
            }

            val entries = remember(data.classbookEntries, activeClassId, session) {
                data.classbookEntries.filter {
                    (activeClassId.isBlank() || it.classId == activeClassId) &&
                        (session.type == UserRole.ADMIN || it.schoolId == session.schoolId)
                }
            }
            if (entries.isEmpty()) {
                NexoraNoticeBox("Noch keine Klassenbucheinträge vorhanden.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    entries.forEach { entry ->
                        val cls = data.schools.flatMap { it.classes }.find { it.id == entry.classId }
                        val tag = when (entry.entryType) {
                            "incident" -> "Vorfall"
                            "positive" -> "Positive Beobachtung"
                            else -> "Notiz"
                        }
                        NexoraCard(containerColor = NexoraCardElevated.copy(alpha = 0.55f), contentPadding = PaddingValues(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${entry.studentName} · ${entry.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                NexoraBadge(
                                    text = tag,
                                    tone = if (entry.entryType == "incident") BadgeTone.RED else BadgeTone.ORANGE
                                )
                            }
                            Text(
                                "${cls?.name.orEmpty()} · ${entry.date} · ${entry.author.ifBlank { "Lehrkraft" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexoraMuted
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = NexoraText)
                            if (canManageRecord(entry.schoolId)) {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NexoraSecondaryButton("Bearbeiten", onClick = { editingEntry = entry })
                                    NexoraDangerButton("Endgültig löschen", onClick = { deletingEntry = entry })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Grades Entry & List
        NexoraCard {
            Text("Noten eintragen", style = MaterialTheme.typography.titleLarge, color = Color.White)
            NexoraFieldLabel("Schülername")
            OutlinedTextField(
                value = gradeStudent,
                onValueChange = { gradeStudent = it },
                placeholder = { Text("z.B. Max Mustermann", color = NexoraMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = nexoraTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("grade_student_input")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NexoraFieldLabel("Fach")
                    OutlinedTextField(
                        value = gradeSubject,
                        onValueChange = { gradeSubject = it },
                        placeholder = { Text("z.B. Mathematik", color = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    NexoraFieldLabel("Note / Punkte")
                    OutlinedTextField(
                        value = gradeValue,
                        onValueChange = { gradeValue = it },
                        placeholder = { Text("z.B. 2+ oder 12", color = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors()
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            NexoraPrimaryButton(
                text = "Note speichern",
                enabled = activeClassId.isNotBlank() && gradeStudent.isNotBlank() && gradeSubject.isNotBlank() && gradeValue.isNotBlank(),
                onClick = {
                    onSaveGrade(activeClassId, gradeStudent, gradeSubject, gradeValue) {
                        gradeStudent = ""
                        gradeValue = ""
                    }
                },
                testTag = "save_grade_button"
            )

            Spacer(Modifier.height(16.dp))
            Text("Bereits eingetragene Noten", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            val grades = remember(data.grades, activeClassId, session) {
                data.grades.filter {
                    (activeClassId.isBlank() || it.classId == activeClassId) &&
                        (session.type == UserRole.ADMIN || it.schoolId == session.schoolId)
                }
            }
            if (grades.isEmpty()) {
                NexoraNoticeBox("Keine Noten für diese Klasse eingetragen.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    grades.forEach { g ->
                        NexoraCard(containerColor = NexoraCardElevated.copy(alpha = 0.55f), contentPadding = PaddingValues(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(g.studentName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("${g.subject} · ${g.gradeType}", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NexoraBadge(g.gradeValue, BadgeTone.ORANGE)
                                    NexoraDangerButton("Löschen", onClick = { onDeleteGrade(g.id) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        EditClassbookEntryDialog(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { title, desc, type ->
                onUpdateClassbookEntry(entry.id, title, desc, type) { editingEntry = null }
            }
        )
    }

    deletingEntry?.let { entry ->
        NexoraConfirmDialog(
            title = "Eintrag löschen?",
            message = "Diesen Klassenbucheintrag dauerhaft aus Supabase löschen?",
            confirmLabel = "Endgültig löschen",
            onConfirm = {
                val id = entry.id
                deletingEntry = null
                onDeleteClassbookEntry(id)
            },
            onDismiss = { deletingEntry = null }
        )
    }

    editingAttendance?.let { rec ->
        EditAttendanceDialog(
            record = rec,
            onDismiss = { editingAttendance = null },
            onSave = { date, timeIdx, status, mins ->
                onUpdateAttendance(rec.id, date, timeIdx, status, mins) { editingAttendance = null }
            }
        )
    }

    deletingAttendance?.let { rec ->
        NexoraConfirmDialog(
            title = "Fehlzeit löschen?",
            message = "Fehlzeit von ${rec.studentName} am ${rec.date} dauerhaft löschen?",
            onConfirm = {
                val id = rec.id
                deletingAttendance = null
                onDeleteAttendance(id)
            },
            onDismiss = { deletingAttendance = null }
        )
    }
}

@Composable
fun EditClassbookEntryDialog(
    entry: ClassbookEntry,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, entryType: String) -> Unit
) {
    var title by remember { mutableStateOf(entry.title) }
    var description by remember { mutableStateOf(entry.description) }
    var type by remember { mutableStateOf(entry.entryType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Column {
                Text("Klassenbucheintrag bearbeiten", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${entry.studentName} · ${entry.date}", color = NexoraMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NexoraDropdownSelector(
                    label = "Art des Eintrags",
                    selectedText = ENTRY_TYPES.find { it.first == type }?.second ?: "Vorfall / Regelverstoß",
                    items = ENTRY_TYPES,
                    itemLabel = { it.second },
                    onSelect = { type = it.first }
                )
                NexoraFieldLabel("Titel")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                NexoraFieldLabel("Beschreibung")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = "Speichern",
                enabled = title.isNotBlank() && description.isNotBlank(),
                onClick = { onSave(title, description, type) },
                modifier = Modifier.width(130.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )
}

@Composable
fun EditAttendanceDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onSave: (date: String, timeIndex: Int, status: String, lateMinutes: Int) -> Unit
) {
    var date by remember { mutableStateOf(record.date) }
    var timeIndex by remember { mutableIntStateOf(record.timeIndex.coerceIn(1, NexoraConstants.TIMES.size)) }
    var status by remember { mutableStateOf(record.status) }
    var minutesStr by remember { mutableStateOf(if (record.lateMinutes > 0) record.lateMinutes.toString() else "") }
    val validLate = status != "late" || (minutesStr.toIntOrNull()?.let { it in 1..600 } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Column {
                Text("Fehlzeit / Verspätung bearbeiten", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text(record.studentName, color = NexoraMuted, style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NexoraFieldLabel("Datum (YYYY-MM-DD)")
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                NexoraDropdownSelector(
                    label = "Unterrichtsstunde",
                    selectedText = "$timeIndex. Stunde · ${NexoraConstants.TIMES.getOrElse(timeIndex - 1) { "" }}",
                    items = NexoraConstants.TIMES.indices.map { it + 1 },
                    itemLabel = { "$it. Stunde · ${NexoraConstants.TIMES[it - 1]}" },
                    onSelect = { timeIndex = it }
                )
                NexoraDropdownSelector(
                    label = "Status",
                    selectedText = ATTENDANCE_STATUS_OPTIONS.find { it.first == status }?.second ?: "Fehlend",
                    items = ATTENDANCE_STATUS_OPTIONS,
                    itemLabel = { it.second },
                    onSelect = { status = it.first }
                )
                if (status == "late") {
                    NexoraFieldLabel("Verspätung in Minuten (1–600)")
                    OutlinedTextField(
                        value = minutesStr,
                        onValueChange = { minutesStr = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = "Speichern",
                enabled = date.isNotBlank() && validLate,
                onClick = {
                    val mins = if (status == "late") minutesStr.toIntOrNull() ?: 0 else 0
                    onSave(date, timeIndex, status, mins)
                },
                modifier = Modifier.width(130.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )
}
