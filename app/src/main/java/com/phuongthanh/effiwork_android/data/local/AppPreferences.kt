package com.phuongthanh.effiwork_android.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSelectedProjectId(projectId: String) {
        prefs.edit().putString(KEY_SELECTED_PROJECT_ID, projectId).apply()
    }

    fun getSelectedProjectId(): String? {
        return prefs.getString(KEY_SELECTED_PROJECT_ID, null)
    }

    fun clearSelectedProjectId() {
        prefs.edit().remove(KEY_SELECTED_PROJECT_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "effiwork_prefs"
        private const val KEY_SELECTED_PROJECT_ID = "selected_project_id"
    }
}