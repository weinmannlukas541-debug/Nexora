package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewDay
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CalendarEvent
import com.example.data.Homework
import com.example.data.Lesson
import com.example.data.NexoraConstants
import com.example.data.NexoraData
import com.example.data.NexoraHelpers
import com.example.data.School
import com.example.data.UserRole
import com.example.data.UserSession
import com.example.data.WeekOverride
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
import com.example.ui.theme.NexoraBlueLight
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraGreen
import com.example.ui.theme.NexoraGreenLight
import com.example.ui.theme.NexoraLine
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraOrange
import com.example.ui.theme.NexoraOrangeLight
import com.example.ui.theme.NexoraRed
import com.example.ui.theme.NexoraRedLight
import com.example.ui.theme.NexoraText

private sealed interface TimetableModal {
    data class StudentDetail(val school: School, val lesson: Lesson) : TimetableModal
    data class TeacherEdit(val school: School, val lesson: Lesson) : TimetableModal
    data class AdminOrDirectorEdit(
        val school: School,
        val lesson: Lesson?,
        val day: Int,
        val timeIndex: Int
    ) : TimetableModal
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
    session: UserSession,
    data: NexoraData,
    weekOffset: Int,
    selectedSchoolId: String,
    selectedClassId: String,
    isTeacherHomeworkMode: Boolean = false,
    onSelectSchool: (String) -> Unit,
    onSelectClass: (String) -> Unit,
    onChangeWeek: (Int) -> Unit,
    onModalOpenChanged: (Boolean) -> Unit,
    onSaveTeacherLesson: (lessonId: String, week: String, isExam: Boolean, hwText: String, hwDate: String, onDone: () -> Unit) -> Unit,
    onSaveAdminLesson: (
        schoolId: String,
        lessonId: String?,
        classId: String,
        day: Int,
        timeIndex: Int,
        subject: String,
        teacher: String,
        room: String,
        colorStyle: String,
        isExam: Boolean,
        week: String,
        status: String,
        subTeacher: String,
        subRoom: String,
        hwText: String,
        hwDate: String,
        existingHwId: String,
        existingOv: WeekOverride?,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteLesson: (lessonId: String, onDone: () -> Unit) -> Unit
) {
    val weekKey = remember(weekOffset) { NexoraHelpers.getWeekKey(weekOffset) }
    val weekLabel = remember(weekOffset) { NexoraHelpers.getWeekDisplayLabel(weekOffset) }
    val activeSchool = remember(session, data.schools, selectedSchoolId) {
        when (session.type) {
            UserRole.ADMIN -> data.schools.find { it.id == selectedSchoolId } ?: data.schools.firstOrNull()
            else -> data.schools.find { it.id == session.schoolId }
        }
    }
    val activeClassId = remember(session, activeSchool, selectedClassId) {
        when (session.type) {
            UserRole.CLASS -> session.classId
            else -> activeSchool?.classes?.find { it.id == selectedClassId }?.id
                ?: activeSchool?.classes?.firstOrNull()?.id ?: ""
        }
    }
    val activeClass = activeSchool?.classes?.find { it.id == activeClassId }

    var activeModal by remember { mutableStateOf<TimetableModal?>(null) }
    LaunchedEffect(activeModal) { onModalOpenChanged(activeModal != null) }

    val (weekStart, weekEnd) = remember(weekKey) { NexoraHelpers.getWeekBounds(weekKey) }
    val weekEvents = remember(data.calendar, activeSchool?.id, weekStart, weekEnd) {
        if (activeSchool == null) emptyList()
        else data.calendar.filter {
            it.schoolId == activeSchool.id && it.date.isNotBlank() && it.date >= weekStart && it.date <= weekEnd
        }.sortedBy { it.date }
    }

    val title = when {
        isTeacherHomeworkMode -> "Hausaufgaben & Arbeiten verwalten"
        session.type == UserRole.ADMIN -> "Stundenplan verwalten"
        session.type == UserRole.CLASS -> "Mein Stundenplan"
        else -> "Stundenplan"
    }
    val subtitle = when {
        isTeacherHomeworkMode -> "${activeSchool?.name.orEmpty()} · Wähle deine Stunde, um Aufgaben einzutragen."
        session.type == UserRole.ADMIN -> "Klicke auf eine Stunde zum Bearbeiten oder Hinzufügen."
        session.type == UserRole.DIRECTOR -> "Stundenplan deiner Schule ansehen und bearbeiten."
        session.type == UserRole.TEACHER -> "${activeSchool?.name.orEmpty()} · Stundenplan der ausgewählten Klasse"
        else -> "${activeSchool?.name.orEmpty()} · Klasse ${activeClass?.name.orEmpty()}"
    }

    val canEdit = session.type != UserRole.CLASS
    val canCreateNew = session.type == UserRole.ADMIN || session.type == UserRole.DIRECTOR
    val activeTeacher = remember(session, data.teachers) {
        if (session.type == UserRole.TEACHER) data.teachers.find { it.id == session.teacherId } else null
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 700.dp
        var useGridView by remember(isWide) { mutableStateOf(isWide) }
        var selectedDayIndex by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            NexoraPageHeader(title = title, subtitle = subtitle)

            // School / Class selectors
            if (session.type == UserRole.ADMIN) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NexoraDropdownSelector(
                        label = "Schule",
                        selectedText = activeSchool?.name.orEmpty(),
                        items = data.schools,
                        itemLabel = { it.name },
                        onSelect = { onSelectSchool(it.id) },
                        modifier = Modifier.weight(1f),
                        testTag = "admin_school_selector"
                    )
                    NexoraDropdownSelector(
                        label = "Klasse",
                        selectedText = activeClass?.name.orEmpty(),
                        items = activeSchool?.classes.orEmpty(),
                        itemLabel = { it.name },
                        onSelect = { onSelectClass(it.id) },
                        modifier = Modifier.weight(1f),
                        testTag = "admin_class_selector"
                    )
                }
                Spacer(Modifier.height(12.dp))
            } else if (session.type == UserRole.DIRECTOR || session.type == UserRole.TEACHER) {
                NexoraDropdownSelector(
                    label = "Klasse auswählen",
                    selectedText = activeClass?.name.orEmpty(),
                    items = activeSchool?.classes.orEmpty(),
                    itemLabel = { it.name },
                    onSelect = { onSelectClass(it.id) },
                    modifier = Modifier.widthIn(max = 320.dp),
                    testTag = "timetable_class_selector"
                )
                Spacer(Modifier.height(12.dp))
            }

