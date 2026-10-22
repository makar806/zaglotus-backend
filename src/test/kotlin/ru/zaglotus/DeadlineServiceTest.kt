package ru.zaglotus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import ru.zaglotus.application.service.DeadlineService
import ru.zaglotus.application.transaction.TransactionManager
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.exception.ValidationException
import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.DeadlineStatus
import ru.zaglotus.domain.model.Difficulty
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.repository.DeadlineRepository
import ru.zaglotus.domain.repository.PetRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class DeadlineServiceTest {
    private val now = Instant.parse("2026-10-21T12:00:00Z")
    private val userId = UUID.randomUUID()
    private val deadlines = mutableMapOf<UUID, Deadline>()
    private val pets = mutableMapOf(userId to Pet.create(userId, "Жора"))
    private val deadlineRepository = object : DeadlineRepository {
        override fun create(deadline: Deadline) = deadline.also { deadlines[it.id] = it }
        override fun findById(userId: UUID, deadlineId: UUID) = deadlines[deadlineId]?.takeIf { it.userId == userId }
        override fun findByIdForUpdate(userId: UUID, deadlineId: UUID) = findById(userId, deadlineId)
        override fun findAll(userId: UUID, status: DeadlineStatus?) =
            deadlines.values.filter { it.userId == userId && (status == null || it.status == status) }

        override fun update(deadline: Deadline) {
            deadlines[deadline.id] = deadline
        }

        override fun deleteOpen(userId: UUID, deadlineId: UUID): Boolean {
            val deadline = deadlines[deadlineId]
            if (deadline?.userId != userId || deadline.status != DeadlineStatus.OPEN) return false
            deadlines.remove(deadlineId)
            return true
        }
    }
    private val petRepository = object : PetRepository {
        override fun create(pet: Pet) {
            pets[pet.userId] = pet
        }

        override fun findByUserId(userId: UUID) = pets[userId]
        override fun findByUserIdForUpdate(userId: UUID) = pets[userId]

        override fun update(pet: Pet) {
            pets[pet.userId] = pet
        }
    }
    private val transactions = object : TransactionManager {
        override fun <T> execute(block: () -> T): T = block()
    }
    private val service = DeadlineService(
        deadlineRepository,
        petRepository,
        transactions,
        Clock.fixed(now, ZoneOffset.UTC)
    )

    @Test
    fun `hard deadline gives one hundred experience`() {
        val deadline = service.create(
            userId,
            "Сдать лабораторную",
            null,
            now.plusSeconds(3600),
            Difficulty.HARD
        )

        val result = service.complete(userId, deadline.id)

        assertEquals(100, result.gainedExperience)
        assertEquals(100, result.pet.experience)
        assertEquals(1, result.pet.meals)
        assertEquals(2, result.pet.level)
        assertEquals(DeadlineStatus.COMPLETED, result.deadline.status)
    }

    @Test
    fun `deadline cannot be completed twice`() {
        val deadline = service.create(
            userId,
            "Сдать лабораторную",
            null,
            now.plusSeconds(3600),
            Difficulty.MEDIUM
        )
        service.complete(userId, deadline.id)

        assertFailsWith<ConflictException> {
            service.complete(userId, deadline.id)
        }
    }

    @Test
    fun `deadline in past is rejected`() {
        assertFailsWith<ValidationException> {
            service.create(
                userId,
                "Опоздавший дедлайн",
                null,
                now.minusSeconds(1),
                Difficulty.EASY
            )
        }
    }

    @Test
    fun `list can filter completed deadlines`() {
        val completed = service.create(
            userId,
            "Первый дедлайн",
            null,
            now.plusSeconds(3600),
            Difficulty.EASY
        )
        service.create(
            userId,
            "Второй дедлайн",
            null,
            now.plusSeconds(7200),
            Difficulty.MEDIUM
        )
        service.complete(userId, completed.id)

        assertEquals(1, service.list(userId, DeadlineStatus.COMPLETED).size)
        assertEquals(1, service.list(userId, DeadlineStatus.OPEN).size)
    }

    @Test
    fun `update does not create extra deadline`() {
        val deadline = service.create(
            userId,
            "Первое название",
            null,
            now.plusSeconds(3600),
            Difficulty.EASY
        )

        service.update(
            userId,
            deadline.id,
            "Новое название",
            "Описание",
            now.plusSeconds(7200),
            Difficulty.HARD
        )

        assertEquals(1, deadlines.size)
        assertEquals("Новое название", deadlines[deadline.id]?.title)
        assertEquals(Difficulty.HARD, deadlines[deadline.id]?.difficulty)
    }
}
