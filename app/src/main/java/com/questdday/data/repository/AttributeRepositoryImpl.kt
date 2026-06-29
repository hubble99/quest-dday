package com.questdday.data.repository

import com.questdday.data.local.dao.AttributeDao
import com.questdday.data.local.dao.QuestDao
import com.questdday.data.local.entity.toDomainModel
import com.questdday.data.local.entity.toEntity
import com.questdday.domain.exception.QuestValidationException
import com.questdday.domain.model.Attribute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AttributeRepositoryImpl(
    private val attributeDao: AttributeDao,
    private val questDao: QuestDao
) : AttributeRepository {

    override fun getAllAttributes(): Flow<List<Attribute>> {
        return attributeDao.getAllAttributes().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAttributeById(id: Long): Attribute? {
        return attributeDao.getAttributeById(id)?.toDomainModel()
    }

    override suspend fun addCustomAttribute(code: String, name: String, icon: String?): Long {
        if (name.isBlank()) {
            throw QuestValidationException("Attribute name cannot be empty")
        }
        val existingCount = attributeDao.countAttributeByCode(code)
        if (existingCount > 0) {
            throw QuestValidationException("Attribute code '$code' already exists")
        }

        val newAttribute = Attribute(
            code = code,
            displayName = name,
            icon = icon,
            isDefault = false,
            sortOrder = 100 // Custom attributes can have a high sort order or we could calculate the max
        )
        return attributeDao.insertAttribute(newAttribute.toEntity())
    }

    override suspend fun updateAttribute(id: Long, name: String, icon: String?) {
        if (name.isBlank()) {
            throw QuestValidationException("Attribute name cannot be empty")
        }
        attributeDao.updateAttribute(id, name, icon)
    }

    override suspend fun deleteCustomAttribute(id: Long) {
        val attribute = attributeDao.getAttributeById(id) ?: return
        if (attribute.isDefault == 1) {
            throw QuestValidationException("Cannot delete a default attribute")
        }

        val activeQuestsCount = questDao.countActiveQuestsByAttribute(id)
        if (activeQuestsCount > 0) {
            throw QuestValidationException("Cannot delete attribute as it is being used by $activeQuestsCount active quest(s)")
        }

        attributeDao.deleteCustomAttribute(id)
    }
}
