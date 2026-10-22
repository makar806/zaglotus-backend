package ru.zaglotus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.Difficulty
import ru.zaglotus.domain.model.Pet
import java.time.Instant
import java.util.UUID

class DomainRulesTest {
    @Test
    fun `pet gains level after enough experience`() {
        val pet = Pet.create(UUID.randomUUID(), "Жора")

        val fedPet = pet.feed(100)

        assertEquals(100, fedPet.experience)
        assertEquals(1, fedPet.meals)
        assertEquals(2, fedPet.level)
    }

    @Test
    fun `completed deadline cannot be changed`() {
        val now = Instant.parse("2026-10-21T12:00:00Z")
        val deadline = Deadline.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Сдать лабораторную",
            null,
            now.plusSeconds(3600),
            Difficulty.HARD,
            now
        ).complete(now.plusSeconds(1200))

        assertFailsWith<ConflictException> {
            deadline.update(
                "Другое название",
                null,
                now.plusSeconds(7200),
                Difficulty.EASY,
                now
            )
        }
    }
}
