// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import com.fasterxml.jackson.annotation.JsonValue
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.domain.Rectangle
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJobSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.security.KeyStore
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import java.util.UUID
import org.springframework.core.env.Environment
import org.springframework.core.io.UrlResource

/**
 * A type-safe configuration parsed from environment variables / other property sources supported by
 * Spring Boot by default
 */
data class EvakaEnv(
    val koskiEnabled: Boolean,
    val sfiEnabled: Boolean,
    val vtjEnabled: Boolean,
    val webPushEnabled: Boolean,
    val jamixEnabled: Boolean,
    val aromiEnabled: Boolean,
    val särmäEnabled: Boolean,
    val nekkuEnabled: Boolean,
    val forceUnpublishDocumentTemplateEnabled: Boolean,
    val asyncJobRunnerDisabled: Boolean,
    val frontendBaseUrlFi: String,
    val frontendBaseUrlSv: String,
    val feeDecisionMinDate: LocalDate,
    val maxAttachmentsPerUser: Int,
    val mockClock: Boolean,
    val nrOfDaysFeeDecisionCanBeSentInAdvance: Long,
    val nrOfDaysVoucherValueDecisionCanBeSentInAdvance: Long,
    val plannedAbsenceEnabledForHourBasedServiceNeeds: Boolean,
    val personAddressEnvelopeWindowPosition: Rectangle,
    val replacementInvoicesStart: YearMonth?,
    val passwordBlacklistDirectory: String?,
    val placementToolServiceNeedOptionId: ServiceNeedOptionId?,
) {
    companion object {
        fun fromEnvironment(env: Environment): EvakaEnv {
            return EvakaEnv(
                koskiEnabled = env.lookup("evaka.integration.koski.enabled") ?: false,
                sfiEnabled = env.lookup("evaka.integration.sfi.enabled") ?: false,
                vtjEnabled = env.lookup("evaka.integration.vtj.enabled") ?: false,
                webPushEnabled = env.lookup("evaka.web_push.enabled") ?: false,
                jamixEnabled = env.lookup("evaka.integration.jamix.enabled") ?: false,
                aromiEnabled = env.lookup("evaka.integration.aromi.enabled") ?: false,
                särmäEnabled = env.lookup("evaka.integration.sarma.enabled") ?: false,
                nekkuEnabled = env.lookup("evaka.integration.nekku.enabled") ?: false,
                forceUnpublishDocumentTemplateEnabled =
                    env.lookup("evaka.not_for_prod.force_unpublish_document_template_enabled")
                        ?: false,
                asyncJobRunnerDisabled =
                    env.lookup("evaka.async_job_runner.disable_runner") ?: false,
                frontendBaseUrlFi = env.lookup("evaka.frontend.base_url.fi"),
                frontendBaseUrlSv = env.lookup("evaka.frontend.base_url.sv"),
                feeDecisionMinDate =
                    LocalDate.parse(env.lookup<String>("evaka.fee_decision.min_date")),
                maxAttachmentsPerUser = env.lookup("evaka.max_attachments_per_user"),
                mockClock = env.lookup("evaka.clock.mock") ?: false,
                nrOfDaysFeeDecisionCanBeSentInAdvance =
                    env.lookup("evaka.fee_decision.days_in_advance") ?: 0,
                nrOfDaysVoucherValueDecisionCanBeSentInAdvance =
                    env.lookup("evaka.voucher_value_decision.days_in_advance") ?: 0,
                plannedAbsenceEnabledForHourBasedServiceNeeds =
                    env.lookup("evaka.planned_absence.enabled_for_hour_based_service_needs")
                        ?: false,
                personAddressEnvelopeWindowPosition =
                    env.lookup<String?>("evaka.person_address_envelope_window_position")?.let {
                        Rectangle.fromString(it)
                    } ?: Rectangle.iPostWindowPosition,
                replacementInvoicesStart =
                    env.lookup<String?>("evaka.replacement_invoices_start")?.let {
                        YearMonth.parse(it)
                    },
                passwordBlacklistDirectory = env.lookup("evaka.password_blacklist_directory"),
                placementToolServiceNeedOptionId =
                    env.lookup<String?>("evaka.placement_tool.service_need_option_id")?.let {
                        ServiceNeedOptionId(UUID.fromString(it))
                    },
            )
        }
    }
}

