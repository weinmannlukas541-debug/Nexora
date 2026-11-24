package com.example.data

import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class UserRole(val raw: String) {
    ADMIN("admin"),
    DIRECTOR("director"),
    TEACHER("teacher"),
    CLASS("class");

    companion object {
        fun fromRaw(raw: String?): UserRole? = entries.find { it.raw == raw }
    }
}

data class UserSession(
    val type: UserRole,
    val schoolId: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val schoolDirectorId: String = "",
    val name: String = "",
    val email: String = "",
    val accessToken: String = "",
    val refreshToken: String = ""
)

data class SchoolClass(
    val id: String,
    val schoolId: String,
    val name: String,
    val username: String
)

data class Lesson(
    val id: String,
    val schoolId: String,
    val classId: String,
    val day: Int,
    val time: Int,
    val subject: String,
    val teacher: String,
    val room: String,
    val note: String,
    val colorStyle: String,
    val isExam: Boolean
)

data class School(
    val id: String,
    val name: String,
    val city: String,
    val announcements: List<String>,
    val classes: List<SchoolClass>,
    val lessons: List<Lesson>
)

data class WeekOverride(
    val id: String,
    val lessonId: String,
    val week: String,
    val status: String,
    val subTeacher: String,
    val subRoom: String,
    val note: String,
    val isExam: Boolean?,
    val homeworkId: String
)

data class Homework(
    val id: String,
    val title: String,
    val text: String,
    val date: String,
    val classId: String,
    val className: String
)

data class MessageItem(
    val id: String,
    val title: String,
    val text: String,
    val date: String,
    val schoolId: String
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val date: String,
    val text: String,
    val schoolId: String
)

data class Teacher(
    val id: String,
    val schoolId: String,
    val name: String,
    val username: String,
    val password: String,
    val subject: String
)

data class Student(
    val id: String,
    val schoolId: String,
    val classId: String,
    val name: String
)

data class AttendanceRecord(
    val id: String,
    val schoolId: String,
    val classId: String,
    val studentName: String,
    val date: String,
    val timeIndex: Int,
    val status: String,
    val lateMinutes: Int,
    val note: String
)

data class GradeRecord(
    val id: String,
    val schoolId: String,
    val classId: String,
    val studentName: String,
    val subject: String,
    val gradeType: String,
    val gradeValue: String
)

data class ClassbookEntry(
    val id: String,
    val schoolId: String,
    val classId: String,
    val studentName: String,
    val entryType: String,
    val title: String,
    val description: String,
    val date: String,
    val author: String,
    val createdAt: String
)

data class ProblemReport(
    val id: String,
    val schoolId: String,
    val reporterType: String,
    val reporterName: String,
    val title: String,
    val description: String,
    val status: String,
    val createdAt: String
)

data class NexoraUpdate(
    val id: String,
    val title: String,
    val description: String,
    val createdBy: String,
    val createdAt: String
)

data class SchoolDirector(
    val id: String,
    val displayName: String,
    val schoolId: String,
    val createdAt: String
)

data class NexoraData(
    val schools: List<School> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val homework: List<Homework> = emptyList(),
    val calendar: List<CalendarEvent> = emptyList(),
    val weekOverrides: List<WeekOverride> = emptyList(),
    val teachers: List<Teacher> = emptyList(),
    val students: List<Student> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val grades: List<GradeRecord> = emptyList(),
    val classbookEntries: List<ClassbookEntry> = emptyList(),
    val problemReports: List<ProblemReport> = emptyList(),
    val updates: List<NexoraUpdate> = emptyList()
)

enum class AppPage(val key: String, val label: String) {
    DASHBOARD("dashboard", "Übersicht"),
    DIRECTOR_DASHBOARD("director_dashboard", "Meine Schule"),
    TIMETABLE("timetable", "Stundenplan"),
    DIRECTOR_TIMETABLE("director_timetable", "Stundenplan"),
    TEACHER_LESSONS("teacher_lessons", "Hausaufgaben & Arbeiten"),
    SCHOOLS("schools", "Schulen"),
    CLASSES("classes", "Klassen"),
    DIRECTOR_CLASSES("director_classes", "Klassen & Schüler"),
    TEACHERS("teachers", "Lehrer"),
    DIRECTOR_TEACHERS("director_teachers", "Lehrer konfigurieren"),
    DIRECTORS("directors", "Schuldirektor"),
    CLASSBOOK("classbook", "Klassenbuch"),
    ATTENDANCE_REPORTS("attendance_reports", "Fehlzeiten / Berichte"),
    HOMEWORK("homework", "Hausaufgaben"),
    MESSAGES("messages", "Nachrichten"),
    CALENDAR("calendar", "Schulveranstaltungen"),
    DIRECTOR_CALENDAR("director_calendar", "Veranstaltungen"),
    ANNOUNCEMENTS("announcements", "Ankündigungen"),
    REPORT_PROBLEM("report_problem", "Problem melden"),
    DIRECTOR_REPORT_PROBLEM("director_report_problem", "Problem melden"),
    REPORTED_PROBLEMS("reported_problems", "Gemeldete Probleme"),
    UPDATES("updates", "Update"),
    UPDATES_ADMIN("updates_admin", "Updates verwalten");

