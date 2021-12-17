// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJob
import fi.espoo.evaka.shared.job.ScheduledJobSettings
import mu.KotlinLogging
import org.springframework.core.env.Environment
import software.amazon.awssdk.regions.Region
import java.net.URI
import java.time.LocalDate
import java.util.Locale

/**
 * A type-safe configuration parsed from environment variables / other property sources supported by Spring Boot by default
 */
data class EvakaEnv(
    val koskiEnabled: Boolean,
    val sfiEnabled: Boolean,
    val vtjEnabled: Boolean,
    val awsRegion: Region,
    val asyncJobRunnerDisabled: Boolean,
    val frontendBaseUrlFi: String,
    val frontendBaseUrlSv: String,
    val feeDecisionMinDate: LocalDate,
    val maxAttachmentsPerUser: Int,
    val httpClientCertificateCheck: Boolean,
    val fiveYearsOldDaycareEnabled: Boolean,
    val mockClock: Boolean,
) {
    companion object {
        fun fromEnvironment(env: Environment): EvakaEnv {
            return EvakaEnv(
                koskiEnabled = env.lookup("evaka.integration.koski.enabled", "fi.espoo.integration.koski.enabled") ?: false,
                sfiEnabled = env.lookup("evaka.integration.sfi.enabled", "fi.espoo.evaka.message.enabled") ?: false,
                vtjEnabled = env.lookup("evaka.integration.vtj.enabled", "fi.espoo.voltti.vtj.enabled") ?: false,
                awsRegion = Region.of(env.lookup("evaka.aws.region", "aws.region")),
                asyncJobRunnerDisabled = env.lookup("evaka.async_job_runner.disable_runner") ?: false,
                frontendBaseUrlFi = env.lookup("evaka.frontend.base_url.fi", "application.frontend.baseurl"),
                frontendBaseUrlSv = env.lookup("evaka.frontend.base_url.sv", "application.frontend.baseurl.sv"),
                feeDecisionMinDate = LocalDate.parse(env.lookup<String>("evaka.fee_decision.min_date", "fee_decision_min_date")),
                maxAttachmentsPerUser = env.lookup(
                    "evaka.max_attachments_per_user",
                    "fi.espoo.evaka.maxAttachmentsPerUser"
                ),
                httpClientCertificateCheck = env.lookup(
                    "evaka.http_client.certificate_check",
                    "fuel.certificate.check"
                ) ?: true,
                fiveYearsOldDaycareEnabled = env.lookup(
                    "evaka.five_years_old_daycare.enabled",
                    "fi.espoo.evaka.five_years_old_daycare.enabled"
                ) ?: true,
                mockClock = env.lookup("evaka.clock.mock") ?: false,
            )
        }
    }
}

data class JwtEnv(val publicKeysUrl: URI, val privateKeyUrl: URI) {
    companion object {
        fun fromEnvironment(env: Environment) = JwtEnv(
            publicKeysUrl = env.lookup("evaka.jwt.public_keys_url", "fi.espoo.voltti.auth.jwks.default.url"),
            privateKeyUrl = env.lookup(
                "evaka.jwt.private_key_url",
                "fi.espoo.voltti.auth.jwt.provider.private.key.file",
                "fi.espoo.voltti.auth.jwt.provider.privateKeyFile"
            )
        )
    }
}

data class DatabaseEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val flywayUsername: String,
    val flywayPassword: Sensitive<String>,
    val leakDetectionThreshold: Long,
    val maximumPoolSize: Int,
    val logSql: Boolean
) {
    companion object {
        fun fromEnvironment(env: Environment) = DatabaseEnv(
            url = env.lookup("evaka.database.url", "spring.datasource.url"),
            username = env.lookup("evaka.database.username", "spring.datasource.username"),
            password = Sensitive(env.lookup("evaka.database.password", "spring.datasource.password")),
            flywayUsername = env.lookup("evaka.database.flyway.username", "flyway.username"),
            flywayPassword = Sensitive(env.lookup("evaka.database.flyway.password", "flyway.password")),
            leakDetectionThreshold = env.lookup(
                "evaka.database.leak_detection_threshold",
                "spring.datasource.hikari.leak-detection-threshold"
            ) ?: 0,
            maximumPoolSize = env.lookup("evaka.database.maximum_pool_size", "spring.datasource.hikari.maximumPoolSize")
                ?: 10,
            logSql = env.lookup("evaka.database.log_sql") ?: false
        )
    }
}

