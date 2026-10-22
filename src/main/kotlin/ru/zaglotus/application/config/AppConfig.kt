package ru.zaglotus.application.config

data class AppConfig(
    val database: DatabaseConfig,
    val jwt: JwtConfig
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            return AppConfig(
                database = DatabaseConfig(
                    url = env.required("DB_URL"),
                    user = env.required("DB_USER"),
                    password = env.required("DB_PASSWORD")
                ),
                jwt = JwtConfig(
                    secret = env.required("JWT_SECRET"),
                    issuer = env["JWT_ISSUER"] ?: "zaglotus",
                    audience = env["JWT_AUDIENCE"] ?: "zaglotus-users",
                    realm = env["JWT_REALM"] ?: "zaglotus-api"
                )
            )
        }
    }
}

data class DatabaseConfig(val url: String, val user: String, val password: String)

data class JwtConfig(val secret: String, val issuer: String, val audience: String, val realm: String)

private fun Map<String, String>.required(name: String): String =
    this[name]?.takeIf { it.isNotBlank() } ?: error("Environment variable $name is required")
