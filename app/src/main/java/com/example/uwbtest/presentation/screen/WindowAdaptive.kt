package com.example.uwbtest.presentation.screen

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable

internal const val EXPANDED_WIDTH_BREAKPOINT = 840

@Composable
internal fun isExpandedLayout(): Boolean =
    currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(EXPANDED_WIDTH_BREAKPOINT)
