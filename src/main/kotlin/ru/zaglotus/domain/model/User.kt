package ru.zaglotus.domain.model

import ru.zaglotus.domain.exception.ValidationException
import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val name: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Instant
) {
    companion object {
        private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun create(id: UUID, name: String, email: String, passwordHash: String, createdAt: Instant): User {
            val cleanName = normalizeName(name)
            val normalizedEmail = normalizeEmail(email)
            return User(id, cleanName, normalizedEmail, passwordHash, createdAt)
        }

        fun normalizeName(name: String): String {
            val cleanName = name.trim()
            if (cleanName.length !in 2..50) {
                throw ValidationException("Имя пользователя должно содержать от 2 до 50 символов")
            }
            return cleanName
        }

        fun normalizeEmail(email: String): String {
            val normalized = email.trim().lowercase()
            if (normalized.length !in 3..254 || !emailPattern.matches(normalized)) {
                throw ValidationException("Некорректный email")
            }
            return normalized
        }

        fun validatePassword(password: String) {
            if (password.length !in 8..72) {
                throw ValidationException("Пароль должен содержать от 8 до 72 символов")
            }
        }
    }
}
