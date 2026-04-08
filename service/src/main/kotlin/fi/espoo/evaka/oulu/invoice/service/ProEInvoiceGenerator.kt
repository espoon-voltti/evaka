// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.invoice.service

import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.integration.InvoiceIntegrationClient
import fi.espoo.evaka.oulu.invoice.config.Product
import fi.espoo.evaka.oulu.util.FieldType
import fi.espoo.evaka.oulu.util.FinanceDateProvider
import fi.espoo.evaka.shared.PersonId
import java.lang.Math.abs
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class InvoiceData(
    val invoiceHeader: Map<InvoiceFieldName, String>,
    val rowsPerChild: Map<PersonId, List<Map<InvoiceFieldName, String>>>,
)

class ProEInvoiceGenerator(val financeDateProvider: FinanceDateProvider) : StringInvoiceGenerator {
    fun generateInvoiceTitle(): String {
        val previousMonth = financeDateProvider.previousMonth()
        return "Varhaiskasvatus $previousMonth"
    }

    fun gatherInvoiceData(invoiceDetailed: InvoiceDetailed): InvoiceData {
        val invoiceDataMap = mutableMapOf<InvoiceFieldName, String>()

        val invoiceDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        invoiceDataMap[InvoiceFieldName.NOT_USED] = ""

        // we have previously made sure the head of family has an SSN but the compiler doesn't
        // realize it
        invoiceDataMap[InvoiceFieldName.INVOICE_IDENTIFIER] = invoiceDetailed.headOfFamily.ssn ?: ""
        invoiceDataMap[InvoiceFieldName.HEADER_ROW_CODE] = "L"
        invoiceDataMap[InvoiceFieldName.CLIENT_GROUP] = "10"
        val clientName =
            invoiceDetailed.headOfFamily.lastName + " " + invoiceDetailed.headOfFamily.firstName
        // CLIENT_NAME1 and CLIENT_NAME2 are 50 characters wide, but Intime only reads the first
        // 30(!)
        invoiceDataMap[InvoiceFieldName.CLIENT_NAME1] =
            clientName.substring(0, Math.min(30, clientName.length))
        invoiceDataMap[InvoiceFieldName.CLIENT_NAME2] =
            if (clientName.length > 30) clientName.substring(30, Math.min(60, clientName.length))
            else ""
        invoiceDataMap[InvoiceFieldName.STREET_ADDRESS] = invoiceDetailed.headOfFamily.streetAddress
        invoiceDataMap[InvoiceFieldName.POSTAL_ADDRESS] =
            invoiceDetailed.headOfFamily.postalCode + " " + invoiceDetailed.headOfFamily.postOffice
        invoiceDataMap[InvoiceFieldName.PHONE_NUMBER] = invoiceDetailed.headOfFamily.phone
        invoiceDataMap[InvoiceFieldName.FAX_NUMBER] = ""
        invoiceDataMap[InvoiceFieldName.CLIENT_CONTACT] = ""
        invoiceDataMap[InvoiceFieldName.CLIENT_BANK] = ""
        // 0 = external client
        invoiceDataMap[InvoiceFieldName.CLIENT_TYPE] = "0"
        // 1 = Finnish
        invoiceDataMap[InvoiceFieldName.LANGUAGE_CODE] = "1"
        invoiceDataMap[InvoiceFieldName.REMINDER_CODE] = ""
        // N = normal invoicing
        invoiceDataMap[InvoiceFieldName.PAYMENT_METHOD] = "N"
        // 0 = no payments defaulted
        invoiceDataMap[InvoiceFieldName.PAYMENT_DEFAULT_CODE] = "0"
        // K = print a normal invoice
        invoiceDataMap[InvoiceFieldName.PRINTING_METHOD] = "K"
        invoiceDataMap[InvoiceFieldName.INVOICE_DATE] =
            invoiceDetailed.invoiceDate.format(invoiceDateFormatter)
        invoiceDataMap[InvoiceFieldName.DUE_DATE] =
            invoiceDetailed.dueDate.format(invoiceDateFormatter)
        invoiceDataMap[InvoiceFieldName.ACCOUNTING_DATE] =
            invoiceDetailed.sentAt?.toLocalDateTime()?.format(invoiceDateFormatter)
                ?: LocalDate.now().format(invoiceDateFormatter)
        invoiceDataMap[InvoiceFieldName.INCLUDED_LATE_PAYMENT_INTEREST] = "0"
        invoiceDataMap[InvoiceFieldName.CREDIT_NOTE_INVOICE_NUMBER] = ""
        invoiceDataMap[InvoiceFieldName.INVOICE_NUMBER] =
            if (invoiceDetailed.number != null) invoiceDetailed.number.toString() else ""
        invoiceDataMap[InvoiceFieldName.REFERENCE_NUMBER] = ""
        // N = normal
        invoiceDataMap[InvoiceFieldName.PAYMENT_TYPE] = "N"
        invoiceDataMap[InvoiceFieldName.PARTNER_CODE] = "N"
        invoiceDataMap[InvoiceFieldName.CURRENCY] = ""
        invoiceDataMap[InvoiceFieldName.INVOICE_TYPE] = ""
        invoiceDataMap[InvoiceFieldName.INVOICING_UNIT] = "000"
        // what should we put here?
        invoiceDataMap[InvoiceFieldName.DESCRIPTION] = generateInvoiceTitle()
        invoiceDataMap[InvoiceFieldName.SECURITY_DENIAL] = ""
        invoiceDataMap[InvoiceFieldName.CONTRACT_NUMBER] = ""
        invoiceDataMap[InvoiceFieldName.ORDER_NUMBER] = ""
        invoiceDataMap[InvoiceFieldName.ADDRESS2] = ""
        invoiceDataMap[InvoiceFieldName.COUNTRY] = ""
        invoiceDataMap[InvoiceFieldName.SSN] = ""
        invoiceDataMap[InvoiceFieldName.LATE_PAYMENT_INTEREST] = ""
        invoiceDataMap[InvoiceFieldName.VAT_IDENTIFIER] = ""
        invoiceDataMap[InvoiceFieldName.DELIVERY_DATE] = ""
        invoiceDataMap[InvoiceFieldName.OVT_IDENTIFIER] = ""
        invoiceDataMap[InvoiceFieldName.PAYMENT_TERM] = ""
        invoiceDataMap[InvoiceFieldName.RF_REFERENCE] = ""

        invoiceDataMap[InvoiceFieldName.CODEBTOR_ROW_CODE] = "Y"

        val codebtor = invoiceDetailed.codebtor
        if (codebtor != null) {
            invoiceDataMap[InvoiceFieldName.CODEBTOR_IDENTIFIER] = codebtor.ssn ?: ""
            invoiceDataMap[InvoiceFieldName.CODEBTOR_NAME] =
                codebtor.lastName + " " + codebtor.firstName
            invoiceDataMap[InvoiceFieldName.CODEBTOR_STREET_ADDRESS] = codebtor.streetAddress
            invoiceDataMap[InvoiceFieldName.CODEBTOR_POSTAL_ADDRESS] =
                codebtor.postalCode + " " + codebtor.postOffice
            invoiceDataMap[InvoiceFieldName.CODEBTOR_PHONE_NUMBER] = codebtor.phone
        } else {
            invoiceDataMap[InvoiceFieldName.CODEBTOR_IDENTIFIER] = ""
            invoiceDataMap[InvoiceFieldName.CODEBTOR_NAME] = ""
            invoiceDataMap[InvoiceFieldName.CODEBTOR_STREET_ADDRESS] = ""
            invoiceDataMap[InvoiceFieldName.CODEBTOR_POSTAL_ADDRESS] = ""
            invoiceDataMap[InvoiceFieldName.CODEBTOR_PHONE_NUMBER] = ""
        }
        invoiceDataMap[InvoiceFieldName.CODEBTOR_LANGUAGE_CODE] = "1"
        invoiceDataMap[InvoiceFieldName.CODEBTOR_ADDRESS2] = ""
        invoiceDataMap[InvoiceFieldName.CODEBTOR_COUNTRY] = ""
        invoiceDataMap[InvoiceFieldName.CODEBTOR_NAME2] = ""
        invoiceDataMap[InvoiceFieldName.CODEBTOR_VAT_IDENTIFIER] = ""
        invoiceDataMap[InvoiceFieldName.CODEBTOR_OVT_IDENTIFIER] = ""

        val sortedRows = invoiceDetailed.rows.sortedBy { row -> row.child.id }

        val rowsPerChild: MutableMap<PersonId, List<Map<InvoiceFieldName, String>>> = mutableMapOf()
        var currentChild = PersonId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        var childRows: MutableList<Map<InvoiceFieldName, String>> = mutableListOf()

        sortedRows.forEach {
            if (it.child.id != currentChild) {
                currentChild = it.child.id
                childRows = mutableListOf()
                rowsPerChild[currentChild] = childRows
            }

            val invoiceRowDataMap = mutableMapOf<InvoiceFieldName, String>()

            // we have previously made sure the head of family has an SSN but the compiler doesn't
            // realize it
            invoiceRowDataMap[InvoiceFieldName.INVOICE_IDENTIFIER] =
                invoiceDetailed.headOfFamily.ssn ?: ""
            invoiceRowDataMap[InvoiceFieldName.TEXT_ROW_CODE] = "3"
            invoiceRowDataMap[InvoiceFieldName.CHILD_NAME] =
                it.child.lastName + " " + it.child.firstName
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            invoiceRowDataMap[InvoiceFieldName.TIME_PERIOD] =
                it.periodStart.format(dateFormatter) + " - " + it.periodEnd.format(dateFormatter)
            invoiceRowDataMap[InvoiceFieldName.INVOICE_ROW_HEADER] = ""
            invoiceRowDataMap[InvoiceFieldName.CONSTANT_TEXT_IDENTIFIER] = ""

            invoiceRowDataMap[InvoiceFieldName.DETAIL_ROW_CODE] = "1"
            invoiceRowDataMap[InvoiceFieldName.PRODUCT_NAME] =
                Product.valueOf(it.product.value).nameFi
            // sign of unitPrice is moved to a separate field - empty value is interpreted as a plus
            // sign
            invoiceRowDataMap[InvoiceFieldName.PRICE_SIGN] = if (it.unitPrice < 0) "-" else ""
            invoiceRowDataMap[InvoiceFieldName.UNIT_PRICE] = abs(it.unitPrice).toString()
            invoiceRowDataMap[InvoiceFieldName.UNIT] = "kpl"
            // empty value is interpreted as a plus sign
            invoiceRowDataMap[InvoiceFieldName.AMOUNT_SIGN] = ""
            invoiceRowDataMap[InvoiceFieldName.AMOUNT] = it.amount.toString()
            invoiceRowDataMap[InvoiceFieldName.VAT_CODE] = "00"
            invoiceRowDataMap[InvoiceFieldName.VAT_ACCOUNT] = ""
            // format description says "value of this field has not been used", example file has "0"
            // here
            invoiceRowDataMap[InvoiceFieldName.BRUTTO_NETTO] = "0"
            invoiceRowDataMap[InvoiceFieldName.DEBIT_ACCOUNTING] = ""
            if (
                it.daycareType.contains(CareType.FAMILY) or
                    it.daycareType.contains(CareType.GROUP_FAMILY)
            ) {
                invoiceRowDataMap[InvoiceFieldName.CREDIT_ACCOUNTING] =
                    "3271 1104171      " + it.costCenter
            } else {
                invoiceRowDataMap[InvoiceFieldName.CREDIT_ACCOUNTING] =
                    "3271 1104170      " + it.costCenter
            }

            childRows.add(invoiceRowDataMap)
        }

        return InvoiceData(invoiceDataMap, rowsPerChild)
    }

