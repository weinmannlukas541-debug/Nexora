package com.example.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject

data class LoadDataResult(
    val data: NexoraData,
    val classbookLoadError: String = ""
)

class NexoraRepository(private val client: SupabaseClient = SupabaseClient()) {

    private fun JSONObject.str(key: String, default: String = ""): String {
        if (isNull(key)) return default
        return optString(key, default)
    }

    suspend fun loadAllData(
        accessToken: String?,
        previousClassbookEntries: List<ClassbookEntry> = emptyList(),
        previousUpdates: List<NexoraUpdate> = emptyList()
    ): LoadDataResult = coroutineScope {
        val schoolsDeferred = async { client.restGetArray("schools?select=*", accessToken) }
        val classesDeferred = async { client.restGetArray("classes?select=id,school_id,name,username", accessToken) }
        val lessonsDeferred = async { client.restGetArray("lessons?select=*", accessToken) }
        val overridesDeferred = async { client.restGetArray("week_overrides?select=*", accessToken) }
        val homeworkDeferred = async { client.restGetArray("homework?select=*", accessToken) }
        val messagesDeferred = async { client.restGetArray("messages?select=*", accessToken) }
        val calendarDeferred = async { client.restGetArray("calendar?select=*", accessToken) }
        val teachersDeferred = async { client.restGetArray("teachers?select=*", accessToken) }
        val studentsDeferred = async { runCatching { client.restGetArray("students?select=*", accessToken) }.getOrDefault(JSONArray()) }
        val attendanceDeferred = async { runCatching { client.restGetArray("attendance?select=*", accessToken) }.getOrDefault(JSONArray()) }
        val gradesDeferred = async { runCatching { client.restGetArray("grades?select=*", accessToken) }.getOrDefault(JSONArray()) }
        val classbookDeferred = async {
            try {
                Result.success(client.restGetArray("classbook_entries?select=*&order=created_at.desc", accessToken))
            } catch (e: Exception) {
                Result.failure<JSONArray>(e)
            }
        }
        val reportsDeferred = async { runCatching { client.restGetArray("problem_reports?select=*&order=created_at.desc", accessToken) }.getOrDefault(JSONArray()) }

        val schoolsArr = schoolsDeferred.await()
        val classesArr = classesDeferred.await()
        val lessonsArr = lessonsDeferred.await()
        val overridesArr = overridesDeferred.await()
        val homeworkArr = homeworkDeferred.await()
        val messagesArr = messagesDeferred.await()
        val calendarArr = calendarDeferred.await()
        val teachersArr = teachersDeferred.await()
        val studentsArr = studentsDeferred.await()
        val attendanceArr = attendanceDeferred.await()
        val gradesArr = gradesDeferred.await()
        val classbookRes = classbookDeferred.await()
        val reportsArr = reportsDeferred.await()

        val allClasses = (0 until classesArr.length()).map { idx ->
            val c = classesArr.getJSONObject(idx)
            SchoolClass(
                id = c.str("id"),
                schoolId = c.str("school_id"),
                name = c.str("name"),
                username = c.str("username")
            )
        }
        val classById = allClasses.associateBy { it.id }

        val allLessons = (0 until lessonsArr.length()).map { idx ->
            val l = lessonsArr.getJSONObject(idx)
            val meta = NexoraConstants.parseLessonMeta(l.str("note"))
            Lesson(
                id = l.str("id"),
                schoolId = l.str("school_id"),
                classId = l.str("class_id"),
                day = l.optInt("day", 0),
                time = l.optInt("time_index", 0),
                subject = l.str("subject"),
                teacher = l.str("teacher"),
                room = l.str("room"),
                note = meta.note,
                colorStyle = meta.colorStyle,
                isExam = meta.isExam
            )
        }

        val schools = (0 until schoolsArr.length()).map { idx ->
            val s = schoolsArr.getJSONObject(idx)
            val sid = s.str("id")
            val annArr = s.optJSONArray("announcements") ?: JSONArray()
            val announcements = (0 until annArr.length()).mapNotNull { aIdx ->
                annArr.optString(aIdx, null)?.takeIf { it.isNotEmpty() }
            }
            School(
                id = sid,
                name = s.str("name"),
                city = s.str("city"),
                announcements = announcements,
                classes = allClasses.filter { it.schoolId == sid },
                lessons = allLessons.filter { it.schoolId == sid }
            )
        }

        val weekOverrides = (0 until overridesArr.length()).map { idx ->
            val o = overridesArr.getJSONObject(idx)
            WeekOverride(
                id = o.str("id"),
                lessonId = o.str("lesson_id"),
                week = o.str("week"),
                status = o.str("status", "normal"),
                subTeacher = o.str("sub_teacher"),
                subRoom = o.str("sub_room"),
                note = o.str("note"),
                isExam = if (o.isNull("is_exam")) null else o.optBoolean("is_exam"),
                homeworkId = o.str("homework_id")
            )
        }

        val homework = (0 until homeworkArr.length()).map { idx ->
            val h = homeworkArr.getJSONObject(idx)
            val cid = h.str("class_id")
            Homework(
                id = h.str("id"),
                title = h.str("title"),
                text = h.str("text"),
                date = h.str("due_date"),
                classId = cid,
                className = classById[cid]?.name ?: "Alle"
            )
        }

        val messages = (0 until messagesArr.length()).map { idx ->
            val m = messagesArr.getJSONObject(idx)
            MessageItem(
                id = m.str("id"),
                title = m.str("title"),
                text = m.str("text"),
                date = m.str("created_at"),
                schoolId = m.str("school_id")
            )
        }

        val calendar = (0 until calendarArr.length()).map { idx ->
            val e = calendarArr.getJSONObject(idx)
            CalendarEvent(
                id = e.str("id"),
                title = e.str("title"),
                date = e.str("event_date"),
                text = e.str("text"),
                schoolId = e.str("school_id")
            )
        }

        val teachers = (0 until teachersArr.length()).map { idx ->
            val t = teachersArr.getJSONObject(idx)
            Teacher(
                id = t.str("id"),
                schoolId = t.str("school_id"),
                name = t.str("name"),
                username = t.str("username"),
                password = t.str("password_hash"),
                subject = t.str("subject")
            )
        }

        val students = (0 until studentsArr.length()).map { idx ->
            val st = studentsArr.getJSONObject(idx)
            Student(
                id = st.str("id"),
                schoolId = st.str("school_id"),
                classId = st.str("class_id"),
                name = st.str("name")
            )
        }

        val attendance = (0 until attendanceArr.length()).map { idx ->
            val a = attendanceArr.getJSONObject(idx)
            AttendanceRecord(
                id = a.str("id"),
                schoolId = a.str("school_id"),
                classId = a.str("class_id"),
                studentName = a.str("student_name"),
                date = a.str("date"),
                timeIndex = a.optInt("time_index", 1),
                status = a.str("status"),
                lateMinutes = a.optInt("late_minutes", 0),
                note = a.str("note")
            )
        }

        val grades = (0 until gradesArr.length()).map { idx ->
            val g = gradesArr.getJSONObject(idx)
            GradeRecord(
                id = g.str("id"),
                schoolId = g.str("school_id"),
                classId = g.str("class_id"),
                studentName = g.str("student_name"),
                subject = g.str("subject"),
                gradeType = g.str("grade_type"),
                gradeValue = g.str("grade_value")
            )
        }

        var classbookError = ""
        val classbookEntries = if (classbookRes.isSuccess) {
            val arr = classbookRes.getOrThrow()
            (0 until arr.length()).map { idx ->
                val e = arr.getJSONObject(idx)
                ClassbookEntry(
                    id = e.str("id"),
                    schoolId = e.str("school_id"),
                    classId = e.str("class_id"),
                    studentName = e.str("student_name"),
                    entryType = e.str("entry_type"),
                    title = e.str("title"),
                    description = e.str("description"),
                    date = e.str("entry_date"),
                    author = e.str("author"),
                    createdAt = e.str("created_at")
                )
            }
        } else {
            classbookError = classbookRes.exceptionOrNull()?.message ?: "Unbekannter Supabase-Fehler"
            previousClassbookEntries
        }

        val problemReports = (0 until reportsArr.length()).map { idx ->
            val r = reportsArr.getJSONObject(idx)
            ProblemReport(
                id = r.str("id"),
                schoolId = r.str("school_id"),
                reporterType = r.str("reporter_type", "Unbekannt"),
                reporterName = r.str("reporter_name"),
                title = r.str("title"),
                description = r.str("description"),
                status = r.str("status", "offen"),
                createdAt = r.str("created_at")
            )
        }

        LoadDataResult(
            data = NexoraData(
                schools = schools,
                messages = messages,
                homework = homework,
                calendar = calendar,
                weekOverrides = weekOverrides,
                teachers = teachers,
                students = students,
                attendance = attendance,
                grades = grades,
                classbookEntries = classbookEntries,
                problemReports = problemReports,
                updates = previousUpdates
            ),
            classbookLoadError = classbookError
        )
    }

