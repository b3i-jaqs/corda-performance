package com.example.state

import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.util.UUID

@CordaSerializable
data class ChildEntity(
    val id: UUID,
    val name: String,
    val age: BigDecimal,
    val state: StateEnum
)
