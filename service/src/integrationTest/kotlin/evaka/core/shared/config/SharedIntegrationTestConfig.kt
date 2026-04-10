// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import com.auth0.jwt.algorithms.Algorithm
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import evaka.core.BucketEnv
import evaka.core.EvakaEnv
import evaka.core.TestInvoiceProductProvider
import evaka.core.document.archival.ArchivalIntegrationClient
import evaka.core.emailclient.EvakaEmailMessageProvider
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.invoicing.domain.PaymentIntegrationClient
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.core.invoicing.service.DefaultInvoiceGenerationLogic
import evaka.core.invoicing.service.DefaultInvoiceNumberProvider
import evaka.core.invoicing.service.EspooIncomeTypesProvider
import evaka.core.invoicing.service.IncomeCoefficientMultiplierProvider
import evaka.core.invoicing.service.IncomeTypesProvider
import evaka.core.invoicing.service.InvoiceNumberProvider
import evaka.core.invoicing.service.InvoiceProductProvider
import evaka.core.mealintegration.DefaultMealTypeMapper
import evaka.core.mealintegration.MealTypeMapper
import evaka.core.shared.ArchiveProcessConfig
import evaka.core.shared.ArchiveProcessType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.db.configureJdbi
import evaka.core.shared.dev.resetDatabase
import evaka.core.shared.dev.runSqlScript
import evaka.core.shared.message.EvakaMessageProvider
import evaka.core.shared.message.IMessageProvider
import evaka.core.shared.noopTracer
import evaka.core.shared.security.Action
import evaka.core.shared.security.actionrule.ActionRuleMapping
import evaka.core.shared.security.actionrule.HasGlobalRole
import evaka.core.shared.security.actionrule.HasUnitRole
import evaka.core.shared.security.actionrule.ScopedActionRule
import evaka.core.shared.security.actionrule.UnscopedActionRule
import evaka.core.shared.template.EvakaTemplateProvider
import evaka.core.shared.template.ITemplateProvider
import evaka.core.titania.TitaniaEmployeeIdConverter
import evaka.instance.espoo.DefaultPasswordSpecification
import evaka.instance.espoo.invoicing.EspooIncomeCoefficientMultiplierProvider
import fi.espoo.voltti.auth.JwtKeys
import fi.espoo.voltti.auth.loadPublicKeys
import javax.sql.DataSource
import org.flywaydb.core.Flyway
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension
import org.jdbi.v3.core.Jdbi
import org.postgresql.ds.PGSimpleDataSource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.thymeleaf.ITemplateEngine
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.internal.http.loader.DefaultSdkHttpClientBuilder
import software.amazon.awssdk.http.SdkHttpConfigurationOption
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.utils.AttributeMap
import tools.jackson.databind.json.JsonMapper

// Hides Closeable interface from Spring, which would close the shared instance otherwise
class TestDataSource(pool: HikariDataSource) : DataSource by pool

private val globalLock = object {}
private var testDataSource: TestDataSource? = null

private val dbPort = System.getenv("EVAKA_DATABASE_PORT")?.toIntOrNull() ?: 5432
private val dbUrl = "jdbc:postgresql://localhost:$dbPort/evaka_it"

fun getTestDataSource(): TestDataSource =
    synchronized(globalLock) {
        testDataSource
            ?: TestDataSource(
                    HikariDataSource(
                            HikariConfig().apply {
                                jdbcUrl = dbUrl
                                username = "evaka_it"
                                password = "evaka_it"
                            }
                        )
                        .also {
                            Flyway.configure()
                                .apply {
                                    pluginRegister
                                        .getExact(PostgreSQLConfigurationExtension::class.java)
                                        .isTransactionalLock = false
                                }
                                .validateMigrationNaming(true)
                                .dataSource(
                                    PGSimpleDataSource().apply {
                                        setUrl(dbUrl)
                                        user = "evaka_migration_local"
                                        password = "flyway"
                                    }
                                )
                                .placeholders(
                                    mapOf(
                                        "application_user" to "evaka_application_local",
                                        "migration_user" to "evaka_migration_local",
                                    )
                                )
                                .load()
                                .run { migrate() }
                            Database(Jdbi.create(it), noopTracer()).connect { db ->
                                db.transaction { tx ->
                                    tx.runSqlScript("dev-data/reset-database.sql")
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
        val attrs =
            AttributeMap.builder()
                .put(SdkHttpConfigurationOption.TRUST_ALL_CERTIFICATES, true)
                .build()
        val client =
            S3Client.builder()
                .httpClient(DefaultSdkHttpClientBuilder().buildWithDefaults(attrs))
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                    S3Configuration.builder().pathStyleAccessEnabled(true).build()
                )
                .endpointOverride(env.localS3Url)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            env.localS3AccessKeyId,
                            env.localS3SecretAccessKey,
                        )
                    )
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
            .endpointOverride(env.localS3Url)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(env.localS3AccessKeyId, env.localS3SecretAccessKey)
                )
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

    @Bean fun invoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean
    fun paymentIntegrationClient(jsonMapper: JsonMapper): PaymentIntegrationClient =
        PaymentIntegrationClient.MockClient(jsonMapper)

    @Bean fun messageProvider(): IMessageProvider = EvakaMessageProvider()

    @Bean
    fun emailMessageProvider(env: EvakaEnv): IEmailMessageProvider = EvakaEmailMessageProvider(env)

    // TODO: It's a convenience to have espoo templates in integration tests, should be better to
    // have test-specific templates instead
    @Bean fun templateEngine(): ITemplateEngine = PDFConfig.templateEngine("espoo")

    @Bean fun templateProvider(): ITemplateProvider = EvakaTemplateProvider()

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean fun featureConfig(): FeatureConfig = testFeatureConfig

    @Bean fun invoiceProductProvider(): InvoiceProductProvider = TestInvoiceProductProvider()

    @Bean
    fun invoiceNumberProvider(): InvoiceNumberProvider = DefaultInvoiceNumberProvider(5000000000)

    @Bean
    fun coefficientMultiplierProvider(): IncomeCoefficientMultiplierProvider =
        EspooIncomeCoefficientMultiplierProvider()

    @Bean fun actionRuleMapping(): ActionRuleMapping = TestActionRuleMapping()

    @Bean
    fun titaniaEmployeeIdConverter(): TitaniaEmployeeIdConverter =
        object : TitaniaEmployeeIdConverter {
            override fun fromTitania(employeeId: String): String = employeeId.trimStart('0')
        }

    @Bean fun mealTypeMapper(): MealTypeMapper = DefaultMealTypeMapper

    @Bean
    fun passwordSpecification(): PasswordSpecification =
        DefaultPasswordSpecification(
            PasswordConstraints.UNCONSTRAINED.copy(minLength = 8, minDigits = 1)
        )

    @Bean
    fun archivalIntegrationClient(): ArchivalIntegrationClient =
        ArchivalIntegrationClient.MockClient()
}