data class RedisEnv(
    val url: String,
    val port: Int,
    val password: Sensitive<String>,
    val useSsl: Boolean
) {
    companion object {
        fun fromEnvironment(env: Environment) = RedisEnv(
            url = env.lookup("evaka.redis.url", "redis.url"),
            port = env.lookup("evaka.redis.port", "redis.port"),
            password = Sensitive(env.lookup("evaka.redis.password", "redis.password")),
            useSsl = env.lookup("evaka.redis.use_ssl", "redis.ssl")
        )
    }
}

data class EmailEnv(
    val enabled: Boolean,
    val whitelist: List<Regex>?,
    val senderAddress: String,
    val senderNameFi: String,
    val senderNameSv: String,
    val applicationReceivedSenderAddressFi: String,
    val applicationReceivedSenderAddressSv: String,
    val applicationReceivedSenderNameFi: String,
    val applicationReceivedSenderNameSv: String
) {
    companion object {
        fun fromEnvironment(env: Environment) = EmailEnv(
            enabled = env.lookup("evaka.email.enabled", "application.email.enabled") ?: false,
            whitelist = env.lookup<List<String>?>("evaka.email.whitelist", "application.email.whitelist")?.map(::Regex),
            senderAddress = env.lookup("evaka.email.sender_address", "fi.espoo.evaka.email.reply_to_address"),
            senderNameFi = env.lookup("evaka.email.sender_name.fi", "fi.espoo.evaka.email.sender_name.fi"),
            senderNameSv = env.lookup("evaka.email.sender_name.sv", "fi.espoo.evaka.email.sender_name.sv"),
            applicationReceivedSenderAddressFi = env.lookup(
                "evaka.email.application_received.sender_address.fi",
                "application.email.address.fi"
            ) ?: "",
            applicationReceivedSenderAddressSv = env.lookup(
                "evaka.email.application_received.sender_address.sv",
                "application.email.address.sv"
            ) ?: "",
            applicationReceivedSenderNameFi = env.lookup(
                "evaka.email.application_received.sender_name.fi",
                "application.email.name.fi"
            ) ?: "",
            applicationReceivedSenderNameSv = env.lookup(
                "evaka.email.application_received.sender_name.sv",
                "application.email.name.sv"
            ) ?: ""
        )
    }
}

data class BucketEnv(
    val s3MockUrl: URI,
    val data: String,
    val attachments: String,
    val decisions: String,
    val feeDecisions: String,
    val voucherValueDecisions: String
) {
    fun allBuckets() = listOf(data, attachments, decisions, feeDecisions, voucherValueDecisions)

    companion object {
        fun fromEnvironment(env: Environment) = BucketEnv(
            s3MockUrl = env.lookup("evaka.s3mock.url", "fi.espoo.voltti.s3mock.url"),
            data = env.lookup("evaka.bucket.data", "fi.espoo.voltti.document.bucket.data"),
            attachments = env.lookup("evaka.bucket.attachments", "fi.espoo.voltti.document.bucket.attachments"),
            decisions = env.lookup("evaka.bucket.decisions", "fi.espoo.voltti.document.bucket.daycaredecision"),
            feeDecisions = env.lookup("evaka.bucket.fee_decisions", "fi.espoo.voltti.document.bucket.paymentdecision"),
            voucherValueDecisions = env.lookup(
                "evaka.bucket.voucher_value_decisions",
                "fi.espoo.voltti.document.bucket.vouchervaluedecision"
            ),
        )
    }
}

data class KoskiEnv(val url: String, val sourceSystem: String, val user: String, val secret: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) = KoskiEnv(
            url = env.lookup("evaka.integration.koski.url", "fi.espoo.integration.koski.url"),
            sourceSystem = env.lookup(
                "evaka.integration.koski.source_system",
                "fi.espoo.integration.koski.source_system"
            ),
            user = env.lookup("evaka.integration.koski.user", "fi.espoo.integration.koski.user"),
            secret = Sensitive(env.lookup("evaka.integration.koski.secret", "fi.espoo.integration.koski.secret"))
        )
    }
}

