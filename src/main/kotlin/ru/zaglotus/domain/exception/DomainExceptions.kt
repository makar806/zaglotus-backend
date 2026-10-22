package ru.zaglotus.domain.exception

sealed class DomainException(message: String) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message)
class ConflictException(message: String) : DomainException(message)
class NotFoundException(message: String) : DomainException(message)
class UnauthorizedException(message: String = "Неверный email или пароль") : DomainException(message)
