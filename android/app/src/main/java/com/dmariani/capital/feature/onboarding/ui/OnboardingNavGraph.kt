package com.dmariani.capital.feature.onboarding.ui

import androidx.compose.runtime.Composable
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
) {
    val navController = rememberNavController()

    // TODO TSK-ON-16: Obtain OnboardingViewModel via hiltViewModel() and collect side effects:
    //   NavigateToSetYourName  → navController.navigate(OnboardingRoutes.SET_YOUR_NAME)
    //   NavigateToAddAnAccount → navController.navigate(OnboardingRoutes.ADD_AN_ACCOUNT)
    //   NavigateToHome         → onOnboardingComplete()

    NavHost(
        navController = navController,
        startDestination = OnboardingRoutes.FEATURE_SLIDES,
    ) {
        composable(OnboardingRoutes.FEATURE_SLIDES) {
            // TODO TSK-ON-20: Replace with FeatureSlidesScreen
        }
        composable(OnboardingRoutes.SET_YOUR_NAME) {
            // TODO TSK-ON-21: Replace with SetYourNameScreen
        }
        composable(OnboardingRoutes.ADD_AN_ACCOUNT) {
            // TODO TSK-ON-22: Replace with AddAnAccountScreen
        }
    }
}
