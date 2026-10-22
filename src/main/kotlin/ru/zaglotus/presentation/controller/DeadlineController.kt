package ru.zaglotus.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import ru.zaglotus.application.service.DeadlineService
import ru.zaglotus.domain.exception.ValidationException
import ru.zaglotus.domain.model.DeadlineStatus
import ru.zaglotus.domain.model.Difficulty
import ru.zaglotus.presentation.dto.DeadlineRequest
import ru.zaglotus.presentation.dto.toResponse

fun Route.deadlineController(deadlineService: DeadlineService) {
    route("/deadlines") {
        post {
            val request = call.receive<DeadlineRequest>()
            val deadline = databaseCall {
                deadlineService.create(
                    call.userId(),
                    request.title,
                    request.description,
                    parseInstant(request.dueAt),
                    parseDifficulty(request.difficulty)
                )
            }
            call.respond(HttpStatusCode.Created, deadline.toResponse())
        }
        get {
            val status = call.request.queryParameters["status"]?.let(::parseStatus)
            val deadlines = databaseCall { deadlineService.list(call.userId(), status) }
            call.respond(deadlines.map { it.toResponse() })
        }
        get("/{id}") {
            val deadline = databaseCall {
                deadlineService.get(call.userId(), parseUuid(call.parameters["id"]))
            }
            call.respond(deadline.toResponse())
        }
        put("/{id}") {
            val request = call.receive<DeadlineRequest>()
            val deadline = databaseCall {
                deadlineService.update(
                    call.userId(),
                    parseUuid(call.parameters["id"]),
                    request.title,
                    request.description,
                    parseInstant(request.dueAt),
                    parseDifficulty(request.difficulty)
                )
            }
            call.respond(deadline.toResponse())
        }
        delete("/{id}") {
            databaseCall {
                deadlineService.delete(call.userId(), parseUuid(call.parameters["id"]))
            }
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{id}/complete") {
            val result = databaseCall {
                deadlineService.complete(call.userId(), parseUuid(call.parameters["id"]))
            }
            call.respond(result.toResponse())
        }
    }
}

private fun parseDifficulty(value: String): Difficulty =
    runCatching { Difficulty.valueOf(value.uppercase()) }
        .getOrElse { throw ValidationException("Сложность должна быть EASY, MEDIUM или HARD") }

private fun parseStatus(value: String): DeadlineStatus =
    runCatching { DeadlineStatus.valueOf(value.uppercase()) }
        .getOrElse { throw ValidationException("Статус должен быть OPEN или COMPLETED") }
