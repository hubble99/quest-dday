package com.questdday.data.repository

import kotlinx.coroutines.flow.Flow
import com.questdday.data.local.entity.AttributeEntity

interface AttributeRepository {
    fun getAllAttributes(): Flow<List<AttributeEntity>>
    suspend fun insertAttribute(attribute: AttributeEntity)
    // other operations...
}