data class JwtEnv(val publicKeysUrl: URI) {
    companion object {
        fun fromEnvironment(env: Environment) =
            JwtEnv(publicKeysUrl = env.lookup("evaka.jwt.public_keys_url"))
    }
}

data class WebPushEnv(val vapidPrivateKey: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) =
            WebPushEnv(vapidPrivateKey = Sensitive(env.lookup("evaka.web_push.vapid_private_key")))
    }
}

data class DatabaseEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val flywayUsername: String,
    val flywayPassword: Sensitive<String>,
    val flywayLocations: List<String>,
    val flywayIgnoreFutureMigrations: Boolean,
    val leakDetectionThreshold: Long,
    val defaultStatementTimeout: Duration,
    val maximumPoolSize: Int,
    val logSql: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            DatabaseEnv(
                url = env.lookup("evaka.database.url"),
                username = env.lookup("evaka.database.username"),
                password = Sensitive(env.lookup("evaka.database.password")),
                flywayUsername = env.lookup("evaka.database.flyway.username"),
                flywayPassword = Sensitive(env.lookup("evaka.database.flyway.password")),
                flywayLocations =
                    env.lookup("evaka.database.flyway.locations") ?: listOf("db/migration"),
                flywayIgnoreFutureMigrations =
                    env.lookup("evaka.database.flyway.ignore-future-migrations") ?: true,
                leakDetectionThreshold = env.lookup("evaka.database.leak_detection_threshold") ?: 0,
                defaultStatementTimeout =
                    env.lookup("evaka.database.default_statement_timeout")
                        ?: Duration.ofSeconds(60),
                maximumPoolSize = env.lookup("evaka.database.maximum_pool_size") ?: 10,
                logSql = env.lookup("evaka.database.log_sql") ?: false,
            )
    }
}

data class EmailEnv(
    val enabled: Boolean,
    val whitelist: List<Regex>?,
    val senderAddress: String,
    val senderNameFi: String,
    val senderNameSv: String,
    val subjectPostfix: String?,
    val applicationReceivedSenderAddressFi: String,
    val applicationReceivedSenderAddressSv: String,
    val applicationReceivedSenderNameFi: String,
    val applicationReceivedSenderNameSv: String,
) {
    fun sender(language: Language): String =
        when (language) {
            Language.sv -> "$senderNameSv <$senderAddress>"
            else -> "$senderNameFi <$senderAddress>"
        }

    fun applicationReceivedSender(language: Language): String =
        when (language) {
            Language.sv -> "$applicationReceivedSenderNameSv <$applicationReceivedSenderAddressSv>"
            else -> "$applicationReceivedSenderNameFi <$applicationReceivedSenderAddressFi>"
        }

    companion object {
        private fun getLegacyPostfix(): String? =
            when (val volttiEnv = System.getenv("VOLTTI_ENV")) {
                "prod",
                null -> null
                else -> volttiEnv
            }

        fun fromEnvironment(env: Environment) =
            EmailEnv(
                enabled = env.lookup("evaka.email.enabled") ?: false,
                whitelist = env.lookup<List<String>?>("evaka.email.whitelist")?.map(::Regex),
                senderAddress = env.lookup("evaka.email.sender_address"),
                senderNameFi = env.lookup("evaka.email.sender_name.fi"),
                senderNameSv = env.lookup("evaka.email.sender_name.sv"),
                subjectPostfix = env.lookup("evaka.email.subject_postfix") ?: getLegacyPostfix(),
                applicationReceivedSenderAddressFi =
                    env.lookup("evaka.email.application_received.sender_address.fi") ?: "",
                applicationReceivedSenderAddressSv =
                    env.lookup("evaka.email.application_received.sender_address.sv") ?: "",
                applicationReceivedSenderNameFi =
                    env.lookup("evaka.email.application_received.sender_name.fi") ?: "",
                applicationReceivedSenderNameSv =
                    env.lookup("evaka.email.application_received.sender_name.sv") ?: "",
            )
    }
}

