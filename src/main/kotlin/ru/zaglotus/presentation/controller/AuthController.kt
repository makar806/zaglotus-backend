package ru.zaglotus.presentation.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.zaglotus.application.service.AuthService
import ru.zaglotus.presentation.dto.AuthResponse
import ru.zaglotus.presentation.dto.LoginRequest
import ru.zaglotus.presentation.dto.RegisterRequest

fun Route.authController(authService: AuthService) {
    route("/api/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val result = databaseCall {
                authService.register(request.name, request.email, request.password, request.petName)
            }
            call.respond(HttpStatusCode.Created, AuthResponse(result.userId.toString(), result.name, result.token))
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val result = databaseCall {
                authService.login(request.email, request.password)
            }
            call.respond(AuthResponse(result.userId.toString(), result.name, result.token))
        }
    }
}
