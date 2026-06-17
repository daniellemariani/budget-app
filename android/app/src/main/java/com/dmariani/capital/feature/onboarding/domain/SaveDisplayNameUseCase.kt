package com.dmariani.capital.feature.onboarding.domain

import com.dmariani.capital.core.data.PreferencesDataSource
import javax.inject.Inject

class SaveDisplayNameUseCase @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
) {

    operator fun invoke(name: String) {
        preferencesDataSource.saveDisplayName(name.trim())
    }
}
