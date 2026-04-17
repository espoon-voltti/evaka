// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.kangasala.invoice.service

import evaka.core.daycare.CareType
import evaka.core.invoicing.domain.InvoiceDetailed
import evaka.core.invoicing.domain.InvoiceRowDetailed
import evaka.core.invoicing.domain.PersonDetailed
import evaka.core.invoicing.integration.InvoiceIntegrationClient
import evaka.instance.kangasala.invoice.config.Product
import evaka.instance.kangasala.util.FieldType
import java.lang.Math.abs
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Component

private val restrictedStreetAddress = "Sivistyskeskus, Varhaiskasvatus- ja esiopetuspalvelut, PL 50"
private val restrictedPostCode = "36201"
private val restrictedPostOffice = "Kangasala"

@Component
class ProEInvoiceGenerator(private val invoiceChecker: InvoiceChecker) : StringInvoiceGenerator {

    fun generateInvoiceTitle(period: YearMonth): String {
        val previousMonth = period.format(DateTimeFormatter.ofPattern("MM.yyyy"))
        return "Varhaiskasvatus " + previousMonth
    }

    fun gatherInvoiceData(invoiceDetailed: InvoiceDetailed, period: YearMonth): InvoiceData {
        val invoiceData = InvoiceData()

        val invoiceDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        invoiceData.setAlphanumericValue(InvoiceFieldName.NOT_USED, "")

        // we have previously made sure the head of family has an SSN but the compiler doesn't
        // realize it
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.INVOICE_IDENTIFIER,
            invoiceDetailed.headOfFamily.ssn ?: "",
        )
        invoiceData.setAlphanumericValue(InvoiceFieldName.HEADER_ROW_CODE, "L")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CLIENT_GROUP, "10")
        val clientName =
            invoiceDetailed.headOfFamily.lastName + " " + invoiceDetailed.headOfFamily.firstName
        // CLIENT_NAME1 and CLIENT_NAME2 are 50 characters wide, but Intime only reads the first
        // 30(!)
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.CLIENT_NAME1,
            clientName.substring(0, Math.min(30, clientName.length)),
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.CLIENT_NAME2,
            if (clientName.length > 30) clientName.substring(30, Math.min(60, clientName.length))
            else "",
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.STREET_ADDRESS,
            invoiceDetailed.headOfFamily.streetAddress,
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.POSTAL_ADDRESS,
            invoiceDetailed.headOfFamily.postalCode + " " + invoiceDetailed.headOfFamily.postOffice,
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.PHONE_NUMBER,
            invoiceDetailed.headOfFamily.phone,
        )
        invoiceData.setAlphanumericValue(InvoiceFieldName.FAX_NUMBER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CLIENT_CONTACT, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CLIENT_BANK, "")
        // 0 = external client
        invoiceData.setAlphanumericValue(InvoiceFieldName.CLIENT_TYPE, "0")
        // 1 = Finnish
        invoiceData.setAlphanumericValue(InvoiceFieldName.LANGUAGE_CODE, "1")
        invoiceData.setAlphanumericValue(InvoiceFieldName.REMINDER_CODE, "")
        // N = normal invoicing
        invoiceData.setAlphanumericValue(InvoiceFieldName.PAYMENT_METHOD, "N")
        // 0 = no payments defaulted
        invoiceData.setAlphanumericValue(InvoiceFieldName.PAYMENT_DEFAULT_CODE, "0")
        // K = print a normal invoice
        invoiceData.setAlphanumericValue(InvoiceFieldName.PRINTING_METHOD, "K")
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.INVOICE_DATE,
            invoiceDetailed.invoiceDate.format(invoiceDateFormatter),
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.DUE_DATE,
            invoiceDetailed.dueDate.format(invoiceDateFormatter),
        )
        invoiceData.setAlphanumericValue(
            InvoiceFieldName.ACCOUNTING_DATE,
            invoiceDetailed.sentAt?.toLocalDateTime()?.format(invoiceDateFormatter)
                ?: LocalDate.now().format(invoiceDateFormatter),
        )
        invoiceData.setNumericValue(InvoiceFieldName.INCLUDED_LATE_PAYMENT_INTEREST, 0)
        invoiceData.setAlphanumericValue(InvoiceFieldName.CREDIT_NOTE_INVOICE_NUMBER, "")
        invoiceData.setNumericLongValue(
            InvoiceFieldName.INVOICE_NUMBER,
            invoiceDetailed.number ?: 0,
        )
        invoiceData.setAlphanumericValue(InvoiceFieldName.REFERENCE_NUMBER, "")
        // N = normal
        invoiceData.setAlphanumericValue(InvoiceFieldName.PAYMENT_TYPE, "N")
        invoiceData.setAlphanumericValue(InvoiceFieldName.PARTNER_CODE, "N")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CURRENCY, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.INVOICE_TYPE, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.INVOICING_UNIT, "000")
        // what should we put here?
        invoiceData.setAlphanumericValue(InvoiceFieldName.DESCRIPTION, generateInvoiceTitle(period))
        invoiceData.setAlphanumericValue(InvoiceFieldName.SECURITY_DENIAL, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CONTRACT_NUMBER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.ORDER_NUMBER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.ADDRESS2, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.COUNTRY, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.SSN, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.LATE_PAYMENT_INTEREST, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.VAT_IDENTIFIER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.DELIVERY_DATE, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.OVT_IDENTIFIER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.PAYMENT_TERM, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.RF_REFERENCE, "")

        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_ROW_CODE, "Y")

        val codebtor = invoiceDetailed.codebtor
        if (codebtor != null) {
            invoiceData.setAlphanumericValue(
                InvoiceFieldName.CODEBTOR_IDENTIFIER,
                codebtor.ssn ?: "",
            )
            invoiceData.setAlphanumericValue(
                InvoiceFieldName.CODEBTOR_NAME,
                codebtor.lastName + " " + codebtor.firstName,
            )
            invoiceData.setAlphanumericValue(
                InvoiceFieldName.CODEBTOR_STREET_ADDRESS,
                codebtor.streetAddress,
            )
            invoiceData.setAlphanumericValue(
                InvoiceFieldName.CODEBTOR_POSTAL_ADDRESS,
                codebtor.postalCode + " " + codebtor.postOffice,
            )
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_PHONE_NUMBER, codebtor.phone)
        } else {
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_IDENTIFIER, "")
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_NAME, "")
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_STREET_ADDRESS, "")
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_POSTAL_ADDRESS, "")
            invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_PHONE_NUMBER, "")
        }
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_LANGUAGE_CODE, "1")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_ADDRESS2, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_COUNTRY, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_NAME2, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_VAT_IDENTIFIER, "")
        invoiceData.setAlphanumericValue(InvoiceFieldName.CODEBTOR_OVT_IDENTIFIER, "")

        val sortedRows = invoiceDetailed.rows.sortedBy { row -> row.child.firstName }

        val rowsPerChild: MutableMap<String, List<InvoiceData>> = mutableMapOf()
        var currentChild = ""
        var childRows: MutableList<InvoiceData> = mutableListOf()

        sortedRows.forEach {
            if (it.child.firstName != currentChild) {
                currentChild = it.child.firstName
                childRows = mutableListOf()
                rowsPerChild.put(currentChild, childRows)
            }

            val invoiceRowData = InvoiceData()

            // we have previously made sure the head of family has an SSN but the compiler doesn't
            // realize it
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.INVOICE_IDENTIFIER,
                invoiceDetailed.headOfFamily.ssn ?: "",
            )
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.TEXT_ROW_CODE, "3")
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.CHILD_NAME,
                it.child.lastName + " " + it.child.firstName,
            )
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.TIME_PERIOD,
                it.periodStart.format(dateFormatter) + " - " + it.periodEnd.format(dateFormatter),
            )
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.INVOICE_ROW_HEADER, "")
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.CONSTANT_TEXT_IDENTIFIER, "")

            invoiceRowData.setAlphanumericValue(InvoiceFieldName.DETAIL_ROW_CODE, "1")
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.PRODUCT_NAME,
                Product.valueOf(it.product.value).nameFi,
            )
            // sign of unitPrice is moved to a separate field - empty value is interpreted as a plus
            // sign
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.PRICE_SIGN, "")
            invoiceRowData.setNumericValue(InvoiceFieldName.UNIT_PRICE, abs(it.unitPrice))
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.UNIT, "kpl")
            // empty value is interpreted as a plus sign
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.AMOUNT_SIGN,
                if (it.unitPrice < 0) "-" else "",
            )
            invoiceRowData.setNumericValue(InvoiceFieldName.AMOUNT, it.amount)
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.VAT_CODE, "00")
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.VAT_ACCOUNT, "")
            // format description says "value of this field has not been used", example file has "0"
            // here
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.BRUTTO_NETTO, "0")
            invoiceRowData.setAlphanumericValue(InvoiceFieldName.DEBIT_ACCOUNTING, "")
            invoiceRowData.setAlphanumericValue(
                InvoiceFieldName.CREDIT_ACCOUNTING,
                getCreditAccounting(it),
            )

            childRows.add(invoiceRowData)
        }

        invoiceData.setChildRowMap(rowsPerChild)

        return invoiceData
    }

    private fun getCreditAccounting(it: InvoiceRowDetailed): String {
        val tili = "323011"
        val toiminto =
            with(it.daycareType) {
                when {
                    contains(CareType.FAMILY) || contains(CareType.GROUP_FAMILY) -> ""
                    it.costCenter.startsWith("1914014") -> "1918404"
                    else -> "1913021"
                }
            }
        return "$tili${it.costCenter}$toiminto"
    }

    fun generateRow(fields: List<InvoiceField>, invoiceData: InvoiceData): String {
        var result = ""

        fields.forEach {
            when (it.fieldType) {
                FieldType.ALPHANUMERIC -> {
                    val value = invoiceData.getAlphanumericValue(it.field) ?: ""
                    result += value.take(it.length).padEnd(it.length)
                }

                FieldType.NUMERIC -> {
                    val value = invoiceData.getNumericValue(it.field) ?: 0
                    val stringValue = value.toString().padStart(it.length, '0')
                    // all Evaka values seem to be Int so we can just pad
                    // the decimal part with the correct number of zeroes
                    result += stringValue.padEnd(it.length + it.decimals, '0')
                }

                FieldType.NUMERIC_LONG -> {
                    val value = invoiceData.getNumericLongValue(it.field) ?: 0
                    result += value.toString().padStart(it.length, ' ')
                }

                FieldType.MONETARY -> {
                    val value = invoiceData.getNumericValue(it.field) ?: 0
                    // if the value is non-zero it has been multiplied by 100 to already contain two
                    // decimals
                    val decimals = if (value == 0) it.decimals else it.decimals - 2
                    val length = if (value == 0) it.length else it.length + 2
                    val stringValue = value.toString().padStart(length, '0')
                    result += stringValue.padEnd(length + decimals, '0')
                }
            }
        }

        result = result + "\n"

        return result
    }

    fun formatInvoice(invoiceData: InvoiceData): String {
        var result = generateRow(headerRowFields, invoiceData)

        if (invoiceData.getAlphanumericValue(InvoiceFieldName.CODEBTOR_IDENTIFIER) != "") {
            result += generateRow(codebtorRowFields, invoiceData)
        }

        val rowsPerChild = invoiceData.getChildRowMap()
        rowsPerChild.forEach {
            result += generateRow(childHeaderRowFields, it.value[0])
            it.value.forEach {
                result += generateRow(rowHeaderRowFields, it)
                result += generateRow(detailRowFields, it)
            }
        }

        return result
    }

    override fun generateInvoice(
        invoices: List<InvoiceDetailed>,
        period: YearMonth,
    ): StringInvoiceGenerator.InvoiceGeneratorResult {
        var invoiceString = ""
        val successList = mutableListOf<InvoiceDetailed>()
        val failedList = mutableListOf<InvoiceDetailed>()
        val manuallySentList = mutableListOf<InvoiceDetailed>()

        val (manuallySent, succeeded) =
            invoices.partition { invoice -> invoiceChecker.shouldSendManually(invoice) }
        manuallySentList.addAll(manuallySent)

        succeeded.forEach { invoice ->
            val invoiceWithCorrectedData =
                invoice.copy(
                    headOfFamily = handlePerson(invoice.headOfFamily),
                    codebtor = invoice.codebtor?.let { handlePerson(it) },
                )
            val invoiceData = gatherInvoiceData(invoiceWithCorrectedData, period)
            invoiceString += formatInvoice(invoiceData)
            successList.add(invoiceWithCorrectedData)
        }

        return StringInvoiceGenerator.InvoiceGeneratorResult(
            InvoiceIntegrationClient.SendResult(successList, failedList, manuallySentList),
            invoiceString,
        )
    }
}

