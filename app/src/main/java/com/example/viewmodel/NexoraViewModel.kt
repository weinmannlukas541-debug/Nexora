package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPage
import com.example.data.NexoraData
import com.example.data.NexoraHelpers
import com.example.data.NexoraRepository
import com.example.data.NexoraUpdate
import com.example.data.SchoolDirector
import com.example.data.SessionStore
import com.example.data.UserRole
import com.example.data.UserSession
import com.example.data.WeekOverride
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NexoraUiState(
    val isInitializing: Boolean = true,
    val isBusy: Boolean = false,
    val isSyncing: Boolean = false,
    val session: UserSession? = null,
    val data: NexoraData = NexoraData(),
    val currentPage: AppPage = AppPage.TIMETABLE,
    val weekOffset: Int = 0,
    val selectedSchoolId: String = "",
    val selectedClassId: String = "",
    val classbookDate: String = NexoraHelpers.todayIso(),
    val directorsList: List<SchoolDirector> = emptyList(),
    val directorsError: String = "",
    val classbookLoadError: String = "",
    val problemReportsError: String = "",
    val toastMessage: String? = null,
    val isModalOpen: Boolean = false,
    val initError: String? = null
)

class NexoraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NexoraRepository()
    private val sessionStore = SessionStore(application.applicationContext)

    private val _uiState = MutableStateFlow(NexoraUiState())
    val uiState: StateFlow<NexoraUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        initializeApp()
    }

    fun initializeApp() {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitializing = true, initError = null) }
            sessionStore.checkAndSyncMondayKey(NexoraHelpers.getWeekKey(0))
            val savedSession = sessionStore.loadSession()
            val savedPage = sessionStore.loadCurrentPage()
            try {
                val res = repository.loadAllData(savedSession?.accessToken)
                val validSession = validateSession(savedSession, res.data)
                if (validSession == null && savedSession != null) {
                    sessionStore.clearAll()
                }
                val resolvedPage = resolvePage(validSession, savedPage)
                val (defaultSchoolId, defaultClassId) = resolveDefaultSelectors(validSession, res.data)
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        session = validSession,
                        data = res.data,
                        currentPage = resolvedPage,
                        selectedSchoolId = defaultSchoolId,
                        selectedClassId = defaultClassId,
                        classbookLoadError = res.classbookLoadError
                    )
                }
                if (validSession != null) {
                    startAutoRefresh()
                    if (resolvedPage == AppPage.DIRECTORS) loadDirectors()
                    if (resolvedPage == AppPage.UPDATES || resolvedPage == AppPage.UPDATES_ADMIN) loadUpdates()
                }
            } catch (e: Exception) {
                sessionStore.clearAll()
                _uiState.update {
                    it.copy(
                        isInitializing = false,
                        session = null,
                        initError = e.message ?: "Verbindung zu Supabase fehlgeschlagen."
                    )
                }
            }
        }
    }

    private fun validateSession(session: UserSession?, data: NexoraData): UserSession? {
        session ?: return null
        return when (session.type) {
            UserRole.ADMIN -> session
            UserRole.DIRECTOR -> if (data.schools.any { it.id == session.schoolId }) session else null
            UserRole.TEACHER -> {
                val s = data.schools.find { it.id == session.schoolId }
                val t = data.teachers.find { it.id == session.teacherId }
                if (s != null && t != null) session else null
            }
            UserRole.CLASS -> {
                val s = data.schools.find { it.id == session.schoolId }
                val c = s?.classes?.find { it.id == session.classId }
                if (s != null && c != null) session else null
            }
        }
    }

    private fun resolvePage(session: UserSession?, requested: AppPage?): AppPage {
        session ?: return AppPage.TIMETABLE
        val allowed = NexoraHelpers.allowedPagesFor(session.type)
        return if (requested != null && allowed.contains(requested)) requested
        else NexoraHelpers.defaultPageFor(session.type)
    }

    private fun resolveDefaultSelectors(
        session: UserSession?,
        data: NexoraData,
        prevSchoolId: String = "",
        prevClassId: String = ""
    ): Pair<String, String> {
        val schoolId = when (session?.type) {
            UserRole.DIRECTOR, UserRole.TEACHER, UserRole.CLASS -> session.schoolId
            else -> data.schools.find { it.id == prevSchoolId }?.id ?: data.schools.firstOrNull()?.id ?: ""
        }
        val school = data.schools.find { it.id == schoolId }
        val classId = when (session?.type) {
            UserRole.CLASS -> session.classId
            else -> school?.classes?.find { it.id == prevClassId }?.id ?: school?.classes?.firstOrNull()?.id ?: ""
        }
        return schoolId to classId
    }

    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000L)
                val state = _uiState.value
                if (state.session != null && !state.isModalOpen && !state.isBusy) {
                    if (sessionStore.checkAndSyncMondayKey(NexoraHelpers.getWeekKey(0))) {
                        _uiState.update { it.copy(weekOffset = 0) }
                    }
                    syncNow(silent = true)
                }
            }
        }
    }

    fun syncNow(silent: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val activeSession = state.session
            if (!silent) _uiState.update { it.copy(isSyncing = true) }
            try {
                val res = repository.loadAllData(
                    accessToken = activeSession?.accessToken,
                    previousClassbookEntries = state.data.classbookEntries,
                    previousUpdates = state.data.updates
                )
                val validSession = validateSession(activeSession, res.data)
                val (schoolId, classId) = resolveDefaultSelectors(
                    validSession,
                    res.data,
                    state.selectedSchoolId,
                    state.selectedClassId
                )
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        session = validSession,
                        data = res.data,
                        selectedSchoolId = schoolId,
                        selectedClassId = classId,
                        classbookLoadError = res.classbookLoadError
                    )
                }
                if (!silent) showToast("Daten aktualisiert.")
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
                if (!silent) showToast("Aktualisierung fehlgeschlagen.")
            }
        }
    }

    fun setModalOpen(open: Boolean) {
        _uiState.update { it.copy(isModalOpen = open) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun navigateTo(page: AppPage) {
        val state = _uiState.value
        val session = state.session ?: return
        if (!NexoraHelpers.allowedPagesFor(session.type).contains(page)) return
        sessionStore.saveCurrentPage(page)
        _uiState.update { it.copy(currentPage = page) }
        when (page) {
            AppPage.DIRECTORS -> loadDirectors()
            AppPage.UPDATES, AppPage.UPDATES_ADMIN -> loadUpdates()
            AppPage.REPORTED_PROBLEMS -> loadProblemReports()
            else -> Unit
        }
    }

    fun changeWeek(deltaOrZero: Int) {
        _uiState.update {
            val next = if (deltaOrZero == 0) 0 else it.weekOffset + deltaOrZero
            it.copy(weekOffset = next)
        }
    }

    fun selectSchool(schoolId: String) {
        val school = _uiState.value.data.schools.find { it.id == schoolId }
        val firstClass = school?.classes?.firstOrNull()?.id ?: ""
        _uiState.update { it.copy(selectedSchoolId = schoolId, selectedClassId = firstClass) }
    }

    fun selectClass(classId: String) {
        val schoolId = _uiState.value.data.schools.find { s -> s.classes.any { it.id == classId } }?.id
            ?: _uiState.value.selectedSchoolId
        _uiState.update { it.copy(selectedSchoolId = schoolId, selectedClassId = classId) }
    }

    fun setClassbookDate(date: String) {
        _uiState.update { it.copy(classbookDate = date) }
    }

    // --- Authentication ---
    fun loginAdmin(email: String, password: String, onSuccess: () -> Unit = {}) {
        runAction(onSuccess = onSuccess) {
            val newSession = repository.loginAdmin(email.trim(), password)
            completeLogin(newSession, "Erfolgreich angemeldet.")
        }
    }

    fun loginDirector(name: String, password: String, onSuccess: () -> Unit = {}) {
        runAction(onSuccess = onSuccess) {
            val newSession = repository.loginDirector(name.trim(), password)
            completeLogin(newSession, "Als Schuldirektor angemeldet.")
        }
    }

    fun loginSchoolUser(schoolId: String, username: String, password: String) {
        runAction {
            val newSession = repository.loginTeacherOrClass(schoolId, username.trim(), password)
            completeLogin(newSession, "Willkommen bei Nexora!")
        }
    }

    private suspend fun completeLogin(newSession: UserSession, welcomeMessage: String) {
        val res = repository.loadAllData(newSession.accessToken)
        val validated = validateSession(newSession, res.data) ?: newSession
        val defaultPage = NexoraHelpers.defaultPageFor(validated.type)
        val (sId, cId) = resolveDefaultSelectors(validated, res.data)
        sessionStore.saveSession(validated)
        sessionStore.saveCurrentPage(defaultPage)
        _uiState.update {
            it.copy(
                session = validated,
                data = res.data,
                currentPage = defaultPage,
                selectedSchoolId = sId,
                selectedClassId = cId,
                classbookLoadError = res.classbookLoadError,
                toastMessage = welcomeMessage
            )
        }
        startAutoRefresh()
    }

    fun logout() {
        refreshJob?.cancel()
        sessionStore.clearAll()
        _uiState.update {
            it.copy(
                session = null,
                currentPage = AppPage.TIMETABLE,
                weekOffset = 0,
                directorsList = emptyList(),
                toastMessage = "Abgemeldet."
            )
        }
    }

    // --- Updates ---
    fun loadUpdates(onLoaded: (List<NexoraUpdate>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val updates = repository.loadUpdates(_uiState.value.session?.accessToken)
                _uiState.update { it.copy(data = it.data.copy(updates = updates)) }
                onLoaded(updates)
            } catch (_: Exception) {
                _uiState.update { it.copy(data = it.data.copy(updates = emptyList())) }
                onLoaded(emptyList())
            }
        }
    }

    fun publishUpdate(title: String, description: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        if (session.type != UserRole.ADMIN) return showToast("Nur der Administrator kann Updates veröffentlichen.")
        runAction("Update wurde veröffentlicht.", onDone) {
            repository.publishUpdate(title.trim(), description.trim(), session.email.ifBlank { "Administrator" }, session.accessToken)
            val updates = repository.loadUpdates(session.accessToken)
            _uiState.update { it.copy(data = it.data.copy(updates = updates)) }
        }
    }

    fun deleteUpdate(id: String) {
        val session = _uiState.value.session ?: return
        if (session.type != UserRole.ADMIN) return showToast("Nur der Administrator kann Updates löschen.")
        runAction("Update gelöscht.") {
            repository.deleteUpdate(id, session.accessToken)
            val updates = repository.loadUpdates(session.accessToken)
            _uiState.update { it.copy(data = it.data.copy(updates = updates)) }
        }
    }

    // --- Directors ---
    fun loadDirectors() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            try {
                val list = repository.loadDirectors(session.accessToken)
                _uiState.update { it.copy(directorsList = list, directorsError = "") }
            } catch (_: Exception) {
                _uiState.update { it.copy(directorsError = "Direktor-Liste nicht erreichbar. SQL/RLS einrichten.") }
            }
        }
    }

    fun createDirector(schoolId: String, name: String, password: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        runAction("Schuldirektor-Konto erstellt.", onDone) {
            repository.createDirector(schoolId, name.trim(), password, session.accessToken)
            val list = repository.loadDirectors(session.accessToken)
            _uiState.update { it.copy(directorsList = list, directorsError = "") }
        }
    }

    fun deleteDirector(directorId: String) {
        val session = _uiState.value.session ?: return
        runAction("Direktor-Zugang gelöscht.") {
            repository.deleteDirector(directorId, session.accessToken)
            val list = repository.loadDirectors(session.accessToken)
            _uiState.update { it.copy(directorsList = list) }
        }
    }

    // --- Schools ---
    fun saveSchool(id: String?, name: String, city: String, onDone: () -> Unit = {}) {
        runAction("Gespeichert.", onDone) {
            repository.saveSchool(id, name.trim(), city.trim(), _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteSchool(id: String) {
        runAction("Schule gelöscht.") {
            repository.deleteSchool(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun addAnnouncement(schoolId: String, text: String, onDone: () -> Unit = {}) {
        val school = _uiState.value.data.schools.find { it.id == schoolId } ?: return
        val updated = school.announcements + text.trim()
        runAction("Ankündigung hinzugefügt.", onDone) {
            repository.updateSchoolAnnouncements(schoolId, updated, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteAnnouncement(schoolId: String, index: Int) {
        val school = _uiState.value.data.schools.find { it.id == schoolId } ?: return
        if (index !in school.announcements.indices) return
        val updated = school.announcements.toMutableList().apply { removeAt(index) }
        runAction("Ankündigung gelöscht.") {
            repository.updateSchoolAnnouncements(schoolId, updated, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Classes ---
    fun saveClass(id: String?, schoolId: String, name: String, username: String, password: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        val scoped = if (session.type == UserRole.DIRECTOR) session.schoolId else null
        val targetSchool = scoped ?: schoolId
        runAction("Klasse gespeichert.", onDone) {
            repository.saveClass(id, targetSchool, name.trim(), username.trim(), password, scoped, session.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteClass(id: String) {
        val session = _uiState.value.session ?: return
        val scoped = if (session.type == UserRole.DIRECTOR) session.schoolId else null
        runAction("Klasse gelöscht.") {
            repository.deleteClass(id, scoped, session.accessToken)
            reloadDataLocked()
        }
    }

    // --- Students ---
    fun addStudent(schoolId: String, classId: String, name: String) {
        runAction("Schüler hinzugefügt.") {
            repository.addStudent(schoolId, classId, name.trim(), _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteStudent(studentId: String) {
        runAction("Schüler gelöscht.") {
            repository.deleteStudent(studentId, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Teachers ---
    fun saveTeacher(id: String?, schoolId: String, name: String, username: String, subjects: String, password: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        val scoped = if (session.type == UserRole.DIRECTOR) session.schoolId else null
        val targetSchool = scoped ?: schoolId
        runAction("Lehrer gespeichert.", onDone) {
            repository.saveTeacher(id, targetSchool, name.trim(), username.trim(), subjects, password, scoped, session.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteTeacher(teacherId: String) {
        runAction("Lehrer gelöscht.") {
            repository.deleteTeacher(teacherId, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Lessons ---
    fun saveTeacherLesson(lessonId: String, week: String, isExam: Boolean, homeworkText: String, homeworkDate: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        runAction("Erfolgreich gespeichert.", onDone) {
            repository.saveTeacherLesson(session.teacherId, lessonId, week, isExam, homeworkText.trim(), homeworkDate)
            reloadDataLocked()
        }
    }

    fun saveAdminOrDirectorLesson(
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
        homeworkText: String,
        homeworkDate: String,
        existingHomeworkId: String,
        existingOverride: WeekOverride?,
        onDone: () -> Unit = {}
    ) {
        runAction("Gespeichert.", onDone) {
            repository.saveAdminOrDirectorLesson(
                schoolId = schoolId,
                lessonId = lessonId,
                classId = classId,
                day = day,
                timeIndex = timeIndex,
                subject = subject.trim(),
                teacher = teacher.trim(),
                room = room.trim(),
                colorStyle = colorStyle,
                isExam = isExam,
                week = week,
                status = status,
                subTeacher = subTeacher.trim(),
                subRoom = subRoom.trim(),
                homeworkText = homeworkText.trim(),
                homeworkDate = homeworkDate,
                existingHomeworkId = existingHomeworkId,
                existingOverride = existingOverride,
                accessToken = _uiState.value.session?.accessToken
            )
            reloadDataLocked()
        }
    }

    fun deleteLesson(lessonId: String, onDone: () -> Unit = {}) {
        runAction("Stunde gelöscht.", onDone) {
            repository.deleteLesson(lessonId, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Homework ---
    fun saveHomework(title: String, text: String, dueDate: String, classId: String, onDone: () -> Unit = {}) {
        runAction("Hausaufgabe veröffentlicht.", onDone) {
            repository.saveHomework(title.trim(), text.trim(), dueDate, classId, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteHomework(id: String) {
        runAction("Gelöscht.") {
            repository.deleteHomework(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Messages ---
    fun saveMessage(title: String, text: String, onDone: () -> Unit = {}) {
        runAction("Nachricht gesendet.", onDone) {
            repository.saveMessage(title.trim(), text.trim(), _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteMessage(id: String) {
        runAction("Nachricht gelöscht.") {
            repository.deleteMessage(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Calendar ---
    fun saveCalendarEvent(schoolId: String, title: String, eventDate: String, text: String, onDone: () -> Unit = {}) {
        val session = _uiState.value.session ?: return
        val targetSchool = if (session.type == UserRole.DIRECTOR) session.schoolId else schoolId
        runAction("Veranstaltung gespeichert.", onDone) {
            repository.saveCalendarEvent(targetSchool, title.trim(), eventDate, text.trim(), session.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteCalendarEvent(id: String) {
        val session = _uiState.value.session ?: return
        val scoped = if (session.type == UserRole.DIRECTOR) session.schoolId else null
        runAction("Veranstaltung gelöscht.") {
            repository.deleteCalendarEvent(id, scoped, session.accessToken)
            reloadDataLocked()
        }
    }

    // --- Attendance ---
    fun saveAttendance(
        classId: String,
        date: String,
        timeIndex: Int,
        rows: List<Triple<String, String, Int>>
    ) {
        val state = _uiState.value
        val session = state.session ?: return
        val schoolId = if (session.type == UserRole.ADMIN) {
            state.data.schools.find { sc -> sc.classes.any { it.id == classId } }?.id ?: ""
        } else {
            session.schoolId
        }
        if (schoolId.isBlank() || classId.isBlank()) {
            return showToast("Schule oder Klasse konnte nicht zugeordnet werden.")
        }
        runAction("Anwesenheit gespeichert.") {
            repository.upsertAttendanceBatch(schoolId, classId, date, timeIndex, rows, session.accessToken)
            reloadDataLocked()
        }
    }

    fun updateAttendance(id: String, date: String, timeIndex: Int, status: String, lateMinutes: Int, onDone: () -> Unit = {}) {
        runAction("Fehlzeit aktualisiert.", onDone) {
            repository.updateAttendance(id, date, timeIndex, status, lateMinutes, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteAttendance(id: String) {
        runAction("Fehlzeit dauerhaft gelöscht.") {
            repository.deleteAttendance(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Classbook Entries ---
    fun saveClassbookEntry(
        studentId: String,
        entryType: String,
        title: String,
        description: String,
        entryDate: String,
        onDone: () -> Unit = {}
    ) {
        val state = _uiState.value
        val session = state.session ?: return
        val student = state.data.students.find {
            it.id == studentId && (session.type == UserRole.ADMIN || it.schoolId == session.schoolId)
        } ?: return showToast("Schüler konnte keiner Klasse zugeordnet werden.")

        val schoolId = student.schoolId.ifBlank {
            state.data.schools.find { sc -> sc.classes.any { it.id == student.classId } }?.id ?: ""
        }
        if (schoolId.isBlank() || student.classId.isBlank()) {
            return showToast("Schule oder Klasse fehlt.")
        }

        val author = when (session.type) {
            UserRole.ADMIN -> "Administrator"
            UserRole.DIRECTOR -> "Schuldirektor · ${session.name}"
            UserRole.TEACHER -> state.data.teachers.find { it.id == session.teacherId }?.name
                ?: session.teacherName.ifBlank { "Lehrkraft" }
            UserRole.CLASS -> "Klasse"
        }

        runAction("Klassenbucheintrag dauerhaft gespeichert.", onDone) {
            repository.saveClassbookEntry(
                schoolId = schoolId,
                classId = student.classId,
                studentName = student.name,
                entryType = entryType,
                title = title.trim(),
                description = description.trim(),
                entryDate = entryDate,
                author = author,
                accessToken = session.accessToken
            )
            reloadDataLocked()
        }
    }

    fun updateClassbookEntry(id: String, title: String, description: String, entryType: String, onDone: () -> Unit = {}) {
        runAction("Klassenbucheintrag aktualisiert.", onDone) {
            repository.updateClassbookEntry(id, title.trim(), description.trim(), entryType, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteClassbookEntry(id: String) {
        runAction("Eintrag dauerhaft gelöscht.") {
            repository.deleteClassbookEntry(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Grades ---
    fun saveGrade(classId: String, studentName: String, subject: String, gradeValue: String, onDone: () -> Unit = {}) {
        val state = _uiState.value
        val session = state.session ?: return
        val schoolId = if (session.type == UserRole.ADMIN) {
            state.data.schools.find { sc -> sc.classes.any { it.id == classId } }?.id ?: ""
        } else {
            session.schoolId
        }
        if (schoolId.isBlank() || classId.isBlank()) {
            return showToast("Schule oder Klasse fehlt.")
        }
        runAction("Note erfolgreich eingetragen!", onDone) {
            repository.saveGrade(schoolId, classId, studentName.trim(), subject.trim(), gradeValue.trim(), session.accessToken)
            reloadDataLocked()
        }
    }

    fun deleteGrade(id: String) {
        runAction("Note gelöscht.") {
            repository.deleteGrade(id, _uiState.value.session?.accessToken)
            reloadDataLocked()
        }
    }

    // --- Problem Reports ---
    fun loadProblemReports() {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            try {
                val list = repository.loadProblemReports(session.accessToken)
                _uiState.update {
                    it.copy(
                        data = it.data.copy(problemReports = list),
                        problemReportsError = ""
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(problemReportsError = "Problemmeldungen konnten nicht geladen werden. Bitte prüfe die Supabase-Berechtigungen.")
                }
            }
        }
    }

    fun submitProblemReport(title: String, description: String, onDone: () -> Unit = {}) {
        val state = _uiState.value
        val session = state.session ?: return
        val schoolId = session.schoolId.ifBlank {
            state.data.schools.find { s -> s.classes.any { it.id == session.classId } }?.id
        }
        val reporterType = when (session.type) {
            UserRole.ADMIN -> "Administrator"
            UserRole.DIRECTOR -> "Schuldirektor"
            UserRole.TEACHER -> "Lehrkraft"
            UserRole.CLASS -> "Schüler/Klasse"
        }
        val reporterName = when (session.type) {
            UserRole.DIRECTOR -> session.name
            UserRole.TEACHER -> state.data.teachers.find { it.id == session.teacherId }?.name ?: session.teacherName
            UserRole.CLASS -> state.data.schools.find { it.id == session.schoolId }
                ?.classes?.find { it.id == session.classId }?.name ?: ""
            UserRole.ADMIN -> ""
        }
        runAction("Problem wurde gemeldet.", onDone) {
            repository.submitProblemReport(schoolId, reporterType, reporterName, title.trim(), description.trim(), session.accessToken)
            if (session.type == UserRole.ADMIN) {
                val reports = repository.loadProblemReports(session.accessToken)
                _uiState.update { it.copy(data = it.data.copy(problemReports = reports)) }
            }
        }
    }

    fun deleteProblemReport(id: String) {
        val session = _uiState.value.session ?: return
        if (session.type != UserRole.ADMIN) return showToast("Nur Administratoren können Meldungen löschen.")
        runAction("Meldung gelöscht.") {
            repository.deleteProblemReport(id, session.accessToken)
            val reports = repository.loadProblemReports(session.accessToken)
            _uiState.update { it.copy(data = it.data.copy(problemReports = reports)) }
        }
    }

    // --- Helpers ---
    private suspend fun reloadDataLocked() {
        val state = _uiState.value
        val res = repository.loadAllData(
            accessToken = state.session?.accessToken,
            previousClassbookEntries = state.data.classbookEntries,
            previousUpdates = state.data.updates
        )
        val (sId, cId) = resolveDefaultSelectors(
            state.session,
            res.data,
            state.selectedSchoolId,
            state.selectedClassId
        )
        _uiState.update {
            it.copy(
                data = res.data,
                selectedSchoolId = sId,
                selectedClassId = cId,
                classbookLoadError = res.classbookLoadError
            )
        }
    }

    private fun runAction(
        successToast: String? = null,
        onSuccess: () -> Unit = {},
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            try {
                block()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        toastMessage = successToast ?: it.toastMessage
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                val msg = (e.message ?: "Unbekannter Fehler").take(180)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        toastMessage = msg
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
