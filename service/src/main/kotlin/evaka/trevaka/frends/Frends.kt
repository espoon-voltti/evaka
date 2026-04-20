// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka.frends

import evaka.core.dvv.DvvModificationRequestCustomizer
import okhttp3.Credentials
import okhttp3.Interceptor
import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.springframework.ws.transport.WebServiceMessageSender
import org.springframework.ws.transport.http.HttpComponents5ClientFactory
import org.springframework.ws.transport.http.SimpleHttpComponents5MessageSender

private const val HEADER_NAME_API_KEY = "X-API-KEY"

fun frendsWebServiceMessageSender(apiKey: String): WebServiceMessageSender =
    SimpleHttpComponents5MessageSender(newFrendsHttpClient(apiKey))

fun newFrendsHttpClient(apiKey: String): HttpClient =
    HttpClientBuilder.create()
        .addRequestInterceptorFirst(HttpComponents5ClientFactory.RemoveSoapHeadersInterceptor())
        .addRequestInterceptorFirst { request, _, _ ->
            request.addHeader(HEADER_NAME_API_KEY, apiKey)
        }
        .build()

fun dvvModificationRequestCustomizer(apiKey: String) = DvvModificationRequestCustomizer { request ->
    request.header(HEADER_NAME_API_KEY, apiKey)
}

data class FrendsArchivalProperties(val username: String, val password: String)

fun basicAuthInterceptor(properties: FrendsArchivalProperties) = Interceptor { chain ->
    chain.proceed(
        chain
            .request()
            .newBuilder()
            .header(
                "Authorization",
                Credentials.basic(properties.username, properties.password, Charsets.UTF_8),
            )
            .build()
    )
}