internal fun handlePerson(person: PersonDetailed): PersonDetailed {
    val (lastName, firstName) =
        if (person.invoiceRecipientName.isNotBlank()) {
            person.invoiceRecipientName.trim() to ""
        } else {
            person.lastName.trim() to person.firstName.trim()
        }
    val (streetAddress, postalCode, postOffice) =
        when (person.restrictedDetailsEnabled) {
            true -> Triple(restrictedStreetAddress, restrictedPostCode, restrictedPostOffice)

            false ->
                if (hasInvoicingAddress(person)) {
                    Triple(
                        person.invoicingStreetAddress.trim(),
                        person.invoicingPostalCode.trim(),
                        person.invoicingPostOffice.trim(),
                    )
                } else {
                    Triple(
                        person.streetAddress.trim(),
                        person.postalCode.trim(),
                        person.postOffice.trim(),
                    )
                }
        }
    return person.copy(
        lastName = lastName,
        firstName = firstName,
        streetAddress = streetAddress,
        postalCode = postalCode,
        postOffice = postOffice,
    )
}

internal fun hasInvoicingAddress(person: PersonDetailed): Boolean =
    person.invoicingStreetAddress.isNotBlank() &&
        person.invoicingPostalCode.isNotBlank() &&
        person.invoicingPostOffice.isNotBlank()
