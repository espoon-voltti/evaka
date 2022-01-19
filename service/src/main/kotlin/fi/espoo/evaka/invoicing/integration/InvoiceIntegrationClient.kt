// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.integration

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface InvoiceIntegrationClient {
    data class SendResult(
        val succeeded: List<InvoiceDetailed> = listOf(),
        val failed: List<InvoiceDetailed> = listOf()
    )

    fun send(invoices: List<InvoiceDetailed>): SendResult

    class MockClient(private val objectMapper: JsonMapper) : InvoiceIntegrationClient {
        override fun send(invoices: List<InvoiceDetailed>): SendResult {
            logger.info("Mock invoice integration client got invoices ${objectMapper.writeValueAsString(invoices)}")
            return SendResult(invoices)
        }
    }
}
