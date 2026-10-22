package ru.zaglotus

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import ru.zaglotus.application.config.AppConfig
import ru.zaglotus.application.config.DatabaseConfig
import ru.zaglotus.application.config.DatabaseFactory
import ru.zaglotus.application.config.JwtConfig
import ru.zaglotus.application.persistence.JdbcPetRepository
import ru.zaglotus.application.persistence.JdbcUserRepository
import ru.zaglotus.application.transaction.JdbcConnectionContext
import ru.zaglotus.application.transaction.JdbcTransactionManager
import ru.zaglotus.domain.model.Pet
import ru.zaglotus.domain.model.User
import ru.zaglotus.presentation.module
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private class TestPostgres(imageName: String) : PostgreSQLContainer<TestPostgres>(imageName)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZaglotusIntegrationTest {
    private val postgres = TestPostgres("postgres:17-alpine")
    private lateinit var databaseConfig: DatabaseConfig
    private lateinit var appConfig: AppConfig

    @BeforeAll
    fun startDatabase() {
        postgres.start()
        databaseConfig = DatabaseConfig(postgres.jdbcUrl, postgres.username, postgres.password)
        appConfig = AppConfig(
            databaseConfig,
            JwtConfig(
                secret = "integration-test-secret-that-is-long-enough",
                issuer = "zaglotus-test",
                audience = "zaglotus-test-users",
                realm = "zaglotus-test-api"
            )
        )
        DatabaseFactory.create(databaseConfig).close()
    }

    @AfterAll
    fun stopDatabase() {
        postgres.stop()
    }

    @BeforeEach
    fun cleanDatabase() {
        DriverManager.getConnection(databaseConfig.url, databaseConfig.user, databaseConfig.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("TRUNCATE TABLE deadlines, pets, users CASCADE")
            }
        }
    }

    @Test
    fun `flyway creates required tables`() {
        DriverManager.getConnection(databaseConfig.url, databaseConfig.user, databaseConfig.password).use { connection ->
            connection.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
            ).use { statement ->
                statement.executeQuery().use { result ->
                    val tables = mutableSetOf<String>()
                    while (result.next()) tables += result.getString("table_name")
                    assertEquals(true, tables.containsAll(setOf("users", "pets", "deadlines", "flyway_schema_history")))
                }
            }
        }
    }

    @Test
    fun `transaction rollback removes all changes`() {
        val dataSource = DatabaseFactory.create(databaseConfig)
        try {
            val connections = JdbcConnectionContext(dataSource)
            val transactions = JdbcTransactionManager(dataSource, connections)
            val users = JdbcUserRepository(connections)
            val pets = JdbcPetRepository(connections)
            val userId = UUID.randomUUID()
            val user = User.create(
                userId,
                "Макар",
                "rollback@example.com",
                "password-hash",
                Instant.parse("2026-10-22T12:00:00Z")
            )
            val pet = Pet.create(userId, "Жора")

            assertFailsWith<IllegalStateException> {
                transactions.execute {
                    users.create(user)
                    pets.create(pet)
                    throw IllegalStateException("test rollback")
                }
            }

            assertNull(users.findByEmail(user.email))
            assertNull(pets.findByUserId(userId))
        } finally {
            dataSource.close()
        }
    }

    @Test
    fun `http scenario registers user completes deadline and feeds pet`() = testApplication {
        application { module(appConfig) }

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"name":"Макар","email":"makar@example.com","password":"password123","petName":"Жора"}"""
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val token = Json.parseToJsonElement(registerResponse.bodyAsText())
            .jsonObject["token"]
            ?.jsonPrimitive
            ?.content
        assertNotNull(token)

        val deadlineResponse = client.post("/api/v1/deadlines") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                """{"title":"Сдать лабораторную","description":"До дедлайна","dueAt":"2030-10-22T20:00:00Z","difficulty":"HARD"}"""
            )
        }
        assertEquals(HttpStatusCode.Created, deadlineResponse.status)
        val deadlineId = Json.parseToJsonElement(deadlineResponse.bodyAsText())
            .jsonObject["id"]
            ?.jsonPrimitive
            ?.content
        assertNotNull(deadlineId)

        val completionResponse = client.post("/api/v1/deadlines/$deadlineId/complete") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, completionResponse.status)

        val petResponse = client.get("/api/v1/pet") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, petResponse.status)
        val pet = Json.parseToJsonElement(petResponse.bodyAsText()).jsonObject
        assertEquals(100, pet["experience"]?.jsonPrimitive?.content?.toInt())
        assertEquals(1, pet["meals"]?.jsonPrimitive?.content?.toInt())
        assertEquals(2, pet["level"]?.jsonPrimitive?.content?.toInt())
    }
}
