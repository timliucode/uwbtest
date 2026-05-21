package com.example.uwbtest.domain.model

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process cache for the last successful UWB capability check.
 * Written by [com.example.uwbtest.presentation.screen.capability.CapabilityCheckViewModel]
 * and read by [com.example.uwbtest.presentation.screen.ranging.RangingViewModel].
 */
@Singleton
class UwbCapabilityStore @Inject constructor() {
    var lastCapability: UwbCapability? = null
}
