package ru.zaglotus.application.transaction

import java.sql.Connection
import javax.sql.DataSource

interface TransactionManager {
    fun <T> execute(block: () -> T): T
}

class JdbcConnectionContext(private val dataSource: DataSource) {
    private val connectionHolder = ThreadLocal<Connection?>()

    fun <T> withConnection(block: (Connection) -> T): T {
        val current = connectionHolder.get()
        return if (current != null) block(current) else dataSource.connection.use(block)
    }

    fun current(): Connection? = connectionHolder.get()

    fun bind(connection: Connection) {
        connectionHolder.set(connection)
    }

    fun clear() {
        connectionHolder.remove()
    }
}

class JdbcTransactionManager(
    private val dataSource: DataSource,
    private val connectionContext: JdbcConnectionContext
) : TransactionManager {
    override fun <T> execute(block: () -> T): T {
        if (connectionContext.current() != null) return block()
        return dataSource.connection.use { connection ->
            connection.autoCommit = false
            connectionContext.bind(connection)
            try {
                block().also { connection.commit() }
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connectionContext.clear()
            }
        }
    }
}