data class BucketEnv(
    val localS3Url: URI,
    val localS3AccessKeyId: String,
    val localS3SecretAccessKey: String,
    val proxyThroughNginx: Boolean,
    val data: String,
    val attachments: String,
    val decisions: String,
    val feeDecisions: String,
    val voucherValueDecisions: String,
) {
    fun allBuckets() = listOf(data, attachments, decisions, feeDecisions, voucherValueDecisions)

    companion object {
        fun fromEnvironment(env: Environment) =
            BucketEnv(
                localS3Url = env.lookup("evaka.local_s3.url"),
                localS3AccessKeyId = env.lookup("evaka.local_s3.access_key_id"),
                localS3SecretAccessKey = env.lookup("evaka.local_s3.secret_access_key"),
                proxyThroughNginx = env.lookup("evaka.bucket.proxy_through_nginx") ?: true,
                data = env.lookup("evaka.bucket.data"),
                attachments = env.lookup("evaka.bucket.attachments"),
                decisions = env.lookup("evaka.bucket.decisions"),
                feeDecisions = env.lookup("evaka.bucket.fee_decisions"),
                voucherValueDecisions = env.lookup("evaka.bucket.voucher_value_decisions"),
            )
    }
}

data class KoskiEnv(
    val url: String,
    val sourceSystem: String,
    val user: String,
    val secret: Sensitive<String>,
    val municipalityCallerId: String,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            KoskiEnv(
                url = env.lookup("evaka.integration.koski.url"),
                sourceSystem = env.lookup("evaka.integration.koski.source_system"),
                user = env.lookup("evaka.integration.koski.user"),
                secret = Sensitive(env.lookup("evaka.integration.koski.secret")),
                municipalityCallerId =
                    env.lookup("evaka.integration.koski.municipality_caller_id") ?: "espooevaka",
            )
    }
}

data class VardaEnv(
    val url: URI,
    val sourceSystem: String,
    val basicAuth: Sensitive<String>,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val localDevPort: Int?,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            VardaEnv(
                url = env.lookup("evaka.integration.varda.url"),
                sourceSystem = env.lookup("evaka.integration.varda.source_system"),
                basicAuth = Sensitive(env.lookup("evaka.integration.varda.basic_auth") ?: ""),
                startDate = env.lookup("evaka.integration.varda.start_date"),
                endDate = env.lookup("evaka.integration.varda.end_date"),

                // Port that's forwarded to Varda for local development
                localDevPort = env.lookup("evaka.integration.varda.local_dev_port"),
            )
    }
}

data class DvvModificationsEnv(
    val url: String,
    val userId: String,
    val password: Sensitive<String>,
    val xroadClientId: String,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            DvvModificationsEnv(
                url =
                    env.lookup(
                        "evaka.integration.dvv_modifications.url",
                        "fi.espoo.integration.dvv-modifications-service.url",
                    ),
                userId = env.lookup("evaka.integration.dvv_modifications.user_id"),
                password = Sensitive(env.lookup("evaka.integration.dvv_modifications.password")),
                xroadClientId = env.lookup("evaka.integration.dvv_modifications.xroad_client_id"),
            )
    }
}

data class VtjEnv(val username: String, val password: Sensitive<String>?) {
    companion object {
        fun fromEnvironment(env: Environment) =
            VtjEnv(
                username = env.lookup("evaka.integration.vtj.username"),
                password = env.lookup<String?>("evaka.integration.vtj.password")?.let(::Sensitive),
            )
    }
}

data class VtjXroadEnv(
    val trustStore: KeystoreEnv?,
    val keyStore: KeystoreEnv?,
    val address: String,
    val httpClientCertificateCheck: Boolean,
    val client: VtjXroadClientEnv,
    val service: VtjXroadServiceEnv,
    val protocolVersion: String,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            VtjXroadEnv(
                trustStore =
                    env.lookup<URI?>("evaka.integration.vtj.xroad.trust_store.location")?.let {
                        location ->
                        KeystoreEnv(
                            location = location,
                            type =
                                env.lookup("evaka.integration.vtj.xroad.trust_store.type")
                                    ?: "pkcs12",
                            password =
                                Sensitive(
                                    env.lookup("evaka.integration.vtj.xroad.trust_store.password")
                                        ?: ""
                                ),
                        )
                    },
                keyStore =
                    env.lookup<URI?>("evaka.integration.vtj.xroad.key_store.location")?.let {
                        location ->
                        KeystoreEnv(
                            location = location,
                            type =
                                env.lookup("evaka.integration.vtj.xroad.key_store.type")
                                    ?: "pkcs12",
                            password =
                                Sensitive(
                                    env.lookup("evaka.integration.vtj.xroad.key_store.password")
                                        ?: ""
                                ),
                        )
                    },
                address = env.lookup("evaka.integration.vtj.xroad.address") ?: "",
                httpClientCertificateCheck =
                    env.lookup("evaka.integration.vtj.xroad.certificate_check") ?: true,
                client = VtjXroadClientEnv.fromEnvironment(env),
                service = VtjXroadServiceEnv.fromEnvironment(env),
                protocolVersion =
                    env.lookup("evaka.integration.vtj.xroad.protocol_version") ?: "4.0",
            )
    }
}

