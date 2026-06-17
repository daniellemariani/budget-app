package com.dmariani.capital.feature.onboarding.di

import android.content.Context
import com.dmariani.capital.core.data.PreferencesDataSource
import com.dmariani.capital.feature.onboarding.data.OnboardingRepositoryImpl
import com.dmariani.capital.feature.onboarding.domain.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    @Binds
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl
    ): OnboardingRepository

    companion object {

        @Provides
        fun providePreferencesDataSource(
            @ApplicationContext context: Context
        ): PreferencesDataSource = PreferencesDataSource(context)
    }
}
