package ru.zaglotus.domain.repository

import ru.zaglotus.domain.model.User

interface UserRepository {
    fun create(user: User)
    fun findByEmail(email: String): User?
}
