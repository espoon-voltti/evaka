// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.sficlient.SfiMessagesClient
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class Config(env: SfiEnv) {
    val urls =
        ApiUrls(
            requireNotNull(env.restAddress?.toHttpUrlOrNull()) {
                "SFI REST address must be set (was ${env.restAddress})"
            }
        )
    private val printingAndEnvelopingService =
        PrintingAndEnvelopingService(
            PostiMessaging(
                contactDetails =
                    ContactDetails(
                        email =
                            requireNotNull(env.contactPerson.email) {
                                "SFI contact person email must be set"
                            }
                    ),
                username = env.printing.billingId,
                password =
                    requireNotNull(env.printing.billingPassword?.value) {
                        "SFI printing billing password must be set"
                    },
            )
        )

    private val sender = Sender(serviceId = env.serviceIdentifier)
    private val paperMailSender =
        NewPaperMailSender(
            Address(
                name =
                    requireNotNull(env.contactOrganization.name) {
                        "SFI contact organization name must be set"
                    },
                streetAddress =
                    requireNotNull(env.contactOrganization.streetAddress) {
                        "SFI contact organization street address must be set"
                    },
                zipCode =
                    requireNotNull(env.contactOrganization.postalCode) {
                        "SFI contact organization postal code must be set"
                    },
                city =
                    requireNotNull(env.contactOrganization.postOffice) {
                        "SFI contact organization post office must be set"
                    },
            )
        )
    val accessTokenRequestBody =
        AccessTokenRequestBody(
            username = requireNotNull(env.restUsername) { "SFI REST username must be set" },
            password = requireNotNull(env.restPassword?.value) { "SFI REST password must be set" },
        )

    fun messageRequestBody(msg: SfiMessage, file: FileReference) =
        NewMessageFromClientOrganisation(
            msg.messageId,
            NewElectronicMessage(
                title = msg.messageHeader,
                body = msg.messageContent,
                files = listOf(file),
            ),
            NewNormalPaperMail(
                createCoverPage = true,
                listOf(file),
                printingAndEnvelopingService,
                NewPaperMailRecipient(
                    Address(
                        name = "${msg.lastName} ${msg.firstName}",
                        streetAddress = msg.streetAddress,
                        zipCode = msg.postalCode,
                        city = msg.postOffice,
                        countryCode = msg.countryCode,
                    )
                ),
                paperMailSender,
            ),
            Recipient(id = msg.ssn),
            sender,
        )
}

class SfiMessagesRestClient(
    env: SfiEnv,
    private val getDocument: (bucketName: String, key: String) -> Document,
) : SfiMessagesClient {
    private val config = Config(env)
    private val jsonMapper = defaultJsonMapperBuilder().build()
    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMinutes(1))
            .readTimeout(Duration.ofMinutes(1))
            .writeTimeout(Duration.ofMinutes(1))
            // This authenticator gets automatically triggered on 401 Unauthorized responses
            .authenticator { _, response ->
                val usedAuthorization = response.request.header("Authorization")
                val isFirstAttempt = response.priorResponse == null
                if (usedAuthorization != null && isFirstAttempt) {
                    authorizationHeader.expire(Sensitive(usedAuthorization))
                    response.request
                        .newBuilder()
                        .header("Authorization", authorizationHeader.get().value)
                        .build()
                } else null
            }
            .build()

    private val authorizationHeader = InMemoryStore { Sensitive("Bearer ${getAccessToken()}") }

    private val logger = KotlinLogging.logger {}

    private val jsonMediaType = "application/json".toMediaType()

    private fun jsonRequestBody(value: Any) =
        jsonMapper.writeValueAsString(value).toRequestBody(jsonMediaType)

    private inline fun <reified T : Any> jsonResponseBody(response: Response) =
        response.body?.let { body ->
            if (body.contentType() == null || body.contentType() == jsonMediaType)
                jsonMapper.readValue<T>(body.charStream())
            else
                error(
                    "Expected JSON response body ${T::class}, got ${body.contentType()} with length ${body.contentLength()} (${body.string()})"
                )
        } ?: error("Expected JSON response body ${T::class}, got nothing")

    private fun getAccessToken(): String {
        logger.info { "Requesting new access token" }

        httpClient
            .newCall(
                Request.Builder()
                    .url(config.urls.token)
                    .header("Accept", "application/json")
                    .post(jsonRequestBody(config.accessTokenRequestBody))
                    .build()
            )
            .execute()
            .use { response ->
                if (response.isSuccessful) {
                    val body = jsonResponseBody<AccessTokenResponse>(response)
                    return body.access_token
                } else {
                    val body = jsonResponseBody<ApiError>(response)
                    error(
                        "Access token request failed with HTTP ${response.code} ${response.message}: $body"
                    )
                }
            }
    }

    private fun uploadFile(fileName: String, pdfBytes: ByteArray): FileReference {
        logger.info { "Uploading file $fileName (${pdfBytes.size} bytes)" }

        httpClient
            .newCall(
                Request.Builder()
                    .url(config.urls.files)
                    .header("Authorization", authorizationHeader.get().value)
                    .header("Accept", "application/json")
                    .post(pdfUploadBody(fileName, pdfBytes))
                    .build()
            )
            .execute()
            .use { response ->
                if (response.isSuccessful) {
                    val body = jsonResponseBody<NewFileResponse>(response)
                    return FileReference(body.fileId)
                } else {
                    val body = jsonResponseBody<ApiError>(response)
                    error(
                        "File upload request failed with HTTP ${response.code} ${response.message}: $body"
                    )
                }
            }
    }

    override fun send(msg: SfiMessage) {
        val pdfBytes = getDocument(msg.documentBucket, msg.documentKey).bytes

        logger.info(
            mapOf("meta" to mapOf("documentId" to msg.documentId, "messageId" to msg.messageId))
        ) {
            "Sending SFI message about ${msg.documentId} with messageId: ${msg.messageId}"
        }

        val fileReference = uploadFile(msg.documentDisplayName, pdfBytes)

        httpClient
            .newCall(
                Request.Builder()
                    .url(config.urls.messages)
                    .header("Authorization", authorizationHeader.get().value)
                    .header("Accept", "application/json")
                    .post(jsonRequestBody(config.messageRequestBody(msg, fileReference)))
                    .build()
            )
            .execute()
            .use { response ->
                if (response.isSuccessful) {
                    val body = jsonResponseBody<MessageResponse>(response)
                    logger.info {
                        "SFI message delivery succeeded with messageId: ${body.messageId}"
                    }
                    body.messageId
                } else if (response.code == 409) {
                    logger.info {
                        "SFI message delivery failed with HTTP ${response.code} ${response.message}. Skipping duplicate message"
                    }
                } else {
                    val body = jsonResponseBody<ApiError>(response)
                    error(
                        "Message request failed with HTTP ${response.code} ${response.message}: $body"
                    )
                }
            }
    }
}