data class VardaEnv(
    val url: String,
    val sourceSystem: String,
    val basicAuth: Sensitive<String>
) {
    companion object {
        fun fromEnvironment(env: Environment) = VardaEnv(
            url = env.lookup("evaka.integration.varda.url", "fi.espoo.integration.varda.url"),
            sourceSystem = env.lookup(
                "evaka.integration.varda.source_system",
                "fi.espoo.integration.varda.source_system"
            ),
            basicAuth = Sensitive(
                env.lookup(
                    "evaka.integration.varda.basic_auth",
                    "fi.espoo.integration.varda.basic_auth"
                ) ?: ""
            )
        )
    }
}

data class DvvModificationsEnv(
    val url: String,
    val userId: String,
    val password: Sensitive<String>,
    val xroadClientId: String,
    val productCode: String
) {
    companion object {
        fun fromEnvironment(env: Environment) = DvvModificationsEnv(
            url = env.lookup("evaka.integration.dvv_modifications.url")
                ?: env.lookup("fi.espoo.integration.dvv-modifications-service.url"),
            userId = env.lookup(
                "evaka.integration.dvv_modifications.user_id",
                "fi.espoo.integration.dvv-modifications-service.userId"
            ),
            password = Sensitive(
                env.lookup(
                    "evaka.integration.dvv_modifications.password",
                    "fi.espoo.integration.dvv-modifications-service.password"
                )
            ),
            xroadClientId = env.lookup(
                "evaka.integration.dvv_modifications.xroad_client_id",
                "fi.espoo.integration.dvv-modifications-service.xRoadClientId"
            ),
            productCode = env.lookup(
                "evaka.integration.dvv_modifications.product_code",
                "fi.espoo.integration.dvv-modifications-service.productCode"
            )
        )
    }
}

data class VtjEnv(
    val username: String,
    val password: Sensitive<String>?,
) {
    companion object {
        fun fromEnvironment(env: Environment) = VtjEnv(
            username = env.lookup(
                "evaka.integration.vtj.username",
                "fi.espoo.voltti.vtj.client.username"
            ),
            password = env.lookup<String?>(
                "evaka.integration.vtj.password",
                "fi.espoo.voltti.vtj.client.password"
            )?.let(::Sensitive)
        )
    }
}

