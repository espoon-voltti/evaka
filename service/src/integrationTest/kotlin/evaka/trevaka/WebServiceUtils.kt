// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import java.nio.file.Paths
import org.springframework.core.io.UrlResource
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.client.support.interceptor.PayloadValidatingInterceptor

private val schemaPath = Paths.get("src", "main", "schema").toAbsolutePath()

fun newPayloadValidatingInterceptor(vararg filenames: String) =
    PayloadValidatingInterceptor().apply {
        setSchemas(
            *filenames.map { filename -> UrlResource("file:$schemaPath/$filename") }.toTypedArray()
        )
        setValidateRequest(true)
        setValidateResponse(true)
        afterPropertiesSet()
    }

fun addClientInterceptors(
    webServiceTemplate: WebServiceTemplate,
    vararg clientInterceptor: ClientInterceptor,
) {
    webServiceTemplate.setInterceptors(
        (webServiceTemplate.interceptors ?: emptyArray<ClientInterceptor>()) + clientInterceptor
    )
}