data class VtjXroadClientEnv(
    val instance: String,
    val memberClass: String,
    val memberCode: String,
    val subsystemCode: String,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            VtjXroadClientEnv(
                instance = env.lookup("evaka.integration.vtj.xroad.client.instance") ?: "",
                memberClass = env.lookup("evaka.integration.vtj.xroad.client.member_class") ?: "",
                memberCode = env.lookup("evaka.integration.vtj.xroad.client.member_code") ?: "",
                subsystemCode =
                    env.lookup("evaka.integration.vtj.xroad.client.subsystem_code") ?: "",
            )
    }
}

data class VtjXroadServiceEnv(
    val instance: String,
    val memberClass: String,
    val memberCode: String,
    val subsystemCode: String,
    val serviceCode: String,
    val serviceVersion: String?,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            VtjXroadServiceEnv(
                instance = env.lookup("evaka.integration.vtj.xroad.service.instance") ?: "",
                memberClass = env.lookup("evaka.integration.vtj.xroad.service.member_class") ?: "",
                memberCode = env.lookup("evaka.integration.vtj.xroad.service.member_code") ?: "",
                subsystemCode =
                    env.lookup("evaka.integration.vtj.xroad.service.subsystem_code") ?: "",
                serviceCode = env.lookup("evaka.integration.vtj.xroad.service.service_code") ?: "",
                serviceVersion = env.lookup("evaka.integration.vtj.xroad.service.service_version"),
            )
    }
}

/** Configuration for Suomi.fi Messages API integration */
data class SfiEnv(
    /**
     * Identifier of the service making requests to the Messages API.
     *
     * Given by VIA.
     */
    val serviceIdentifier: String,
    /** Configuration for sending messages on real paper (vs digital messages). */
    val printing: SfiPrintingEnv,
    /**
     * Contact details for a person in the organization making API requests.
     *
     * Not mandatory, but used e.g. to communicate about errors
     */
    val contactPerson: SfiContactPersonEnv,
    /**
     * Contact details for the organization making API requests.
     *
     * Mandatory for paper posting testing
     */
    val contactOrganization: SfiContactOrganizationEnv,
    /** URI of the messages REST API endpoint */
    val restAddress: URI?,
    /** Username for the messages REST API, also known as "systemId" */
    val restUsername: String?,
    /**
     * SSM parameter name for the messages REST API password.
     *
     * The service instance must have the permission to perform the following operations on it:
     * - GetParameter
     * - PutParameter
     * - LabelParameterVersion
     *
     * Required first time manual setup: save the password received from Suomi.fi to SSM and add the
     * label CURRENT to it
     */
    val restPasswordSsmName: String?,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            SfiEnv(
                serviceIdentifier = env.lookup("evaka.integration.sfi.service_identifier"),
                printing = SfiPrintingEnv.fromEnvironment(env),
                contactPerson = SfiContactPersonEnv.fromEnvironment(env),
                contactOrganization = SfiContactOrganizationEnv.fromEnvironment(env),
                restAddress = env.lookup("evaka.integration.sfi.rest_address"),
                restUsername = env.lookup("evaka.integration.sfi.rest_username"),
                restPasswordSsmName = env.lookup("evaka.integration.sfi.rest_password_ssm_name"),
            )
    }
}