    companion object {
        fun fromKey(key: String?): AppPage? = entries.find { it.key == key }
    }
}

data class LessonColorPreset(
    val label: String,
    val cssStyle: String,
    val accent: Color,
    val container: Color,
    val text: Color
)

object NexoraConstants {
    val DAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag")
    val DAYS_SHORT = listOf("Mo", "Di", "Mi", "Do", "Fr")
    val TIMES = listOf("08:00", "08:45", "09:40", "10:25", "11:20", "12:05", "13:00", "13:45")

    const val DEFAULT_COLOR_CSS =
        "border-left:4px solid #3b82f6;background:rgba(37,99,235,0.15);color:#60a5fa;"
    private const val META_PREFIX = "__UNTAS_META__"

    val DEFAULT_SUBJECTS = listOf(
        "Mathematik", "Deutsch", "Englisch", "Biologie", "Chemie", "Physik",
        "Geschichte", "Erdkunde", "Sport", "Musik", "Kunst", "Informatik",
        "Französisch", "Spanisch", "Religion", "Ethik"
    )

    val LESSON_COLORS = listOf(
        LessonColorPreset(
            label = "Kräftiges Blau",
            cssStyle = "border-left:4px solid #3b82f6;background:rgba(37,99,235,0.15);color:#60a5fa;",
            accent = Color(0xFF3B82F6),
            container = Color(0x262563EB),
            text = Color(0xFF60A5FA)
        ),
        LessonColorPreset(
            label = "Leichtes Hellblau",
            cssStyle = "border-left:4px solid #60a5fa;background:rgba(59,130,246,0.1);color:#93c5fd;",
            accent = Color(0xFF60A5FA),
            container = Color(0x1F3B82F6),
            text = Color(0xFF93C5FD)
        ),
        LessonColorPreset(
            label = "Kräftiges Grün",
            cssStyle = "border-left:4px solid #10b981;background:rgba(16,185,129,0.15);color:#6ee7b7;",
            accent = Color(0xFF10B981),
            container = Color(0x2610B981),
            text = Color(0xFF6EE7B7)
        ),
        LessonColorPreset(
            label = "Sanftes Türkis",
            cssStyle = "border-left:4px solid #14b8a6;background:rgba(20,184,166,0.2);color:#5eead4;",
            accent = Color(0xFF14B8A6),
            container = Color(0x3314B8A6),
            text = Color(0xFF5EEAD4)
        ),
        LessonColorPreset(
            label = "Kräftiges Gelb / Orange",
            cssStyle = "border-left:4px solid #f59e0b;background:rgba(245,158,11,0.2);color:#fcd34d;",
            accent = Color(0xFFF59E0B),
            container = Color(0x33F59E0B),
            text = Color(0xFFFCD34D)
        ),
        LessonColorPreset(
            label = "Kräftiges Braun",
            cssStyle = "border-left:4px solid #d97706;background:rgba(180,83,9,0.25);color:#fde68a;",
            accent = Color(0xFFD97706),
            container = Color(0x40B45309),
            text = Color(0xFFFDE68A)
        ),
        LessonColorPreset(
            label = "Kräftiges Lila",
            cssStyle = "border-left:4px solid #9333ea;background:rgba(147,51,234,0.2);color:#c084fc;",
            accent = Color(0xFF9333EA),
            container = Color(0x339333EA),
            text = Color(0xFFC084FC)
        ),
        LessonColorPreset(
            label = "Kräftiges Rot",
            cssStyle = "border-left:4px solid #ef4444;background:rgba(239,68,68,0.15);color:#fca5a5;",
            accent = Color(0xFFEF4444),
            container = Color(0x26EF4444),
            text = Color(0xFFFCA5A5)
        ),
        LessonColorPreset(
            label = "Klassisches Grau / Dunkelgrau",
            cssStyle = "border-left:4px solid #64748b;background:rgba(100,116,139,0.2);color:#cbd5e1;",
            accent = Color(0xFF64748B),
            container = Color(0x3364748B),
            text = Color(0xFFCBD5E1)
        )
    )

