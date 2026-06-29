package com.questdday.data.repository

import com.questdday.domain.model.Attribute
import kotlinx.coroutines.flow.Flow

interface AttributeRepository {
    fun getAllAttributes(): Flow<List<Attribute>>
    suspend fun getAttributeById(id: Long): Attribute?
    suspend fun addCustomAttribute(code: String, name: String, icon: String?): Long
    suspend fun updateAttribute(id: Long, name: String, icon: String?)
    suspend fun deleteCustomAttribute(id: Long)
}
