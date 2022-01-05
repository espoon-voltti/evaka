// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.EspooInvoiceIntegrationEnv
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.Product
import mu.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

interface InvoiceIntegrationClient {
    fun sendBatch(invoices: List<InvoiceDetailed>, agreementType: Int): Boolean

    class MockClient(private val objectMapper: ObjectMapper) : InvoiceIntegrationClient {
        val sentBatches = mutableListOf<CommunityInvoiceBatch>()

        override fun sendBatch(invoices: List<InvoiceDetailed>, agreementType: Int): Boolean {
            val batch = createBatchExports(invoices, agreementType, sendCodebtor = true)
            logger.info("Mock invoice integration client got batch ${objectMapper.writeValueAsString(batch)}")
            sentBatches.add(batch)
            return true
        }
    }

    class Client(
        private val env: EspooInvoiceIntegrationEnv,
        private val objectMapper: ObjectMapper
    ) : InvoiceIntegrationClient {
        override fun sendBatch(invoices: List<InvoiceDetailed>, agreementType: Int): Boolean {
            val batch = createBatchExports(invoices, agreementType, sendCodebtor = env.sendCodebtor)
            val payload = objectMapper.writeValueAsString(batch)
            logger.debug("Sending invoice batch ${batch.batchNumber} to integration, payload: $payload")
            val (_, _, result) = Fuel.post("${env.url}/invoice-batches")
                .authentication().basic(env.username, env.password.value)
                .jsonBody(payload)
                .responseString()

            return result.fold(
                { true },
                { error ->
                    logger.error("Failed sending invoice batch", error.exception)
                    false
                }
            )
        }
    }
}

fun createBatchExports(
    batchInvoices: List<InvoiceDetailed>,
    agType: Int,
    sendCodebtor: Boolean
): CommunityInvoiceBatch {
    require(agType <= 999) { "Community agreement type can be at most 3 digits long, was '$agType'" }
    return batchInvoices
        // Do not send negative or very small invoices to Community as complete refunds are not handled through Community
        .filter { it.totalPrice > 0.1 }
        .let { invoices ->
            CommunityInvoiceBatch(
                agreementType = agType,
                batchDate = LocalDate.now(),
                batchNumber = agType,
                invoices = invoices.map { invoice ->
                    val lang =
                        if (invoice.headOfFamily.language == "sv") CommunityLang.SV
                        else CommunityLang.FI

                    val integrtionInvoice = CommunityInvoice(
                        invoiceNumber = invoice.number!!,
                        date = invoice.invoiceDate,
                        dueDate = invoice.dueDate,
                        client = asClient(invoice.headOfFamily, lang),
                        recipient = asRecipient(invoice.headOfFamily),
                        rows = invoice.rows
                            .groupBy { it.child }
                            .toList()
                            .sortedByDescending { (child) -> child.dateOfBirth }
                            .flatMap { (child, rows) ->
                                val nameRow = emptyRow(
                                    "${child.lastName.take(communityLastNameMaxLength)} ${child.firstName}"
                                )
                                val rowsWithContent = rows.map { row ->
                                    invoiceRow(
                                        lang,
                                        agType,
                                        invoice.account,
                                        row.product,
                                        row.periodStart,
                                        row.periodEnd,
                                        row.amount,
                                        row.unitPrice,
                                        row.price,
                                        row.description.trim(),
                                        row.costCenter.trim(),
                                        row.subCostCenter?.trim()
                                    )
                                }
                                listOf(nameRow) + rowsWithContent + listOf(emptyRow())
                            }
                    )

                    if (sendCodebtor) integrtionInvoice.withCodebtor(asCodebtor(invoice.codebtor))
                    else integrtionInvoice
                }
            )
        }
}

private fun addressIsValid(streetAddress: String, postalCode: String, postOffice: String): Boolean {
    // some part of address information is empty
    if (streetAddress.isBlank() || postalCode.isBlank() || postOffice.isBlank()) {
        return false
    }

    // some part of address does not fit Community string length limitations
    if (streetAddress.length > 36 || postalCode.length > 5 || postOffice.length > 40) {
        logger.warn("Invoice recipient address was non-empty, but some part of it was too long for Community, streetAddress: '$streetAddress', postalCode: '$postalCode', postOffice: '$postOffice'")
        return false
    }

    return true
}

const val communityFirstNameMaxLength = 24
const val communityLastNameMaxLength = 50
const val communityDescriptionMaxLength = 52

// make sure these are not too long for Community
const val fallbackStreetAddress = "PL 30"
const val fallbackPostalCode = "02070"
const val fallbackPostOffice = "ESPOON KAUPUNKI"