val testFeatureConfig =
    FeatureConfig(
        valueDecisionCapacityFactorEnabled = false,
        citizenReservationThresholdHours = 150,
        freeAbsenceGivesADailyRefund = true,
        alwaysUseDaycareFinanceDecisionHandler = false,
        paymentNumberSeriesStart = 9000000000,
        unplannedAbsencesAreContractSurplusDays = true,
        maxContractDaySurplusThreshold = null,
        useContractDaysAsDailyFeeDivisor = true,
        requestedStartUpperLimit = 14,
        postOffice = "ESPOO",
        municipalMessageAccountName = "Espoon kaupunki - Esbo stad - City of Espoo",
        serviceWorkerMessageAccountName =
            "Varhaiskasvatuksen palveluohjaus - Småbarnspedagogikens servicehandledning - Early childhood education service guidance",
        financeMessageAccountName =
            "Varhaiskasvatuksen asiakasmaksut - Småbarnspedagogikens avgifter - Early childhood education fees",
        applyPlacementUnitFromDecision = false,
        preferredStartRelativeApplicationDueDate = false,
        fiveYearsOldDaycareEnabled = true,
        archiveMetadataOrganization = "Espoon kaupungin esiopetus ja varhaiskasvatus",
        archiveMetadataConfigs = { type: ArchiveProcessType, _: Int ->
            when (type) {
                ArchiveProcessType.APPLICATION_DAYCARE -> {
                    ArchiveProcessConfig(
                        processDefinitionNumber = "123.123.a",
                        archiveDurationMonths = 10 * 12,
                    )
                }

                ArchiveProcessType.APPLICATION_PRESCHOOL -> {
                    ArchiveProcessConfig(
                        processDefinitionNumber = "123.123.b",
                        archiveDurationMonths = 10 * 12,
                    )
                }

                ArchiveProcessType.APPLICATION_CLUB -> {
                    ArchiveProcessConfig(
                        processDefinitionNumber = "123.123.c",
                        archiveDurationMonths = 10 * 12,
                    )
                }

                ArchiveProcessType.FEE_DECISION -> {
                    ArchiveProcessConfig(
                        processDefinitionNumber = "123.789.a",
                        archiveDurationMonths = 10 * 12,
                    )
                }

                ArchiveProcessType.VOUCHER_VALUE_DECISION -> {
                    ArchiveProcessConfig(
                        processDefinitionNumber = "123.789.b",
                        archiveDurationMonths = 10 * 12,
                    )
                }
            }
        },
    )

private class TestActionRuleMapping : ActionRuleMapping {
    override fun rulesOf(action: Action.UnscopedAction): Sequence<UnscopedActionRule> =
        action.defaultRules.asSequence()

    override fun <T> rulesOf(action: Action.ScopedAction<in T>): Sequence<ScopedActionRule<in T>> =
        when (action) {
            Action.Unit.READ_PRESCHOOL_APPLICATION_REPORT -> {
                @Suppress("UNCHECKED_CAST")
                sequenceOf(
                    HasGlobalRole(UserRole.ADMIN, UserRole.SERVICE_WORKER) as ScopedActionRule<in T>
                ) +
                    sequenceOf(
                        HasUnitRole(UserRole.UNIT_SUPERVISOR).inUnit() as ScopedActionRule<in T>
                    )
            }

            else -> {
                action.defaultRules.asSequence()
            }
        }
}
