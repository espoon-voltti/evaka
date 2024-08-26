// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.integration

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.EspooInvoiceIntegrationEnv
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.invoicing.service.EspooInvoiceProducts
import fi.espoo.evaka.invoicing.service.ProductKey
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class EspooInvoiceIntegrationClient(
    private val env: EspooInvoiceIntegrationEnv,
    private val jsonMapper: JsonMapper,
) : InvoiceIntegrationClient {
    private val fuel = FuelManager()

    override fun send(invoices: List<InvoiceDetailed>): InvoiceIntegrationClient.SendResult {
        val (withSSN, withoutSSN) =
            invoices.partition { invoice -> invoice.headOfFamily.ssn != null }

        return withSSN
            .groupBy { it.agreementType }
            .map { (agreementType, invoices) ->
                val success =
                    if (agreementType != null) {
                        sendBatch(invoices, agreementType)
                    } else {
                        val areaIds = invoices.asSequence().map { it.areaId }.distinct().sorted()
                        logger.error(
                            "Failed to send ${invoices.size} invoices due to missing areaCode in the following areas: ${areaIds.joinToString()}"
                        )
                        false
                    }
                success to invoices
            }
            .fold(InvoiceIntegrationClient.SendResult(manuallySent = withoutSSN)) {
                result,
                (success, invoices) ->
                if (success) {
                    result.copy(succeeded = result.succeeded + invoices)
                } else {
                    result.copy(failed = result.failed + invoices)
                }
            }
    }

    private fun sendBatch(invoices: List<InvoiceDetailed>, agreementType: Int): Boolean {
        val batch = createBatchExports(invoices, agreementType, sendCodebtor = env.sendCodebtor)
        val payload = jsonMapper.writeValueAsString(batch)
        logger.info(
            mapOf("batchNumber" to batch.batchNumber, "batchLength" to batch.invoices.size)
        ) {
            "Sending invoice batch to integration"
        }
        logger.debug("Sending invoice batch ${batch.batchNumber} to integration, payload: $payload")
        val (_, _, result) =
            fuel
                .post("${env.url}/invoice-batches")
                .authentication()
                .basic(env.username, env.password.value)
                .jsonBody(payload)
                .responseString()

        return result.fold(
            { true },
            { error ->
                val meta = mapOf("errorMessage" to error.errorData.decodeToString())
                logger.error(error, meta) { "Failed sending invoice batch" }
                false
            },
        )
    }

    companion object {
        fun createBatchExports(
            batchInvoices: List<InvoiceDetailed>,
            agType: Int,
            sendCodebtor: Boolean,
        ): EspooInvoiceBatch {
            require(agType <= 999) { "Agreement type can be at most 3 digits long, was '$agType'" }
            return batchInvoices
                // Do not send negative or very small invoices as refunds are handled manually
                // outside evaka
                .filter { it.totalPrice > 0.1 }
                .let { invoices ->
                    EspooInvoiceBatch(
                        agreementType = agType,
                        batchDate = LocalDate.now(europeHelsinki),
                        batchNumber = agType,
                        invoices =
                            invoices.map { invoice ->
                                val lang =
                                    if (invoice.headOfFamily.language == "sv") {
                                        EspooLang.SV
                                    } else {
                                        EspooLang.FI
                                    }

                                val integrtionInvoice =
                                    EspooInvoice(
                                        invoiceNumber = invoice.number!!,
                                        date = invoice.invoiceDate,
                                        dueDate = invoice.dueDate,
                                        client = asClient(invoice.headOfFamily, lang),
                                        recipient = asRecipient(invoice.headOfFamily),
                                        rows =
                                            invoice.rows
                                                .groupBy { it.child }
                                                .toList()
                                                .sortedByDescending { (child) -> child.dateOfBirth }
                                                .flatMap { (child, rows) ->
                                                    val nameRow =
                                                        emptyRow(
                                                            "${child.lastName.take(invoicingLastNameMaxLength)} ${child.firstName}"
                                                        )
                                                    val rowsWithContent =
                                                        rows.map { row ->
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
                                                                row.subCostCenter?.trim(),
                                                            )
                                                        }
                                                    listOf(nameRow) +
                                                        rowsWithContent +
                                                        listOf(emptyRow())
                                                },
                                    )

                                if (sendCodebtor) {
                                    integrtionInvoice.withCodebtor(asCodebtor(invoice.codebtor))
                                } else {
                                    integrtionInvoice
                                }
                            },
                    )
                }
        }

        private fun addressIsValid(
            streetAddress: String,
            postalCode: String,
            postOffice: String,
        ): Boolean {
            // some part of address information is empty
            if (streetAddress.isBlank() || postalCode.isBlank() || postOffice.isBlank()) {
                return false
            }

            // some part of address does not fit string length limitations
            if (streetAddress.length > 36 || postalCode.length > 5 || postOffice.length > 40) {
                logger.warn(
                    "Invoice recipient address was non-empty, but some part of it was too long for invoice integration, streetAddress: '$streetAddress', postalCode: '$postalCode', postOffice: '$postOffice'"
                )
                return false
            }

            return true
        }

        private const val invoicingFirstNameMaxLength = 24
        private const val invoicingLastNameMaxLength = 50
        private const val invoicingDescriptionMaxLength = 52

        const val fallbackStreetAddress = "PL 30"
        const val fallbackPostalCode = "02070"
        const val fallbackPostOffice = "ESPOON KAUPUNKI"

        private fun asClient(headOfFamily: PersonDetailed, lang: EspooLang): EspooClient {
            val (streetAddress, postalCode, postOffice) =
                Triple(
                    headOfFamily.streetAddress.trim(),
                    headOfFamily.postalCode.trim(),
                    headOfFamily.postOffice.trim(),
                )
            val addressIsUseable = addressIsValid(streetAddress, postalCode, postOffice)
            return EspooClient(
                ssn = headOfFamily.ssn!!,
                lastname = headOfFamily.lastName.take(invoicingLastNameMaxLength),
                firstnames = headOfFamily.firstName.take(invoicingFirstNameMaxLength),
                language = lang.value,
                street = streetAddress.takeIf { addressIsUseable },
                postalCode = postalCode.takeIf { addressIsUseable },
                post = postOffice.takeIf { addressIsUseable },
            )
        }

        private fun asRecipient(headOfFamily: PersonDetailed): EspooRecipient {
            val (lastname, firstnames) =
                if (headOfFamily.invoiceRecipientName.isNotBlank()) {
                    headOfFamily.invoiceRecipientName to ""
                } else {
                    headOfFamily.lastName to headOfFamily.firstName
                }

            val address =
                Triple(
                    headOfFamily.streetAddress.trim(),
                    headOfFamily.postalCode.trim(),
                    headOfFamily.postOffice.trim(),
                )
            val invoiceAddress =
                Triple(
                    headOfFamily.invoicingStreetAddress.trim(),
                    headOfFamily.invoicingPostalCode.trim(),
                    headOfFamily.invoicingPostOffice.trim(),
                )

            val (street, postalCode, post) =
                when {
                    addressIsValid(
                        invoiceAddress.first,
                        invoiceAddress.second,
                        invoiceAddress.third,
                    ) -> invoiceAddress
                    addressIsValid(address.first, address.second, address.third) -> address
                    else -> Triple(fallbackStreetAddress, fallbackPostalCode, fallbackPostOffice)
                }

            return EspooRecipient(
                lastname.take(invoicingLastNameMaxLength),
                firstnames.take(invoicingFirstNameMaxLength),
                street,
                post,
                postalCode,
            )
        }

        private fun asCodebtor(codebtor: PersonDetailed?): EspooCodebtor? {
            if (codebtor?.ssn == null) {
                return null
            }

            return EspooCodebtor(
                ssn = codebtor.ssn,
                firstnames = codebtor.firstName,
                lastname = codebtor.lastName,
                street = codebtor.streetAddress,
                postalCode = codebtor.postalCode,
                post = codebtor.postOffice,
            )
        }

        private const val invoicingProductCodeLength = 12
        private const val invoicingSubCostCenterLength = 2

        private fun invoiceRow(
            lang: EspooLang,
            agType: Int,
            acc: Int,
            product: ProductKey,
            periodStart: LocalDate,
            periodEnd: LocalDate,
            n: Int,
            unitP: Int,
            price: Int,
            desc: String,
            costC: String,
            subCostC: String?,
        ): EspooInvoiceRow {
            val espooProduct = EspooInvoiceProducts.findProduct(product)

            val productCode = "$agType${espooProduct.code}"
            require(productCode.length <= invoicingProductCodeLength) {
                "Invoice product code can be at most $invoicingProductCodeLength characters long, was '$productCode'"
            }
            require((subCostC?.length ?: 0) <= invoicingSubCostCenterLength) {
                "Invoice sub cost center should be at most $invoicingSubCostCenterLength characters long, was '$subCostC'"
            }

            return EspooInvoiceRow(
                productGroup = productCode,
                periodStartDate = periodStart,
                periodEndDate = periodEnd,
                unitCount = 100 * n,
                unitPrice = unitP,
                amount = price,
                description =
                    desc
                        .ifBlank {
                            when (lang) {
                                EspooLang.FI -> espooProduct.nameOnInvoiceFi
                                EspooLang.SV -> espooProduct.nameOnInvoiceSv
                            }
                        }
                        .take(invoicingDescriptionMaxLength),
                account = acc,
                costCenter = costC,
                subCostCenter1 =
                    if (subCostC.isNullOrBlank()) {
                        null
                    } else {
                        subCostC.padStart(invoicingSubCostCenterLength, '0')
                    },
            )
        }

        private fun emptyRow(desc: String = ""): EspooInvoiceRow =
            EspooInvoiceRow(
                productGroup = "",
                periodStartDate = null,
                periodEndDate = null,
                unitCount = 0,
                unitPrice = 0,
                amount = 0,
                description = desc.take(invoicingDescriptionMaxLength),
                account = 0,
                costCenter = "",
                subCostCenter1 = null,
            )

        data class EspooInvoiceBatch(
            val agreementType: Int,
            val batchDate: LocalDate,
            val batchNumber: Int,
            val invoices: List<IntegrationInvoice>,
        ) {
            val currency = "EUR"
            val systemId = "EPH"
            val sourcePrinted = false
        }

        interface IntegrationInvoice {
            val invoiceNumber: Long
            val date: LocalDate
            val dueDate: LocalDate
            val client: EspooClient
            val recipient: EspooRecipient
            val rows: List<EspooInvoiceRow>
            val useInvoiceNumber: Boolean
            val printDate: LocalDate?
        }

        data class EspooInvoice(
            override val invoiceNumber: Long,
            override val date: LocalDate,
            override val dueDate: LocalDate,
            override val client: EspooClient,
            override val recipient: EspooRecipient,
            override val rows: List<EspooInvoiceRow>,
        ) : IntegrationInvoice {
            override val useInvoiceNumber = false
            override val printDate = null

            fun withCodebtor(codebtor: EspooCodebtor?) =
                EspooInvoiceWithCodebtor(
                    invoiceNumber = this.invoiceNumber,
                    date = this.date,
                    dueDate = this.dueDate,
                    client = this.client,
                    recipient = this.recipient,
                    rows = this.rows,
                    codebtor = codebtor,
                )
        }

        data class EspooInvoiceWithCodebtor(
            override val invoiceNumber: Long,
            override val date: LocalDate,
            override val dueDate: LocalDate,
            override val client: EspooClient,
            override val recipient: EspooRecipient,
            override val rows: List<EspooInvoiceRow>,
            val codebtor: EspooCodebtor?,
        ) : IntegrationInvoice {
            override val useInvoiceNumber = false
            override val printDate = null
        }

        data class EspooClient(
            val ssn: String,
            val lastname: String,
            val firstnames: String,
            val language: String,
            val street: String?,
            val postalCode: String?,
            val post: String?,
        ) {
            val ytunnus = null
            val registerNumber = null
            val contactPerson = ""
            val homePhone = null
            val mobilePhone = null
            val faxNumber = null
            val email = null
        }

        data class EspooRecipient(
            val lastname: String,
            val firstnames: String,
            val street: String?,
            val post: String?,
            val postalCode: String?,
        )

        data class EspooCodebtor(
            val ssn: String,
            val lastname: String,
            val firstnames: String,
            val street: String,
            val post: String,
            val postalCode: String,
        )

        data class EspooInvoiceRow(
            val productGroup: String,
            val periodStartDate: LocalDate?,
            val periodEndDate: LocalDate?,
            val unitCount: Int,
            val unitPrice: Int,
            val amount: Int,
            val description: String,
            val account: Int,
            val costCenter: String,
            val subCostCenter1: String?,
        ) {
            val productComponent = this.productGroup
            val vatAmount = 0
            val subCostCenter2 = null
            val project = ""
            val product = ""
        }

        enum class EspooLang(val value: String) {
            FI("fi"),
            SV("sv"),
        }
    }
}
