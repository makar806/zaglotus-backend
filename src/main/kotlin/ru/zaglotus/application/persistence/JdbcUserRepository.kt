package ru.zaglotus.application.persistence

import org.postgresql.util.PSQLException
import ru.zaglotus.application.transaction.JdbcConnectionContext
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.model.User
import ru.zaglotus.domain.repository.UserRepository
import java.sql.ResultSet

class JdbcUserRepository(
    private val connections: JdbcConnectionContext
) : UserRepository {
    override fun create(user: User) {
        try {
            connections.withConnection { connection ->
                connection.prepareStatement(
                    "INSERT INTO users(id, name, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)"
                ).use { statement ->
                    statement.setObject(1, user.id)
                    statement.setString(2, user.name)
                    statement.setString(3, user.email)
                    statement.setString(4, user.passwordHash)
                    statement.setTimestamp(5, java.sql.Timestamp.from(user.createdAt))
                    statement.executeUpdate()
                }
            }
        } catch (error: PSQLException) {
            if (error.sqlState == "23505") {
                throw ConflictException("Пользователь с таким email уже существует")
            }
            throw error
        }
    }

    override fun findByEmail(email: String): User? = connections.withConnection { connection ->
        connection.prepareStatement(
            "SELECT id, name, email, password_hash, created_at FROM users WHERE email = ?"
        ).use { statement ->
            statement.setString(1, email)
            statement.executeQuery().use { result ->
                if (result.next()) result.toUser() else null
            }
        }
    }

    private fun ResultSet.toUser() = User(
        id = getObject("id", java.util.UUID::class.java),
        name = getString("name"),
        email = getString("email"),
        passwordHash = getString("password_hash"),
        createdAt = getTimestamp("created_at").toInstant()
    )
}