    data class ParsedMeta(val note: String, val colorStyle: String, val isExam: Boolean)

    fun parseLessonMeta(raw: String?): ParsedMeta {
        val text = raw ?: ""
        if (!text.startsWith(META_PREFIX)) {
            return ParsedMeta(note = text, colorStyle = DEFAULT_COLOR_CSS, isExam = false)
        }
        val nl = text.indexOf('\n')
        val jsonPart = if (nl < 0) text.substring(META_PREFIX.length) else text.substring(META_PREFIX.length, nl)
        val notePart = if (nl < 0) "" else text.substring(nl + 1)
        return try {
            val obj = JSONObject(jsonPart)
            ParsedMeta(
                note = notePart,
                colorStyle = obj.optString("color", DEFAULT_COLOR_CSS).ifBlank { DEFAULT_COLOR_CSS },
                isExam = obj.optBoolean("isExam", false)
            )
        } catch (_: Exception) {
            ParsedMeta(note = text, colorStyle = DEFAULT_COLOR_CSS, isExam = false)
        }
    }

    fun packLessonNote(note: String, colorStyle: String, isExam: Boolean): String {
        val meta = JSONObject().apply {
            put("color", colorStyle.ifBlank { DEFAULT_COLOR_CSS })
            put("isExam", isExam)
        }
        return "$META_PREFIX$meta\n$note"
    }

    fun resolveColorPreset(cssStyle: String): LessonColorPreset {
        return when {
            cssStyle.contains("59,130,246,0.1") -> LESSON_COLORS[1]
            cssStyle.contains("16,185,129") -> LESSON_COLORS[2]
            cssStyle.contains("14b8a6") -> LESSON_COLORS[3]
            cssStyle.contains("245,158,11") -> LESSON_COLORS[4]
            cssStyle.contains("180,83,9") || cssStyle.contains("d97706") -> LESSON_COLORS[5]
            cssStyle.contains("147,51,234") -> LESSON_COLORS[6]
            cssStyle.contains("239,68,68") -> LESSON_COLORS[7]
            cssStyle.contains("64748b") || cssStyle.contains("100,116,139") -> LESSON_COLORS[8]
            else -> LESSON_COLORS[0]
        }
    }
}

object NexoraHelpers {
    fun getSchoolSubjects(school: School?): List<String> {
        val set = LinkedHashSet<String>()
        school?.lessons?.forEach { if (it.subject.isNotBlank()) set.add(it.subject.trim()) }
        set.addAll(NexoraConstants.DEFAULT_SUBJECTS)
        return set.sorted()
    }

    fun normalizeTeacherName(v: String?): String {
        return (v ?: "")
            .lowercase(Locale.GERMAN)
            .replace(Regex("\\b(herr|frau|teacher|lehrer|lehrerin)\\b"), "")
            .replace(Regex("[^a-z0-9äöüß]+"), "")
            .trim()
    }

    fun teacherOwnsLesson(lesson: Lesson?, teacher: Teacher?): Boolean {
        if (lesson == null || teacher == null) return false
        val assigned = normalizeTeacherName(lesson.teacher)
        val names = listOf(teacher.name, teacher.username)
            .map { normalizeTeacherName(it) }
            .filter { it.isNotBlank() }
        return assigned.isNotBlank() && names.contains(assigned)
    }

    fun teacherAllowsSubject(lesson: Lesson?, teacher: Teacher?): Boolean {
        if (lesson == null || teacher == null) return false
        val subjects = teacher.subject
            .split(",")
            .map { it.trim().lowercase(Locale.GERMAN) }
            .filter { it.isNotBlank() }
        return subjects.isEmpty() || subjects.contains(lesson.subject.trim().lowercase(Locale.GERMAN))
    }

    fun lessonState(lesson: Lesson, overrides: List<WeekOverride>, week: String): WeekOverride {
        return overrides.find { it.lessonId == lesson.id && it.week == week }
            ?: WeekOverride(
                id = "",
                lessonId = lesson.id,
                week = week,
                status = "normal",
                subTeacher = "",
                subRoom = "",
                note = "",
                isExam = null,
                homeworkId = ""
            )
    }

