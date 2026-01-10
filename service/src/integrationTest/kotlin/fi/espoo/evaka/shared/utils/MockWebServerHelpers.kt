// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import tools.jackson.databind.json.JsonMapper

class MockWebServerHelper : AutoCloseable {
    private val server = MockWebServer()
    private val routes = mutableMapOf<String, (RecordedRequest) -> MockResponse>()

    init {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val key = "${request.method} ${request.path}"
                    val handler = routes[key]
                    return if (handler != null) {
                        handler(request)
                    } else {
                        // Try to match path without query parameters
                        val pathWithoutQuery = request.path?.substringBefore('?') ?: ""
                        val keyWithoutQuery = "${request.method} $pathWithoutQuery"
                        routes[keyWithoutQuery]?.invoke(request)
                            ?: MockResponse().setResponseCode(404).setBody("Not Found")
                    }
                }
            }
        server.start()
    }

    val baseUrl: String
        get() = server.url("/").toString()

    val mockServer: MockWebServer
        get() = server

    fun addRoute(method: String, path: String, handler: (RecordedRequest) -> MockResponse) {
        routes["$method $path"] = handler
    }

    fun addJsonGetRoute(path: String, jsonBody: String, responseCode: Int = 200) {
        addRoute("GET", path) {
            MockResponse()
                .setResponseCode(responseCode)
                .setBody(jsonBody)
                .addHeader("Content-Type", "application/json")
        }
    }

    fun addJsonPostRoute(path: String, jsonBody: String = "", responseCode: Int = 200) {
        addRoute("POST", path) {
            MockResponse()
                .setResponseCode(responseCode)
                .setBody(jsonBody)
                .addHeader("Content-Type", "application/json")
        }
    }

    fun addJsonGetRoute(path: String, body: Any?, mapper: JsonMapper, responseCode: Int = 200) {
        addJsonGetRoute(path, serializeJson(body, mapper), responseCode)
    }

    fun addJsonPostRoute(path: String, body: Any?, mapper: JsonMapper, responseCode: Int = 200) {
        addJsonPostRoute(path, serializeJson(body, mapper), responseCode)
    }

    private fun serializeJson(body: Any?, mapper: JsonMapper): String {
        return if (body == null) "" else mapper.writeValueAsString(body)
    }

    override fun close() {
        server.shutdown()
    }
}
