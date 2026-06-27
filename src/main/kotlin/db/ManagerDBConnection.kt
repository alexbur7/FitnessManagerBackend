package ru.alexbur.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import java.sql.Connection
import java.sql.DriverManager

private var dataSource: HikariDataSource? = null

fun Application.initConnectionPool() {
    val url = environment.config.property("postgres.url").getString()
    val user = environment.config.property("postgres.user").getString()
    val postgresPassword = environment.config.property("postgres.password").getString()

    val config = HikariConfig().apply {
        jdbcUrl = url
        username = user
        password = postgresPassword
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30_000
        idleTimeout = 600_000
        maxLifetime = 1_800_000
    }
    dataSource = HikariDataSource(config)
    log.info("Connection pool initialized for $url")
}

fun Application.getConnection(embedded: Boolean): Connection {
    if (embedded) {
        log.info("Using embedded H2 database for testing")
        return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")
    }
    return dataSource?.connection ?: error("Connection pool is not initialized. Call initConnectionPool() first.")
}
