package com.questdday.data.local.entity

import com.questdday.domain.model.Attribute

fun AttributeEntity.toDomainModel(): Attribute {
    return Attribute(
        id = id,
        code = code,
        displayName = displayName,
        icon = icon,
        isDefault = isDefault == 1,
        sortOrder = sortOrder
    )
}

fun Attribute.toEntity(): AttributeEntity {
    return AttributeEntity(
        id = id,
        code = code,
        displayName = displayName,
        icon = icon,
        isDefault = if (isDefault) 1 else 0,
        sortOrder = sortOrder
    )
}
