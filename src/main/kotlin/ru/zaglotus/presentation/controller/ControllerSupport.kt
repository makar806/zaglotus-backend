package ru.zaglotus.presentation.controller

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.zaglotus.domain.exception.UnauthorizedException
import ru.zaglotus.domain.exception.ValidationException
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

fun ApplicationCall.userId(): UUID {
    val subject = principal<JWTPrincipal>()?.payload?.subject
        ?: throw UnauthorizedException("Требуется авторизация")
    return runCatching { UUID.fromString(subject) }
        .getOrElse { throw UnauthorizedException("Некорректный токен") }
}

fun parseUuid(value: String?): UUID = runCatching { UUID.fromString(value) }
    .getOrElse { throw ValidationException("Некорректный идентификатор") }

fun parseInstant(value: String): Instant = try {
    Instant.parse(value)
} catch (_: DateTimeParseException) {
    throw ValidationException("Дата должна быть в ISO-8601 UTC формате, например 2026-10-22T18:00:00Z")
}

suspend fun <T> databaseCall(block: () -> T): T =
    withContext(Dispatchers.IO) { block() }
