// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.invoicing.client.S3DocumentClient
import fi.espoo.evaka.invoicing.data.approveFeeDecisionDraftsForSending
import fi.espoo.evaka.invoicing.data.deleteFeeDecisions
import fi.espoo.evaka.invoicing.data.findFeeDecisionsForHeadOfFamily
import fi.espoo.evaka.invoicing.data.getDetailedFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getFeeDecision
import fi.espoo.evaka.invoicing.data.getFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.setFeeDecisionSent
import fi.espoo.evaka.invoicing.data.setFeeDecisionType
import fi.espoo.evaka.invoicing.data.setFeeDecisionWaitingForManualSending
import fi.espoo.evaka.invoicing.data.updateFeeDecisionDocumentKey
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.FeeDecisionType
import fi.espoo.evaka.invoicing.domain.MailAddress
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeDecisionApproved
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.message.IEvakaMessageClient
import fi.espoo.evaka.shared.message.SuomiFiMessage
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionService(
    private val objectMapper: ObjectMapper,
    private val txManager: PlatformTransactionManager,
    private val dataSource: DataSource,
    private val pdfService: PDFService,
    private val s3Client: S3DocumentClient,
    private val evakaMessageClient: IEvakaMessageClient,
    private val asyncJobRunner: AsyncJobRunner
) {
    @Transactional
    fun confirmDrafts(user: AuthenticatedUser, ids: List<UUID>) {
        val decisions = withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                lockFeeDecisions(ids)(h)
                getFeeDecisionsByIds(h, objectMapper, ids)
            }
        }
        if (decisions.isEmpty()) return

        val notDrafts = decisions.filterNot { it.status == FeeDecisionStatus.DRAFT }
        if (notDrafts.isNotEmpty()) {
            throw BadRequest("Some fee decisions were not drafts")
        }

        val notYetValidDecisions = decisions.filter { it.validFrom > LocalDate.now() }
        if (notYetValidDecisions.isNotEmpty()) {
            throw BadRequest("Some of the fee decisions are not valid yet")
        }

        withSpringTx(txManager) {
            withSpringHandle(dataSource) { h ->
                val conflicts = decisions
                    .flatMap {
                        lockFeeDecisionsForHeadOfFamily(it.headOfFamily.id)(h)
                        findFeeDecisionsForHeadOfFamily(
                            h,
                            objectMapper,
                            it.headOfFamily.id,
                            Period(it.validFrom, it.validTo),
                            listOf(FeeDecisionStatus.SENT)
                        )
                    }
                    .distinctBy { it.id }
                    .filter { !ids.contains(it.id) }

                val updatedConflicts = updateEndDatesOrStatusForConflictingDecisions(decisions, conflicts)
                upsertFeeDecisions(h, objectMapper, updatedConflicts)

                val (emptyDecisions, validDecisions) = decisions
                    .partition { it.parts.isEmpty() }

                deleteFeeDecisions(h, emptyDecisions.map { it.id })

                val validIds = validDecisions.map { it.id }

                approveFeeDecisionDraftsForSending(h, validIds, user.id)

                validIds.forEach {
                    asyncJobRunner.plan(h, listOf(NotifyFeeDecisionApproved(it)))
                }
            }
        }

        asyncJobRunner.scheduleImmediateRun()
    }

    private fun getDecisionLanguage(decision: FeeDecisionDetailed): String {
        val defaultLanguage = if (decision.headOfFamily.language == "sv") "sv" else "fi"

        val youngestChildUnitLanguage =
            decision.parts.maxBy { it.child.dateOfBirth }?.placementUnit?.language

        return if (youngestChildUnitLanguage == "sv") "sv" else defaultLanguage
    }

    fun createFeeDecisionPdf(h: Handle, id: UUID) {
        val decision = getFeeDecision(h, objectMapper, id)
            ?: throw NotFound("No fee decision found with ID ($id)")
        if (!decision.documentKey.isNullOrBlank()) {
            throw Conflict("Fee decision $id has document key already!")
        }

        val lang = getDecisionLanguage(decision)

        val pdfByteArray = pdfService.generateFeeDecisionPdf(FeeDecisionPdfData(decision, lang))
        val key = s3Client.uploadFeeDecisionPdf(decision.id, pdfByteArray, lang)
        updateFeeDecisionDocumentKey(h, decision.id, key)
    }

    fun sendDecision(h: Handle, id: UUID) {
        val decision = getFeeDecision(h, objectMapper, id)
            ?: throw NotFound("No fee decision found with given ID ($id)")

        if (decision.status != FeeDecisionStatus.WAITING_FOR_SENDING) {
            throw Exception("Cannot send fee decision ${decision.id} - has status ${decision.status}")
        }

        if (decision.documentKey == null) {
            throw Exception("Cannot send fee decision ${decision.id} - missing document key")
        }

        if (decision.requiresManualSending()) {
            setFeeDecisionWaitingForManualSending(h, decision.id)
            return
        }

        val recipient = decision.headOfFamily
        val lang = getDecisionLanguage(decision)
        val sendAddress = MailAddress.fromPerson(recipient, lang)

        val feeDecisionDisplayName =
            if (lang == "sv") "Beslut_om_avgift_för_småbarnspedagogik.pdf" else "Varhaiskasvatuksen_maksupäätös.pdf"

        val documentUri = s3Client.getFeeDecisionDocumentUri(decision.documentKey)

        val message = SuomiFiMessage(
            messageId = decision.id.toString(),
            documentId = decision.id.toString(),
            documentDisplayName = feeDecisionDisplayName,
            documentUri = documentUri,
            language = lang,
            firstName = recipient.firstName,
            lastName = recipient.lastName,
            streetAddress = sendAddress.streetAddress,
            postalCode = sendAddress.postalCode,
            postOffice = sendAddress.postOffice,
            ssn = recipient.ssn!!,
            messageHeader = messageHeader.getValue(langWithDefault(lang)),
            messageContent = messageContent.getValue(langWithDefault(lang))
        )

        logger.info("Sending fee decision as suomi.fi message ${message.documentId}")

        evakaMessageClient.send(message)
        setFeeDecisionSent(h, listOf(decision.id))
    }

    private fun langWithDefault(lang: String): String = if (lang.toLowerCase() == "sv") "sv" else "fi"

    val messageHeader = mapOf(
        "fi" to """Espoon varhaiskasvatukseen liittyvät päätökset""",
        "sv" to """Beslut gällande Esbos småbarnspedagogik"""
    )

    val messageContent = mapOf(
        "fi" to """Kunnallisen varhaiskasvatuksen asiakasmaksut vaihtelevat perheen koon ja tulojen sekä varhaiskasvatusajan mukaan. Huoltajat saavat varhaiskasvatuksen maksuista kirjallisen päätöksen. Maksut laskutetaan palvelun antamisesta seuraavan kuukauden puolivälissä.\n\nVarhaiskasvatuksen asiakasmaksu on voimassa toistaiseksi ja perheellä on velvollisuus ilmoittaa, mikäli perheen tulot olennaisesti muuttuvat (+/- 10 %). Koska olette ottanut Suomi.fi -palvelun käyttöönne, on päätös luettavissa alla olevista liitteistä.\n\nIn English:\n\nThe client fees for municipal early childhood education vary according to family size, income and the number of hours the child spends attending early childhood education. The City of Espoo sends the guardians a written decision on early childhood education fees. The fees are invoiced in the middle of the month following the provision of the service.\n\nThe early childhood education fee will remain in force until further notice. Your family has an obligation to notify the City of Espoo if the family’s income changes substantially (+/– 10%). As you are a user of Suomi.fi, you can find the decision in the attachments below.""",
        "sv" to """Klientavgifterna för kommunal småbarnspedagogik varierar enligt familjens storlek och inkomster samt tiden för småbarnspedagogiken. Vårdnadshavarna får ett skriftligt beslut om avgifterna för småbarnspedagogik. Avgifterna faktureras i mitten av den månad som följer på den månad då servicen getts.\n\nKlientavgiften för småbarnspedagogik gäller tills vidare och familjen är skyldig att meddela om familjens inkomster väsentligt förändras (+/- 10 %). Eftersom du har tagit Suomi.fi-tjänsten i bruk, kan du läsa beslutet i bilagorna nedan."""
    )

    fun setSent(h: Handle, ids: List<UUID>) {
        val decisions = getDetailedFeeDecisionsByIds(h, objectMapper, ids)
        if (decisions.any { it.status != FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING }) {
            throw BadRequest("Some decisions were not supposed to be sent manually")
        }
        setFeeDecisionSent(h, ids)
    }

    data class PdfResult(
        val documentKey: String,
        val pdfBytes: ByteArray
    )

    fun getFeeDecisionPdf(h: Handle, decisionId: UUID): PdfResult {
        val documentKey = getFeeDecisionDocumentKey(h, decisionId)
            ?: throw NotFound("Document key not found for decision $decisionId")
        return PdfResult(documentKey, s3Client.getFeeDecisionPdf(documentKey))
    }

    fun setType(h: Handle, decisionId: UUID, type: FeeDecisionType) {
        val decision = getFeeDecision(h, objectMapper, decisionId)
            ?: throw BadRequest("Decision not found with id $decisionId")
        if (decision.status != FeeDecisionStatus.DRAFT) {
            throw BadRequest("Can't change type for decision $decisionId")
        }

        setFeeDecisionType(h, decisionId, type)
    }
}

