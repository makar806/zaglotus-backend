package ru.zaglotus.application.persistence

import ru.zaglotus.application.transaction.JdbcConnectionContext
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.repository.PetRepository
import java.sql.ResultSet
import java.util.UUID

class JdbcPetRepository(
    private val connections: JdbcConnectionContext
) : PetRepository {
    override fun create(pet: Pet) {
        connections.withConnection { connection ->
            connection.prepareStatement(
                "INSERT INTO pets(user_id, name, experience, meals, version) VALUES (?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setObject(1, pet.userId)
                statement.setString(2, pet.name)
                statement.setInt(3, pet.experience)
                statement.setInt(4, pet.meals)
                statement.setLong(5, pet.version)
                statement.executeUpdate()
            }
        }
    }

    override fun findByUserId(userId: UUID): Pet? = find(userId, false)

    override fun findByUserIdForUpdate(userId: UUID): Pet? = find(userId, true)

    override fun update(pet: Pet) {
        connections.withConnection { connection ->
            connection.prepareStatement(
                "UPDATE pets SET name = ?, experience = ?, meals = ?, version = ? WHERE user_id = ? AND version = ?"
            ).use { statement ->
                statement.setString(1, pet.name)
                statement.setInt(2, pet.experience)
                statement.setInt(3, pet.meals)
                statement.setLong(4, pet.version)
                statement.setObject(5, pet.userId)
                statement.setLong(6, pet.version - 1)
                if (statement.executeUpdate() == 0) {
                    throw ConflictException("Питомец уже был изменён другим запросом")
                }
            }
        }
    }

    private fun find(userId: UUID, forUpdate: Boolean): Pet? = connections.withConnection { connection ->
        val lock = if (forUpdate) " FOR UPDATE" else ""
        connection.prepareStatement(
            "SELECT user_id, name, experience, meals, version FROM pets WHERE user_id = ?$lock"
        ).use { statement ->
            statement.setObject(1, userId)
            statement.executeQuery().use { result ->
                if (result.next()) result.toPet() else null
            }
        }
    }

    private fun ResultSet.toPet() = Pet(
        userId = getObject("user_id", UUID::class.java),
        name = getString("name"),
        experience = getInt("experience"),
        meals = getInt("meals"),
        version = getLong("version")
    )
}
