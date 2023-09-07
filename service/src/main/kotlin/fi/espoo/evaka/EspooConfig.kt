// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.children.consent.ChildConsentType
import fi.espoo.evaka.emailclient.EvakaEmailMessageProvider
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.espoo.EspooScheduledJob
import fi.espoo.evaka.espoo.EspooScheduledJobs
import fi.espoo.evaka.espoo.bi.EspooBiPoc
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.invoicing.integration.EspooInvoiceIntegrationClient
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.invoicing.service.DefaultInvoiceGenerationLogic
import fi.espoo.evaka.invoicing.service.EspooIncomeTypesProvider
import fi.espoo.evaka.invoicing.service.EspooInvoiceProducts
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.invoicing.service.InvoiceProductProvider
import fi.espoo.evaka.logging.defaultAccessLoggingValve
import fi.espoo.evaka.reports.patu.EspooPatuIntegrationClient
import fi.espoo.evaka.reports.patu.PatuAsyncJobProcessor
import fi.espoo.evaka.reports.patu.PatuIntegrationClient
import fi.espoo.evaka.reports.patu.PatuReportingService
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.getAuthenticatedUser
import fi.espoo.evaka.shared.controllers.ErrorResponse
import fi.espoo.evaka.shared.controllers.ExceptionHandler
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DevDataInitializer
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Unauthorized
import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.security.actionrule.ActionRuleMapping
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import fi.espoo.evaka.shared.template.EvakaTemplateProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import io.opentracing.Tracer
import java.io.IOException
import org.jdbi.v3.core.Jdbi
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
@Profile("espoo_evaka")
class EspooConfig {
    @Bean fun espooEnv(env: Environment) = EspooEnv.fromEnvironment(env)

    @Bean
    fun invoiceIntegrationClient(
        env: EspooEnv,
        invoiceEnv: ObjectProvider<EspooInvoiceIntegrationEnv>,
        jsonMapper: JsonMapper
    ): InvoiceIntegrationClient =
        when (env.invoiceIntegrationEnabled) {
            true -> EspooInvoiceIntegrationClient(invoiceEnv.getObject(), jsonMapper)
            false -> InvoiceIntegrationClient.MockClient(jsonMapper)
        }

    @Bean
    fun patuIntegrationClient(
        env: EspooEnv,
        patuEnv: ObjectProvider<EspooPatuIntegrationEnv>,
        jsonMapper: JsonMapper
    ): PatuIntegrationClient =
        when (env.patuIntegrationEnabled) {
            true -> EspooPatuIntegrationClient(patuEnv.getObject(), jsonMapper)
            false -> PatuIntegrationClient.MockPatuClient(jsonMapper)
        }

    @Bean
    fun patuReportingService(env: EvakaEnv, client: PatuIntegrationClient): PatuReportingService =
        PatuReportingService(env, client)

    @Bean
    @Lazy
    fun espooInvoiceIntegrationEnv(env: Environment) =
        EspooInvoiceIntegrationEnv.fromEnvironment(env)

    @Bean fun espooInvoiceGenerationLogicChooser() = DefaultInvoiceGenerationLogic

    @Bean
    @Lazy
    fun espooPatuIntegrationEnv(env: Environment) = EspooPatuIntegrationEnv.fromEnvironment(env)

    @Bean
    fun espooPatuAsyncJobProcessor(
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        patuReportingService: PatuReportingService
    ) = PatuAsyncJobProcessor(asyncJobRunner, patuReportingService)

    @Bean
    @Profile("local")
    fun paymentIntegrationMockClient(jsonMapper: JsonMapper): PaymentIntegrationClient =
        PaymentIntegrationClient.MockClient(jsonMapper)

    @Bean
    @Profile("production")
    fun paymentIntegrationClient(): PaymentIntegrationClient =
        PaymentIntegrationClient.FailingClient()

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

    @Bean
    fun espooScheduledJobEnv(env: Environment): ScheduledJobsEnv<EspooScheduledJob> =
        ScheduledJobsEnv.fromEnvironment(
            EspooScheduledJob.values().associateWith { it.defaultSettings },
            "espoo.job",
            env
        )

    @Bean
    @Profile("local")
    fun devDataInitializer(jdbi: Jdbi, tracer: Tracer) = DevDataInitializer(jdbi, tracer)

    @Bean fun incomeTypesProvider(): IncomeTypesProvider = EspooIncomeTypesProvider()

    @Bean fun invoiceProductsProvider(): InvoiceProductProvider = EspooInvoiceProducts.Provider()

