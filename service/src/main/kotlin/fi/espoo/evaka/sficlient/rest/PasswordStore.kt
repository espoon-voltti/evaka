// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.SfiEnv
import mu.KotlinLogging
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.LabelParameterVersionRequest
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.ParameterVersionNotFoundException
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import software.amazon.awssdk.services.ssm.model.SsmException

/** Backing store for a single password with support for multiple versions/labels */
interface PasswordStore {
    /** Gets the password from the store which has the given label */
    fun getPassword(label: Label): VersionedPassword?

    /**
     * Saves a new password to the store, returning its internal version that can be used for
     * applying labels
     */
    fun putPassword(password: Sensitive<String>): Version

    /** Moves the given label to a saved password that has the given version */
    fun moveLabel(version: Version, label: Label)

    data class VersionedPassword(val password: Sensitive<String>, val version: Version)

    /** An internal version number for a specific password value */
    @JvmInline value class Version(val value: Long)

    /** Unique, transferable label for a password */
    enum class Label {
        PENDING,
        CURRENT,
    }
}

class AwsSsmPasswordStore(private val client: SsmClient, env: SfiEnv) : PasswordStore {
    private val ssmName =
        env.restPasswordSsmName ?: error("SFI REST password SSM name is not configured")
    private val logger = KotlinLogging.logger {}

    override fun getPassword(label: PasswordStore.Label): PasswordStore.VersionedPassword? {
        logger.info { "Fetching a password with label $label" }
        return try {
            client
                .getParameter(
                    GetParameterRequest.builder()
                        .name("$ssmName:$label")
                        .withDecryption(true)
                        .build()
                )
                .let { response ->
                    PasswordStore.VersionedPassword(
                        Sensitive(response.parameter().value()),
                        PasswordStore.Version(response.parameter().version()),
                    )
                }
        } catch (e: SsmException) {
            if (e is ParameterNotFoundException || e is ParameterVersionNotFoundException) null
            else throw e
        }
    }

    override fun putPassword(password: Sensitive<String>): PasswordStore.Version =
        client
            .putParameter(
                PutParameterRequest.builder()
                    .name(ssmName)
                    .type(ParameterType.SECURE_STRING)
                    .overwrite(true)
                    .value(password.value)
                    .build()
            )
            .let { response ->
                logger.info { "Saved a new password with version ${response.version()}" }
                PasswordStore.Version(response.version())
            }

    override fun moveLabel(version: PasswordStore.Version, label: PasswordStore.Label) {
        logger.info { "Applying label $label to ${version.value}" }
        client.labelParameterVersion(
            LabelParameterVersionRequest.builder()
                .name(ssmName)
                .labels(label.toString())
                .parameterVersion(version.value)
                .build()
        )
    }
}
