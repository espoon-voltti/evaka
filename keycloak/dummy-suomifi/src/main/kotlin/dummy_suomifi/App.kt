// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dummy_suomifi

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.http4k.base64DecodedArray
import org.http4k.core.*
import org.http4k.core.body.form
import org.http4k.filter.CachingFilters
import org.http4k.filter.RequestFilters
import org.http4k.filter.ServerFilters
import org.http4k.lens.contentType
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import org.unbescape.html.HtmlEscape.escapeHtml5
import org.w3c.dom.Document
import java.util.*

class Routes {
    val handler = routes(
        "/health" bind Method.GET to ::healthCheck,
        "/idp/sso" bind Method.GET to ::idpLoginGet,
        "/idp/sso" bind Method.POST to ::idpLoginPost,
        "/idp/sso-login-finish" bind Method.POST to ::idpLoginFinish,
        "/idp/users/clear" bind Method.POST to ::clearUsers,
        "/idp/users" bind Method.POST to ::addUser
    )

    private val jsonMapper = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val users = UserStore()

    fun healthCheck(@Suppress("UNUSED_PARAMETER") request: Request): Response =
        Response(Status.OK).contentType(ContentType.TEXT_PLAIN).body("OK")

    // Login endpoint with HTTP-Redirect binding
    fun idpLoginGet(request: Request): Response = login(
        // The SAML request is passed as a query parameter that has first been DEFLATE-encoded
        // and then base64-encoded
        // Reference: SAML 3.4.4 Message Encoding
        xmlDocument = requireNotNull(request.query("SAMLRequest")) {
            "Missing SAMLRequest query parameter"
        }.base64DecodedArray().inflate().parseXml(),
        // The optional RelayState parameter is passed as a query parameter
        // Reference: SAML 3.4.3 RelayState
        relayState = request.query("RelayState")
    )

    // Login endpoint with HTTP-POST binding
    fun idpLoginPost(request: Request): Response = login(
        // The SAML request is passed as a base64-encoded form field
        // Reference: SAML 3.5.4 Message encoding
        xmlDocument = requireNotNull(request.form("SAMLRequest")) { "Missing SAMLRequest in POST data" }.base64DecodedArray()
            .parseXml(),
        // The optional RelayState parameter is passed as a form field
        // Reference: SAML 3.5.3 RelayState
        relayState = request.form("RelayState")
    )

    private fun login(xmlDocument: Document, relayState: String?): Response {
        val loginRequest = readSamlLoginRequest(xmlDocument, relayState)
        val loginRequestJson = jsonMapper.writeValueAsString(loginRequest)

        val choices = users.getAll().mapIndexed { index, user ->
            """<div>
    <input type="radio" id="${escapeHtml5(user.ssn)}" name="ssn" value="${escapeHtml5(user.ssn)}"${" checked".takeIf { index == 0 }}>
    <label for="${escapeHtml5(user.ssn)}">${escapeHtml5(user.displayName)}</label>
</div>"""
        }
        return Response(Status.OK).contentType(ContentType.TEXT_HTML).body(
            htmlPage(
                """<h1>"suomi.fi"</h1>
<form action="/idp/sso-login-finish" method="POST">
    <input type="hidden" name="login-request" value="${escapeHtml5(loginRequestJson)}">
    <div style="margin-bottom: 20px">
      <button type="submit">Kirjaudu</input>
    </div>
${choices.joinToString(separator = "\n").prependIndent("    ")}
</form>"""
            )
        )
    }

    fun idpLoginFinish(request: Request): Response {
        val loginRequest = jsonMapper.readValue<LoginRequest>(requireNotNull(request.form("login-request")) {
            "Missing login-request in POST data"
        })
        val ssn = requireNotNull(request.form("ssn")) { "Missing ssn in POST data" }
        val user = requireNotNull(users.getAll().find { it.ssn == ssn }) { "Unknown ssn $ssn" }

        val samlResponseBytes = writeSamlLoginResponse(loginRequest, user)
        val responseEncoded = Base64.getEncoder().encodeToString(samlResponseBytes)

        val attrs = user.samlAttributes().map {
            """<li>
    <strong>${escapeHtml5(it.value)}</strong> (${escapeHtml5(it.urn)} / ${escapeHtml5(it.friendlyName)})
</li>"""
        }

        return Response(Status.OK).body(
            htmlPage(
                """<h1>"suomi.fi"</h1>
<form action="${escapeHtml5(loginRequest.assertionConsumerServiceURL)}" method="POST">
    <input type="hidden" name="SAMLResponse" value="${escapeHtml5(responseEncoded)}">
    ${if (loginRequest.relayState != null) """<input type="hidden" name="RelayState" value="${escapeHtml5(loginRequest.relayState)}">""" else ""}
    <button type="submit">Jatka</button>
    <h2>Välitettävät tiedot:</h2>
    <ul>
${attrs.joinToString(separator = "\n").prependIndent("        ")}
    </ul>
</form>"""
            )
        )
    }

    fun clearUsers(@Suppress("UNUSED_PARAMETER") request: Request): Response {
        users.reset()
        return Response(Status.OK)
    }

    fun addUser(request: Request): Response {
        val user = jsonMapper.readValue<User>(request.bodyString())
        users.add(user)
        return Response(Status.OK)
    }
}

fun main() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", Config.logLevel.toString())
    System.setProperty("org.jboss.logging.provider", "slf4j")

    val logger = LoggerFactory.getLogger("dummy_suomifi.App")

    val logRequest = RequestFilters.Tap { req ->
        logger.debug(
            "***** REQUEST: {}: {} *****\n{}", req.method, req.uri, req
        )
    }
    val logResponse = Filter { next ->
        {
            try {
                next(it).let { response ->
                    logger.debug(
                        "***** RESPONSE {} to {}: {} *****\n{}", response.status.code, it.method, it.uri, response
                    )
                    response
                }
            } catch (e: Exception) {
                logger.debug("***** RESPONSE FAILED to ${it.method}: ${it.uri} *****", e)
                throw e
            }
        }
    }

    val routes = Routes()
    val app = routes.handler.withFilter(CachingFilters.CacheResponse.NoCache()).withFilter { next ->
        {
            try {
                next(it)
            } catch (e: IllegalArgumentException) {
                Response(Status.BAD_REQUEST).body(e.message ?: "")
            }
        }
    }.withFilter(ServerFilters.CatchAll()).withFilter(logRequest).withFilter(logResponse)
    app.asServer(SunHttp(port = Config.port)).start()
    logger.info("Started dummy-suomifi on port ${Config.port}")
}

private fun htmlPage(body: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <style>
  html {
    font-size: 20px;
  }
  button {
    font-size: 150%;
  }
  </style>
  <title>dummy-suomifi</title>
</head>
<body>
${body.prependIndent("    ")}
</body>
</html>
"""

