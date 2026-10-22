package ru.zaglotus.application.service

import ru.zaglotus.domain.exception.NotFoundException
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.repository.PetRepository
import java.util.UUID

class PetService(private val pets: PetRepository) {
    fun get(userId: UUID): Pet =
        pets.findByUserId(userId) ?: throw NotFoundException("Питомец не найден")
}
