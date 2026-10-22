package ru.zaglotus.application.service

import org.slf4j.LoggerFactory
import ru.zaglotus.application.security.PasswordHasher
import ru.zaglotus.application.security.TokenService
import ru.zaglotus.application.transaction.TransactionManager
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.exception.UnauthorizedException
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.model.User
import ru.zaglotus.domain.repository.PetRepository
import ru.zaglotus.domain.repository.UserRepository
import java.time.Clock
import java.util.UUID

private val authLogger = LoggerFactory.getLogger(AuthService::class.java)

data class AuthResult(val userId: UUID, val name: String, val token: String)

class AuthService(
    private val users: UserRepository,
    private val pets: PetRepository,
    private val transactions: TransactionManager,
    private val passwordHasher: PasswordHasher,
    private val tokens: TokenService,
    private val clock: Clock = Clock.systemUTC()
) {
    fun register(name: String, email: String, password: String, petName: String): AuthResult {
        User.validatePassword(password)
        val id = UUID.randomUUID()
        val pet = Pet.create(id, petName)
        val user = User.create(id, name, email, passwordHasher.hash(password), clock.instant())
        transactions.execute {
            if (users.findByEmail(user.email) != null) {
                throw ConflictException("Пользователь с таким email уже существует")
            }
            users.create(user)
            pets.create(pet)
        }
        authLogger.info("User registered: userId={}", id)
        return AuthResult(id, user.name, tokens.create(id))
    }

    fun login(email: String, password: String): AuthResult {
        val normalizedEmail = User.normalizeEmail(email)
        val user = users.findByEmail(normalizedEmail) ?: throw UnauthorizedException()
        if (!passwordHasher.verify(password, user.passwordHash)) throw UnauthorizedException()
        authLogger.info("User logged in: userId={}", user.id)
        return AuthResult(user.id, user.name, tokens.create(user.id))
    }
}
