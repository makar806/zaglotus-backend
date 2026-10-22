package ru.zaglotus.presentation.dto

import kotlinx.serialization.Serializable
import ru.zaglotus.application.service.CompletionResult
import ru.zaglotus.domain.model.Deadline
import ru.zaglotus.domain.model.Pet

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String, val petName: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val userId: String, val name: String, val token: String)

@Serializable
data class DeadlineRequest(
    val title: String,
    val description: String? = null,
    val dueAt: String,
    val difficulty: String
)

@Serializable
data class DeadlineResponse(
    val id: String,
    val title: String,
    val description: String?,
    val dueAt: String,
    val difficulty: String,
    val status: String,
    val createdAt: String,
    val completedAt: String?
)

@Serializable
data class PetResponse(
    val name: String,
    val experience: Int,
    val meals: Int,
    val level: Int
)

@Serializable
data class PetRewardResponse(
    val name: String,
    val experience: Int,
    val meals: Int,
    val level: Int,
    val gainedExperience: Int
)

@Serializable
data class CompletionResponse(
    val deadline: DeadlineResponse,
    val pet: PetRewardResponse
)

@Serializable
data class ErrorResponse(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String)

fun Deadline.toResponse() = DeadlineResponse(
    id = id.toString(),
    title = title,
    description = description,
    dueAt = dueAt.toString(),
    difficulty = difficulty.name,
    status = status.name,
    createdAt = createdAt.toString(),
    completedAt = completedAt?.toString()
)

fun Pet.toResponse() = PetResponse(name, experience, meals, level)

fun CompletionResult.toResponse() = CompletionResponse(
    deadline = deadline.toResponse(),
    pet = PetRewardResponse(
        name = pet.name,
        experience = pet.experience,
        meals = pet.meals,
        level = pet.level,
        gainedExperience = gainedExperience
    )
)
