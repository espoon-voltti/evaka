// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.allAdults
import fi.espoo.evaka.allBasicChildren
import fi.espoo.evaka.allChildren
import fi.espoo.evaka.allDaycares
import fi.espoo.evaka.allWorkers
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPartDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionPartSummary
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.domain.PersonData
import kotlin.math.max

fun toDetailed(feeDecision: FeeDecision): FeeDecisionDetailed = FeeDecisionDetailed(
    id = feeDecision.id,
    status = feeDecision.status,
    decisionNumber = feeDecision.decisionNumber,
    decisionType = feeDecision.decisionType,
    validFrom = feeDecision.validFrom,
    validTo = feeDecision.validTo,
    headOfFamily = allAdults.find { it.id == feeDecision.headOfFamily.id }!!,
    partner = allAdults.find { it.id == feeDecision.partner?.id },
    headOfFamilyIncome = feeDecision.headOfFamilyIncome,
    partnerIncome = feeDecision.partnerIncome,
    familySize = feeDecision.familySize,
    pricing = feeDecision.pricing,
    parts = feeDecision.parts.map { part ->
        FeeDecisionPartDetailed(
            child = allChildren.find { it.id == part.child.id }!!,
            placement = part.placement,
            placementUnit = allDaycares.find { it.id == part.placement.unit }!!,
            baseFee = part.baseFee,
            siblingDiscount = part.siblingDiscount,
            fee = part.fee,
            feeAlterations = part.feeAlterations
        )
    },
    documentKey = feeDecision.documentKey,
    approvedBy = allWorkers.find { it.id == feeDecision.approvedBy?.id },
    approvedAt = feeDecision.approvedAt,
    createdAt = feeDecision.createdAt
)

fun toSummary(feeDecision: FeeDecision): FeeDecisionSummary = FeeDecisionSummary(
    id = feeDecision.id,
    status = feeDecision.status,
    decisionNumber = feeDecision.decisionNumber,
    validFrom = feeDecision.validFrom,
    validTo = feeDecision.validTo,
    headOfFamily = allAdults.find { it.id == feeDecision.headOfFamily.id }!!.let {
        PersonData.Basic(
            id = it.id,
            dateOfBirth = it.dateOfBirth,
            firstName = it.firstName,
            lastName = it.lastName,
            ssn = it.ssn
        )
    },
    parts = feeDecision.parts.map { part ->
        FeeDecisionPartSummary(
            child = allChildren.find { it.id == part.child.id }!!.let {
                PersonData.Basic(
                    id = it.id,
                    dateOfBirth = it.dateOfBirth,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    ssn = it.ssn
                )
            }
        )
    },
    approvedAt = feeDecision.approvedAt,
    createdAt = feeDecision.createdAt,
    finalPrice = max(0, feeDecision.parts.fold(0) { sum, part -> sum + part.finalFee() })
)

fun toDetailed(invoice: Invoice): InvoiceDetailed = InvoiceDetailed(
    id = invoice.id,
    status = invoice.status,
    periodStart = invoice.periodStart,
    periodEnd = invoice.periodEnd,
    dueDate = invoice.dueDate,
    invoiceDate = invoice.invoiceDate,
    agreementType = invoice.agreementType,
    headOfFamily = allAdults.find { it.id == invoice.headOfFamily.id }!!,
    rows = invoice.rows.map { row ->
        InvoiceRowDetailed(
            id = row.id!!,
            child = allChildren.find { it.id == row.child.id }!!,
            amount = row.amount,
            unitPrice = row.unitPrice,
            periodStart = row.periodStart,
            periodEnd = row.periodEnd,
            product = row.product,
            costCenter = row.costCenter,
            subCostCenter = row.subCostCenter,
            description = row.description
        )
    },
    number = invoice.number,
    sentBy = invoice.sentBy,
    sentAt = invoice.sentAt
)

fun toSummary(invoice: Invoice): InvoiceSummary = InvoiceSummary(
    id = invoice.id,
    status = invoice.status,
    periodStart = invoice.periodStart,
    periodEnd = invoice.periodEnd,
    headOfFamily = allAdults.find { it.id == invoice.headOfFamily.id }!!,
    rows = invoice.rows.map { row ->
        InvoiceRowSummary(
            id = row.id!!,
            child = allBasicChildren.find { it.id == row.child.id }!!,
            amount = row.amount,
            unitPrice = row.unitPrice
        )
    },
    sentBy = invoice.sentBy,
    sentAt = invoice.sentAt
)