            // Week toolbar + view mode toggle
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NexoraSecondaryButton(
                    text = "← Vorherige",
                    onClick = { onChangeWeek(-1) },
                    testTag = "prev_week_button"
                )
                NexoraSecondaryButton(
                    text = "Aktuelle Woche",
                    onClick = { onChangeWeek(0) },
                    testTag = "current_week_button"
                )
                NexoraSecondaryButton(
                    text = "Nächste →",
                    onClick = { onChangeWeek(1) },
                    testTag = "next_week_button"
                )
                FilterChip(
                    selected = !useGridView,
                    onClick = { useGridView = false },
                    label = { Text("Tagesansicht") },
                    leadingIcon = { Icon(Icons.Rounded.CalendarViewDay, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraBlueBright.copy(alpha = 0.22f),
                        selectedLabelColor = NexoraBlueLight
                    )
                )
                FilterChip(
                    selected = useGridView,
                    onClick = { useGridView = true },
                    label = { Text("Wochenraster") },
                    leadingIcon = { Icon(Icons.Rounded.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoraBlueBright.copy(alpha = 0.22f),
                        selectedLabelColor = NexoraBlueLight
                    )
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = weekLabel,
                style = MaterialTheme.typography.titleSmall,
                color = NexoraBlueLight
            )
            Spacer(Modifier.height(14.dp))

            if (weekEvents.isNotEmpty()) {
                WeekEventsBanner(events = weekEvents)
                Spacer(Modifier.height(14.dp))
            }

            if (activeSchool == null || activeSchool.classes.isEmpty() || activeClassId.isBlank()) {
                NexoraNoticeBox("Keine Klasse vorhanden.")
            } else {
                val classLessons = remember(activeSchool, activeClassId) {
                    activeSchool.lessons.filter { it.classId == activeClassId }
                }

                val onLessonTap: (Lesson) -> Unit = { lesson ->
                    val lessonEditable = canEdit && when (session.type) {
                        UserRole.ADMIN, UserRole.DIRECTOR -> true
                        UserRole.TEACHER -> NexoraHelpers.teacherOwnsLesson(lesson, activeTeacher)
                        UserRole.CLASS -> false
                    }
                    activeModal = when {
                        session.type == UserRole.TEACHER && (lessonEditable || isTeacherHomeworkMode) ->
                            TimetableModal.TeacherEdit(activeSchool, lesson)
                        lessonEditable && (session.type == UserRole.ADMIN || session.type == UserRole.DIRECTOR) ->
                            TimetableModal.AdminOrDirectorEdit(activeSchool, lesson, lesson.day, lesson.time)
                        else -> TimetableModal.StudentDetail(activeSchool, lesson)
                    }
                }

                val onEmptyTap: (Int, Int) -> Unit = { day, timeIndex ->
                    if (canCreateNew) {
                        activeModal = TimetableModal.AdminOrDirectorEdit(activeSchool, null, day, timeIndex)
                    }
                }

                if (useGridView) {
                    WeeklyTimetableGrid(
                        lessons = classLessons,
                        overrides = data.weekOverrides,
                        weekKey = weekKey,
                        canCreateNew = canCreateNew,
                        onLessonTap = onLessonTap,
                        onEmptyTap = onEmptyTap
                    )
                } else {
                    DailyTimetableView(
                        lessons = classLessons,
                        overrides = data.weekOverrides,
                        weekKey = weekKey,
                        selectedDay = selectedDayIndex,
                        onSelectDay = { selectedDayIndex = it },
                        canCreateNew = canCreateNew,
                        onLessonTap = onLessonTap,
                        onEmptyTap = onEmptyTap
                    )
                }
            }
        }
    }

