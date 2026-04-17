// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.invoice.service

import evaka.instance.orivesi.util.DataMapper
import evaka.instance.orivesi.util.FieldType

enum class InvoiceFieldName {
    NOT_USED,
    GAP_IN_SPEC,
    INVOICE_IDENTIFIER,
    HEADER_ROW_CODE,
    CLIENT_NAME1,
    CLIENT_NAME2,
    STREET_ADDRESS,
    POSTAL_ADDRESS,
    CLIENT_CONTACT,
    LANGUAGE_CODE,
    REMINDER_CODE,
    PRINTING_METHOD,
    INVOICE_DATE,
    DUE_DATE,
    ACCOUNTING_DATE,
    PRINTING_DATE,
    CREDIT_NOTE_INVOICE_NUMBER,
    INVOICE_NUMBER,
    REFERENCE_NUMBER,
    PARTNER_CODE,
    CURRENCY,
    INVOICE_TYPE,
    DESCRIPTION,
    SECURITY_DENIAL,
    CONTRACT_NUMBER,
    COUNTRY,
    SSN,
    RF_REFERENCE,
    CODEBTOR_ROW_CODE,
    CODEBTOR_IDENTIFIER,
    CODEBTOR_NAME,
    CODEBTOR_STREET_ADDRESS,
    CODEBTOR_POSTAL_ADDRESS,
    CODEBTOR_LANGUAGE_CODE,
    CODEBTOR_PARTNER_CODE,
    CODEBTOR_COUNTRY,
    TEXT_ROW_CODE,
    CHILD_NAME,
    TIME_PERIOD,
    DETAIL_ROW_CODE,
    PRODUCT_NAME,
    PRICE_SIGN,
    UNIT_PRICE,
    UNIT,
    AMOUNT_SIGN,
    AMOUNT,
    VAT_CODE,
    VAT_ACCOUNT,
    BRUTTO_NETTO,
    DEBIT_ACCOUNTING,
    CREDIT_ACCOUNTING,
    ROW_SUM_SIGN,
    ROW_SUM,
}

class InvoiceField(
    val field: InvoiceFieldName,
    val fieldType: FieldType,
    val start: Int,
    val length: Int,
    val decimals: Int = 0,
)

val headerRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.HEADER_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 13, 2),
        InvoiceField(InvoiceFieldName.CLIENT_NAME1, FieldType.ALPHANUMERIC, 15, 50),
        InvoiceField(InvoiceFieldName.CLIENT_NAME2, FieldType.ALPHANUMERIC, 65, 50),
        InvoiceField(InvoiceFieldName.STREET_ADDRESS, FieldType.ALPHANUMERIC, 115, 30),
        InvoiceField(InvoiceFieldName.POSTAL_ADDRESS, FieldType.ALPHANUMERIC, 145, 30),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 175, 15),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 190, 1),
        InvoiceField(InvoiceFieldName.GAP_IN_SPEC, FieldType.ALPHANUMERIC, 191, 14),
        InvoiceField(InvoiceFieldName.CLIENT_CONTACT, FieldType.ALPHANUMERIC, 205, 30),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 235, 30),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 265, 15),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 280, 1),
        InvoiceField(InvoiceFieldName.LANGUAGE_CODE, FieldType.ALPHANUMERIC, 281, 1),
        InvoiceField(InvoiceFieldName.REMINDER_CODE, FieldType.ALPHANUMERIC, 282, 1),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 283, 1),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 284, 1),
        InvoiceField(InvoiceFieldName.PRINTING_METHOD, FieldType.ALPHANUMERIC, 285, 1),
        InvoiceField(InvoiceFieldName.INVOICE_DATE, FieldType.ALPHANUMERIC, 286, 8),
        InvoiceField(InvoiceFieldName.DUE_DATE, FieldType.ALPHANUMERIC, 294, 8),
        InvoiceField(InvoiceFieldName.ACCOUNTING_DATE, FieldType.ALPHANUMERIC, 302, 8),
        InvoiceField(InvoiceFieldName.PRINTING_DATE, FieldType.ALPHANUMERIC, 310, 8),
        InvoiceField(InvoiceFieldName.CREDIT_NOTE_INVOICE_NUMBER, FieldType.ALPHANUMERIC, 318, 10),
        InvoiceField(InvoiceFieldName.GAP_IN_SPEC, FieldType.ALPHANUMERIC, 328, 2),
        InvoiceField(InvoiceFieldName.INVOICE_NUMBER, FieldType.ALPHANUMERIC, 330, 8),
        InvoiceField(InvoiceFieldName.REFERENCE_NUMBER, FieldType.ALPHANUMERIC, 338, 20),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 358, 1),
        InvoiceField(InvoiceFieldName.PARTNER_CODE, FieldType.ALPHANUMERIC, 359, 10),
        InvoiceField(InvoiceFieldName.CURRENCY, FieldType.ALPHANUMERIC, 369, 3),
        InvoiceField(InvoiceFieldName.INVOICE_TYPE, FieldType.ALPHANUMERIC, 372, 2),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 374, 3),
        InvoiceField(InvoiceFieldName.DESCRIPTION, FieldType.ALPHANUMERIC, 377, 30),
        InvoiceField(InvoiceFieldName.SECURITY_DENIAL, FieldType.ALPHANUMERIC, 407, 1),
        InvoiceField(InvoiceFieldName.CONTRACT_NUMBER, FieldType.ALPHANUMERIC, 408, 80),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 488, 30),
        InvoiceField(InvoiceFieldName.COUNTRY, FieldType.ALPHANUMERIC, 518, 2),
        InvoiceField(InvoiceFieldName.GAP_IN_SPEC, FieldType.ALPHANUMERIC, 520, 28),
        InvoiceField(InvoiceFieldName.SSN, FieldType.ALPHANUMERIC, 548, 11),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 559, 6),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 565, 35),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 600, 8),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 608, 35),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 643, 10),
        InvoiceField(InvoiceFieldName.RF_REFERENCE, FieldType.ALPHANUMERIC, 653, 25),
    )

var codebtorRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.CODEBTOR_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.CODEBTOR_IDENTIFIER, FieldType.ALPHANUMERIC, 13, 11),
        InvoiceField(InvoiceFieldName.CODEBTOR_NAME, FieldType.ALPHANUMERIC, 24, 50),
        InvoiceField(InvoiceFieldName.CODEBTOR_STREET_ADDRESS, FieldType.ALPHANUMERIC, 74, 30),
        InvoiceField(InvoiceFieldName.CODEBTOR_POSTAL_ADDRESS, FieldType.ALPHANUMERIC, 104, 30),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 134, 15),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 149, 90),
        InvoiceField(InvoiceFieldName.CODEBTOR_LANGUAGE_CODE, FieldType.ALPHANUMERIC, 239, 1),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 240, 1),
        InvoiceField(InvoiceFieldName.CODEBTOR_PARTNER_CODE, FieldType.ALPHANUMERIC, 241, 10),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 251, 1),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 252, 30),
        InvoiceField(InvoiceFieldName.CODEBTOR_COUNTRY, FieldType.ALPHANUMERIC, 282, 2),
        InvoiceField(InvoiceFieldName.GAP_IN_SPEC, FieldType.ALPHANUMERIC, 284, 28),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 312, 50),
    )

var childHeaderRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.TEXT_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.CHILD_NAME, FieldType.ALPHANUMERIC, 13, 80),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 93, 28),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 121, 10),
    )

var rowHeaderRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.TEXT_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.TIME_PERIOD, FieldType.ALPHANUMERIC, 13, 80),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 93, 28),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 121, 10),
    )

var detailRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.DETAIL_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.PRODUCT_NAME, FieldType.ALPHANUMERIC, 13, 40),
        InvoiceField(InvoiceFieldName.PRICE_SIGN, FieldType.ALPHANUMERIC, 53, 1),
        InvoiceField(InvoiceFieldName.UNIT_PRICE, FieldType.MONETARY, 54, 8, 4),
        InvoiceField(InvoiceFieldName.UNIT, FieldType.ALPHANUMERIC, 66, 3),
        InvoiceField(InvoiceFieldName.AMOUNT_SIGN, FieldType.ALPHANUMERIC, 69, 1),
        InvoiceField(InvoiceFieldName.AMOUNT, FieldType.NUMERIC, 70, 8, 4),
        InvoiceField(InvoiceFieldName.VAT_CODE, FieldType.ALPHANUMERIC, 82, 2),
        InvoiceField(InvoiceFieldName.VAT_ACCOUNT, FieldType.ALPHANUMERIC, 84, 60),
        InvoiceField(InvoiceFieldName.BRUTTO_NETTO, FieldType.ALPHANUMERIC, 144, 1),
        InvoiceField(InvoiceFieldName.DEBIT_ACCOUNTING, FieldType.ALPHANUMERIC, 145, 60),
        InvoiceField(
            InvoiceFieldName.CREDIT_ACCOUNTING,
            FieldType.ALPHANUMERIC,
            205,
            59,
        ), // spec says length is 60, but next field starts after 59 chars
        InvoiceField(InvoiceFieldName.ROW_SUM_SIGN, FieldType.ALPHANUMERIC, 264, 1),
        InvoiceField(InvoiceFieldName.ROW_SUM, FieldType.MONETARY, 265, 9, 2),
    )

var descriptionRowFields =
    listOf(
        InvoiceField(InvoiceFieldName.INVOICE_IDENTIFIER, FieldType.ALPHANUMERIC, 1, 11),
        InvoiceField(InvoiceFieldName.TEXT_ROW_CODE, FieldType.ALPHANUMERIC, 12, 1),
        InvoiceField(InvoiceFieldName.DESCRIPTION, FieldType.ALPHANUMERIC, 13, 80),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 93, 28),
        InvoiceField(InvoiceFieldName.NOT_USED, FieldType.ALPHANUMERIC, 121, 10),
    )

typealias InvoiceData = DataMapper<InvoiceFieldName>