internal fun updateEndDatesOrStatusForConflictingDecisions(
    newDecisions: List<FeeDecision>,
    conflicting: List<FeeDecision>
): List<FeeDecision> {
    val fixedConflicts = newDecisions
        .sortedBy { it.validFrom }
        .fold(conflicting) { conflicts, newDecision ->
            val updatedConflicts = conflicts
                .filter { conflict ->
                    conflict.headOfFamily.id == newDecision.headOfFamily.id && Period(
                        conflict.validFrom,
                        conflict.validTo
                    ).overlaps(Period(newDecision.validFrom, newDecision.validTo))
                }
                .map { conflict ->
                    if (newDecision.validFrom <= conflict.validFrom) {
                        conflict.copy(status = FeeDecisionStatus.ANNULLED)
                    } else {
                        conflict.copy(validTo = newDecision.validFrom.minusDays(1))
                    }
                }

            conflicts.map { conflict -> updatedConflicts.find { it.id == conflict.id } ?: conflict }
        }

    val (annulledConflicts, nonAnnulledConflicts) = fixedConflicts.partition { it.status == FeeDecisionStatus.ANNULLED }
    val originalConflictsAnnulled = conflicting
        .filter { conflict -> annulledConflicts.any { it.id == conflict.id } }
        .map { it.copy(status = FeeDecisionStatus.ANNULLED) }

    return nonAnnulledConflicts + originalConflictsAnnulled
}

internal fun lockFeeDecisions(ids: List<UUID>): (Handle) -> Unit = { h ->
    h.createUpdate("SELECT id FROM fee_decision WHERE id = ANY(:ids) FOR UPDATE")
        .bind("ids", ids.toTypedArray())
        .execute()
}
