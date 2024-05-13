// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing

import fi.espoo.evaka.allAdults
import fi.espoo.evaka.allAreas
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
import fi.espoo.evaka.invoicing.domain.InvoiceStatus
import fi.espoo.evaka.invoicing.domain.InvoiceSummary
import fi.espoo.evaka.toEmployeeWithName
import fi.espoo.evaka.toPersonBasic
import fi.espoo.evaka.toPersonDetailed
import fi.espoo.evaka.toUnitData

fun toDetailed(feeDecision: FeeDecision): FeeDecisionDetailed =
    FeeDecisionDetailed(
        id = feeDecision.id,
        status = feeDecision.status,
        decisionNumber = feeDecision.decisionNumber,
        decisionType = feeDecision.decisionType,
        validDuring = feeDecision.validDuring,
        headOfFamily = allAdults.find { it.id == feeDecision.headOfFamilyId }!!.toPersonDetailed(),
        partner = allAdults.find { it.id == feeDecision.partnerId }?.toPersonDetailed(),
        headOfFamilyIncome = feeDecision.headOfFamilyIncome,
        partnerIncome = feeDecision.partnerIncome,
        familySize = feeDecision.familySize,
        feeThresholds = feeDecision.feeThresholds,
        children =
            feeDecision.children.map { child ->
                FeeDecisionChildDetailed(
                    child = allChildren.find { it.id == child.child.id }!!.toPersonDetailed(),
                    placementType = child.placement.type,
                    placementUnit =
                        allDaycares.find { it.id == child.placement.unitId }!!.toUnitData(),
                    serviceNeedOptionId = child.serviceNeed.optionId,
                    serviceNeedFeeCoefficient = child.serviceNeed.feeCoefficient,
                    serviceNeedDescriptionFi = child.serviceNeed.descriptionFi,
                    serviceNeedDescriptionSv = child.serviceNeed.descriptionSv,
                    serviceNeedMissing = child.serviceNeed.missing,
                    baseFee = child.baseFee,
                    siblingDiscount = child.siblingDiscount,
                    fee = child.fee,
                    feeAlterations = child.feeAlterations,
                    finalFee = child.finalFee,
                    childIncome = child.childIncome
                )
            },
        documentKey = feeDecision.documentKey,
        approvedBy = allWorkers.find { it.id == feeDecision.approvedById }?.toEmployeeWithName(),
        approvedAt = feeDecision.approvedAt,
        financeDecisionHandlerFirstName =
            allWorkers.find { it.id == feeDecision.decisionHandlerId }?.firstName,
        financeDecisionHandlerLastName =
            allWorkers.find { it.id == feeDecision.decisionHandlerId }?.lastName,
        created = feeDecision.created,
        documentContainsContactInfo = false
    )

fun toSummary(feeDecision: FeeDecision): FeeDecisionSummary =
    FeeDecisionSummary(
        id = feeDecision.id,
        status = feeDecision.status,
        decisionNumber = feeDecision.decisionNumber,
        validDuring = feeDecision.validDuring,
        headOfFamily = allAdults.find { it.id == feeDecision.headOfFamilyId }!!.toPersonBasic(),
        children =
            feeDecision.children.map { child ->
                allChildren.find { it.id == child.child.id }!!.toPersonBasic()
            },
        approvedAt = feeDecision.approvedAt,
        finalPrice = feeDecision.children.fold(0) { sum, child -> sum + child.finalFee },
        created = feeDecision.created,
        difference = feeDecision.difference
    )

fun toDetailed(invoice: Invoice): InvoiceDetailed =
    InvoiceDetailed(
        id = invoice.id,
        status = invoice.status,
        periodStart = invoice.periodStart,
        periodEnd = invoice.periodEnd,
        dueDate = invoice.dueDate,
        invoiceDate = invoice.invoiceDate,
        agreementType = allAreas.find { it.id == invoice.areaId }?.areaCode!!,
        areaId = invoice.areaId,
        headOfFamily = allAdults.find { it.id == invoice.headOfFamily }!!.toPersonDetailed(),
        codebtor = allAdults.find { it.id == invoice.codebtor }?.toPersonDetailed(),
        rows =
            invoice.rows.map { row ->
                val costCenter = allDaycares.find { it.id == row.unitId }?.costCenter!!
                val daycareType = allDaycares.find { it.id == row.unitId }?.type!!
                val unitName = allDaycares.find { it.id == row.unitId }?.name!!
                InvoiceRowDetailed(
                    id = row.id!!,
                    child = allChildren.find { it.id == row.child }!!.toPersonDetailed(),
                    amount = row.amount,
                    unitPrice = row.unitPrice,
                    periodStart = row.periodStart,
                    periodEnd = row.periodEnd,
                    product = row.product,
                    unitId = row.unitId,
                    unitName = unitName,
                    daycareType = daycareType,
                    costCenter = costCenter,
                    subCostCenter = allAreas.find { it.id == invoice.areaId }?.subCostCenter!!,
                    savedCostCenter =
                        if (
                            invoice.status == InvoiceStatus.SENT ||
                                invoice.status == InvoiceStatus.WAITING_FOR_SENDING
                        )
                            costCenter
                        else null,
                    description = row.description,
                    correctionId = row.correctionId,
                    note = null
                )
            },
        number = invoice.number,
        sentBy = invoice.sentBy,
        sentAt = invoice.sentAt,
        relatedFeeDecisions = emptyList()
    )

fun toSummary(invoice: Invoice): InvoiceSummary =
    InvoiceSummary(
        id = invoice.id,
        status = invoice.status,
        periodStart = invoice.periodStart,
        periodEnd = invoice.periodEnd,
        headOfFamily = allAdults.find { it.id == invoice.headOfFamily }!!.toPersonDetailed(),
        codebtor = allAdults.find { it.id == invoice.codebtor }?.toPersonDetailed(),
        rows =
            invoice.rows.map { row ->
                InvoiceRowSummary(
                    id = row.id!!,
                    child = allChildren.find { it.id == row.child }!!.toPersonBasic(),
                    amount = row.amount,
                    unitPrice = row.unitPrice
                )
            },
        sentBy = invoice.sentBy,
        sentAt = invoice.sentAt
    )
