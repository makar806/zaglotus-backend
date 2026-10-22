package ru.zaglotus.domain.repository

import ru.zaglotus.domain.model.Pet
import java.util.UUID

interface PetRepository {
    fun create(pet: Pet)
    fun findByUserId(userId: UUID): Pet?
    fun findByUserIdForUpdate(userId: UUID): Pet?
    fun update(pet: Pet)
}
