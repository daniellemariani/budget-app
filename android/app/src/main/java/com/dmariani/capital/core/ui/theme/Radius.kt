package com.dmariani.capital.core.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Radius(
    val sm: Dp = 6.dp,
    val md: Dp = 10.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
)

val LocalRadius = compositionLocalOf { Radius() }
