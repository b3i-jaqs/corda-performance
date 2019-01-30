package com.example.state

import net.corda.core.serialization.CordaSerializable
import java.util.UUID

@CordaSerializable
data class ParentEntity(
    val id: UUID,
    val other: OtherEntity,
    val children: List<ChildEntity>
)
