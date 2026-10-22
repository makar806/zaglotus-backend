package ru.zaglotus.presentation.controller

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.documentationController() {
    swaggerUI(path = "/swagger", swaggerFile = "openapi/documentation.yaml") {
        version = "5.31.0"
    }
    get("/openapi.yaml") {
        val specification = checkNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream("openapi/documentation.yaml")
        ).use { it.readBytes() }
        call.respondBytes(specification, ContentType.parse("application/yaml"))
    }
}
