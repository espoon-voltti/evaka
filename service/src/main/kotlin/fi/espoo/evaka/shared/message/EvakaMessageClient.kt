// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.message

import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.encodeSignedJwtToken
import mu.KotlinLogging
import org.springframework.core.env.Environment

interface IEvakaMessageClient {
    fun send(msg: SuomiFiMessage)
}

data class SuomiFiMessage(
    val messageId: String,
    val documentId: String, // This is sent to suomi.fi as reference id (UUID)
    val documentUri: String, // Uri to S3 document: s3://<bucket name>/<document key>
    val documentDisplayName: String, // Document name is shown to end user
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val language: String = "fi",
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val countryCode: String = "FI",
    val messageHeader: String,
    val messageContent: String,
    val emailHeader: String? = null,
    val emailContent: String? = null
)

private val logger = KotlinLogging.logger { }

class MockEvakaMessageClient : IEvakaMessageClient {
    override fun send(msg: SuomiFiMessage) {
        logger.info("Mock message client got $msg")
        data.put(msg.messageId, msg)
    }

    companion object {
        private val data = mutableMapOf<String, SuomiFiMessage>()
        fun getMessages() = data.values.toList()
        fun clearMessages() = data.clear()
    }
}

class EvakaMessageClient(
    env: Environment,
    private val algorithm: Algorithm,
    private val objectMapper: ObjectMapper
) : IEvakaMessageClient {
    private val evakaMessageServiceUrl = env.getRequiredProperty("fi.espoo.evaka.message.url")

    override fun send(msg: SuomiFiMessage) {
        logger.info("Sending suomi.fi message ${msg.documentId}")
        sendRequest(msg, "$evakaMessageServiceUrl/message/send")
    }

    private fun sendRequest(msg: SuomiFiMessage, path: String) {
        val token = encodeSignedJwtToken(algorithm, user = AuthenticatedUser.SystemInternalUser)
        val (_, _, result) = Fuel.post(path)
            .authentication().bearer(token)
            .jsonBody(objectMapper.writeValueAsString(msg))
            .response()
        result.get()
    }
}