    // --- Authentication ---
    suspend fun loginAdmin(email: String, password: String): UserSession {
        val auth = client.authPasswordToken(email, password)
        val token = auth.optString("access_token")
        if (token.isBlank()) throw Exception("Anmeldung fehlgeschlagen.")
        val userId = auth.optJSONObject("user")?.optString("id") ?: ""
        val rawBool = client.rpcRaw("is_untas_admin", accessToken = token).trim()
        if (rawBool != "true") throw Exception("Keine Admin-Berechtigung.")
        return UserSession(
            type = UserRole.ADMIN,
            accessToken = token,
            email = email,
            name = "Administrator",
            schoolDirectorId = userId
        )
    }

    suspend fun loginDirector(name: String, password: String): UserSession {
        val payload = JSONObject().apply {
            put("action", "login")
            put("name", name)
            put("password", password)
        }
        val res = client.callEdgeFunction("school-director-auth", payload)
        val token = res.optString("access_token")
        val schoolId = res.optString("school_id")
        if (token.isBlank() || schoolId.isBlank()) {
            throw Exception(res.optString("error", "Anmeldung fehlgeschlagen."))
        }
        return UserSession(
            type = UserRole.DIRECTOR,
            schoolId = schoolId,
            schoolDirectorId = res.optString("director_id"),
            name = res.optString("name", name),
            accessToken = token,
            refreshToken = res.optString("refresh_token")
        )
    }