private fun asClient(headOfFamily: PersonData.Detailed, lang: CommunityLang): CommunityClient {
    val addressIsUseable = addressIsValid(
        headOfFamily.streetAddress.trim(),
        headOfFamily.postalCode.trim(),
        headOfFamily.postOffice.trim()
    )
    return CommunityClient(
        ssn = headOfFamily.ssn!!,
        lastname = headOfFamily.lastName.take(communityLastNameMaxLength),
        firstnames = headOfFamily.firstName.take(communityFirstNameMaxLength),
        language = lang.value,
        street = headOfFamily.streetAddress.takeIf { addressIsUseable },
        postalCode = headOfFamily.postalCode.takeIf { addressIsUseable },
        post = headOfFamily.postOffice.takeIf { addressIsUseable }
    )
}

private fun asRecipient(headOfFamily: PersonData.Detailed): CommunityRecipient {
    val (lastname, firstnames) =
        if (headOfFamily.invoiceRecipientName.isNotBlank()) headOfFamily.invoiceRecipientName to ""
        else headOfFamily.lastName to headOfFamily.firstName

    val addressIsUseable = addressIsValid(
        headOfFamily.streetAddress.trim(),
        headOfFamily.postalCode.trim(),
        headOfFamily.postOffice.trim()
    )
    val shouldUseInvoicingAddress = addressIsValid(
        headOfFamily.invoicingStreetAddress.trim(),
        headOfFamily.invoicingPostalCode.trim(),
        headOfFamily.invoicingPostOffice.trim()
    )

    val (street, postalCode, post) = when {
        shouldUseInvoicingAddress -> Triple(
            headOfFamily.invoicingStreetAddress,
            headOfFamily.invoicingPostalCode,
            headOfFamily.invoicingPostOffice
        )
        addressIsUseable -> Triple(
            headOfFamily.streetAddress,
            headOfFamily.postalCode,
            headOfFamily.postOffice
        )
        else -> Triple(
            fallbackStreetAddress,
            fallbackPostalCode,
            fallbackPostOffice
        )
    }

    return CommunityRecipient(lastname.take(communityLastNameMaxLength), firstnames.take(communityFirstNameMaxLength), street, post, postalCode)
}

private fun asCodebtor(codebtor: PersonData.Detailed?): CommunityCodebtor? {
    if (codebtor?.ssn == null) {
        return null
    }

    return CommunityCodebtor(
        ssn = codebtor.ssn,
        firstnames = codebtor.firstName,
        lastname = codebtor.lastName,
        street = codebtor.streetAddress,
        postalCode = codebtor.postalCode,
        post = codebtor.postOffice
    )
}

const val communityProductCodeLength = 12
const val communitySubCostCenterLength = 2

private fun invoiceRow(
    lang: CommunityLang,
    agType: Int,
    acc: Int,
    product: Product,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    n: Int,
    unitP: Int,
    price: Int,
    desc: String,
    costC: String,
    subCostC: String?
): CommunityInvoiceRow {
    val productCode = "$agType${product.code}"
    require(productCode.length <= communityProductCodeLength) {
        "Community product code can be at most $communityProductCodeLength characters long, was '$productCode'"
    }
    require((subCostC?.length ?: 0) <= communitySubCostCenterLength) {
        "Community sub cost center should be at most $communitySubCostCenterLength characters long, was '$subCostC'"
    }

    return CommunityInvoiceRow(
        productGroup = productCode,
        periodStartDate = periodStart,
        periodEndDate = periodEnd,
        unitCount = 100 * n,
        unitPrice = unitP,
        amount = price,
        description = (
            desc.ifBlank { localizedProduct(lang, product) }
            ).take(communityDescriptionMaxLength),
        account = acc,
        costCenter = costC,
        subCostCenter1 =
        if (subCostC.isNullOrBlank()) null
        else subCostC.padStart(communitySubCostCenterLength, '0')
    )
}

private fun emptyRow(desc: String = ""): CommunityInvoiceRow = CommunityInvoiceRow(
    productGroup = "",
    periodStartDate = null,
    periodEndDate = null,
    unitCount = 0,
    unitPrice = 0,
    amount = 0,
    description = desc.take(communityDescriptionMaxLength),
    account = 0,
    costCenter = "",
    subCostCenter1 = null
)

data class CommunityInvoiceBatch(
    val agreementType: Int,
    val batchDate: LocalDate,
    val batchNumber: Int,
    val invoices: List<IntegrationInvoice>
) {
    val currency = "EUR"
    val systemId = "EPH"
    val sourcePrinted = false
}

interface IntegrationInvoice {
    val invoiceNumber: Long
    val date: LocalDate
    val dueDate: LocalDate
    val client: CommunityClient
    val recipient: CommunityRecipient
    val rows: List<CommunityInvoiceRow>
    val useInvoiceNumber: Boolean
    val printDate: LocalDate?
}