data class SfiPrintingEnv(
    /**
     * If false, real paper messages are never sent. If true, real paper messages may be sent.
     *
     * Even if printing is disabled, printing provider and billing id are marked as required fields
     * by Suomi.fi.
     */
    val enabled: Boolean,
    /** Force messages to be sent on paper, even if the recipient has a digital mailbox. */
    val forcePrintForElectronicUser: Boolean,
    /**
     * Name of printing provider.
     *
     * The organization sending requests to the Messages API must have a printing agreement with the
     * provider.
     */
    val printingProvider: String,
    /** Billing ID sent to the printing provider */
    val billingId: String,
    /** Billing password, if required by the printing provider */
    val billingPassword: Sensitive<String>?,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            SfiPrintingEnv(
                enabled = env.lookup("evaka.integration.sfi.printing.enabled") ?: false,
                forcePrintForElectronicUser =
                    env.lookup("evaka.integration.sfi.printing.force_print_for_electronic_user")
                        ?: false,
                printingProvider = env.lookup("evaka.integration.sfi.printing.provider"),
                billingId = env.lookup("evaka.integration.sfi.printing.billing.id"),
                billingPassword =
                    env.lookup<String?>("evaka.integration.sfi.printing.billing.password")
                        ?.let(::Sensitive),
            )
    }
}

data class SfiContactPersonEnv(val name: String?, val email: String?, val phone: String?) {
    companion object {
        fun fromEnvironment(env: Environment) =
            SfiContactPersonEnv(
                name = env.lookup("evaka.integration.sfi.contact_person.name"),
                phone = env.lookup("evaka.integration.sfi.contact_person.phone"),
                email = env.lookup("evaka.integration.sfi.contact_person.email"),
            )
    }
}

data class SfiContactOrganizationEnv(
    val name: String?,
    val streetAddress: String?,
    val postalCode: String?,
    val postOffice: String?,
) {
    companion object {
        fun fromEnvironment(env: Environment) =
            SfiContactOrganizationEnv(
                name = env.lookup("evaka.integration.sfi.contact_organization.name"),
                streetAddress =
                    env.lookup("evaka.integration.sfi.contact_organization.street_address"),
                postalCode = env.lookup("evaka.integration.sfi.contact_organization.postal_code"),
                postOffice = env.lookup("evaka.integration.sfi.contact_organization.post_office"),
            )
    }
}

data class KeystoreEnv(
    val location: URI,
    val type: String = "pkcs12",
    val password: Sensitive<String>? = null,
) {
    fun load(): KeyStore =
        KeyStore.getInstance(type).apply {
            UrlResource(location).inputStream.use { load(it, password?.value?.toCharArray()) }
        }
}

data class ScheduledJobsEnv<T : Enum<T>>(val jobs: Map<T, ScheduledJobSettings>) {
    companion object {
        fun <T : Enum<T>> fromEnvironment(
            defaults: Map<T, ScheduledJobSettings>,
            prefix: String,
            env: Environment,
        ) =
            ScheduledJobsEnv(
                defaults.mapValues { (job, default) ->
                    val envPrefix = "$prefix.${snakeCaseName(job)}"
                    ScheduledJobSettings(
                        enabled = env.lookup("$envPrefix.enabled") ?: default.enabled,
                        schedule =
                            env.lookup<String?>("$envPrefix.cron")?.let(JobSchedule::cron)
                                ?: default.schedule,
                        retryCount = env.lookup("$envPrefix.retry_count") ?: default.retryCount,
                    )
                }
            )
    }
}

data class OphEnv(val organizerOid: String, val organizerId: String, val municipalityCode: String) {
    companion object {
        fun fromEnvironment(env: Environment): OphEnv {
            return OphEnv(
                organizerOid = env.lookup("evaka.oph.organizer_oid"),
                municipalityCode = env.lookup("evaka.oph.municipality_code"),
                organizerId = env.lookup("evaka.oph.organizer_id"),
            )
        }
    }
}

data class CitizenCalendarEnv(val calendarOpenBeforePlacementDays: Int) {
    companion object {
        fun fromEnvironment(env: Environment): CitizenCalendarEnv {
            return CitizenCalendarEnv(
                calendarOpenBeforePlacementDays =
                    env.lookup("evaka.citizen.calendar.calendar_open_before_placement_days") ?: 14
            )
        }
    }
}

