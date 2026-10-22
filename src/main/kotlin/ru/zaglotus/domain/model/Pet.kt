package ru.zaglotus.domain.model

import ru.zaglotus.domain.exception.ValidationException
import java.util.UUID

data class Pet(
    val userId: UUID,
    val name: String,
    val experience: Int,
    val meals: Int,
    val version: Long
) {
    val level: Int
        get() = experience / 100 + 1

    fun feed(experienceReward: Int): Pet {
        if (experienceReward <= 0) throw ValidationException("Награда должна быть положительной")
        return copy(experience = experience + experienceReward, meals = meals + 1, version = version + 1)
    }

    companion object {
        fun create(userId: UUID, name: String): Pet {
            val cleanName = name.trim()
            if (cleanName.length !in 2..30) {
                throw ValidationException("Имя питомца должно содержать от 2 до 30 символов")
            }
            return Pet(userId, cleanName, 0, 0, 0)
        }
    }
}
