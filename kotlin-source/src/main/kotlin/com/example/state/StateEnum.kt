package com.example.state

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class StateEnum {
    DONE,
    READY
}
