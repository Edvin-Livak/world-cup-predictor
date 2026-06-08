package com.sned.worldcuppredictor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String? = null,
    val username: String,

    @SerialName("pin_hash")
    val pinHash: String? = null
)