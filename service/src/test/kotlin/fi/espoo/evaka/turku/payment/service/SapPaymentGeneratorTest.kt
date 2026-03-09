// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.payment.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SapPaymentGeneratorTest {
    private val paymentChecker = PaymentChecker()
    private val mockMarshaller = mock<PaymentMarshaller>()
    private val mockIdocGenerator = mock<IdocGenerator>()
    private val sapPaymentGenerator =
        SapPaymentGenerator(paymentChecker, mockMarshaller, mockIdocGenerator)
    private val mockFetcher = mock<PreschoolValuesFetcher>()

    @Test
    fun `result should be equal to a known good format`() {
        val payment = validPayment()
        val correctPayment =
            object {}.javaClass.getResource("/payment-client/CorrectSapPayment.txt")?.readText()
        val result =
            SapPaymentGenerator(paymentChecker, PaymentMarshaller(), IdocGenerator())
                .generatePayments(listOf(payment), mockFetcher)

        assert(result.paymentStrings.count() == 1)
        assert(result.paymentStrings[0] == correctPayment)
    }

    @Test
    fun `should use full preschool accounting amount`() {
        val payment = validPayment()
        val unit = payment.unit.id
        whenever(mockFetcher.fetchPreschoolers(any())).thenReturn(mapOf(unit to 10))
        whenever(mockFetcher.fetchPreschoolAccountingAmount(any())).thenReturn(451)
        whenever(mockFetcher.fetchUnitLanguages(any())).thenReturn(mapOf(unit to "fi"))
        whenever(mockIdocGenerator.generate(any(), any(), any())).thenReturn(FIDCCP02.IDOC())
        whenever(mockMarshaller.marshal(any())).thenReturn("XML")

        sapPaymentGenerator.generatePayments(listOf(payment), mockFetcher)

        verify(mockIdocGenerator).generate(payment, 4510, "fi")
    }
}
