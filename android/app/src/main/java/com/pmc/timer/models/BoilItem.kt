package com.pmc.timer.models

import java.util.UUID

data class BoilItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val minutes: Double
)