    fun sameLesson(a: Lesson, b: Lesson, overrides: List<WeekOverride>, week: String): Boolean {
        val sa = lessonState(a, overrides, week)
        val sb = lessonState(b, overrides, week)
        val examA = sa.isExam ?: a.isExam
        val examB = sb.isExam ?: b.isExam
        return a.subject == b.subject &&
            a.teacher == b.teacher &&
            a.room == b.room &&
            a.colorStyle == b.colorStyle &&
            a.note == b.note &&
            sa.homeworkId == sb.homeworkId &&
            examA == examB &&
            sa.status == sb.status &&
            sa.subTeacher == sb.subTeacher &&
            sa.subRoom == sb.subRoom
    }

    fun getMondayCalendar(offsetWeeks: Int = 0): Calendar {
        val cal = Calendar.getInstance(Locale.GERMAN).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diff = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - dayOfWeek
        }
        cal.add(Calendar.DAY_OF_MONTH, diff + offsetWeeks * 7)
        return cal
    }

    fun getWeekKey(offsetWeeks: Int = 0): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(getMondayCalendar(offsetWeeks).time)
    }

    fun getWeekBounds(weekKey: String): Pair<String, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val mondayDate = sdf.parse(weekKey) ?: getMondayCalendar(0).time
            val cal = Calendar.getInstance(Locale.GERMAN).apply {
                time = mondayDate
                add(Calendar.DAY_OF_MONTH, 4)
            }
            weekKey to sdf.format(cal.time)
        } catch (_: Exception) {
            val m = getWeekKey(0)
            m to m
        }
    }

    fun getWeekDisplayLabel(offsetWeeks: Int = 0): String {
        val monday = getMondayCalendar(offsetWeeks)
        val friday = (monday.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 4) }
        val fmt = SimpleDateFormat("dd.MM.", Locale.GERMAN)
        val base = "${fmt.format(monday.time)} – ${fmt.format(friday.time)}"
        return if (offsetWeeks == 0) "$base (Aktuell)" else base
    }

    fun formatGermanDate(isoDate: String): String {
        if (isoDate.isBlank()) return ""
        return try {
            val clean = isoDate.take(10)
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = parser.parse(clean) ?: return isoDate
            val fmt = SimpleDateFormat("EEEE, dd.MM.", Locale.GERMAN)
            fmt.format(d)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun formatDateTime(isoTimestamp: String): String {
        if (isoTimestamp.isBlank()) return ""
        return try {
            val normalized = isoTimestamp.replace("T", " ").take(16)
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val d = parser.parse(normalized) ?: return isoTimestamp.take(10)
            SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMAN).format(d)
        } catch (_: Exception) {
            isoTimestamp.take(10)
        }
    }

    fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)

    fun allowedPagesFor(role: UserRole): List<AppPage> = when (role) {
        UserRole.ADMIN -> listOf(
            AppPage.DASHBOARD, AppPage.DIRECTORS, AppPage.TIMETABLE, AppPage.SCHOOLS,
            AppPage.CLASSES, AppPage.CLASSBOOK, AppPage.ATTENDANCE_REPORTS, AppPage.TEACHERS,
            AppPage.HOMEWORK, AppPage.MESSAGES, AppPage.CALENDAR, AppPage.ANNOUNCEMENTS,
            AppPage.REPORTED_PROBLEMS, AppPage.REPORT_PROBLEM, AppPage.UPDATES_ADMIN
        )
        UserRole.DIRECTOR -> listOf(
            AppPage.DIRECTOR_DASHBOARD, AppPage.DIRECTOR_CLASSES, AppPage.DIRECTOR_TEACHERS,
            AppPage.DIRECTOR_TIMETABLE, AppPage.CLASSBOOK, AppPage.DIRECTOR_CALENDAR,
            AppPage.DIRECTOR_REPORT_PROBLEM, AppPage.UPDATES
        )
        UserRole.TEACHER -> listOf(
            AppPage.TIMETABLE, AppPage.TEACHER_LESSONS, AppPage.CLASSBOOK,
            AppPage.HOMEWORK, AppPage.MESSAGES, AppPage.ANNOUNCEMENTS,
            AppPage.REPORT_PROBLEM, AppPage.UPDATES
        )
        UserRole.CLASS -> listOf(
            AppPage.TIMETABLE, AppPage.HOMEWORK, AppPage.MESSAGES,
            AppPage.ANNOUNCEMENTS, AppPage.REPORT_PROBLEM, AppPage.UPDATES
        )
    }

    fun defaultPageFor(role: UserRole): AppPage = when (role) {
        UserRole.ADMIN -> AppPage.DASHBOARD
        UserRole.DIRECTOR -> AppPage.DIRECTOR_DASHBOARD
        UserRole.TEACHER, UserRole.CLASS -> AppPage.TIMETABLE
    }
}
