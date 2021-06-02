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
import fi.espoo.evaka.invoicing.domain.FeeDecisionChildDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.InvoiceDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
import fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.invoicing.domain.PersonData

fun toDetailed(feeDecision: FeeDecision): FeeDecisionDetailed = FeeDecisionDetailed(
    id = feeDecision.id,
    status = feeDecision.status,
    decisionNumber = feeDecision.decisionNumber,
    decisionType = feeDecision.decisionType,
    validDuring = feeDecision.validDuring,
    headOfFamily = allAdults.find { it.id == feeDecision.headOfFamily.id }!!,
    partner = allAdults.find { it.id == feeDecision.partner?.id },
    headOfFamilyIncome = feeDecision.headOfFamilyIncome,
    partnerIncome = feeDecision.partnerIncome,
    familySize = feeDecision.familySize,
    pricing = feeDecision.pricing,
    children = feeDecision.children.map { child ->
        FeeDecisionChildDetailed(
            child = allChildren.find { it.id == child.child.id }!!,
            placementType = child.placement.type,
            placementUnit = allDaycares.find { it.id == child.placement.unit.id }!!,
            serviceNeedFeeCoefficient = child.serviceNeed.feeCoefficient,
            serviceNeedDescriptionFi = child.serviceNeed.descriptionFi,
            serviceNeedDescriptionSv = child.serviceNeed.descriptionSv,
            serviceNeedMissing = child.serviceNeed.missing,
            baseFee = child.baseFee,
            siblingDiscount = child.siblingDiscount,
            fee = child.fee,
            feeAlterations = child.feeAlterations,
            finalFee = child.finalFee
        )
    },
    documentKey = feeDecision.documentKey,
    approvedBy = allWorkers.find { it.id == feeDecision.approvedBy?.id },
    approvedAt = feeDecision.approvedAt,
    financeDecisionHandlerFirstName = allWorkers.find { it.id == feeDecision.decisionHandler?.id }
        ?.let { "${it.firstName}" },
    financeDecisionHandlerLastName = allWorkers.find { it.id == feeDecision.decisionHandler?.id }
        ?.let { "${it.lastName}" },
    created = feeDecision.created
)

fun toSummary(feeDecision: FeeDecision): FeeDecisionSummary = FeeDecisionSummary(
    id = feeDecision.id,
    status = feeDecision.status,
    decisionNumber = feeDecision.decisionNumber,
    validDuring = feeDecision.validDuring,
    headOfFamily = allAdults.find { it.id == feeDecision.headOfFamily.id }!!.let {
        PersonData.Basic(
            id = it.id,
            dateOfBirth = it.dateOfBirth,
            firstName = it.firstName,
            lastName = it.lastName,
            ssn = it.ssn
        )
    },
    children = feeDecision.children.map { child ->
        allChildren.find { it.id == child.child.id }!!.let {
            PersonData.Basic(
                id = it.id,
                dateOfBirth = it.dateOfBirth,
                firstName = it.firstName,
                lastName = it.lastName,
                ssn = it.ssn
            )
        }
    },
    approvedAt = feeDecision.approvedAt,
    finalPrice = feeDecision.children.fold(0) { sum, child -> sum + child.finalFee },
    created = feeDecision.created
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
