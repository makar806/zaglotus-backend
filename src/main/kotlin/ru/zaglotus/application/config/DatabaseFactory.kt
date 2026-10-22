package ru.zaglotus.application.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

object DatabaseFactory {
    fun create(config: DatabaseConfig): HikariDataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = 10
            minimumIdle = 2
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            validate()
        }
        return HikariDataSource(hikariConfig).also { dataSource ->
            Flyway.configure().dataSource(dataSource).load().migrate()
        }
    }
}
