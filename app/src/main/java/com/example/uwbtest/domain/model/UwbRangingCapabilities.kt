package com.example.uwbtest.domain.model

data class UwbRangingCapabilities(
    val isDistanceSupported: Boolean,
    val isAzimuthalAngleSupported: Boolean,
    val isElevationAngleSupported: Boolean,
    val minRangingInterval: Int,
    val supportedChannels: Set<Int>,
    val supportedNtfConfigs: Set<Int>,
    val supportedConfigIds: Set<Int>,
    val supportedSlotDurations: Set<Int>,
    val supportedRangingUpdateRates: Set<Int>,
    val isRangingIntervalReconfigureSupported: Boolean,
    val isBackgroundRangingSupported: Boolean,
)
