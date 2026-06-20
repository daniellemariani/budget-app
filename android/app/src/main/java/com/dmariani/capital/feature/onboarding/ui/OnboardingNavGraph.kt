package com.dmariani.capital.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object OnboardingRoutes {
    const val FEATURE_SLIDES = "feature_slides"
    const val SET_YOUR_NAME = "set_your_name"
    const val ADD_AN_ACCOUNT = "add_an_account"
}

@Composable
fun OnboardingNavGraph(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                OnboardingSideEffect.NavigateToSetYourName ->
                    navController.navigate(OnboardingRoutes.SET_YOUR_NAME)
                OnboardingSideEffect.NavigateToAddAnAccount ->
                    navController.navigate(OnboardingRoutes.ADD_AN_ACCOUNT)
                OnboardingSideEffect.NavigateToHome ->
                    onOnboardingComplete()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = OnboardingRoutes.FEATURE_SLIDES,
    ) {
        composable(OnboardingRoutes.FEATURE_SLIDES) {
            // TODO TSK-ON-20: Replace with FeatureSlidesScreen(viewModel)
        }
        composable(OnboardingRoutes.SET_YOUR_NAME) {
            // TODO TSK-ON-21: Replace with SetYourNameScreen(viewModel)
        }
        composable(OnboardingRoutes.ADD_AN_ACCOUNT) {
            // TODO TSK-ON-22: Replace with AddAnAccountScreen(viewModel)
        }
    }
}
