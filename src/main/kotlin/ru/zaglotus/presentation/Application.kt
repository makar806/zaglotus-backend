package ru.zaglotus.presentation

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import ru.zaglotus.application.config.AppConfig
import ru.zaglotus.application.config.DatabaseFactory
import ru.zaglotus.application.persistence.JdbcDeadlineRepository
import ru.zaglotus.application.persistence.JdbcPetRepository
import ru.zaglotus.application.persistence.JdbcUserRepository
import ru.zaglotus.application.security.BcryptPasswordHasher
import ru.zaglotus.application.security.JwtTokenService
import ru.zaglotus.application.service.AuthService
import ru.zaglotus.application.service.DeadlineService
import ru.zaglotus.application.service.PetService
import ru.zaglotus.application.transaction.JdbcConnectionContext
import ru.zaglotus.application.transaction.JdbcTransactionManager
import ru.zaglotus.presentation.controller.authController
import ru.zaglotus.presentation.controller.deadlineController
import ru.zaglotus.presentation.controller.documentationController
import ru.zaglotus.presentation.controller.petController
import ru.zaglotus.presentation.dto.ErrorResponse
import ru.zaglotus.presentation.dto.HealthResponse
import ru.zaglotus.presentation.error.configureErrorHandling

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    module(AppConfig.fromEnvironment())
}

fun Application.module(config: AppConfig) {
    val dataSource = DatabaseFactory.create(config.database)
    val connections = JdbcConnectionContext(dataSource)
    val transactions = JdbcTransactionManager(dataSource, connections)
    val users = JdbcUserRepository(connections)
    val pets = JdbcPetRepository(connections)
    val deadlines = JdbcDeadlineRepository(connections)
    val authService = AuthService(
        users,
        pets,
        transactions,
        BcryptPasswordHasher(),
        JwtTokenService(config.jwt)
    )
    val petService = PetService(pets)
    val deadlineService = DeadlineService(deadlines, pets, transactions)

    monitor.subscribe(ApplicationStopped) { dataSource.close() }

    install(ContentNegotiation) {
        json(Json { encodeDefaults = true; explicitNulls = false })
    }
    install(CallLogging)
    install(Authentication) {
        jwt("auth-jwt") {
            realm = config.jwt.realm
            verifier(
                JWT.require(Algorithm.HMAC256(config.jwt.secret))
                    .withIssuer(config.jwt.issuer)
                    .withAudience(config.jwt.audience)
                    .build()
            )
            validate { credential ->
                credential.payload.subject?.let { JWTPrincipal(credential.payload) }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Требуется авторизация"))
            }
        }
    }
    configureErrorHandling()
    routing {
        get("/health") { call.respond(HealthResponse("ok")) }
        documentationController()
        authController(authService)
        authenticate("auth-jwt") {
            route("/api/v1") {
                petController(petService)
                deadlineController(deadlineService)
            }
        }
    }
}