    suspend fun loginTeacherOrClass(schoolId: String, username: String, password: String): UserSession {
        val teacherPayload = JSONObject().apply {
            put("p_school_id", schoolId)
            put("p_username", username)
            put("p_password", password)
        }
        val tRes = runCatching {
            val raw = client.rpcRaw("login_teacher", teacherPayload)
            JSONArray(raw)
        }.getOrNull()

        if (tRes != null && tRes.length() > 0) {
            val row = tRes.getJSONObject(0)
            return UserSession(
                type = UserRole.TEACHER,
                schoolId = schoolId,
                teacherId = row.optString("id"),
                teacherName = row.optString("name")
            )
        }

        val cRaw = client.rpcRaw("login_class", teacherPayload)
        val cArr = JSONArray(cRaw)
        if (cArr.length() == 0) throw Exception("Benutzername oder Passwort falsch.")
        val cRow = cArr.getJSONObject(0)
        return UserSession(
            type = UserRole.CLASS,
            schoolId = schoolId,
            classId = cRow.optString("id")
        )
    }

    // --- Updates ---
    suspend fun loadUpdates(accessToken: String? = null): List<NexoraUpdate> {
        val arr = client.restGetArray("nexora_updates?select=*&order=created_at.desc", accessToken)
        return (0 until arr.length()).map { idx ->
            val u = arr.getJSONObject(idx)
            NexoraUpdate(
                id = u.str("id"),
                title = u.str("title", "Update"),
                description = u.str("description"),
                createdBy = u.str("created_by"),
                createdAt = u.str("created_at")
            )
        }
    }

