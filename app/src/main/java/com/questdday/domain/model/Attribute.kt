package com.questdday.domain.model

data class Attribute(
    val id: Long = 0,
    val code: String,
    val displayName: String,
    val icon: String?,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
