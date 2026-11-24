package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Lesson
import com.example.data.NexoraConstants
import com.example.data.NexoraHelpers
import com.example.data.SessionStore
import com.example.data.Teacher
import com.example.data.UserRole
import com.example.data.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexora", appName)
    }

    @Test
    fun `lesson metadata packs and parses cleanly`() {
        val color = NexoraConstants.LESSON_COLORS[2].cssStyle
        val packed = NexoraConstants.packLessonNote("Raumänderung", color, true)
        val parsed = NexoraConstants.parseLessonMeta(packed)
        assertEquals("Raumänderung", parsed.note)
        assertEquals(color, parsed.colorStyle)
        assertTrue(parsed.isExam)
    }

    @Test
    fun `teacher ownership matches name or username ignoring honorifics`() {
        val teacher = Teacher("t1", "s1", "Herr Müller", "MUE", "", "Mathematik")
        val lessonByName = Lesson("l1", "s1", "c1", 0, 0, "Mathematik", "Müller", "R101", "", "", false)
        val lessonByCode = Lesson("l2", "s1", "c1", 0, 1, "Mathematik", "MUE", "R101", "", "", false)
        val otherLesson = Lesson("l3", "s1", "c1", 0, 2, "Mathematik", "Frau Schmidt", "R102", "", "", false)

        assertTrue(NexoraHelpers.teacherOwnsLesson(lessonByName, teacher))
        assertTrue(NexoraHelpers.teacherOwnsLesson(lessonByCode, teacher))
        assertFalse(NexoraHelpers.teacherOwnsLesson(otherLesson, teacher))
    }

    @Test
    fun `session store persists and restores session`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)
        val session = UserSession(
            type = UserRole.TEACHER,
            schoolId = "school-1",
            teacherId = "teacher-1",
            teacherName = "Frau Meyer"
        )
        store.saveSession(session)
        val restored = store.loadSession()
        assertEquals(UserRole.TEACHER, restored?.type)
        assertEquals("school-1", restored?.schoolId)
        assertEquals("teacher-1", restored?.teacherId)
        store.clearAll()
    }
}
