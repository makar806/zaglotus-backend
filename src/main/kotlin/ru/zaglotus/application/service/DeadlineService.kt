package ru.zaglotus.application.service

import org.slf4j.LoggerFactory
import ru.zaglotus.application.transaction.TransactionManager
import ru.zaglotus.domain.exception.NotFoundException
import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.DeadlineStatus
import ru.zaglotus.domain.model.Difficulty
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.repository.DeadlineRepository
import ru.zaglotus.domain.repository.PetRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

private val deadlineLogger = LoggerFactory.getLogger(DeadlineService::class.java)

data class CompletionResult(
    val deadline: Deadline,
    val pet: Pet,
    val gainedExperience: Int
)

class DeadlineService(
    private val deadlines: DeadlineRepository,
    private val pets: PetRepository,
    private val transactions: TransactionManager,
    private val clock: Clock = Clock.systemUTC()
) {
    fun create(userId: UUID, title: String, description: String?, dueAt: Instant, difficulty: Difficulty): Deadline {
        val deadline = Deadline.create(UUID.randomUUID(), userId, title, description, dueAt, difficulty, clock.instant())
        val created = deadlines.create(deadline)
        deadlineLogger.info("Deadline created: deadlineId={}, userId={}, difficulty={}", created.id, userId, difficulty)
        return created
    }

    fun list(userId: UUID, status: DeadlineStatus?): List<Deadline> =
        deadlines.findAll(userId, status)

    fun get(userId: UUID, deadlineId: UUID): Deadline =
        deadlines.findById(userId, deadlineId) ?: throw NotFoundException("Дедлайн не найден")

    fun update(
        userId: UUID,
        deadlineId: UUID,
        title: String,
        description: String?,
        dueAt: Instant,
        difficulty: Difficulty
    ): Deadline {
        val deadline = get(userId, deadlineId)
        val updated = deadline.update(title, description, dueAt, difficulty, clock.instant())
        deadlines.update(updated)
        deadlineLogger.info("Deadline updated: deadlineId={}, userId={}", deadlineId, userId)
        return updated
    }

    fun delete(userId: UUID, deadlineId: UUID) {
        if (!deadlines.deleteOpen(userId, deadlineId)) {
            throw NotFoundException("Открытый дедлайн не получилось удалить")
        }
        deadlineLogger.info("Deadline deleted: deadlineId={}, userId={}", deadlineId, userId)
    }

    fun complete(userId: UUID, deadlineId: UUID): CompletionResult = transactions.execute {
        val deadline = deadlines.findByIdForUpdate(userId, deadlineId)
            ?: throw NotFoundException("Дедлайн не найден")
        val pet = pets.findByUserIdForUpdate(userId)
            ?: throw NotFoundException("Питомец не найден")
        val completedDeadline = deadline.complete(clock.instant())
        val fedPet = pet.feed(deadline.difficulty.experienceReward)
        deadlines.update(completedDeadline)
        pets.update(fedPet)
        deadlineLogger.info(
            "Deadline completed: deadlineId={}, userId={}, gainedExperience={}",
            deadlineId,
            userId,
            deadline.difficulty.experienceReward
        )
        CompletionResult(completedDeadline, fedPet, deadline.difficulty.experienceReward)
    }
}
