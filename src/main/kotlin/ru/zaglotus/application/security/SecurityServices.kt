package ru.zaglotus.application.security

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import ru.zaglotus.application.config.JwtConfig
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.UUID

interface PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}

interface TokenService {
    fun create(userId: UUID): String
}

class BcryptPasswordHasher : PasswordHasher {
    override fun hash(password: String): String = BCrypt.withDefaults().hashToString(12, password.toCharArray())

    override fun verify(password: String, hash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified
}

class JwtTokenService(
    private val config: JwtConfig,
    private val clock: Clock = Clock.systemUTC()
) : TokenService {
    override fun create(userId: UUID): String {
        val now = clock.instant()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(Duration.ofHours(24))))
            .sign(Algorithm.HMAC256(config.secret))
    }
}
