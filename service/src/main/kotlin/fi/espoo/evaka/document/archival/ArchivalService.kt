// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.archival

import fi.espoo.evaka.caseprocess.getCaseProcessByApplicationId
import fi.espoo.evaka.caseprocess.getCaseProcessByChildDocumentId
import fi.espoo.evaka.caseprocess.getChildDocumentMetadata
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.decision.markDecisionAsArchived
import fi.espoo.evaka.document.childdocument.getChildDocument
import fi.espoo.evaka.document.childdocument.getChildDocumentKey
import fi.espoo.evaka.document.childdocument.markDocumentAsArchived
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.s3.DocumentKey
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.user.getEvakaUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ArchivalService(
    asyncJobRunner: AsyncJobRunner<AsyncJob>?,
    private val archivalIntegrationClient: ArchivalIntegrationClient,
    private val documentClient: DocumentService,
) {

    init {
        asyncJobRunner?.registerHandler<AsyncJob.ArchiveDecision> { db, clock, msg ->
            uploadDecisionToArchive(db, clock, msg)
        }
        asyncJobRunner?.registerHandler<AsyncJob.ArchiveChildDocument> { db, _, msg ->
            uploadChildDocumentToArchive(db, msg)
        }
    }

    fun uploadDecisionToArchive(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.ArchiveDecision,
    ) {
        logger.info { "Starting archival process for decision ${msg.decisionId}" }

        val user = getUser(db, msg.user)

        val decision =
            db.read { tx ->
                tx.getDecision(msg.decisionId)
                    ?: throw NotFound("Decision ${msg.decisionId} not found")
            }
        validateArchivability(decision)
        val child = getPerson(db, decision.childId)
        val caseProcess =
            db.read { tx ->
                tx.getCaseProcessByApplicationId(decision.applicationId)
                    ?: throw NotFound(
                        "Case process not found for application ${decision.applicationId}"
                    )
            }
        val documentKey =
            decision.documentKey
                ?: throw NotFound("Document key not found for decision ${decision.id}")
        val document = getDocument(DocumentKey.Decision(documentKey))

        val instanceId =
            archivalIntegrationClient.uploadDecisionToArchive(
                caseProcess,
                child,
                decision,
                document,
                user,
            )

        db.transaction { tx -> tx.markDecisionAsArchived(decision.id, clock.now()) }
        logger.info { "Successfully archived decision ${decision.id} to instance $instanceId" }
    }

    fun uploadChildDocumentToArchive(db: Database.Connection, msg: AsyncJob.ArchiveChildDocument) {
        val (documentId, user) = msg
        logger.info { "Starting archival process for document $documentId" }

        val evakaUser = getUser(db, user)

        val document =
            db.read { tx ->
                tx.getChildDocument(msg.documentId)
                    ?: throw NotFound("document $documentId not found")
            }
        val childInfo = getPerson(db, document.child.id)
        val caseProcess = db.read { tx -> tx.getCaseProcessByChildDocumentId(documentId) }
        val documentMetadata = db.read { tx -> tx.getChildDocumentMetadata(documentId) }

        val documentKey =
            db.read { tx ->
                tx.getChildDocumentKey(documentId)
                    ?: throw NotFound("Document key not found for document $documentId")
            }
        val documentContent = getDocument(DocumentKey.ChildDocument(documentKey))

        val instanceId =
            archivalIntegrationClient.uploadChildDocumentToArchive(
                caseProcess = caseProcess,
                documentId = documentId,
                childInfo = childInfo,
                childDocumentDetails = document,
                documentMetadata = documentMetadata,
                documentContent = documentContent,
                evakaUser = evakaUser,
            )

        db.transaction { tx -> tx.markDocumentAsArchived(documentId, HelsinkiDateTime.now()) }
        logger.info { "Successfully archived document $documentId to instance $instanceId" }
    }

    private fun getUser(db: Database.Connection, user: AuthenticatedUser?) =
        (user ?: AuthenticatedUser.SystemInternalUser).let {
            db.read { tx ->
                tx.getEvakaUser(it.evakaUserId)
                    ?: throw NotFound("EvakaUser not found with ${it.evakaUserId}")
            }
        }

    private fun getPerson(db: Database.Connection, personId: PersonId) =
        db.read { tx ->
            tx.getPersonById(personId)
                ?: throw IllegalStateException("No person found with $personId")
        }

    private fun getDocument(key: DocumentKey) = documentClient.get(documentClient.locate(key))
}

fun validateArchivability(decision: Decision) {
    if (decision.status != DecisionStatus.ACCEPTED && decision.status != DecisionStatus.REJECTED) {
        throw BadRequest("Decision must be in ACCEPTED or REJECTED status to be archived")
    }
}
