package ru.zaglotus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import ru.zaglotus.application.security.PasswordHasher
import ru.zaglotus.application.security.TokenService
import ru.zaglotus.application.service.AuthService
import ru.zaglotus.application.transaction.TransactionManager
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.exception.UnauthorizedException
import ru.zaglotus.domain.exception.ValidationException
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.model.User
import ru.zaglotus.domain.repository.PetRepository
import ru.zaglotus.domain.repository.UserRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AuthServiceTest {
    private val users = mutableMapOf<String, User>()
    private val pets = mutableMapOf<UUID, Pet>()
    private val userRepository = object : UserRepository {
        override fun create(user: User) {
            users[user.email] = user
        }

        override fun findByEmail(email: String) = users[email]
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
    private val hasher = object : PasswordHasher {
        override fun hash(password: String) = "hash:$password"
        override fun verify(password: String, hash: String) = hash == "hash:$password"
    }
    private val tokens = object : TokenService {
        override fun create(userId: UUID) = "token:$userId"
    }
    private val service = AuthService(
        userRepository,
        petRepository,
        transactions,
        hasher,
        tokens,
        Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `registration creates user and pet`() {
        val result = service.register(" Иван ", " TEST@Example.com ", "password123", "Жора")

        assertEquals("Иван", users.values.single().name)
        assertEquals("test@example.com", users.values.single().email)
        assertEquals("Жора", pets[result.userId]?.name)
        assertEquals("token:${result.userId}", result.token)
    }

    @Test
    fun `duplicate email is rejected`() {
        service.register("Иван", "test@example.com", "password123", "Жора")

        assertFailsWith<ConflictException> {
            service.register("Пётр", "TEST@example.com", "password456", "Боб")
        }
    }

    @Test
    fun `wrong password is rejected`() {
        service.register("Иван", "test@example.com", "password123", "Жора")

        assertFailsWith<UnauthorizedException> {
            service.login("test@example.com", "wrong-password")
        }
    }

    @Test
    fun `short password is rejected`() {
        assertFailsWith<ValidationException> {
            service.register("Иван", "test@example.com", "short", "Жора")
        }
        assertNotEquals(1, users.size)
    }

    @Test
    fun `short user name is rejected`() {
        assertFailsWith<ValidationException> {
            service.register("И", "test@example.com", "password123", "Жора")
        }
    }
}