data class VtjXroadEnv(
    val trustStore: KeystoreEnv,
    val keyStore: KeystoreEnv,
    val address: String,
    val client: VtjXroadClientEnv,
    val service: VtjXroadServiceEnv,
    val protocolVersion: String,
) {
    companion object {
        fun fromEnvironment(env: Environment) = VtjXroadEnv(
            trustStore = KeystoreEnv(
                type = env.lookup("evaka.integration.vtj.xroad.trust_store.type", "fi.espoo.voltti.vtj.xroad.trustStore.type") ?: "pkcs12",
                location = env.lookup("evaka.integration.vtj.xroad.trust_store.location", "fi.espoo.voltti.vtj.xroad.trustStore.location"),
                password = Sensitive(env.lookup("evaka.integration.vtj.xroad.trust_store.password", "fi.espoo.voltti.vtj.xroad.trustStore.password") ?: "")
            ),
            keyStore = KeystoreEnv(
                type = env.lookup("evaka.integration.vtj.xroad.key_store.type", "fi.espoo.voltti.vtj.xroad.keyStore.type") ?: "pkcs12",
                location = env.lookup("evaka.integration.vtj.xroad.key_store.location", "fi.espoo.voltti.vtj.xroad.keyStore.location"),
                password = Sensitive(env.lookup("evaka.integration.vtj.xroad.key_store.password", "fi.espoo.voltti.vtj.xroad.keyStore.password") ?: "")
            ),
            address = env.lookup("evaka.integration.vtj.xroad.address", "fi.espoo.voltti.vtj.xroad.address") ?: "",
            client = VtjXroadClientEnv.fromEnvironment(env),
            service = VtjXroadServiceEnv.fromEnvironment(env),
            protocolVersion = env.lookup("evaka.integration.vtj.xroad.protocol_version", "fi.espoo.voltti.vtj.xroad.protocolVersion") ?: "4.0"
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
        fun fromEnvironment(env: Environment) = VtjXroadClientEnv(
            instance = env.lookup("evaka.integration.vtj.xroad.client.instance", "fi.espoo.voltti.vtj.xroad.client.instance") ?: "",
            memberClass = env.lookup("evaka.integration.vtj.xroad.client.member_class", "fi.espoo.voltti.vtj.xroad.client.memberClass") ?: "",
            memberCode = env.lookup("evaka.integration.vtj.xroad.client.member_code", "fi.espoo.voltti.vtj.xroad.client.memberCode") ?: "",
            subsystemCode = env.lookup("evaka.integration.vtj.xroad.client.subsystem_code", "fi.espoo.voltti.vtj.xroad.client.subsystemCode") ?: "",
        )
    }
}

data class VtjXroadServiceEnv(
    val instance: String,
    val memberClass: String,
    val memberCode: String,
    val subsystemCode: String,
    val serviceCode: String,
    val serviceVersion: String?
) {
    companion object {
        fun fromEnvironment(env: Environment) = VtjXroadServiceEnv(
            instance = env.lookup("evaka.integration.vtj.xroad.service.instance", "fi.espoo.voltti.vtj.xroad.service.instance") ?: "",
            memberClass = env.lookup("evaka.integration.vtj.xroad.service.member_class", "fi.espoo.voltti.vtj.xroad.service.memberClass") ?: "",
            memberCode = env.lookup("evaka.integration.vtj.xroad.service.member_code", "fi.espoo.voltti.vtj.xroad.service.memberCode") ?: "",
            subsystemCode = env.lookup("evaka.integration.vtj.xroad.service.subsystem_code", "fi.espoo.voltti.vtj.xroad.service.subsystemCode") ?: "",
            serviceCode = env.lookup("evaka.integration.vtj.xroad.service.service_code", "fi.espoo.voltti.vtj.xroad.service.serviceCode") ?: "",
            serviceVersion = env.lookup("evaka.integration.vtj.xroad.service.service_version", "fi.espoo.voltti.vtj.xroad.service.serviceVersion"),
        )
    }
}

data class SfiEnv(
    val trustStore: KeystoreEnv,
    val keyStore: KeystoreEnv,
    val address: String,
    val signingKeyAlias: String,
    val wsSecurityEnabled: Boolean,
    val message: SfiMessageEnv,
    val printing: SfiPrintingEnv,
    val contactPerson: SfiContactPersonEnv
) {
    companion object {
        fun fromEnvironment(env: Environment) = SfiEnv(
            trustStore = KeystoreEnv(
                type = env.lookup("evaka.integration.sfi.trust_store.type", "fi.espoo.evaka.msg.sfi.ws.trustStore.type") ?: "pkcs12",
                location = env.lookup("evaka.integration.sfi.trust_store.location", "fi.espoo.evaka.msg.sfi.ws.trustStore.location"),
                password = Sensitive(env.lookup("evaka.integration.sfi.trust_store.password", "fi.espoo.evaka.msg.sfi.ws.trustStore.password") ?: "")
            ),
            keyStore = KeystoreEnv(
                type = env.lookup("evaka.integration.sfi.key_store.type", "fi.espoo.evaka.msg.sfi.ws.keyStore.type") ?: "pkcs12",
                location = env.lookup("evaka.integration.sfi.key_store.location", "fi.espoo.evaka.msg.sfi.ws.keyStore.location"),
                password = Sensitive(env.lookup("evaka.integration.sfi.key_store.password", "fi.espoo.evaka.msg.sfi.ws.keyStore.password") ?: "")
            ),
            address = env.lookup("evaka.integration.sfi.address", "fi.espoo.evaka.msg.sfi.ws.address") ?: "",
            signingKeyAlias = env.lookup("evaka.integration.sfi.signing_key_alias", "fi.espoo.evaka.msg.sfi.ws.keyStore.signingKeyAlias") ?: "signing-key",
            wsSecurityEnabled = env.lookup("evaka.integration.sfi.ws_security_enabled") ?: true,
            message = SfiMessageEnv.fromEnvironment(env),
            printing = SfiPrintingEnv.fromEnvironment(env),
            contactPerson = SfiContactPersonEnv.fromEnvironment(env)
        )
    }
}

data class SfiMessageEnv(
    val authorityIdentifier: String,
    val serviceIdentifier: String,
    val certificateCommonName: String
) {
    companion object {
        fun fromEnvironment(env: Environment) = SfiMessageEnv(
            authorityIdentifier = env.lookup("evaka.integration.sfi.message.authority_identifier", "fi.espoo.evaka.msg.sfi.message.authorityIdentifier") ?: "",
            serviceIdentifier = env.lookup("evaka.integration.sfi.message.service_identifier", "fi.espoo.evaka.msg.sfi.message.serviceIdentifier") ?: "",
            certificateCommonName = env.lookup("evaka.integration.sfi.message.certificate_common_name", "fi.espoo.evaka.msg.sfi.message.certificateCommonName") ?: "",
        )
    }
}

data class SfiPrintingEnv(
    val enabled: Boolean,
    val forcePrintForElectronicUser: Boolean,
    val printingProvider: String,
    val billingId: String?,
    val billingPassword: String?,
) {
    companion object {
        fun fromEnvironment(env: Environment) = SfiPrintingEnv(
            enabled = env.lookup("evaka.integration.sfi.printing.enabled", "fi.espoo.evaka.msg.sfi.printing.enablePrinting") ?: false,
            forcePrintForElectronicUser = env.lookup("evaka.integration.sfi.printing.force_print_for_electronic_user", "fi.espoo.evaka.msg.sfi.printing.forcePrintForElectronicUser") ?: false,
            printingProvider = env.lookup("evaka.integration.sfi.printing.provider", "fi.espoo.evaka.msg.sfi.printing.printingProvider") ?: "Edita",
            billingId = env.lookup("evaka.integration.sfi.printing.billing.id", "fi.espoo.evaka.msg.sfi.printing.billingId"),
            billingPassword = env.lookup("evaka.integration.sfi.printing.billing.password", "fi.espoo.evaka.msg.sfi.printing.billingPassword"),
        )
    }
}

data class SfiContactPersonEnv(
    val name: String?,
    val email: String?,
    val phone: String?,
) {
    companion object {
        fun fromEnvironment(env: Environment) = SfiContactPersonEnv(
            name = env.lookup("evaka.integration.sfi.contact_person.name", "fi.espoo.evaka.msg.sfi.printing.contactPersonName") ?: "",
            phone = env.lookup("evaka.integration.sfi.contact_person.phone", "fi.espoo.evaka.msg.sfi.printing.contactPersonPhone") ?: "",
            email = env.lookup("evaka.integration.sfi.contact_person.email", "fi.espoo.evaka.msg.sfi.printing.contactPersonEmail") ?: "",
        )
    }
}

data class KeystoreEnv(
    val type: String = "pkcs12",
    val location: String? = null,
    val password: Sensitive<String> = Sensitive(""),
)

data class ScheduledJobsEnv(val jobs: Map<ScheduledJob, ScheduledJobSettings>) {
    companion object {
        fun fromEnvironment(env: Environment) = ScheduledJobsEnv(
            ScheduledJob.values().associate { job ->
                val envPrefix = "evaka.job.${snakeCaseName(job)}"
                val default = ScheduledJobSettings.default(job)
                val settings = ScheduledJobSettings(
                    enabled = env.lookup("$envPrefix.enabled") ?: default.enabled,
                    schedule = env.lookup<String?>("$envPrefix.cron")?.let(JobSchedule::cron)
                        ?: default.schedule,
                    retryCount = env.lookup("$envPrefix.retry_count") ?: default.retryCount
                )
                (job to settings)
            }
        )
    }
}

data class OphEnv(
    val organizerOid: String,
    val organizerId: String,
    val municipalityCode: String
) {
    companion object {
        fun fromEnvironment(env: Environment): OphEnv {
            return OphEnv(
                organizerOid = env.lookup("evaka.oph.organizer_oid"),
                municipalityCode = env.lookup("evaka.oph.municipality_code"),
                organizerId = env.lookup("evaka.oph.organizer_id")
            )
        }
    }
}

data class Sensitive<T>(val value: T) {
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
    deprecatedKeys.asSequence().mapNotNull { legacyKey ->
        getProperty(legacyKey, clazz)?.also {
            logger.warn {
                "Using deprecated configuration key $legacyKey instead of $key (environment variable ${key.toSystemEnvKey()})"
            }
        }
    }.firstOrNull() ?: getProperty(key, clazz)

// Reference: Spring SystemEnvironmentPropertySource
fun String.toSystemEnvKey() = uppercase(Locale.ENGLISH)
    .replace('.', '_')
    .replace('-', '_')

private fun snakeCaseName(job: ScheduledJob): String = job.name.flatMapIndexed { idx, ch ->
    when {
        idx == 0 -> listOf(ch.lowercaseChar())
        ch.isUpperCase() -> listOf('_', ch.lowercaseChar())
        else -> listOf(ch)
    }
}.joinToString(separator = "")