    fun generateRow(
        fields: List<InvoiceField>,
        invoiceData: Map<InvoiceFieldName, String>,
    ): StringBuilder {
        val result = StringBuilder("")

        fields.forEach {
            when (it.fieldType) {
                FieldType.ALPHANUMERIC -> {
                    val value = invoiceData[it.field] ?: ""
                    result.append(value.take(it.length).padEnd(it.length))
                }

                FieldType.NUMERIC -> {
                    val value = invoiceData[it.field] ?: "0"
                    val paddedValue = value.padStart(it.length, '0')
                    // all Evaka values seem to be Int so we can just pad
                    // the decimal part with the correct number of zeroes
                    result.append(paddedValue.padEnd(it.length + it.decimals, '0'))
                }

                FieldType.MONETARY -> {
                    val value = invoiceData[it.field] ?: "0"
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

    fun formatInvoice(invoiceData: InvoiceData): StringBuilder {
        val result = generateRow(headerRowFields, invoiceData.invoiceHeader)

        if ((invoiceData.invoiceHeader[InvoiceFieldName.CODEBTOR_IDENTIFIER] ?: "") != "") {
            result.append(generateRow(codebtorRowFields, invoiceData.invoiceHeader))
        }

        val rowsPerChild = invoiceData.rowsPerChild
        rowsPerChild.forEach { childRows ->
            result.append(generateRow(childHeaderRowFields, childRows.value[0]))
            childRows.value.forEach {
                result.append(generateRow(rowHeaderRowFields, it))
                result.append(generateRow(detailRowFields, it))
            }
        }

        return result
    }

    override fun generateInvoice(
        invoices: List<InvoiceDetailed>
    ): StringInvoiceGenerator.InvoiceGeneratorResult {
        val invoiceString = StringBuilder("")
        val successList = mutableListOf<InvoiceDetailed>()
        val failedList = mutableListOf<InvoiceDetailed>()
        val manuallySentList = mutableListOf<InvoiceDetailed>()

        val (manuallySent, succeeded) =
            invoices.partition { invoice ->
                invoice.headOfFamily.restrictedDetailsEnabled || invoice.headOfFamily.ssn == null
            }
        manuallySentList.addAll(manuallySent)

        succeeded.forEach {
            val invoiceData = gatherInvoiceData(it)
            invoiceString.append(formatInvoice(invoiceData))
            successList.add(it)
        }

        return StringInvoiceGenerator.InvoiceGeneratorResult(
            InvoiceIntegrationClient.SendResult(successList, failedList, manuallySentList),
            invoiceString.toString(),
        )
    }
}
