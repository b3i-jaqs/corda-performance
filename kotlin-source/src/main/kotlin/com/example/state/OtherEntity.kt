package com.example.state

import net.corda.core.serialization.CordaSerializable
import java.time.Instant
import java.util.UUID

@CordaSerializable
data class OtherEntity(
    val id: UUID,
    val time: Instant,
    val note: String
)