data class CommunityInvoice(
    override val invoiceNumber: Long,
    override val date: LocalDate,
    override val dueDate: LocalDate,
    override val client: CommunityClient,
    override val recipient: CommunityRecipient,
    override val rows: List<CommunityInvoiceRow>
) : IntegrationInvoice {
    override val useInvoiceNumber = false
    override val printDate = null

    fun withCodebtor(codebtor: CommunityCodebtor?) = CommunityInvoiceWithCodebtor(
        invoiceNumber = this.invoiceNumber,
        date = this.date,
        dueDate = this.dueDate,
        client = this.client,
        recipient = this.recipient,
        rows = this.rows,
        codebtor = codebtor
    )
}

data class CommunityInvoiceWithCodebtor(
    override val invoiceNumber: Long,
    override val date: LocalDate,
    override val dueDate: LocalDate,
    override val client: CommunityClient,
    override val recipient: CommunityRecipient,
    override val rows: List<CommunityInvoiceRow>,
    val codebtor: CommunityCodebtor?,
) : IntegrationInvoice {
    override val useInvoiceNumber = false
    override val printDate = null
}

data class CommunityClient(
    val ssn: String,
    val lastname: String,
    val firstnames: String,
    val language: String,
    val street: String?,
    val postalCode: String?,
    val post: String?
) {
    val ytunnus = null
    val registerNumber = null
    val contactPerson = ""
    val homePhone = null
    val mobilePhone = null
    val faxNumber = null
    val email = null
}

data class CommunityRecipient(
    val lastname: String,
    val firstnames: String,
    val street: String?,
    val post: String?,
    val postalCode: String?
)

data class CommunityCodebtor(
    val ssn: String,
    val lastname: String,
    val firstnames: String,
    val street: String,
    val post: String,
    val postalCode: String
)

data class CommunityInvoiceRow(
    val productGroup: String,
    val periodStartDate: LocalDate?,
    val periodEndDate: LocalDate?,
    val unitCount: Int,
    val unitPrice: Int,
    val amount: Int,
    val description: String,
    val account: Int,
    val costCenter: String,
    val subCostCenter1: String?
) {
    val productComponent = this.productGroup
    val vatAmount = 0
    val subCostCenter2 = null
    val project = ""
    val product = ""
}

enum class CommunityLang(val value: String) {
    FI("fi"),
    SV("sv")
}

private fun localizedProduct(lang: CommunityLang, product: Product) = when (lang) {
    CommunityLang.FI -> when (product) {
        Product.DAYCARE -> "Varhaiskasvatus"
        Product.DAYCARE_DISCOUNT -> "Alennus"
        Product.DAYCARE_INCREASE -> "Lisä"
        Product.PRESCHOOL_WITH_DAYCARE -> "Varhaiskasvatus + esiopetus"
        Product.PRESCHOOL_WITH_DAYCARE_DISCOUNT -> "Alennus"
        Product.PRESCHOOL_WITH_DAYCARE_INCREASE -> "Lisä"
        Product.TEMPORARY_CARE -> "Tilapäinen varhaiskasvatus"
        Product.SICK_LEAVE_100 -> "Laskuun vaikuttava poissaolo 100%"
        Product.SICK_LEAVE_50 -> "Laskuun vaikuttava poissaolo 50%"
        Product.ABSENCE -> "Poissaolovähennys"
        Product.SCHOOL_SHIFT_CARE -> "Koululaisten vuorohoito"
        Product.FREE_OF_CHARGE -> "Poissaolovähennys"
    }
    CommunityLang.SV -> when (product) {
        Product.DAYCARE -> "Småbarnspedagogik"
        Product.DAYCARE_DISCOUNT -> "Avdrag"
        Product.DAYCARE_INCREASE -> "Lisä"
        Product.PRESCHOOL_WITH_DAYCARE -> "Småbarnspedagogik + FKL"
        Product.PRESCHOOL_WITH_DAYCARE_DISCOUNT -> "Avdrag"
        Product.PRESCHOOL_WITH_DAYCARE_INCREASE -> "Lisä"
        Product.TEMPORARY_CARE -> "Temporär småbarnspedagogik"
        Product.SICK_LEAVE_100 -> "Frånvaro som påverkar faktureringen 100%"
        Product.SICK_LEAVE_50 -> "Frånvaro som påverkar faktureringen 50%"
        Product.ABSENCE -> "Avdrag annan frånvaro"
        Product.SCHOOL_SHIFT_CARE -> "Koululaisten vuorohoito (sv)"
        Product.FREE_OF_CHARGE -> "Avdrag annan frånvaro"
    }
}
