package com.example.data

import android.content.Context
import org.json.JSONObject

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("nexora_prefs_v15", Context.MODE_PRIVATE)

    fun saveSession(session: UserSession?) {
        if (session == null) {
            prefs.edit().remove(KEY_SESSION).apply()
            return
        }
        val obj = JSONObject().apply {
            put("type", session.type.raw)
            put("schoolId", session.schoolId)
            put("classId", session.classId)
            put("teacherId", session.teacherId)
            put("teacherName", session.teacherName)
            put("schoolDirectorId", session.schoolDirectorId)
            put("name", session.name)
            put("email", session.email)
            put("accessToken", session.accessToken)
            put("refreshToken", session.refreshToken)
        }
        prefs.edit().putString(KEY_SESSION, obj.toString()).apply()
    }

    fun loadSession(): UserSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val role = UserRole.fromRaw(obj.optString("type")) ?: return null
            UserSession(
                type = role,
                schoolId = obj.optString("schoolId"),
                classId = obj.optString("classId"),
                teacherId = obj.optString("teacherId"),
                teacherName = obj.optString("teacherName"),
                schoolDirectorId = obj.optString("schoolDirectorId"),
                name = obj.optString("name"),
                email = obj.optString("email"),
                accessToken = obj.optString("accessToken"),
                refreshToken = obj.optString("refreshToken")
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveCurrentPage(page: AppPage?) {
        if (page == null) prefs.edit().remove(KEY_PAGE).apply()
        else prefs.edit().putString(KEY_PAGE, page.key).apply()
    }

    fun loadCurrentPage(): AppPage? = AppPage.fromKey(prefs.getString(KEY_PAGE, null))

    fun clearAll() {
        prefs.edit().remove(KEY_SESSION).remove(KEY_PAGE).apply()
    }

    fun checkAndSyncMondayKey(currentMondayKey: String): Boolean {
        val last = prefs.getString(KEY_LAST_MONDAY, null)
        return if (last != currentMondayKey) {
            prefs.edit().putString(KEY_LAST_MONDAY, currentMondayKey).apply()
            true
        } else {
            false
        }
    }

    companion object {
        private const val KEY_SESSION = "untas_session_v15"
        private const val KEY_PAGE = "untas_page_v15"
        private const val KEY_LAST_MONDAY = "untas_last_monday_v15"
    }
}
