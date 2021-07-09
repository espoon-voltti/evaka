// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.springframework.core.env.Environment
import java.net.URI
import java.util.Locale

/**
 * A type-safe configuration parsed from environment variables / other property sources supported by Spring Boot by default
 */
data class EvakaEnv(
    val koskiEnabled: Boolean,
    val frontendBaseUrlFi: String,
    val frontendBaseUrlSv: String,
) {
    companion object {
        fun fromEnvironment(env: Environment): EvakaEnv {
            return EvakaEnv(
                koskiEnabled = env.lookup("evaka.integration.koski.enabled", "fi.espoo.integration.koski.enabled") ?: false,
                frontendBaseUrlFi = env.lookup("evaka.frontend.base_url.fi", "application.frontend.baseurl"),
                frontendBaseUrlSv = env.lookup("evaka.frontend.base_url.sv", "application.frontend.baseurl.sv"),
            )
        }
    }
}

data class DatabaseEnv(
    val url: String,
    val username: String,
    val password: Sensitive<String>,
    val flywayUsername: String,
    val flywayPassword: Sensitive<String>,
    val leakDetectionThreshold: Long,
    val maximumPoolSize: Int
) {
    companion object {
        fun fromEnvironment(env: Environment) = DatabaseEnv(
            url = env.lookup("evaka.database.url", "spring.datasource.url"),
            username = env.lookup("evaka.database.username", "spring.datasource.username"),
            password = Sensitive(env.lookup("evaka.database.password", "spring.datasource.password")),
            flywayUsername = env.lookup("evaka.database.flyway.username", "flyway.username"),
            flywayPassword = Sensitive(env.lookup("evaka.database.flyway.password", "flyway.password")),
            leakDetectionThreshold = env.lookup("evaka.database.leak_detection_threshold", "spring.datasource.hikari.leak-detection-threshold") ?: 0,
            maximumPoolSize = env.lookup("evaka.database.maximum_pool_size", "spring.datasource.hikari.maximumPoolSize") ?: 10
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
            applicationReceivedSenderAddressFi = env.lookup("evaka.email.application_received.sender_address.fi", "application.email.address.fi") ?: "",
            applicationReceivedSenderAddressSv = env.lookup("evaka.email.application_received.sender_address.sv", "application.email.address.sv") ?: "",
            applicationReceivedSenderNameFi = env.lookup("evaka.email.application_received.sender_name.fi", "application.email.name.fi") ?: "",
            applicationReceivedSenderNameSv = env.lookup("evaka.email.application_received.sender_name.sv", "application.email.name.sv") ?: ""
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
            voucherValueDecisions = env.lookup("evaka.bucket.voucher_value_decisions", "fi.espoo.voltti.document.bucket.vouchervaluedecision"),
        )
    }
}

data class KoskiEnv(val url: String, val sourceSystem: String, val user: String, val secret: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) = KoskiEnv(
            url = env.lookup("evaka.integration.koski.url", "fi.espoo.integration.koski.url"),
            sourceSystem = env.lookup("evaka.integration.koski.source_system", "fi.espoo.integration.koski.source_system"),
            user = env.lookup("evaka.integration.koski.user", "fi.espoo.integration.koski.user"),
            secret = Sensitive(env.lookup("evaka.integration.koski.secret", "fi.espoo.integration.koski.secret"))
        )
    }
}

data class VardaEnv(val organizer: String, val url: String, val sourceSystem: String, val basicAuth: Sensitive<String>) {
    companion object {
        fun fromEnvironment(env: Environment) = VardaEnv(
            organizer = env.lookup("evaka.integration.varda.organizer", "fi.espoo.varda.organizer") ?: "Espoo",
            url = env.lookup("evaka.integration.varda.url", "fi.espoo.integration.varda.url"),
            sourceSystem = env.lookup("evaka.integration.varda.source_system", "fi.espoo.integration.varda.source_system"),
            basicAuth = Sensitive(env.lookup("evaka.integration.varda.basic_auth", "fi.espoo.integration.varda.basic_auth") ?: "")
        )
    }
}

data class DvvModificationsEnv(val url: String, val userId: String, val password: Sensitive<String>, val xroadClientId: String, val productCode: String) {
    companion object {
        fun fromEnvironment(env: Environment) = DvvModificationsEnv(
            url = env.lookup("evaka.integration.dvv_modifications.url", "fi.espoo.integration.dvv-modifications-service.url"),
            userId = env.lookup("evaka.integration.dvv_modifications.user_id", "fi.espoo.integration.dvv-modifications-service.userId"),
            password = Sensitive(env.lookup("evaka.integration.dvv_modifications.password", "fi.espoo.integration.dvv-modifications-service.password")),
            xroadClientId = env.lookup("evaka.integration.dvv_modifications.xroad_client_id", "fi.espoo.integration.dvv-modifications-service.xRoadClientId"),
            productCode = env.lookup("evaka.integration.dvv_modifications.product_code", "fi.espoo.integration.dvv-modifications-service.productCode")
        )
    }
}

data class Sensitive<T>(val value: T) {
    override fun toString(): String = "**REDACTED**"
}

inline fun <reified T> Environment.lookup(key: String, vararg legacyKeys: String): T {
    val value = lookup(key, legacyKeys, T::class.java)
    if (value == null && null !is T) {
        error("Missing required configuration: $key (environment variable ${key.toSystemEnvKey()})")
    } else {
        return value as T
    }
}

fun <T> Environment.lookup(key: String, legacyKeys: Array<out String>, clazz: Class<T>): T? =
    sequenceOf(*legacyKeys, key).mapNotNull { getProperty(it, clazz) }.firstOrNull()

// Reference: Spring SystemEnvironmentPropertySource
fun String.toSystemEnvKey() = uppercase(Locale.ENGLISH)
    .replace('.', '_')
    .replace('-', '_')
