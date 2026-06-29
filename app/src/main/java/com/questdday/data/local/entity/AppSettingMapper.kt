package com.questdday.data.local.entity

import com.questdday.domain.model.AppSetting

fun AppSettingEntity.toDomain(): AppSetting {
    return AppSetting(
        key = key,
        value = value,
        updatedAt = updatedAt
    )
}

fun AppSetting.toEntity(): AppSettingEntity {
    return AppSettingEntity(
        key = key,
        value = value,
        updatedAt = updatedAt
    )
}