    @Bean
    fun espooBiPocRouter(
        env: EspooEnv,
        jdbi: Jdbi,
        tracer: Tracer,
        exceptionHandler: ExceptionHandler
    ) =
        if (env.espooBiPocEnabled) {
            fun error(entity: ResponseEntity<ErrorResponse>?): ServerResponse =
                entity?.body?.let { ServerResponse.status(entity.statusCode).body(it) }
                    ?: ServerResponse.status(entity?.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR)
                        .build()

            fun route(
                f: (db: Database, user: AuthenticatedUser.Integration) -> ServerResponse
            ): (ServerRequest) -> ServerResponse = { req ->
                try {
                    f(Database(jdbi, tracer), req.getAuthenticatedUser())
                } catch (e: BadRequest) {
                    error(exceptionHandler.badRequest(req.servletRequest(), e))
                } catch (e: NotFound) {
                    error(exceptionHandler.notFound(req.servletRequest(), e))
                } catch (e: Unauthorized) {
                    error(exceptionHandler.unauthorized(req.servletRequest(), e))
                } catch (e: IOException) {
                    error(exceptionHandler.IOExceptions(req.servletRequest(), e))
                } catch (e: Throwable) {
                    error(exceptionHandler.unexpectedError(req.servletRequest(), e))
                }
            }

            router {
                path("/integration/espoo-bi-poc").nest {
                    GET("/areas", route(EspooBiPoc.getAreas))
                    GET("/units", route(EspooBiPoc.getUnits))
                    GET("/groups", route(EspooBiPoc.getGroups))
                    GET("/children", route(EspooBiPoc.getChildren))
                    GET("/placements", route(EspooBiPoc.getPlacements))
                    GET("/group-placements", route(EspooBiPoc.getGroupPlacements))
                    GET("/absences", route(EspooBiPoc.getAbsences))
                    GET(
                        "/group-caretaker-allocations",
                        route(EspooBiPoc.getGroupCaretakerAllocations)
                    )
                    GET("/applications", route(EspooBiPoc.getApplications))
                    GET("/decisions", route(EspooBiPoc.getDecisions))
                    GET("/service-need-options", route(EspooBiPoc.getServiceNeedOptions))
                    GET("/service-needs", route(EspooBiPoc.getServiceNeeds))
                    GET("/fee-decisions", route(EspooBiPoc.getFeeDecisions))
                    GET("/fee-decision-children", route(EspooBiPoc.getFeeDecisionChildren))
                    GET("/voucher-value-decisions", route(EspooBiPoc.getVoucherValueDecisions))
                    GET("/curriculum-templates", route(EspooBiPoc.getCurriculumTemplates))
                    GET("/curriculum-documents", route(EspooBiPoc.getCurriculumDocuments))
                    GET("/pedagogical-documents", route(EspooBiPoc.getPedagogicalDocuments))
                }
            }
        } else {
            null
        }

    @Bean
    fun featureConfig(): FeatureConfig =
        FeatureConfig(
            valueDecisionCapacityFactorEnabled = false,
            daycareApplicationServiceNeedOptionsEnabled = false,
            citizenReservationThresholdHours = 150,
            dailyFeeDivisorOperationalDaysOverride = null,
            freeSickLeaveOnContractDays = false, // Doesn't affect Espoo
            freeAbsenceGivesADailyRefund = true,
            alwaysUseDaycareFinanceDecisionHandler = false, // Doesn't affect Espoo
            invoiceNumberSeriesStart = 5000000000,
            paymentNumberSeriesStart = 1, // Payments are not yet in use in Espoo
            unplannedAbsencesAreContractSurplusDays = false, // Doesn't affect Espoo
            maxContractDaySurplusThreshold = null, // Doesn't affect Espoo
            useContractDaysAsDailyFeeDivisor = true, // Doesn't affect Espoo
            enabledChildConsentTypes = setOf(ChildConsentType.EVAKA_PROFILE_PICTURE),
            curriculumDocumentPermissionToShareRequired = true,
            assistanceDecisionMakerRoles = null,
            requestedStartUpperLimit = 14,
            partialAbsenceThresholdsEnabled = true,
            postOffice = "ESPOO",
            municipalMessageAccountName = "Espoon kaupunki - Esbo stad - City of Espoo",
            serviceWorkerMessageAccountName =
                "Varhaiskasvatuksen palveluohjaus - Småbarnspedagogikens servicehandledning - Early childhood education service guidance",
            applyPlacementUnitFromDecision = false,
        )

    @Bean
    fun tomcatCustomizer(env: Environment) =
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
            it.addContextValves(defaultAccessLoggingValve(env))
            it.setDisableMBeanRegistry(false)
        }

    @Bean fun actionRuleMapping(): ActionRuleMapping = DefaultActionRuleMapping()

    @Bean
    fun espooScheduledJobs(
        patuReportingService: PatuReportingService,
        env: ScheduledJobsEnv<EspooScheduledJob>
    ): EspooScheduledJobs = EspooScheduledJobs(patuReportingService, env)
}

data class EspooEnv(
    val invoiceIntegrationEnabled: Boolean,
    val patuIntegrationEnabled: Boolean,
    val espooBiPocEnabled: Boolean
) {
    companion object {
        fun fromEnvironment(env: Environment): EspooEnv =
            EspooEnv(
                invoiceIntegrationEnabled =
                    env.lookup(
                        "espoo.integration.invoice.enabled",
                        "fi.espoo.integration.invoice.enabled"
                    )
                        ?: true,
                patuIntegrationEnabled = env.lookup("espoo.integration.patu.enabled") ?: false,
                espooBiPocEnabled = env.lookup("espoo.integration.bi_poc.enabled") ?: false
            )
    }
}

data class EspooInvoiceIntegrationEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val sendCodebtor: Boolean
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            EspooInvoiceIntegrationEnv(
                url =
                    env.lookup("espoo.integration.invoice.url", "fi.espoo.integration.invoice.url"),
                username =
                    env.lookup(
                        "espoo.integration.invoice.username",
                        "fi.espoo.integration.invoice.username"
                    ),
                password =
                    Sensitive(
                        env.lookup(
                            "espoo.integration.invoice.password",
                            "fi.espoo.integration.invoice.password"
                        )
                    ),
                sendCodebtor = env.lookup("espoo.integration.invoice.send_codebtor") ?: false
            )
    }
}

data class EspooPatuIntegrationEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            EspooPatuIntegrationEnv(
                url = env.lookup("fi.espoo.integration.patu.url"),
                username = env.lookup("fi.espoo.integration.patu.username"),
                password = Sensitive(env.lookup("fi.espoo.integration.patu.password"))
            )
    }
}
