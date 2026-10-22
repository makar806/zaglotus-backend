package ru.zaglotus.domain.model

import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.exception.ValidationException
import java.time.Instant
import java.util.UUID

enum class Difficulty(val experienceReward: Int) {
    EASY(20),
    MEDIUM(50),
    HARD(100)
}

enum class DeadlineStatus {
    OPEN,
    COMPLETED
}

data class Deadline(
    val id: UUID,
    val userId: UUID,
    val title: String,
    val description: String?,
    val dueAt: Instant,
    val difficulty: Difficulty,
    val status: DeadlineStatus,
    val createdAt: Instant,
    val completedAt: Instant?
) {
    fun update(title: String, description: String?, dueAt: Instant, difficulty: Difficulty, now: Instant): Deadline {
        if (status == DeadlineStatus.COMPLETED) throw ConflictException("Закрытый дедлайн нельзя изменить")
        val values = validateValues(title, description, dueAt, now)
        return copy(title = values.first, description = values.second, dueAt = dueAt, difficulty = difficulty)
    }

    fun complete(completedAt: Instant): Deadline {
        if (status == DeadlineStatus.COMPLETED) throw ConflictException("Дедлайн уже закрыт")
        return copy(status = DeadlineStatus.COMPLETED, completedAt = completedAt)
    }

    companion object {
        fun create(
            id: UUID,
            userId: UUID,
            title: String,
            description: String?,
            dueAt: Instant,
            difficulty: Difficulty,
            createdAt: Instant
        ): Deadline {
            val values = validateValues(title, description, dueAt, createdAt)
            return Deadline(id, userId, values.first, values.second, dueAt, difficulty, DeadlineStatus.OPEN, createdAt, null)
        }

        private fun validateValues(title: String, description: String?, dueAt: Instant, now: Instant): Pair<String, String?> {
            val cleanTitle = title.trim()
            val cleanDescription = description?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanTitle.length !in 2..120) {
                throw ValidationException("Название должно содержать от 2 до 120 символов")
            }
            if (cleanDescription != null && cleanDescription.length > 1000) {
                throw ValidationException("Описание не должно быть длиннее 1000 символов")
            }
            if (!dueAt.isAfter(now)) throw ValidationException("Дедлайн должен быть в будущем")
            return cleanTitle to cleanDescription
        }
    }
}