    when (val m = activeModal) {
        is TimetableModal.StudentDetail -> StudentLessonDetailDialog(
            lesson = m.lesson,
            override = NexoraHelpers.lessonState(m.lesson, data.weekOverrides, weekKey),
            homeworkList = data.homework,
            onDismiss = { activeModal = null }
        )

        is TimetableModal.TeacherEdit -> TeacherLessonDialog(
            school = m.school,
            lesson = m.lesson,
            teacher = activeTeacher,
            weekKey = weekKey,
            override = data.weekOverrides.find { it.lessonId == m.lesson.id && it.week == weekKey },
            homeworkList = data.homework,
            onDismiss = { activeModal = null },
            onSave = { isExam, hwText, hwDate ->
                onSaveTeacherLesson(m.lesson.id, weekKey, isExam, hwText, hwDate) {
                    activeModal = null
                }
            }
        )

        is TimetableModal.AdminOrDirectorEdit -> AdminLessonDialog(
            school = m.school,
            lesson = m.lesson,
            defaultClassId = activeClassId,
            defaultDay = m.day,
            defaultTimeIndex = m.timeIndex,
            weekKey = weekKey,
            override = m.lesson?.let { l -> data.weekOverrides.find { it.lessonId == l.id && it.week == weekKey } },
            homeworkList = data.homework,
            onDismiss = { activeModal = null },
            onDelete = { lessonId ->
                onDeleteLesson(lessonId) { activeModal = null }
            },
            onSave = { cid, day, time, subj, teacher, room, color, isExam, status, subT, subR, hwText, hwDate, existingHwId, existingOv ->
                onSaveAdminLesson(
                    m.school.id, m.lesson?.id, cid, day, time, subj, teacher, room, color,
                    isExam, weekKey, status, subT, subR, hwText, hwDate, existingHwId, existingOv
                ) { activeModal = null }
            }
        )
        null -> Unit
    }
}

