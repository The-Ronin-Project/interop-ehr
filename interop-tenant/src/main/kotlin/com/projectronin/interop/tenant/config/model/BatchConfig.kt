package com.projectronin.interop.tenant.config.model

import java.time.LocalTime

/**
 * Configuration associated to batch data pulls for a tenant.
 * @property availableStart The time at which a batch can start.
 * @property availableEnd The time by which a batch should end.
 */
data class BatchConfig(
    val availableStart: LocalTime,
    val availableEnd: LocalTime
)
