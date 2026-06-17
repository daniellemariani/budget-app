package com.dmariani.capital.core.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        context.getSharedPreferences("capital_prefs", Context.MODE_PRIVATE)
    }

    fun saveDisplayName(name: String) {
        prefs.edit {
            putString(PreferenceKeys.DISPLAY_NAME, name)
            putBoolean(PreferenceKeys.ONBOARDING_COMPLETED, true)
        }
    }

    fun getDisplayName(): String? =
        prefs.getString(PreferenceKeys.DISPLAY_NAME, null)

    fun isOnboardingCompleted(): Boolean =
        prefs.getBoolean(PreferenceKeys.ONBOARDING_COMPLETED, false)
}
