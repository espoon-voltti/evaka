// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.json.JsonMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.TestInvoiceProductProvider
import fi.espoo.evaka.children.consent.ChildConsentType
import fi.espoo.evaka.emailclient.EvakaEmailMessageProvider
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.DefaultInvoiceGenerationLogic
import fi.espoo.evaka.invoicing.service.EspooIncomeTypesProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.reports.patu.PatuIntegrationClient
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.configureJdbi
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.dev.runDevScript
import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import fi.espoo.evaka.shared.template.EvakaTemplateProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import fi.espoo.evaka.titania.TitaniaEmployeeIdConverter
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPublicKeys
import io.opentracing.noop.NoopTracerFactory
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner

// Hides Closeable interface from Spring, which would close the shared instance otherwise
class TestDataSource(pool: HikariDataSource) : DataSource by pool

private val globalLock = object {}
private var testDataSource: TestDataSource? = null

fun getTestDataSource(): TestDataSource =
    synchronized(globalLock) {
        testDataSource
            ?: TestDataSource(
                    HikariDataSource(
                            HikariConfig().apply {
                                jdbcUrl = "jdbc:postgresql://localhost:5432/evaka_it"
                                username = "evaka_it"
                                password = "evaka_it"
                            }
                        )
                        .also {
                            Flyway.configure()
                                .apply {
                                    pluginRegister
                                        .getPlugin(PostgreSQLConfigurationExtension::class.java)
                                        .isTransactionalLock = false
                                }
                                .dataSource(
                                    PGSimpleDataSource().apply {
                                        setUrl("jdbc:postgresql://localhost:5432/evaka_it")
                                        user = "evaka_migration_local"
                                        password = "flyway"
                                    }
                                )
                                .placeholders(
                                    mapOf(
                                        "application_user" to "evaka_application_local",
                                        "migration_user" to "evaka_migration_local"
                                    )
                                )
                                .load()
                                .run { migrate() }
                            Database(Jdbi.create(it), NoopTracerFactory.create()).connect { db ->
                                db.transaction { tx ->
                                    tx.runDevScript("reset-database.sql")
                                    tx.resetDatabase()
                                }
                            }
                        }
                )
                .also { testDataSource = it }
    }

@TestConfiguration
class SharedIntegrationTestConfig {
    @Bean fun jdbi(dataSource: DataSource) = configureJdbi(Jdbi.create(dataSource))

    @Bean fun dataSource(): DataSource = getTestDataSource()

    @Bean
    fun s3Client(env: BucketEnv): S3Client {
        val client =
            S3Client.builder()
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
                .endpointOverride(env.s3MockUrl)
                .credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
                )
                .build()

        val existingBuckets = client.listBuckets().buckets().map { it.name()!! }
        for (bucket in env.allBuckets().filterNot { existingBuckets.contains(it) }) {
            val request = CreateBucketRequest.builder().bucket(bucket).build()
            client.createBucket(request)
        }
        return client
    }

    @Bean
    fun s3Presigner(env: BucketEnv): S3Presigner =
        S3Presigner.builder()
            .region(Region.US_EAST_1)
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .endpointOverride(env.s3MockUrl)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("foo", "bar"))
            )
            .build()

    @Bean
    fun integrationTestJwtAlgorithm(): Algorithm {
        val publicKeys =
            SecurityConfig::class
                .java
                .getResourceAsStream("/evaka-integration-test/jwks.json")
                .use { loadPublicKeys(it) }
        return Algorithm.RSA256(JwtKeys(publicKeys))
    }

    @Bean
    fun invoiceIntegrationClient(jsonMapper: JsonMapper): InvoiceIntegrationClient =
        InvoiceIntegrationClient.MockClient(jsonMapper)

    @Bean
    fun patuIntegrationClient(jsonMapper: JsonMapper): PatuIntegrationClient =
        PatuIntegrationClient.MockPatuClient(jsonMapper)

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean
    fun paymentIntegrationClient(jsonMapper: JsonMapper): PaymentIntegrationClient =
        PaymentIntegrationClient.MockClient(jsonMapper)

    @Bean fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun emailMessageProvider(env: EvakaEnv): IEmailMessageProvider = EvakaEmailMessageProvider(env)

    @Bean
    fun documentService(
        s3Client: S3Client,
        s3Presigner: S3Presigner,
        env: BucketEnv
    ): DocumentService = DocumentService(s3Client, s3Presigner, env.proxyThroughNginx)

    @Bean fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean fun featureConfig(): FeatureConfig = testFeatureConfig

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = TestInvoiceProductProvider()

    @Bean fun actionRuleMapping(): ActionRuleMapping = DefaultActionRuleMapping()

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        object : TitaniaEmployeeIdConverter {
            override fun fromTitania(employeeId: String): String = employeeId.trimStart('0')
        }
}

val testFeatureConfig =
    FeatureConfig(
        valueDecisionCapacityFactorEnabled = false,
        daycareApplicationServiceNeedOptionsEnabled = false,
        citizenReservationThresholdHours = 150,
        dailyFeeDivisorOperationalDaysOverride = null,
        freeSickLeaveOnContractDays = false,
        freeAbsenceGivesADailyRefund = true,
        alwaysUseDaycareFinanceDecisionHandler = false,
        invoiceNumberSeriesStart = 5000000000,
        paymentNumberSeriesStart = 9000000000,
        unplannedAbsencesAreContractSurplusDays = true,
        maxContractDaySurplusThreshold = null,
        useContractDaysAsDailyFeeDivisor = true,
        enabledChildConsentTypes = setOf(ChildConsentType.EVAKA_PROFILE_PICTURE),
        curriculumDocumentPermissionToShareRequired = true,
        assistanceDecisionMakerRoles = null,
        preschoolAssistanceDecisionMakerRoles = null,
        requestedStartUpperLimit = 14,
        partialAbsenceThresholdsEnabled = true,
        postOffice = "ESPOO",
        municipalMessageAccountName = "Espoon kaupunki - Esbo stad - City of Espoo",
        serviceWorkerMessageAccountName =
            "Varhaiskasvatuksen palveluohjaus - Småbarnspedagogikens servicehandledning - Early childhood education service guidance",
        applyPlacementUnitFromDecision = false,
        preferredStartRelativeApplicationDueDate = false,
    )
