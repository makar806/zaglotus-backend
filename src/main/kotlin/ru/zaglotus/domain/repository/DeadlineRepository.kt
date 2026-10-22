package ru.zaglotus.domain.repository

import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.DeadlineStatus
import java.util.UUID

interface DeadlineRepository {
    fun create(deadline: Deadline): Deadline
    fun findById(userId: UUID, deadlineId: UUID): Deadline?
    fun findByIdForUpdate(userId: UUID, deadlineId: UUID): Deadline?
    fun findAll(userId: UUID, status: DeadlineStatus?): List<Deadline>
    fun update(deadline: Deadline)
    fun deleteOpen(userId: UUID, deadlineId: UUID): Boolean
}
