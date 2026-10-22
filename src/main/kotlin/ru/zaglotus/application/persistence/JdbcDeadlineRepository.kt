package ru.zaglotus.application.persistence

import ru.zaglotus.application.transaction.JdbcConnectionContext
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.DeadlineStatus
import ru.zaglotus.domain.model.Difficulty
import ru.zaglotus.domain.repository.DeadlineRepository
import java.sql.ResultSet
import java.util.UUID

class JdbcDeadlineRepository(
    private val connections: JdbcConnectionContext
) : DeadlineRepository {
    override fun create(deadline: Deadline): Deadline {
        connections.withConnection { connection ->
            connection.prepareStatement(
                "INSERT INTO deadlines(id, user_id, title, description, due_at, difficulty, status, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            ).use { statement ->
                statement.setObject(1, deadline.id)
                statement.setObject(2, deadline.userId)
                statement.setString(3, deadline.title)
                statement.setString(4, deadline.description)
                statement.setTimestamp(5, java.sql.Timestamp.from(deadline.dueAt))
                statement.setString(6, deadline.difficulty.name)
                statement.setString(7, deadline.status.name)
                statement.setTimestamp(8, java.sql.Timestamp.from(deadline.createdAt))
                statement.setTimestamp(9, deadline.completedAt?.let(java.sql.Timestamp::from))
                statement.executeUpdate()
            }
        }
        return deadline
    }

    override fun findById(userId: UUID, deadlineId: UUID): Deadline? =
        find(userId, deadlineId, false)

    override fun findByIdForUpdate(userId: UUID, deadlineId: UUID): Deadline? =
        find(userId, deadlineId, true)

    override fun findAll(userId: UUID, status: DeadlineStatus?): List<Deadline> =
        connections.withConnection { connection ->
            val sql = buildString {
                append("SELECT * FROM deadlines WHERE user_id = ?")
                if (status != null) append(" AND status = ?")
                append(" ORDER BY due_at, created_at")
            }
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, userId)
                if (status != null) statement.setString(2, status.name)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) add(result.toDeadline())
                    }
                }
            }
        }

    override fun update(deadline: Deadline) {
        if (deadline.status == DeadlineStatus.COMPLETED) {
            complete(deadline)
        } else {
            updateOpen(deadline)
        }
    }

    override fun deleteOpen(userId: UUID, deadlineId: UUID): Boolean =
        connections.withConnection { connection ->
            connection.prepareStatement(
                "DELETE FROM deadlines WHERE id = ? AND user_id = ? AND status = 'OPEN'"
            ).use { statement ->
                statement.setObject(1, deadlineId)
                statement.setObject(2, userId)
                statement.executeUpdate() == 1
            }
        }

    private fun updateOpen(deadline: Deadline) {
        connections.withConnection { connection ->
            connection.prepareStatement(
                "UPDATE deadlines SET title = ?, description = ?, due_at = ?, difficulty = ? WHERE id = ? AND user_id = ? AND status = 'OPEN'"
            ).use { statement ->
                statement.setString(1, deadline.title)
                statement.setString(2, deadline.description)
                statement.setTimestamp(3, java.sql.Timestamp.from(deadline.dueAt))
                statement.setString(4, deadline.difficulty.name)
                statement.setObject(5, deadline.id)
                statement.setObject(6, deadline.userId)
                if (statement.executeUpdate() == 0) {
                    throw ConflictException("Дедлайн уже был закрыт или удалён")
                }
            }
        }
    }

    private fun complete(deadline: Deadline) {
        connections.withConnection { connection ->
            connection.prepareStatement(
                "UPDATE deadlines SET status = 'COMPLETED', completed_at = ? WHERE id = ? AND user_id = ? AND status = 'OPEN'"
            ).use { statement ->
                statement.setTimestamp(1, java.sql.Timestamp.from(requireNotNull(deadline.completedAt)))
                statement.setObject(2, deadline.id)
                statement.setObject(3, deadline.userId)
                if (statement.executeUpdate() == 0) {
                    throw ConflictException("Дедлайн уже закрыт")
                }
            }
        }
    }

    private fun find(userId: UUID, deadlineId: UUID, forUpdate: Boolean): Deadline? =
        connections.withConnection { connection ->
            val lock = if (forUpdate) " FOR UPDATE" else ""
            connection.prepareStatement(
                "SELECT * FROM deadlines WHERE id = ? AND user_id = ?$lock"
            ).use { statement ->
                statement.setObject(1, deadlineId)
                statement.setObject(2, userId)
                statement.executeQuery().use { result ->
                    if (result.next()) result.toDeadline() else null
                }
            }
        }

    private fun ResultSet.toDeadline() = Deadline(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        title = getString("title"),
        description = getString("description"),
        dueAt = getTimestamp("due_at").toInstant(),
        difficulty = Difficulty.valueOf(getString("difficulty")),
        status = DeadlineStatus.valueOf(getString("status")),
        createdAt = getTimestamp("created_at").toInstant(),
        completedAt = getTimestamp("completed_at")?.toInstant()
    )
}
