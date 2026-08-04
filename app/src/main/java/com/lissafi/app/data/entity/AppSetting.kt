package com.lissafi.app.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppSetting(
    val key: String,
    val value: String,
    @SerialName("user_id") val userId: String = ""
)
