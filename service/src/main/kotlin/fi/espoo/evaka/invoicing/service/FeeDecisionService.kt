// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.approveFeeDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getDetailedFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.getFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.isElementaryFamily
import fi.espoo.evaka.invoicing.data.lockFeeDecisions
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.setFeeDecisionSent
import fi.espoo.evaka.invoicing.data.setFeeDecisionType
import fi.espoo.evaka.invoicing.data.setFeeDecisionWaitingForManualSending
import fi.espoo.evaka.invoicing.data.updateFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateFeeDecisionStatusAndDates
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus.DRAFT
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus.SENT
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus.WAITING_FOR_SENDING
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.isRetroactive
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.setting.getSettings
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.MessageLanguage
import fi.espoo.evaka.shared.message.langWithDefault
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionService(
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageProvider: IMessageProvider,
    private val sfiAsyncJobRunner: AsyncJobRunner<SuomiFiAsyncJob>,
    private val env: EvakaEnv
) {
    fun confirmDrafts(
        tx: Database.Transaction,
        user: AuthenticatedUser.Employee,
        ids: List<FeeDecisionId>,
        confirmDateTime: HelsinkiDateTime,
        alwaysUseDaycareFinanceDecisionHandler: Boolean
    ): List<FeeDecisionId> {
        tx.lockFeeDecisions(ids)
        val decisions = tx.getFeeDecisionsByIds(ids)
        if (decisions.isEmpty()) return listOf()
        val notDrafts = decisions.filterNot { it.status == DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some fee decisions were not drafts")
        }
        val today = confirmDateTime.toLocalDate()
        val approvedAt = confirmDateTime.toInstant()
        val lastPossibleDecisionValidFromDate = today.plusDays(env.nrOfDaysFeeDecisionCanBeSentInAdvance)
        val decisionsNotValidForConfirmation = decisions.filter {
            it.validFrom > lastPossibleDecisionValidFromDate
        }
        if (decisionsNotValidForConfirmation.isNotEmpty()) {
            throw BadRequest(
                "Some of the fee decisions are not valid yet",
                "feeDecisions.confirmation.tooFarInFuture"
            )
        }

        val (conflicts, waitingForSending) = decisions
            .flatMap {
                tx.lockFeeDecisionsForHeadOfFamily(it.headOfFamilyId)
                tx.findFeeDecisionsForHeadOfFamily(
                    it.headOfFamilyId,
                    DateRange(it.validFrom, it.validTo),
                    listOf(WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING, SENT)
                )
            }
            .distinctBy { it.id }
            .filter { !ids.contains(it.id) }
            .partition { it.status != WAITING_FOR_SENDING && it.status != WAITING_FOR_MANUAL_SENDING }

        if (waitingForSending.isNotEmpty()) {
            logger.info("Warning: when creating fee decisions, skipped ${waitingForSending.size} fee decisions because head of family had overlapping fee decisions waiting for sending")
        }

        val remainingDecisions = decisions.filter { fd ->
            waitingForSending.none { wfd ->
                wfd.headOfFamilyId == fd.headOfFamilyId
            }
        }

        val remainingConflicts = conflicts.filter { conflict ->
            waitingForSending.none { wfd ->
                wfd.headOfFamilyId == conflict.headOfFamilyId
            }
        }

        val updatedConflicts = updateEndDatesOrAnnulConflictingDecisions(remainingDecisions, remainingConflicts)
        tx.updateFeeDecisionStatusAndDates(updatedConflicts)

        val (emptyDecisions, validDecisions) = remainingDecisions
            .partition { it.children.isEmpty() }

        tx.deleteFeeDecisions(emptyDecisions.map { it.id })

        val (retroactiveDecisions, otherValidDecisions) = validDecisions.partition {
            isRetroactive(it.validFrom, today)
        }

        tx.approveFeeDecisionDraftsForSending(
            retroactiveDecisions.map { it.id },
            approvedBy = user.id,
            isRetroactive = true,
            approvedAt = approvedAt,
            alwaysUseDaycareFinanceDecisionHandler = alwaysUseDaycareFinanceDecisionHandler
        )
        tx.approveFeeDecisionDraftsForSending(otherValidDecisions.map { it.id }, approvedBy = user.id, approvedAt = approvedAt, alwaysUseDaycareFinanceDecisionHandler = alwaysUseDaycareFinanceDecisionHandler)

        return validDecisions.map { it.id }
    }

    private fun getDecisionLanguage(decision: FeeDecisionDetailed): String {
        val defaultLanguage = if (decision.headOfFamily.language == "sv") "sv" else "fi"

        val youngestChildUnitLanguage =
            decision.children.maxByOrNull { it.child.dateOfBirth }?.placementUnit?.language

        return if (youngestChildUnitLanguage == "sv") "sv" else defaultLanguage
    }

    fun createFeeDecisionPdf(tx: Database.Transaction, id: FeeDecisionId) {
        val decision = tx.getFeeDecision(id)?.let {
            val isElementaryFamily = tx.isElementaryFamily(it.headOfFamily.id, it.partner?.id, it.children.map { it.child.id })
            it.copy(isElementaryFamily = isElementaryFamily)
        } ?: throw NotFound("No fee decision found with ID ($id)")

        if (!decision.documentKey.isNullOrBlank()) {
            throw Conflict("Fee decision $id has document key already!")
        }

        val settings = tx.getSettings()
        val lang = getDecisionLanguage(decision)

        val pdfByteArray = pdfService.generateFeeDecisionPdf(FeeDecisionPdfData(decision, settings, lang))
        val key = s3Client.uploadFeeDecisionPdf(decision.id, pdfByteArray, lang)
        tx.updateFeeDecisionDocumentKey(decision.id, key)
    }

    fun sendDecision(tx: Database.Transaction, id: FeeDecisionId): Boolean {
        val decision = tx.getFeeDecision(id)
            ?: throw NotFound("No fee decision found with given ID ($id)")

        if (decision.status != WAITING_FOR_SENDING) {
            error("Cannot send fee decision ${decision.id} - has status ${decision.status}")
        }

        if (decision.documentKey == null) {
            error("Cannot send fee decision ${decision.id} - missing document key")
        }

        if (decision.requiresManualSending) {
            tx.setFeeDecisionWaitingForManualSending(decision.id)
            return false
        }

        val recipient = decision.headOfFamily
        val lang = getDecisionLanguage(decision)
        val sendAddress = DecisionSendAddress.fromPerson(recipient) ?: when (lang) {
            "sv" -> messageProvider.getDefaultFeeDecisionAddress(MessageLanguage.SV)
            else -> messageProvider.getDefaultFeeDecisionAddress(MessageLanguage.FI)
        }

        val feeDecisionDisplayName =
            if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf" else "Varhaiskasvatuksen_maksupäätös.pdf"

        val message = SfiMessage(
            messageId = decision.id.toString(),
            documentId = decision.id.toString(),
            documentDisplayName = feeDecisionDisplayName,
            documentBucket = s3Client.getFeeDecisionBucket(),
            documentKey = decision.documentKey,
            language = lang,
            firstName = recipient.firstName,
            lastName = recipient.lastName,
            streetAddress = sendAddress.street,
            postalCode = sendAddress.postalCode,
            postOffice = sendAddress.postOffice,
            ssn = recipient.ssn!!,
            messageHeader = messageProvider.getFeeDecisionHeader(langWithDefault(lang)),
            messageContent = messageProvider.getFeeDecisionContent(langWithDefault(lang))
        )

        logger.info("Sending fee decision as suomi.fi message ${message.documentId}")

        sfiAsyncJobRunner.plan(tx, listOf(SuomiFiAsyncJob.SendMessage(message)))
        tx.setFeeDecisionSent(listOf(decision.id))

        return true
    }

    fun setSent(tx: Database.Transaction, ids: List<FeeDecisionId>) {
        val decisions = tx.getDetailedFeeDecisionsByIds(ids)
        if (decisions.any { it.status != WAITING_FOR_MANUAL_SENDING }) {
            throw BadRequest("Some decisions were not supposed to be sent manually")
        }
        tx.setFeeDecisionSent(ids)
    }

    data class PdfResult(
        val documentKey: String,
        val pdfBytes: ByteArray
    )

    fun getFeeDecisionPdf(tx: Database.Read, decisionId: FeeDecisionId): PdfResult {
        val documentKey = tx.getFeeDecisionDocumentKey(decisionId)
            ?: throw NotFound("Document key not found for decision $decisionId")
        return PdfResult(documentKey, s3Client.getFeeDecisionPdf(documentKey))
    }

    fun setType(tx: Database.Transaction, decisionId: FeeDecisionId, type: FeeDecisionType) {
        val decision = tx.getFeeDecision(decisionId)
            ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setFeeDecisionType(decisionId, type)
    }
}