    suspend fun publishUpdate(title: String, description: String, createdBy: String, accessToken: String) {
        val body = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("created_by", createdBy)
        }
        client.restMutate("POST", "nexora_updates", body.toString(), accessToken)
    }

    suspend fun deleteUpdate(id: String, accessToken: String) {
        client.restMutate("DELETE", "nexora_updates?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Directors ---
    suspend fun loadDirectors(accessToken: String): List<SchoolDirector> {
        val arr = client.restGetArray(
            "school_directors?select=id,display_name,school_id,created_at&order=created_at.desc",
            accessToken
        )
        return (0 until arr.length()).map { idx ->
            val d = arr.getJSONObject(idx)
            SchoolDirector(
                id = d.str("id"),
                displayName = d.str("display_name"),
                schoolId = d.str("school_id"),
                createdAt = d.str("created_at")
            )
        }
    }

    suspend fun createDirector(schoolId: String, name: String, password: String, accessToken: String) {
        val payload = JSONObject().apply {
            put("action", "create")
            put("school_id", schoolId)
            put("name", name)
            put("password", password)
        }
        client.callEdgeFunction("school-director-auth", payload, accessToken)
    }

    suspend fun deleteDirector(directorId: String, accessToken: String) {
        val payload = JSONObject().apply {
            put("action", "delete")
            put("director_id", directorId)
        }
        client.callEdgeFunction("school-director-auth", payload, accessToken)
    }

    // --- Schools ---
    suspend fun saveSchool(id: String?, name: String, city: String, accessToken: String?) {
        val body = JSONObject().apply {
            put("name", name)
            put("city", city)
            if (id.isNullOrBlank()) put("announcements", JSONArray())
        }
        if (id.isNullOrBlank()) {
            client.restMutate("POST", "schools", body.toString(), accessToken)
        } else {
            client.restMutate("PATCH", "schools?id=eq.${client.encode(id)}", body.toString(), accessToken)
        }
    }

    suspend fun deleteSchool(id: String, accessToken: String?) {
        client.restMutate("DELETE", "schools?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    suspend fun updateSchoolAnnouncements(schoolId: String, announcements: List<String>, accessToken: String?) {
        val body = JSONObject().apply {
            put("announcements", JSONArray(announcements))
        }
        client.restMutate("PATCH", "schools?id=eq.${client.encode(schoolId)}", body.toString(), accessToken)
    }

    // --- Classes ---
    suspend fun saveClass(
        id: String?,
        schoolId: String,
        name: String,
        username: String,
        password: String,
        scopedSchoolId: String?,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("name", name)
            put("username", username)
            if (password.isNotBlank()) put("password_hash", password)
        }
        if (id.isNullOrBlank()) {
            client.restMutate("POST", "classes", body.toString(), accessToken)
        } else {
            val filter = if (!scopedSchoolId.isNullOrBlank()) {
                "classes?id=eq.${client.encode(id)}&school_id=eq.${client.encode(scopedSchoolId)}"
            } else {
                "classes?id=eq.${client.encode(id)}"
            }
            client.restMutate("PATCH", filter, body.toString(), accessToken)
        }
    }

    suspend fun deleteClass(id: String, scopedSchoolId: String?, accessToken: String?) {
        val filter = if (!scopedSchoolId.isNullOrBlank()) {
            "classes?id=eq.${client.encode(id)}&school_id=eq.${client.encode(scopedSchoolId)}"
        } else {
            "classes?id=eq.${client.encode(id)}"
        }
        client.restMutate("DELETE", filter, accessToken = accessToken)
    }

    // --- Students ---
    suspend fun addStudent(schoolId: String, classId: String, name: String, accessToken: String?) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("class_id", classId)
            put("name", name)
        }
        client.restMutate("POST", "students", body.toString(), accessToken)
    }

    suspend fun deleteStudent(studentId: String, accessToken: String?) {
        client.restMutate("DELETE", "students?id=eq.${client.encode(studentId)}", accessToken = accessToken)
    }

    // --- Teachers ---
    suspend fun saveTeacher(
        id: String?,
        schoolId: String,
        name: String,
        username: String,
        subjects: String,
        password: String,
        scopedSchoolId: String?,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("name", name)
            put("username", username)
            put("subject", subjects)
            if (password.isNotBlank()) put("password_hash", password)
        }
        if (id.isNullOrBlank()) {
            client.restMutate("POST", "teachers", body.toString(), accessToken)
        } else {
            val filter = if (!scopedSchoolId.isNullOrBlank()) {
                "teachers?id=eq.${client.encode(id)}&school_id=eq.${client.encode(scopedSchoolId)}"
            } else {
                "teachers?id=eq.${client.encode(id)}"
            }
            client.restMutate("PATCH", filter, body.toString(), accessToken)
        }
    }

    suspend fun deleteTeacher(teacherId: String, accessToken: String?) {
        client.restMutate("DELETE", "teachers?id=eq.${client.encode(teacherId)}", accessToken = accessToken)
    }

    // --- Lessons ---
    suspend fun saveTeacherLesson(
        teacherId: String,
        lessonId: String,
        week: String,
        isExam: Boolean,
        homeworkText: String,
        homeworkDate: String
    ) {
        val payload = JSONObject().apply {
            put("p_teacher_id", teacherId)
            put("p_lesson_id", lessonId)
            put("p_week", week)
            put("p_is_exam", isExam)
            put("p_homework_text", homeworkText)
            put("p_homework_date", if (homeworkDate.isBlank()) JSONObject.NULL else homeworkDate)
        }
        client.rpcRaw("teacher_save_lesson", payload)
    }

    suspend fun saveAdminOrDirectorLesson(
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
        accessToken: String?
    ) {
        val notePacked = NexoraConstants.packLessonNote("", colorStyle, isExam)
        val lessonBody = JSONObject().apply {
            put("school_id", schoolId)
            put("class_id", classId)
            put("day", day)
            put("time_index", timeIndex)
            put("subject", subject)
            put("teacher", teacher)
            put("room", room)
            put("note", notePacked)
        }
        var resolvedId = lessonId?.takeIf { it.isNotBlank() }
        if (resolvedId != null) {
            client.restMutate("PATCH", "lessons?id=eq.${client.encode(resolvedId)}", lessonBody.toString(), accessToken)
        } else {
            val res = client.restMutate("POST", "lessons", lessonBody.toString(), accessToken)
            if (res.length() > 0) resolvedId = res.getJSONObject(0).optString("id")
        }

        if (!resolvedId.isNullOrBlank()) {
            var homeworkId = ""
            if (lessonId != null && lessonId.isNotBlank()) {
                if (homeworkText.isNotBlank()) {
                    val hwTitle = "$subject Hausaufgabe"
                    val hwBody = JSONObject().apply {
                        put("title", hwTitle)
                        put("text", homeworkText)
                        put("due_date", homeworkDate)
                        put("class_id", classId)
                    }
                    if (existingHomeworkId.isNotBlank()) {
                        client.restMutate(
                            "PATCH",
                            "homework?id=eq.${client.encode(existingHomeworkId)}",
                            hwBody.toString(),
                            accessToken
                        )
                        homeworkId = existingHomeworkId
                    } else {
                        val hwRes = client.restMutate("POST", "homework", hwBody.toString(), accessToken)
                        if (hwRes.length() > 0) homeworkId = hwRes.getJSONObject(0).optString("id")
                    }
                } else if (existingHomeworkId.isNotBlank()) {
                    client.restMutate(
                        "DELETE",
                        "homework?id=eq.${client.encode(existingHomeworkId)}",
                        accessToken = accessToken
                    )
                }
            }

            val ovBody = JSONObject().apply {
                put("status", status)
                put("sub_teacher", subTeacher)
                put("sub_room", subRoom)
                put("is_exam", isExam)
                put("homework_id", if (homeworkId.isBlank()) JSONObject.NULL else homeworkId)
            }
            if (existingOverride != null && existingOverride.id.isNotBlank()) {
                client.restMutate(
                    "PATCH",
                    "week_overrides?id=eq.${client.encode(existingOverride.id)}",
                    ovBody.toString(),
                    accessToken
                )
            } else if (status != "normal" || isExam || homeworkId.isNotBlank()) {
                ovBody.put("lesson_id", resolvedId)
                ovBody.put("week", week)
                client.restMutate("POST", "week_overrides", ovBody.toString(), accessToken)
            }
        }
    }

    suspend fun deleteLesson(lessonId: String, accessToken: String?) {
        client.restMutate("DELETE", "lessons?id=eq.${client.encode(lessonId)}", accessToken = accessToken)
    }

    // --- Homework ---
    suspend fun saveHomework(title: String, text: String, dueDate: String, classId: String, accessToken: String?) {
        val body = JSONObject().apply {
            put("title", title)
            put("text", text)
            put("due_date", dueDate)
            put("class_id", classId)
        }
        client.restMutate("POST", "homework", body.toString(), accessToken)
    }

    suspend fun deleteHomework(id: String, accessToken: String?) {
        client.restMutate("DELETE", "homework?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Messages ---
    suspend fun saveMessage(title: String, text: String, accessToken: String?) {
        val body = JSONObject().apply {
            put("title", title)
            put("text", text)
        }
        client.restMutate("POST", "messages", body.toString(), accessToken)
    }

    suspend fun deleteMessage(id: String, accessToken: String?) {
        client.restMutate("DELETE", "messages?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Calendar ---
    suspend fun saveCalendarEvent(
        schoolId: String,
        title: String,
        eventDate: String,
        text: String,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("title", title)
            put("event_date", eventDate)
            put("text", text)
        }
        client.restMutate("POST", "calendar", body.toString(), accessToken)
    }

    suspend fun deleteCalendarEvent(id: String, scopedSchoolId: String?, accessToken: String?) {
        val filter = if (!scopedSchoolId.isNullOrBlank()) {
            "calendar?id=eq.${client.encode(id)}&school_id=eq.${client.encode(scopedSchoolId)}"
        } else {
            "calendar?id=eq.${client.encode(id)}"
        }
        client.restMutate("DELETE", filter, accessToken = accessToken)
    }

    // --- Attendance ---
    suspend fun upsertAttendanceBatch(
        schoolId: String,
        classId: String,
        date: String,
        timeIndex: Int,
        rows: List<Triple<String, String, Int>>,
        accessToken: String?
    ) {
        for ((studentName, status, lateMinutes) in rows) {
            val payload = JSONObject().apply {
                put("school_id", schoolId)
                put("class_id", classId)
                put("student_name", studentName)
                put("date", date)
                put("time_index", timeIndex)
                put("status", status)
                put("late_minutes", lateMinutes)
            }
            val qs = "attendance?select=id" +
                "&school_id=eq.${client.encode(schoolId)}" +
                "&class_id=eq.${client.encode(classId)}" +
                "&student_name=eq.${client.encode(studentName)}" +
                "&date=eq.${client.encode(date)}" +
                "&time_index=eq.$timeIndex&limit=1"
            val existing = client.restGetArray(qs, accessToken)
            if (existing.length() > 0) {
                val id = existing.getJSONObject(0).optString("id")
                client.restMutate("PATCH", "attendance?id=eq.${client.encode(id)}", payload.toString(), accessToken)
            } else {
                client.restMutate("POST", "attendance", payload.toString(), accessToken)
            }
        }
    }

    suspend fun updateAttendance(
        id: String,
        date: String,
        timeIndex: Int,
        status: String,
        lateMinutes: Int,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("date", date)
            put("time_index", timeIndex)
            put("status", status)
            put("late_minutes", lateMinutes)
        }
        client.restMutate("PATCH", "attendance?id=eq.${client.encode(id)}", body.toString(), accessToken)
    }

    suspend fun deleteAttendance(id: String, accessToken: String?) {
        client.restMutate("DELETE", "attendance?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Classbook Entries ---
    suspend fun saveClassbookEntry(
        schoolId: String,
        classId: String,
        studentName: String,
        entryType: String,
        title: String,
        description: String,
        entryDate: String,
        author: String,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("class_id", classId)
            put("student_name", studentName)
            put("entry_type", entryType)
            put("title", title)
            put("description", description)
            put("entry_date", entryDate)
            put("author", author)
        }
        val saved = client.restMutate("POST", "classbook_entries", body.toString(), accessToken)
        if (saved.length() == 0 || saved.getJSONObject(0).optString("id").isBlank()) {
            throw Exception("Supabase hat keinen gespeicherten Datensatz zurückgegeben.")
        }
    }

    suspend fun updateClassbookEntry(
        id: String,
        title: String,
        description: String,
        entryType: String,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("entry_type", entryType)
        }
        client.restMutate("PATCH", "classbook_entries?id=eq.${client.encode(id)}", body.toString(), accessToken)
    }

    suspend fun deleteClassbookEntry(id: String, accessToken: String?) {
        client.restMutate("DELETE", "classbook_entries?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Grades ---
    suspend fun saveGrade(
        schoolId: String,
        classId: String,
        studentName: String,
        subject: String,
        gradeValue: String,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", schoolId)
            put("class_id", classId)
            put("student_name", studentName)
            put("subject", subject)
            put("grade_type", "Leistungsnachweis")
            put("grade_value", gradeValue)
        }
        client.restMutate("POST", "grades", body.toString(), accessToken)
    }

    suspend fun deleteGrade(id: String, accessToken: String?) {
        client.restMutate("DELETE", "grades?id=eq.${client.encode(id)}", accessToken = accessToken)
    }

    // --- Problem Reports ---
    suspend fun loadProblemReports(accessToken: String?): List<ProblemReport> {
        val arr = client.restGetArray("problem_reports?select=*&order=created_at.desc", accessToken)
        return (0 until arr.length()).map { idx ->
            val r = arr.getJSONObject(idx)
            ProblemReport(
                id = r.str("id"),
                schoolId = r.str("school_id"),
                reporterType = r.str("reporter_type", "Unbekannt"),
                reporterName = r.str("reporter_name"),
                title = r.str("title"),
                description = r.str("description"),
                status = r.str("status", "offen"),
                createdAt = r.str("created_at")
            )
        }
    }

    suspend fun submitProblemReport(
        schoolId: String?,
        reporterType: String,
        reporterName: String,
        title: String,
        description: String,
        accessToken: String?
    ) {
        val body = JSONObject().apply {
            put("school_id", if (schoolId.isNullOrBlank()) JSONObject.NULL else schoolId)
            put("reporter_type", reporterType)
            put("reporter_name", reporterName)
            put("title", title)
            put("description", description)
            put("status", "offen")
        }
        client.restMutate("POST", "problem_reports", body.toString(), accessToken)
    }

    suspend fun deleteProblemReport(id: String, accessToken: String) {
        client.restMutate("DELETE", "problem_reports?id=eq.${client.encode(id)}", accessToken = accessToken)
    }
}
