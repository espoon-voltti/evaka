// SPDX-FileCopyrightText: 2023-2025 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.payment.service

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.invoicing.domain.Payment
import fi.espoo.evaka.invoicing.domain.PaymentIntegrationClient
import fi.espoo.evaka.oulu.util.FieldType
import fi.espoo.evaka.oulu.util.FinanceDateProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Component

@Component
class ProEPaymentGenerator(
    private val paymentChecker: PaymentChecker,
    val financeDateProvider: FinanceDateProvider,
    val bicMapper: BicMapper,
) {
    data class Result(
        val sendResult: PaymentIntegrationClient.SendResult = PaymentIntegrationClient.SendResult(),
        val paymentString: String = "",
    )

    fun gatherPaymentData(payment: Payment): Map<PaymentFieldName, String> {
        // var paymentData = PaymentData()
        val paymentDataMap = mutableMapOf<PaymentFieldName, String>()

        val paymentDateFormatterYYMMDD = DateTimeFormatter.ofPattern("yyMMdd")

        paymentDataMap[PaymentFieldName.INTIME_COMPANY_ID] = "20"
        paymentDataMap[PaymentFieldName.PROVIDER_ID] = payment.unit.providerId ?: ""
        paymentDataMap[PaymentFieldName.INVOICE_ID] = payment.number.toString()
        paymentDataMap[PaymentFieldName.INVOICE_ACCEPTANCE] = "1"
        paymentDataMap[PaymentFieldName.VOUCHER_TYPE] = "3B"
        paymentDataMap[PaymentFieldName.VOUCHER_NUMBER] = payment.number?.toString() ?: "0"
        paymentDataMap[PaymentFieldName.VOUCHER_DATE] = financeDateProvider.previousMonthLastDate()
        paymentDataMap[PaymentFieldName.INVOICE_TYPE] = "1"
        paymentDataMap[PaymentFieldName.ACCOUNT_SUGGESTION] = "1"
        paymentDataMap[PaymentFieldName.PERIOD] = financeDateProvider.previousMonthYYMM()
        paymentDataMap[PaymentFieldName.INVOICE_DATE] =
            payment.paymentDate?.format(paymentDateFormatterYYMMDD)
                ?: LocalDate.now().format(paymentDateFormatterYYMMDD)
        paymentDataMap[PaymentFieldName.DUE_DATE] =
            payment.dueDate?.format(paymentDateFormatterYYMMDD)
                ?: LocalDate.now().format(paymentDateFormatterYYMMDD)
        paymentDataMap[PaymentFieldName.INVOICE_SUM] = payment.amount.toString()
        paymentDataMap[PaymentFieldName.INVOICE_1] = ""
        paymentDataMap[PaymentFieldName.CURRENCY] = ""
        paymentDataMap[PaymentFieldName.CASHBOX_DATE] = ""
        paymentDataMap[PaymentFieldName.CASHBOX_SUM] = ""
        paymentDataMap[PaymentFieldName.CASHBOX_MINUS] = ""
        paymentDataMap[PaymentFieldName.DEBT_ACCOUNT] = "00002545"
        paymentDataMap[PaymentFieldName.SI_DEBT_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.KP_PURCHASE_ACCOUNT] = "4335"
        paymentDataMap[PaymentFieldName.SI_PURCHASE_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.KP_CASHBOX_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.SI_CASHBOX_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.KP_OTHER_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.SI_OTHER_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.KP_KERO_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.SI_KERO_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.STATS] = ""
        val calcIdentifier =
            "1104" +
                if (
                    payment.unit.careType.contains(CareType.FAMILY) or
                        payment.unit.careType.contains(CareType.GROUP_FAMILY)
                ) {
                    "372"
                } else {
                    "371"
                }
        paymentDataMap[PaymentFieldName.CALC_IDENTIFIER] = calcIdentifier
        paymentDataMap[PaymentFieldName.RESP_PERSON] = ""
        paymentDataMap[PaymentFieldName.FACTORING_NUMBER] = ""
        paymentDataMap[PaymentFieldName.MACHINE_REFERENCE_NUMBER] = ""
        paymentDataMap[PaymentFieldName.APPR_TARGET] = ""
        paymentDataMap[PaymentFieldName.ALPHABETICAL_NAME] = ""
        paymentDataMap[PaymentFieldName.NAME] = ""
        paymentDataMap[PaymentFieldName.ADDRESS1] = ""
        paymentDataMap[PaymentFieldName.ADDRESS2] = ""
        paymentDataMap[PaymentFieldName.POSTAL_NUMBER] = ""
        paymentDataMap[PaymentFieldName.COUNTRY] = ""
        paymentDataMap[PaymentFieldName.LANGUAGE] = ""
        paymentDataMap[PaymentFieldName.COUNTRY_CODE] = ""
        paymentDataMap[PaymentFieldName.BANK] = ""
        paymentDataMap[PaymentFieldName.BANK_ACCOUNT] = payment.unit.iban.toString()
        paymentDataMap[PaymentFieldName.NOTE] =
            (payment.unit.providerId ?: "") + " " + payment.unit.name
        paymentDataMap[PaymentFieldName.VAT_PERIOD] = financeDateProvider.previousMonthYYMM()
        paymentDataMap[PaymentFieldName.VAT_VAL] = "0"
        paymentDataMap[PaymentFieldName.INVOICE_2] = ""
        paymentDataMap[PaymentFieldName.RECLAMATION_DATE] = ""
        paymentDataMap[PaymentFieldName.PAYMENT_TERM] = ""
        paymentDataMap[PaymentFieldName.PAYMENT_MESSAGE] = "1"
        paymentDataMap[PaymentFieldName.INVOICE_BANK_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.KOM_TRANSFER_BAN] = "0"
        paymentDataMap[PaymentFieldName.VALUATION_BAN] = "0"
        paymentDataMap[PaymentFieldName.PAYMENT_TERM_CODE] = "00"
        paymentDataMap[PaymentFieldName.EURO_CODE] = "1"
        paymentDataMap[PaymentFieldName.PROVIDER_ADDITIONAL_KEY] = ""
        paymentDataMap[PaymentFieldName.APP_KEY] = ""
        paymentDataMap[PaymentFieldName.SUBSTITUTE] = ""
        paymentDataMap[PaymentFieldName.ADDITIONAL_INFO] = ""
        paymentDataMap[PaymentFieldName.BANKS_2_3] = ""
        paymentDataMap[PaymentFieldName.FACT_PROVIDER_ADD_KEY] = ""
        paymentDataMap[PaymentFieldName.INVOICE_ID_ARCHIVE] = ""
        paymentDataMap[PaymentFieldName.XML_ATTACHMENT_ID] = ""
        paymentDataMap[PaymentFieldName.INVOICE_ID_2] = ""
        paymentDataMap[PaymentFieldName.PICTURE_FILE] = ""
        paymentDataMap[PaymentFieldName.INT_REF_NRO] = ""
        paymentDataMap[PaymentFieldName.BANK_BIC_CODE] = bicMapper.mapIban(payment.unit.iban ?: "")
        paymentDataMap[PaymentFieldName.SUBLEDGER] = ""
        paymentDataMap[PaymentFieldName.DOC_ID] = ""
        paymentDataMap[PaymentFieldName.SUBST_DOC_ID] = ""
        paymentDataMap[PaymentFieldName.CONSTRUCTION_KEY] = ""
        paymentDataMap[PaymentFieldName.CONSTRUCTION_NUMBER] = ""
        paymentDataMap[PaymentFieldName.CONTRACT] = ""
        paymentDataMap[PaymentFieldName.CREDIT_TARGET_2] = ""
        paymentDataMap[PaymentFieldName.SUBSTITUTE_FIELD] = ""
        paymentDataMap[PaymentFieldName.BREAKDOWN_TYPE] = "9"
        paymentDataMap[PaymentFieldName.DESCRIPTION] =
            (payment.unit.providerId ?: "") + " " + payment.unit.name
        paymentDataMap[PaymentFieldName.SUB_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.VAT_CODE] = "105"
        paymentDataMap[PaymentFieldName.AMOUNT1] = ""
        paymentDataMap[PaymentFieldName.AMOUNT2] = ""
        paymentDataMap[PaymentFieldName.DELIVERY_PERIOD] = financeDateProvider.previousMonthYYMM()
        paymentDataMap[PaymentFieldName.VAT_BALANCE_SUM] = ""
        paymentDataMap[PaymentFieldName.ACCURUAL_ACCOUNT] = ""
        paymentDataMap[PaymentFieldName.ACCURUAL_PERIODS] = ""
        paymentDataMap[PaymentFieldName.ACCURUAL_START] = ""
        paymentDataMap[PaymentFieldName.FIXED_ASSET_ITEM] = ""
        paymentDataMap[PaymentFieldName.KOM_TARGET_NAME] = ""
        paymentDataMap[PaymentFieldName.KOM_TARGET_RESP_PERSON] = ""
        paymentDataMap[PaymentFieldName.KOM_TARGET_GROUP] = ""
        paymentDataMap[PaymentFieldName.KOM_TARGET_REM_GROUP] = ""
        paymentDataMap[PaymentFieldName.KOM_TARGET_START_DATE] = ""
        paymentDataMap[PaymentFieldName.APPROVER] = ""
        paymentDataMap[PaymentFieldName.APPROVE_DATE] = ""
        paymentDataMap[PaymentFieldName.INSPECTOR] = ""
        paymentDataMap[PaymentFieldName.INSPECTOR_DATE] = ""
        paymentDataMap[PaymentFieldName.ACCOUNT_REFERENCE] = ""
        paymentDataMap[PaymentFieldName.ROW_NUMBER] = ""
        paymentDataMap[PaymentFieldName.EMPTY_FIELD] = ""

        return paymentDataMap
    }

    fun generateRow(
        fields: List<PaymentField>,
        paymentData: Map<PaymentFieldName, String>,
    ): StringBuilder {
        val result = StringBuilder("")

        fields.forEach {
            when (it.fieldType) {
                FieldType.ALPHANUMERIC -> {
                    val value = paymentData[it.field] ?: ""
                    result.append(value.take(it.length).padEnd(it.length))
                }

                FieldType.NUMERIC -> {
                    val value = paymentData[it.field] ?: "0"
                    val paddedValue = value.padStart(it.length, '0')
                    // all Evaka values seem to be Int so we can just pad
                    // the decimal part with the correct number of zeroes
                    result.append(paddedValue.padEnd(it.length + it.decimals, '0'))
                }

                FieldType.MONETARY -> {
                    val value = paymentData[it.field] ?: "0"
                    // if the value is non-zero it has been multiplied by 100 to already contain two
                    // decimals
                    val decimals = if (value == "0") it.decimals else it.decimals - 2
                    val length = if (value == "0") it.length else it.length + 2
                    val paddedValue = value.padStart(length, '0')
                    result.append(paddedValue.padEnd(length + decimals, '0'))
                }
            }
        }

        result.append("\n")

        return result
    }

    fun formatPayment(paymentData: Map<PaymentFieldName, String>): StringBuilder {
        val result = StringBuilder(generateRow(headerRowFields, paymentData))
        result.append(generateRow(paymentRowFields, paymentData))
        return result
    }

    fun generatePayments(payments: List<Payment>): Result {
        val successList = mutableListOf<Payment>()
        val failedList = mutableListOf<Payment>()

        val (failed, succeeded) =
            payments.partition { payment -> paymentChecker.shouldFail(payment) }
        failedList.addAll(failed)

        val paymentString = StringBuilder("")
        succeeded.forEach {
            if (it.amount > 0) {
                val paymentData = gatherPaymentData(it)
                paymentString.append(formatPayment(paymentData))
            }
            successList.add(it)
        }

        return Result(
            PaymentIntegrationClient.SendResult(successList, failedList),
            paymentString.toString(),
        )
    }
}