data class JamixEnv(val url: URI, val user: String, val password: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment): JamixEnv {
            return JamixEnv(
                // URL up to the operation name, e.g. https://fi.jamix.cloud/japi/pirnet/
                url = URI.create(env.lookup("evaka.integration.jamix.url")),
                user = env.lookup("evaka.integration.jamix.user"),
                password = Sensitive(env.lookup("evaka.integration.jamix.password")),
            )
        }
    }
}

data class NekkuEnv(val url: URI, val apikey: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment): NekkuEnv {
            return NekkuEnv(
                // URL up to the operation name/
                url = URI.create(env.lookup("evaka.integration.nekku.url")),
                apikey = Sensitive(env.lookup("evaka.integration.nekku.apikey")),
            )
        }
    }
}

data class AromiEnv(val sftp: SftpEnv, val filePattern: String) {
    companion object {
        fun fromEnvironment(env: Environment): AromiEnv {
            return AromiEnv(
                sftp =
                    SftpEnv(
                        host = env.lookup("evaka.integration.aromi.sftp.host"),
                        port = env.lookup("evaka.integration.aromi.sftp.port") ?: 22,
                        hostKeys = env.lookup("evaka.integration.aromi.sftp.host_keys"),
                        username = env.lookup("evaka.integration.aromi.sftp.username"),
                        password = Sensitive(env.lookup("evaka.integration.aromi.sftp.password")),
                    ),
                filePattern = env.lookup("evaka.integration.aromi.file_pattern"),
            )
        }
    }
}

data class SftpEnv(
    val host: String,
    val port: Int,
    val hostKeys: List<String>,
    val username: String,
    val password: Sensitive<String>,
)

data class Sensitive<T>(@JsonValue val value: T) {
    override fun toString(): String = "**REDACTED**"
}

inline fun <reified T> Environment.lookup(key: String, vararg deprecatedKeys: String): T {
    val value = lookup(key, deprecatedKeys, T::class.java)
    if (value == null && null !is T) {
        error("Missing required configuration: $key (environment variable ${key.toSystemEnvKey()})")
    } else {
        return value as T
    }
}

private val logger = KotlinLogging.logger {}

fun <T> Environment.lookup(key: String, deprecatedKeys: Array<out String>, clazz: Class<T>): T? =
    deprecatedKeys
        .asSequence()
        .mapNotNull { legacyKey ->
            try {
                getProperty(legacyKey, clazz)?.also {
                    logger.warn {
                        "Using deprecated configuration key $legacyKey instead of $key (environment variable ${key.toSystemEnvKey()})"
                    }
                }
            } catch (e: Exception) {
                throw EnvLookupException(legacyKey, e)
            }
        }
        .firstOrNull()
        ?: try {
            getProperty(key, clazz)
        } catch (e: Exception) {
            throw EnvLookupException(key, e)
        }

class EnvLookupException(key: String, cause: Throwable) :
    RuntimeException(
        "Failed to lookup configuration key $key (environment variable ${key.toSystemEnvKey()})",
        cause,
    )

// Reference: Spring SystemEnvironmentPropertySource
fun String.toSystemEnvKey() = uppercase(Locale.ENGLISH).replace('.', '_').replace('-', '_')

private fun snakeCaseName(job: Enum<*>): String =
    job.name
        .flatMapIndexed { idx, ch ->
            when {
                idx == 0 -> listOf(ch.lowercaseChar())
                ch.isUpperCase() -> listOf('_', ch.lowercaseChar())
                else -> listOf(ch)
            }
        }
        .joinToString(separator = "")

data class ArchiveEnv(
    /** URL up to the endpoint name e.g. http://10.0.0.10/archive-core/ */
    val url: URI,
    val useMockClient: Boolean,
    val userId: String,
    val userRole: String,
) {

    companion object {
        fun fromEnvironment(env: Environment) =
            ArchiveEnv(
                url = URI.create(env.lookup("evaka.integration.sarma.url")),
                useMockClient = env.lookup("evaka.integration.sarma.use_mock_client") ?: false,
                userId = env.lookup("evaka.integration.sarma.user_id"),
                userRole = env.lookup("evaka.integration.sarma.user_role"),
            )
    }
}
