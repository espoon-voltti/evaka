// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.json.JsonMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.emailclient.EvakaEmailMessageProvider
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.EspooIncomeTypesProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.dev.runDevScript
import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.security.PermittedRoleActions
import fi.espoo.evaka.shared.security.StaticPermittedRoleActions
import fi.espoo.evaka.shared.template.EvakaTemplateProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPublicKeys
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import redis.clients.jedis.JedisPool
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.net.URI
import javax.sql.DataSource

// Hides Closeable interface from Spring, which would close the shared instance otherwise
class TestDataSource(pool: HikariDataSource) : DataSource by pool

private val globalLock = object {}
private var testDataSource: TestDataSource? = null
fun getTestDataSource(): TestDataSource = synchronized(globalLock) {
    testDataSource ?: TestDataSource(
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://localhost:15432/evaka_it"
                username = "evaka_it"
                password = "evaka_it"
            }
        ).also {
            Flyway.configure()
                .dataSource(it)
                .placeholders(mapOf("application_user" to "evaka_it"))
                .load()
                .run {
                    migrate()
                }
            Database(Jdbi.create(it)).connect { db ->
                db.transaction { tx ->
                    tx.runDevScript("reset-database.sql")
                    tx.resetDatabase()
                }
            }
        }
    ).also {
        testDataSource = it
    }
}

@TestConfiguration
class SharedIntegrationTestConfig {
    @Bean
    fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean
    fun dataSource(): DataSource = getTestDataSource()

    @Bean
    fun redisPool(): JedisPool {
        // Use database 1 to avoid conflicts with normal development setup in database 0
        val database = 1
        return JedisPool(
            GenericObjectPoolConfig(),
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

    @Bean
    fun s3Client(env: BucketEnv): S3Client {
        val port = 9876
        val client = S3Client.builder()
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(URI.create("http://localhost:$port"))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .build()

        for (bucket in env.allBuckets()) {
            val request = CreateBucketRequest.builder().bucket(bucket).build()
            client.createBucket(request)
        }
        return client
    }

    @Bean
    fun integrationTestJwtAlgorithm(): Algorithm {
        val publicKeys =
            SecurityConfig::class.java.getResourceAsStream("/evaka-integration-test/jwks.json").use { loadPublicKeys(it) }
        return Algorithm.RSA256(JwtKeys(publicKeys))
    }

    @Bean
    fun invoiceIntegrationClient(jsonMapper: JsonMapper): InvoiceIntegrationClient = InvoiceIntegrationClient.MockClient(jsonMapper)

    @Bean
    fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun emailMessageProvider(): IEmailMessageProvider = EvakaEmailMessageProvider()

    @Bean
    fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean
    fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean
    fun featureConfig(): FeatureConfig = testFeatureConfig

    @Bean
    fun invoiceProductProvider(): InvoiceProductProvider = TestInvoiceProductProvider()

    @Bean
    fun permittedRoleActions(): PermittedRoleActions = StaticPermittedRoleActions()
}

val testFeatureConfig = FeatureConfig(
    valueDecisionCapacityFactorEnabled = true,
    daycareApplicationServiceNeedOptionsEnabled = false,
    citizenReservationThresholdHours = 150,
    dailyFeeDivisorOperationalDaysOverride = null,
)
