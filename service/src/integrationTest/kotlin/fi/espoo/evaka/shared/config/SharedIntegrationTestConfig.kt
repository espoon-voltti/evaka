// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.amazonaws.ClientConfigurationFactory
import com.amazonaws.Protocol
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.dev.runDevScript
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPublicKeys
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import redis.clients.jedis.JedisPool
import javax.sql.DataSource

fun isCiEnvironment() = System.getenv("CI") == "true"

// Hides Closeable interface from Spring, which would close the shared instance otherwise
class TestDataSource(pool: HikariDataSource) : DataSource by pool

private val globalLock = object {}
private var testDataSource: TestDataSource? = null
fun getTestDataSource(): TestDataSource = synchronized(globalLock) {
    testDataSource ?: TestDataSource(
        HikariDataSource(
            when (isCiEnvironment()) {
                true -> {
                    PostgresContainer.getInstance().let {
                        HikariConfig().apply {
                            jdbcUrl = it.jdbcUrl
                            username = it.username
                            password = it.password
                            maximumPoolSize = 32
                        }
                    }
                }
                false -> {
                    HikariConfig().apply {
                        jdbcUrl = "jdbc:postgresql://localhost:15432/evaka_it"
                        username = "evaka_it"
                        password = "evaka_it"
                    }
                }
            }
        ).also {
            Flyway.configure()
                .dataSource(it)
                .placeholders(mapOf("application_user" to "evaka_it"))
                .load()
                .run {
                    migrate()
                }
            Jdbi.create(it).handle { h ->
                h.runDevScript("reset-database.sql")
                h.resetDatabase()
            }
        }
    ).also {
        testDataSource = it
    }
}

val mockS3Bucket = "test-bucket"

@TestConfiguration
class SharedIntegrationTestConfig {
    @Bean
    fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean
    fun asyncJobRunner(jdbi: Jdbi, dataSource: DataSource) = AsyncJobRunner(jdbi, syncMode = true)

    @Bean
    fun dataSource(): DataSource = getTestDataSource()

    @Bean
    fun redisPool(): JedisPool =
        when (isCiEnvironment()) {
            true -> RedisContainer.getInstance().let {
                JedisPool(it.containerIpAddress, it.getMappedPort(6379))
            }
            false -> {
                // Use database 1 to avoid conflicts with normal development setup in database 0
                val database = 1
                JedisPool(
                    GenericObjectPoolConfig<Any>(),
                    "localhost",
                    6379,
                    redis.clients.jedis.Protocol.DEFAULT_TIMEOUT,
                    null,
                    database
                ).also { pool ->
                    pool.resource.use {
                        // Clear all data from database 1
                        it.flushDB()
                    }
                }
            }
        }

    @Bean
    fun s3Client(): AmazonS3 {
        val port = when (isCiEnvironment()) {
            true -> S3Container.getInstance().getMappedPort(9090)
            false -> 9876
        }
        val client = AmazonS3ClientBuilder
            .standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(
                    "http://s3.lvh.me:$port",
                    "us-east-1"
                )
            )
            .withClientConfiguration(ClientConfigurationFactory().config.withProtocol(Protocol.HTTP))
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("foo", "bar")))
            .build()

        client.createBucket(mockS3Bucket)
        client.createBucket("evaka-clubdecisions-it")
        client.createBucket("evaka-daycaredecisions-it")
        client.createBucket("evaka-paymentdecisions-it")
        client.createBucket("evaka-vouchervaluedecisions-it")
        client.createBucket("evaka-attachments-it")
        return client
    }

    @Bean
    fun integrationTestJwtAlgorithm(): Algorithm {
        val publicKeys =
            SecurityConfig::class.java.getResourceAsStream("/evaka-integration-test/jwks.json").use { loadPublicKeys(it) }
        return Algorithm.RSA256(JwtKeys(privateKeyId = null, privateKey = null, publicKeys = publicKeys))
    }

    @Bean
    fun invoiceIntegrationClient(objectMapper: ObjectMapper): InvoiceIntegrationClient = InvoiceIntegrationClient.MockClient(objectMapper)
}
