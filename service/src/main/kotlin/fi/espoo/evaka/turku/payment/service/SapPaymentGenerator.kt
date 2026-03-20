// SPDX-FileCopyrightText: 2023-2025 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.payment.service

import fi.espoo.evaka.invoicing.domain.Payment
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import jakarta.xml.bind.JAXBException
import org.springframework.stereotype.Component

@Component
class SapPaymentGenerator(
    private val paymentMarshaller: PaymentMarshaller,
    private val idocGenerator: IdocGenerator,
) {
    data class Result(
        val sendResult: PaymentIntegrationClient.SendResult = PaymentIntegrationClient.SendResult(),
        val paymentStrings: MutableList<String> = mutableListOf(),
    )

    private fun shouldFail(payment: Payment): Boolean {
        if (payment.unit.iban == null) return true
        if (payment.unit.businessId == null) return true
        if (payment.unit.providerId == null) return true
        if (payment.amount <= 0) return true
        return false
    }

    fun generatePayments(payments: List<Payment>, preschoolValues: PreschoolValuesFetcher): Result {
        var successList = mutableListOf<Payment>()
        var failedList = mutableListOf<Payment>()

        val paymentStrings = mutableListOf<String>()

        val preSchoolerMap = preschoolValues.fetchPreschoolers(payments)
        val languageMap = preschoolValues.fetchUnitLanguages(payments)
        val preSchoolAccountingAmount =
            preschoolValues.fetchPreschoolAccountingAmount(payments[0].period)

        val (failed, succeeded) = payments.partition { payment -> shouldFail(payment) }
        failedList.addAll(failed)

        succeeded.forEach {
            var preSchoolAmount = (preSchoolerMap[it.unit.id] ?: 0) * preSchoolAccountingAmount
            val language = languageMap[it.unit.id] ?: "fi"
            val idocs: MutableList<FIDCCP02.IDOC> = mutableListOf()
            idocs.add(idocGenerator.generate(it, preSchoolAmount, language))

            try {
                paymentStrings.add(paymentMarshaller.marshal(idocs))
                successList.add(it)
            } catch (e: JAXBException) {
                failedList.add(it)
            }
        }

        return Result(PaymentIntegrationClient.SendResult(successList, failedList), paymentStrings)
    }
}
