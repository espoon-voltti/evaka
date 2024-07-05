// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dummy_suomifi

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType
import org.keycloak.dom.saml.v2.assertion.AttributeType
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType
import org.keycloak.saml.SAML2LoginResponseBuilder
import org.keycloak.saml.common.constants.JBossSAMLURIConstants
import org.keycloak.saml.common.exceptions.ParsingException
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.util.*

private val logger = LoggerFactory.getLogger("dummy_suomifi.Saml")

fun readSamlLoginRequest(xmlDocument: Document, relayState: String?): LoginRequest {
    val parsed =
        try {
            SAML2Request.getSAML2ObjectFromDocument(xmlDocument)
        } catch (e: ParsingException) {
            throw IllegalArgumentException("Failed to parse SAMLRequest", e)
        }
    logger.debug("Incoming SAML request: ${prettyPrintXml(parsed.samlDocument)}")
    val samlRequest =
        requireNotNull(parsed.samlObject as? AuthnRequestType) { "Invalid SAMLRequest type" }
    return LoginRequest(
        id = samlRequest.id,
        assertionConsumerServiceURL = samlRequest.assertionConsumerServiceURL.toString(),
        issuer = samlRequest.issuer.value,
        relayState = relayState
    )
}

fun writeSamlLoginResponse(request: LoginRequest, user: User): ByteArray {
    val sessionId = UUID.randomUUID().toString()
    val response =
        SAML2LoginResponseBuilder()
            .requestID(request.id)
            .destination(request.assertionConsumerServiceURL)
            .issuer("http://localhost:${Config.port}/idp/sso")
            .requestIssuer(request.issuer)
            .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get(), sessionId)
            .buildModel()
            .apply {
                val assertion = assertions[0].assertion
                user.samlAttributes().forEach { attr ->
                    assertion.addStatement(
                        AttributeStatementType().apply {
                            addAttribute(
                                AttributeStatementType.ASTChoiceType(
                                    AttributeType(attr.urn).apply {
                                        friendlyName = attr.friendlyName
                                        nameFormat =
                                            JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get()
                                        addAttributeValue(attr.value)
                                    })
                            )
                        })
                }
            }
    val xmlString = prettyPrintXml(SAML2Response.convert(response))
    logger.debug("Outgoing SAML response: $xmlString")
    return xmlString.toByteArray()
}

data class LoginRequest(
    val id: String,
    val assertionConsumerServiceURL: String,
    val issuer: String,
    val relayState: String?
)