@Composable
private fun WeekEventsBanner(events: List<CalendarEvent>) {
    NexoraCard(
        containerColor = NexoraOrange.copy(alpha = 0.08f),
        borderColor = NexoraOrange.copy(alpha = 0.3f),
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = "Schulveranstaltungen diese Woche",
            style = MaterialTheme.typography.titleSmall,
            color = NexoraOrangeLight
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            events.forEach { ev ->
                Surface(
                    color = NexoraBlueBright.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NexoraBlueLight.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "📅 ${NexoraHelpers.formatGermanDate(ev.date)} · ${ev.title}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        if (ev.text.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(ev.text, style = MaterialTheme.typography.bodySmall, color = NexoraText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyTimetableView(
    lessons: List<Lesson>,
    overrides: List<WeekOverride>,
    weekKey: String,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    canCreateNew: Boolean,
    onLessonTap: (Lesson) -> Unit,
    onEmptyTap: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NexoraConstants.DAYS.forEachIndexed { idx, dayName ->
            val selected = idx == selectedDay
            FilterChip(
                selected = selected,
                onClick = { onSelectDay(idx) },
                label = { Text(dayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NexoraBlueBright,
                    selectedLabelColor = Color.White,
                    containerColor = NexoraCard,
                    labelColor = NexoraMuted
                )
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    val dayLessons = remember(lessons, selectedDay) { lessons.filter { it.day == selectedDay } }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NexoraConstants.TIMES.forEachIndexed { timeIdx, timeLabel ->
            val atSlot = dayLessons.filter { it.time == timeIdx }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NexoraCardElevated.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NexoraLine),
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("${timeIdx + 1}. Std", style = MaterialTheme.typography.labelSmall, color = NexoraBlueLight)
                        Text(timeLabel, style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                    }
                }

                if (atSlot.isEmpty()) {
                    Surface(
                        color = NexoraCard.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, NexoraLine),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 60.dp)
                            .let {
                                if (canCreateNew) it.clickable { onEmptyTap(selectedDay, timeIdx) } else it
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (canCreateNew) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Stunde hinzufügen", tint = NexoraBlueLight, modifier = Modifier.size(18.dp))
                                    Text("Stunde hinzufügen", style = MaterialTheme.typography.bodySmall, color = NexoraBlueLight)
                                }
                            } else {
                                Text("Freistunde", style = MaterialTheme.typography.bodySmall, color = NexoraMuted.copy(alpha = 0.6f))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        atSlot.take(2).forEach { lesson ->
                            LessonCard(
                                lesson = lesson,
                                override = NexoraHelpers.lessonState(lesson, overrides, weekKey),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 68.dp),
                                onClick = { onLessonTap(lesson) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTimetableGrid(
    lessons: List<Lesson>,
    overrides: List<WeekOverride>,
    weekKey: String,
    canCreateNew: Boolean,
    onLessonTap: (Lesson) -> Unit,
    onEmptyTap: (Int, Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val slotHeight = 76.dp
    val dayColWidth = 132.dp
    val timeColWidth = 54.dp

    NexoraCard(contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // Header row
            Row(
                modifier = Modifier
                    .background(NexoraCardElevated)
                    .height(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.width(timeColWidth).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Zeit", style = MaterialTheme.typography.labelSmall, color = NexoraMuted)
                }
                NexoraConstants.DAYS.forEach { day ->
                    Box(
                        modifier = Modifier
                            .width(dayColWidth)
                            .fillMaxHeight()
                            .border(0.5.dp, NexoraLine),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, style = MaterialTheme.typography.labelMedium, color = NexoraMuted)
                    }
                }
            }

            // Grid Body: Left time column + 5 day columns with merged spans
            Row {
                Column(modifier = Modifier.width(timeColWidth)) {
                    NexoraConstants.TIMES.forEach { time ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(slotHeight)
                                .background(NexoraCardElevated.copy(alpha = 0.4f))
                                .border(0.5.dp, NexoraLine),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(time, style = MaterialTheme.typography.labelSmall, color = NexoraMuted)
                        }
                    }
                }

                for (day in 0..4) {
                    val dayLessons = lessons.filter { it.day == day }
                    val used = HashSet<Int>()
                    Column(modifier = Modifier.width(dayColWidth)) {
                        var ti = 0
                        while (ti < NexoraConstants.TIMES.size) {
                            if (used.contains(ti)) {
                                ti++
                                continue
                            }
                            val at = dayLessons.filter { it.time == ti }
                            if (at.isNotEmpty()) {
                                val first = at[0]
                                var span = 1
                                if (at.size == 1) {
                                    while (ti + span < NexoraConstants.TIMES.size) {
                                        val next = dayLessons.filter { it.time == ti + span }
                                        if (next.size == 1 && NexoraHelpers.sameLesson(first, next[0], overrides, weekKey)) {
                                            span++
                                        } else break
                                    }
                                }
                                for (x in 0 until span) used.add(ti + x)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(slotHeight * span)
                                        .border(0.5.dp, NexoraLine)
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    at.take(2).forEach { l ->
                                        LessonCard(
                                            lesson = l,
                                            override = NexoraHelpers.lessonState(l, overrides, weekKey),
                                            compact = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onClick = { onLessonTap(l) }
                                        )
                                    }
                                }
                                ti += span
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(slotHeight)
                                        .border(0.5.dp, NexoraLine)
                                        .let {
                                            if (canCreateNew) it.clickable { onEmptyTap(day, ti) } else it
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (canCreateNew) {
                                        Text("+", color = NexoraMuted.copy(alpha = 0.6f), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                ti++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    override: WeekOverride,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val cancelled = override.status == "cancelled"
    val isSub = override.status == "substitution"
    val teacher = if (isSub && override.subTeacher.isNotBlank()) override.subTeacher else lesson.teacher
    val room = if (isSub && override.subRoom.isNotBlank()) override.subRoom else lesson.room
    val isExam = override.isExam ?: lesson.isExam
    val hasHw = override.homeworkId.isNotBlank()

    val preset = NexoraConstants.resolveColorPreset(lesson.colorStyle)
    val bgColor = when {
        cancelled -> NexoraRed.copy(alpha = 0.16f)
        isSub -> NexoraGreen.copy(alpha = 0.16f)
        else -> preset.container
    }
    val accentColor = when {
        cancelled -> NexoraRed
        isSub -> NexoraGreen
        else -> preset.accent
    }
    val textColor = when {
        cancelled -> NexoraRedLight
        isSub -> NexoraGreenLight
        else -> preset.text
    }

    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .then(
                if (isExam) Modifier.border(2.dp, NexoraOrange, shape)
                else Modifier.border(0.5.dp, accentColor.copy(alpha = 0.35f), shape)
            )
            .clickable(onClick = onClick)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = lesson.subject,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val subLine = buildString {
                append(teacher)
                if (room.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(room)
                }
            }
            if (subLine.isNotBlank()) {
                Text(
                    text = subLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (cancelled || isSub || isExam) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (cancelled) NexoraBadge("Entf.", BadgeTone.RED)
                    else if (isSub) NexoraBadge("Vertr.", BadgeTone.GREEN)
                    if (isExam) NexoraBadge("Arbeit", BadgeTone.ORANGE)
                }
            }
        }

        if (hasHw) {
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.HomeWork,
                    contentDescription = "Hausaufgabe vorhanden",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StudentLessonDetailDialog(
    lesson: Lesson,
    override: WeekOverride,
    homeworkList: List<Homework>,
    onDismiss: () -> Unit
) {
    val teacher = if (override.status == "substitution" && override.subTeacher.isNotBlank()) override.subTeacher else lesson.teacher
    val room = if (override.status == "substitution" && override.subRoom.isNotBlank()) override.subRoom else lesson.room
    val isExam = override.isExam ?: lesson.isExam
    val hw = override.homeworkId.takeIf { it.isNotBlank() }?.let { id -> homeworkList.find { it.id == id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Column {
                Text(lesson.subject, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Stunden-Details für die ausgewählte Woche", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(12.dp)) {
                    Text("STATUS", style = MaterialTheme.typography.labelSmall, color = NexoraMuted)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (override.status) {
                            "cancelled" -> {
                                Text("Diese Stunde fällt aus.", color = Color.White, fontWeight = FontWeight.SemiBold)
                                NexoraBadge("Entfällt", BadgeTone.RED)
                            }
                            "substitution" -> {
                                Text("Vertretungsstunde", color = Color.White, fontWeight = FontWeight.SemiBold)
                                NexoraBadge("Vertretung", BadgeTone.GREEN)
                            }
                            else -> Text("Regulärer Unterricht", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(12.dp)) {
                    Text("LEHRKRAFT & RAUM", style = MaterialTheme.typography.labelSmall, color = NexoraMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append(teacher.ifBlank { "-" })
                            if (room.isNotBlank()) append(" · Raum $room")
                        },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isExam) {
                    NexoraNoticeBox("KLASSENARBEIT: In dieser Woche wird eine Klassenarbeit geschrieben!")
                }

                NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(12.dp)) {
                    Text("HAUSAUFGABE (FÜR DIESE WOCHE)", style = MaterialTheme.typography.labelSmall, color = NexoraMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hw != null) {
                            hw.text + if (hw.date.isNotBlank()) " (Fällig: ${hw.date})" else ""
                        } else {
                            "Keine Hausaufgabe für diese Woche hinterlegt."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            NexoraPrimaryButton(text = "Schließen", onClick = onDismiss, modifier = Modifier.width(130.dp))
        }
    )
}

@Composable
private fun TeacherLessonDialog(
    school: School,
    lesson: Lesson,
    teacher: com.example.data.Teacher?,
    weekKey: String,
    override: WeekOverride?,
    homeworkList: List<Homework>,
    onDismiss: () -> Unit,
    onSave: (isExam: Boolean, hwText: String, hwDate: String) -> Unit
) {
    val lessonAssigned = NexoraHelpers.teacherOwnsLesson(lesson, teacher)
    val subjectAllowed = NexoraHelpers.teacherAllowsSubject(lesson, teacher)

    if (!lessonAssigned || !subjectAllowed) {
        val reason = if (!lessonAssigned) {
            "Diese Stunde ist ${lesson.teacher.ifBlank { "einer anderen Lehrkraft" }} zugewiesen. Du kannst nur deine eigenen Stunden bearbeiten."
        } else {
            "Du unterrichtest das Fach ${lesson.subject} nicht."
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = NexoraCard,
            title = { Text("Keine Berechtigung", color = Color.White) },
            text = { NexoraNoticeBox(reason) },
            confirmButton = {
                NexoraPrimaryButton(text = "Schließen", onClick = onDismiss, modifier = Modifier.width(130.dp))
            }
        )
        return
    }

    val existingHw = override?.homeworkId?.takeIf { it.isNotBlank() }?.let { id -> homeworkList.find { it.id == id } }
    var isExam by remember { mutableStateOf(override?.isExam ?: lesson.isExam) }
    var hwText by remember { mutableStateOf(existingHw?.text.orEmpty()) }
    var hwDate by remember { mutableStateOf(existingHw?.date.orEmpty()) }
    val className = school.classes.find { it.id == lesson.classId }?.name ?: "-"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Column {
                Text("Stunde bearbeiten: ${lesson.subject}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Lehrer-Bereich: Hausaufgabe & Klassenarbeit ($weekKey)", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NexoraCard(containerColor = NexoraCardElevated, contentPadding = PaddingValues(12.dp)) {
                    Text("Klasse: $className", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text("Lehrkraft: ${lesson.teacher} | Raum: ${lesson.room.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = NexoraMuted)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isExam = !isExam }
                ) {
                    Checkbox(
                        checked = isExam,
                        onCheckedChange = { isExam = it },
                        colors = CheckboxDefaults.colors(checkedColor = NexoraOrange)
                    )
                    Text("Als Klassenarbeit markieren (für diese Woche)", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                NexoraFieldLabel("Hausaufgabe für diese Woche")
                OutlinedTextField(
                    value = hwText,
                    onValueChange = { hwText = it },
                    placeholder = { Text("z.B. S. 42 Nr. 5", color = NexoraMuted) },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                NexoraFieldLabel("Fällig am (Datum YYYY-MM-DD)")
                OutlinedTextField(
                    value = hwDate,
                    onValueChange = { hwDate = it },
                    placeholder = { Text("2026-11-30", color = NexoraMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = "Speichern",
                onClick = { onSave(isExam, hwText, hwDate) },
                modifier = Modifier.width(130.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )
}

@Composable
private fun AdminLessonDialog(
    school: School,
    lesson: Lesson?,
    defaultClassId: String,
    defaultDay: Int,
    defaultTimeIndex: Int,
    weekKey: String,
    override: WeekOverride?,
    homeworkList: List<Homework>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onSave: (
        classId: String,
        day: Int,
        timeIndex: Int,
        subject: String,
        teacher: String,
        room: String,
        colorStyle: String,
        isExam: Boolean,
        status: String,
        subTeacher: String,
        subRoom: String,
        hwText: String,
        hwDate: String,
        existingHwId: String,
        existingOv: WeekOverride?
    ) -> Unit
) {
    var classId by remember { mutableStateOf(lesson?.classId ?: defaultClassId) }
    var day by remember { mutableIntStateOf(lesson?.day ?: defaultDay) }
    var timeIndex by remember { mutableIntStateOf(lesson?.time ?: defaultTimeIndex) }
    var subject by remember { mutableStateOf(lesson?.subject.orEmpty()) }
    var teacher by remember { mutableStateOf(lesson?.teacher.orEmpty()) }
    var room by remember { mutableStateOf(lesson?.room.orEmpty()) }
    var isExam by remember { mutableStateOf(override?.isExam ?: lesson?.isExam ?: false) }
    var colorPreset by remember {
        mutableStateOf(NexoraConstants.resolveColorPreset(lesson?.colorStyle ?: NexoraConstants.DEFAULT_COLOR_CSS))
    }
    var status by remember { mutableStateOf(override?.status ?: "normal") }
    var subTeacher by remember { mutableStateOf(override?.subTeacher.orEmpty()) }
    var subRoom by remember { mutableStateOf(override?.subRoom.orEmpty()) }

    val existingHwId = override?.homeworkId.orEmpty()
    val existingHw = existingHwId.takeIf { it.isNotBlank() }?.let { id -> homeworkList.find { it.id == id } }
    var hwText by remember { mutableStateOf(existingHw?.text.orEmpty()) }
    var hwDate by remember { mutableStateOf(existingHw?.date.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val subjects = remember(school) { NexoraHelpers.getSchoolSubjects(school) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NexoraCard,
        title = {
            Text(
                if (lesson != null) "Stunde bearbeiten" else "Stunde hinzufügen",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                NexoraDropdownSelector(
                    label = "Klasse",
                    selectedText = school.classes.find { it.id == classId }?.name.orEmpty(),
                    items = school.classes,
                    itemLabel = { it.name },
                    onSelect = { classId = it.id }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NexoraDropdownSelector(
                        label = "Tag",
                        selectedText = NexoraConstants.DAYS.getOrElse(day) { "Montag" },
                        items = NexoraConstants.DAYS.indices.toList(),
                        itemLabel = { NexoraConstants.DAYS[it] },
                        onSelect = { day = it },
                        modifier = Modifier.weight(1f)
                    )
                    NexoraDropdownSelector(
                        label = "Zeit",
                        selectedText = NexoraConstants.TIMES.getOrElse(timeIndex) { "08:00" },
                        items = NexoraConstants.TIMES.indices.toList(),
                        itemLabel = { "${it + 1}. Std · ${NexoraConstants.TIMES[it]}" },
                        onSelect = { timeIndex = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                NexoraFieldLabel("Fach")
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = { Text("z.B. Mathematik", color = NexoraMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = nexoraTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                NexoraDropdownSelector(
                    label = "",
                    selectedText = "Vorschlag übernehmen...",
                    items = subjects,
                    itemLabel = { it },
                    onSelect = { subject = it }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        NexoraFieldLabel("Lehrer")
                        OutlinedTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = nexoraTextFieldColors()
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        NexoraFieldLabel("Raum")
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = nexoraTextFieldColors()
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isExam = !isExam }
                ) {
                    Checkbox(
                        checked = isExam,
                        onCheckedChange = { isExam = it },
                        colors = CheckboxDefaults.colors(checkedColor = NexoraOrange)
                    )
                    Text("Als Klassenarbeit markieren ($weekKey)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }

                NexoraDropdownSelector(
                    label = "Farbe / Stil",
                    selectedText = colorPreset.label,
                    items = NexoraConstants.LESSON_COLORS,
                    itemLabel = { it.label },
                    onSelect = { colorPreset = it }
                )

                val statusLabels = listOf("normal" to "Normal", "cancelled" to "Entfällt", "substitution" to "Vertretung")
                NexoraDropdownSelector(
                    label = "Status für Woche ($weekKey)",
                    selectedText = statusLabels.find { it.first == status }?.second ?: "Normal",
                    items = statusLabels,
                    itemLabel = { it.second },
                    onSelect = { status = it.first }
                )

                if (status == "substitution") {
                    NexoraFieldLabel("Vertretungs-Lehrer")
                    OutlinedTextField(
                        value = subTeacher,
                        onValueChange = { subTeacher = it },
                        placeholder = { Text("z.B. Herr Müller", color = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    NexoraFieldLabel("Vertretungs-Raum")
                    OutlinedTextField(
                        value = subRoom,
                        onValueChange = { subRoom = it },
                        placeholder = { Text("z.B. R204", color = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (lesson != null) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = NexoraLine)
                    Spacer(Modifier.height(10.dp))
                    Text("Hausaufgabe ($weekKey)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    NexoraFieldLabel("Hausaufgaben-Text")
                    OutlinedTextField(
                        value = hwText,
                        onValueChange = { hwText = it },
                        placeholder = { Text("z.B. S. 42 Nr. 5", color = NexoraMuted) },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    NexoraFieldLabel("Fällig am (YYYY-MM-DD)")
                    OutlinedTextField(
                        value = hwDate,
                        onValueChange = { hwDate = it },
                        placeholder = { Text("2026-11-30", color = NexoraMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = nexoraTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (lesson != null) {
                    Spacer(Modifier.height(14.dp))
                    NexoraDangerButton(
                        text = "Stunde löschen",
                        onClick = { confirmDelete = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            NexoraPrimaryButton(
                text = "Speichern",
                enabled = subject.isNotBlank() && classId.isNotBlank(),
                onClick = {
                    onSave(
                        classId, day, timeIndex, subject, teacher, room,
                        colorPreset.cssStyle, isExam, status, subTeacher, subRoom,
                        hwText, hwDate, existingHwId, override
                    )
                },
                modifier = Modifier.width(130.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = NexoraMuted) }
        }
    )

    if (confirmDelete && lesson != null) {
        NexoraConfirmDialog(
            title = "Stunde löschen?",
            message = "Möchtest du ${lesson.subject} wirklich löschen?",
            onConfirm = {
                confirmDelete = false
                onDelete(lesson.id)
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
