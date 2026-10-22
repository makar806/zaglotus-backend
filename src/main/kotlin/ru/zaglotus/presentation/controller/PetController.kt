package ru.zaglotus.presentation.controller

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import ru.zaglotus.application.service.PetService
import ru.zaglotus.presentation.dto.toResponse

fun Route.petController(petService: PetService) {
    get("/pet") {
        val pet = databaseCall { petService.get(call.userId()) }
        call.respond(pet.toResponse())
    }
}
