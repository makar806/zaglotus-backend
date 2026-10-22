package ru.zaglotus.presentation.error

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import ru.zaglotus.domain.exception.ConflictException
import ru.zaglotus.domain.exception.NotFoundException
import ru.zaglotus.domain.exception.UnauthorizedException
import ru.zaglotus.domain.exception.ValidationException
import ru.zaglotus.presentation.dto.ErrorResponse

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandler")
    install(StatusPages) {
        exception<ValidationException> { call, error ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("validation_error", error.message ?: "Ошибка валидации"))
        }
        exception<BadRequestException> { call, error ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("bad_request", error.message ?: "Некорректный запрос"))
        }
        exception<NotFoundException> { call, error ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", error.message ?: "Не найдено"))
        }
        exception<ConflictException> { call, error ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("conflict", error.message ?: "Конфликт"))
        }
        exception<UnauthorizedException> { call, error ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", error.message ?: "Нет доступа"))
        }
        exception<Throwable> { call, error ->
            logger.error("Unhandled request error", error)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Внутренняя ошибка сервера"))
        }
    }
}
