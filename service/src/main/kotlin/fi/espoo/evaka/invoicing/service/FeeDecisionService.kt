// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.approveFeeDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getDetailedFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.getFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.lockFeeDecisions
import fi.espoo.evaka.invoicing.data.lockFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.setFeeDecisionSent
import fi.espoo.evaka.invoicing.data.setFeeDecisionType
import fi.espoo.evaka.invoicing.data.setFeeDecisionWaitingForManualSending
import fi.espoo.evaka.invoicing.data.updateFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.updateFeeDecisionStatusAndDates
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.MailAddress
import fi.espoo.evaka.invoicing.domain.isRetroactive
import fi.espoo.evaka.invoicing.domain.updateEndDatesOrAnnulConflictingDecisions
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.shared.message.langWithDefault
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionService(
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val messageProvider: IMessageProvider,
    private val evakaMessageClient: IEvakaMessageClient
) {
    fun confirmDrafts(tx: Database.Transaction, user: AuthenticatedUser, ids: List<UUID>, now: Instant): List<UUID> {
        tx.lockFeeDecisions(ids)
        val decisions = tx.getFeeDecisionsByIds(ids)
        if (decisions.isEmpty()) return listOf()

        val notDrafts = decisions.filterNot { it.status == FeeDecisionStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some fee decisions were not drafts")
        }

        val nowDate = LocalDate.from(now.atZone(helsinkiZone))
        val notYetValidDecisions = decisions.filter { it.validFrom > nowDate }
        if (notYetValidDecisions.isNotEmpty()) {
            throw BadRequest("Some of the fee decisions are not valid yet")
        }

        val conflicts = decisions
            .flatMap {
                tx.lockFeeDecisionsForHeadOfFamily(it.headOfFamily.id)
                tx.findFeeDecisionsForHeadOfFamily(
                    it.headOfFamily.id,
                    DateRange(it.validFrom, it.validTo),
                    listOf(FeeDecisionStatus.SENT)
                )
            }
            .distinctBy { it.id }
            .filter { !ids.contains(it.id) }

        val updatedConflicts = updateEndDatesOrAnnulConflictingDecisions(decisions, conflicts)
        tx.updateFeeDecisionStatusAndDates(updatedConflicts)

        val (emptyDecisions, validDecisions) = decisions
            .partition { it.children.isEmpty() }

        tx.deleteFeeDecisions(emptyDecisions.map { it.id })

        val (retroactiveDecisions, otherValidDecisions) = validDecisions.partition {
            isRetroactive(it.validFrom, nowDate)
        }

        tx.approveFeeDecisionDraftsForSending(retroactiveDecisions.map { it.id }, approvedBy = user.id, isRetroactive = true, approvedAt = now)
        tx.approveFeeDecisionDraftsForSending(otherValidDecisions.map { it.id }, approvedBy = user.id, approvedAt = now)

        return validDecisions.map { it.id }
    }

    private fun getDecisionLanguage(decision: FeeDecisionDetailed): String {
        val defaultLanguage = if (decision.headOfFamily.language == "sv") "sv" else "fi"

        val youngestChildUnitLanguage =
            decision.children.maxByOrNull { it.child.dateOfBirth }?.placementUnit?.language

        return if (youngestChildUnitLanguage == "sv") "sv" else defaultLanguage
    }

    fun createFeeDecisionPdf(tx: Database.Transaction, id: UUID) {
        val decision = tx.getFeeDecision(id)
            ?: throw NotFound("No fee decision found with ID ($id)")
        if (!decision.documentKey.isNullOrBlank()) {
            throw Conflict("Fee decision $id has document key already!")
        }

        val lang = getDecisionLanguage(decision)

        val pdfByteArray = pdfService.generateFeeDecisionPdf(FeeDecisionPdfData(decision, lang))
        val key = s3Client.uploadFeeDecisionPdf(decision.id, pdfByteArray, lang)
        tx.updateFeeDecisionDocumentKey(decision.id, key)
    }

    fun sendDecision(tx: Database.Transaction, id: UUID) {
        val decision = tx.getFeeDecision(id)
            ?: throw NotFound("No fee decision found with given ID ($id)")

        if (decision.status != FeeDecisionStatus.WAITING_FOR_SENDING) {
            throw Exception("Cannot send fee decision ${decision.id} - has status ${decision.status}")
        }

        if (decision.documentKey == null) {
            throw Exception("Cannot send fee decision ${decision.id} - missing document key")
        }

        if (decision.requiresManualSending()) {
            tx.setFeeDecisionWaitingForManualSending(decision.id)
            return
        }

        val recipient = decision.headOfFamily
        val lang = getDecisionLanguage(decision)
        val sendAddress = MailAddress.fromPerson(recipient, lang)

        val feeDecisionDisplayName =
            if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf" else "Varhaiskasvatuksen_maksupäätös.pdf"

        val message = SuomiFiMessage(
            messageId = decision.id.toString(),
            documentId = decision.id.toString(),
            documentDisplayName = feeDecisionDisplayName,
            documentBucket = s3Client.getFeeDecisionBucket(),
            documentKey = decision.documentKey,
            language = lang,
            firstName = recipient.firstName,
            lastName = recipient.lastName,
            streetAddress = sendAddress.streetAddress,
            postalCode = sendAddress.postalCode,
            postOffice = sendAddress.postOffice,
            ssn = recipient.ssn!!,
            messageHeader = messageProvider.getFeeDecisionHeader(langWithDefault(lang)),
            messageContent = messageProvider.getFeeDecisionContent(langWithDefault(lang))
        )

        logger.info("Sending fee decision as suomi.fi message ${message.documentId}")

        evakaMessageClient.send(message)
        tx.setFeeDecisionSent(listOf(decision.id))
    }

    fun setSent(tx: Database.Transaction, ids: List<UUID>) {
        val decisions = tx.getDetailedFeeDecisionsByIds(ids)
        if (decisions.any { it.status != FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING }) {
            throw BadRequest("Some decisions were not supposed to be sent manually")
        }
        tx.setFeeDecisionSent(ids)
    }

    data class PdfResult(
        val documentKey: String,
        val pdfBytes: ByteArray
    )

    fun getFeeDecisionPdf(tx: Database.Read, decisionId: UUID): PdfResult {
        val documentKey = tx.getFeeDecisionDocumentKey(decisionId)
            ?: throw NotFound("Document key not found for decision $decisionId")
        return PdfResult(documentKey, s3Client.getFeeDecisionPdf(documentKey))
    }

    fun setType(tx: Database.Transaction, decisionId: UUID, type: FeeDecisionType) {
        val decision = tx.getFeeDecision(decisionId)
            ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != FeeDecisionStatus.DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        tx.setFeeDecisionType(decisionId, type)
    }
}
