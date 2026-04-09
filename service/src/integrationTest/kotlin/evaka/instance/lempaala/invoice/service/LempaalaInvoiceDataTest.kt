// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.lempaala.invoice.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LempaalaInvoiceDataTest {

    @Test
    fun `headerRowFields is in order`() {
        assertInvoiceFieldsAreInOrder(headerRowFields)
    }

    @Test
    fun `codebtorRowFields is in order`() {
        assertInvoiceFieldsAreInOrder(codebtorRowFields)
    }

    @Test
    fun `childHeaderRowFields is in order`() {
        assertInvoiceFieldsAreInOrder(childHeaderRowFields)
    }

    @Test
    fun `rowHeaderRowFields is in order`() {
        assertInvoiceFieldsAreInOrder(rowHeaderRowFields)
    }

    @Test
    fun `detailRowFields is in order`() {
        assertInvoiceFieldsAreInOrder(detailRowFields)
    }
}

private fun assertInvoiceFieldsAreInOrder(fields: List<InvoiceField>) {
    for (i in 1..<fields.size) {
        val previous = fields[i - 1]
        val current = fields[i]

        val previousEndPos = previous.start + previous.length + previous.decimals
        val currentStartPos = current.start

        assertEquals(currentStartPos, previousEndPos) {
            "Check ${previous.field} & ${current.field} start/length fields"
        }
    }
}
