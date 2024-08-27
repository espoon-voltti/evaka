// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed.decision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.decision.getSendAddress
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getEmployeesByRoles
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.getArchiveProcessByAssistanceNeedDecisionId
import fi.espoo.evaka.process.insertProcessHistoryRow
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context

private val logger = KotlinLogging.logger {}

const val assistanceNeedDecisionsBucketPrefix = "assistance-need-decisions/"

@Component
class AssistanceNeedDecisionService(
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val pdfGenerator: PdfGenerator,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    private val messageProvider: IMessageProvider,
    bucketEnv: BucketEnv,
    private val emailEnv: EmailEnv,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    private val bucket = bucketEnv.data

    init {
        asyncJobRunner.registerHandler(::runCreateAssistanceNeedDecisionPdf)
        asyncJobRunner.registerHandler(::runSendAssistanceNeedDecisionEmail)
        asyncJobRunner.registerHandler(::runSendSfiDecision)
    }

    fun runCreateAssistanceNeedDecisionPdf(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.CreateAssistanceNeedDecisionPdf,
    ) {
        val decisionId = msg.decisionId
        db.transaction { tx ->
            val decision = tx.getAssistanceNeedDecisionById(decisionId)

            check(!decision.hasDocument) {
                "A pdf already exists for the assistance need decision $decisionId"
            }

            val pdf = generatePdf(clock.today(), decision)
            val key =
                documentClient
                    .upload(
                        bucket,
                        Document(
                            "${assistanceNeedDecisionsBucketPrefix}assistance_need_decision_$decisionId.pdf",
                            pdf,
                            "application/pdf",
                        ),
                    )
                    .key
            tx.updateAssistanceNeedDocumentKey(decision.id, key)

            val emailJobs =
                decision.child?.id?.let { childId ->
                    val guardians = tx.getChildGuardiansAndFosterParents(childId, clock.today())
                    guardians.map { guardianId ->
                        AsyncJob.SendAssistanceNeedDecisionEmail(msg.decisionId, guardianId)
                    }
                } ?: emptyList()
            val sfiJob = AsyncJob.SendAssistanceNeedDecisionSfiMessage(msg.decisionId)
            asyncJobRunner.plan(tx, emailJobs + sfiJob, runAt = clock.now())

            tx.getArchiveProcessByAssistanceNeedDecisionId(msg.decisionId)?.also {
                tx.insertProcessHistoryRow(
                    processId = it.id,
                    state = ArchivedProcessState.COMPLETED,
                    now = clock.now(),
                    userId = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            }

            logger.info {
                "Successfully created assistance need decision pdf (id: ${msg.decisionId})."
            }
        }
    }

    fun runSendAssistanceNeedDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedDecisionEmail,
    ) {
        val decision = db.read { tx -> tx.getAssistanceNeedDecisionById(msg.decisionId) }

        if (decision.child?.id == null) {
            throw IllegalStateException(
                "Assistance need decision must have a child associated with it"
            )
        }

        logger.info { "Sending assistance need decision email (decisionId: $decision)" }

        val language =
            when (decision.language) {
                OfficialLanguage.SV -> Language.sv
                else -> Language.fi
            }
        val fromAddress = emailEnv.applicationReceivedSender(language)
        val content = emailMessageProvider.assistanceNeedDecisionNotification(language)

        Email.create(
                db,
                msg.guardianId,
                EmailMessageType.DECISION_NOTIFICATION,
                fromAddress,
                content,
                "${msg.decisionId} - ${msg.guardianId}",
            )
            ?.also { emailClient.send(it) }

        logger.info { "Successfully sent assistance need decision email (id: ${msg.decisionId})." }
    }

    fun runSendSfiDecision(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendAssistanceNeedDecisionSfiMessage,
    ) {
        val decisionId = msg.decisionId
        db.transaction { tx ->
            val decision = tx.getAssistanceNeedDecisionById(decisionId)

            if (decision.child?.id == null) {
                throw IllegalStateException(
                    "Assistance need decision has to be associated with a child"
                )
            }

            val documentKey =
                tx.getAssistanceNeedDecisionDocumentKey(decisionId)
                    ?: throw IllegalStateException(
                        "Assistance need decision pdf has not yet been generated"
                    )

            val lang =
                if (decision.language == OfficialLanguage.SV) OfficialLanguage.SV
                else OfficialLanguage.FI

            tx.getChildGuardiansAndFosterParents(decision.child.id, clock.today())
                .mapNotNull { tx.getPersonById(it) }
                .forEach { guardian ->
                    if (guardian.identity !is ExternalIdentifier.SSN) {
                        logger.info {
                            "Cannot deliver assistance need decision ${decision.id} to guardian via Sfi. SSN is missing."
                        }
                        return@forEach
                    }

                    val sendAddress = getSendAddress(messageProvider, guardian, lang)

                    val messageHeader = messageProvider.getAssistanceNeedDecisionHeader(lang)
                    val messageContent = messageProvider.getAssistanceNeedDecisionContent(lang)
                    val messageId = "${decision.id}_${guardian.id}"

                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.SendMessage(
                                SfiMessage(
                                    messageId = messageId,
                                    documentId = messageId,
                                    documentDisplayName =
                                        suomiFiDocumentFileName(decision.language),
                                    documentKey = documentKey,
                                    documentBucket = bucket,
                                    firstName = guardian.firstName,
                                    lastName = guardian.lastName,
                                    streetAddress = sendAddress.street,
                                    postalCode = sendAddress.postalCode,
                                    postOffice = sendAddress.postOffice,
                                    ssn = guardian.identity.ssn,
                                    messageHeader = messageHeader,
                                    messageContent = messageContent,
                                )
                            )
                        ),
                        runAt = clock.now(),
                    )
                }

            logger.info {
                "Successfully scheduled assistance need decision pdf for Suomi.fi sending (id: ${msg.decisionId})."
            }
        }
    }

    fun getDecisionPdfResponse(
        dbc: Database.Connection,
        decisionId: AssistanceNeedDecisionId,
    ): ResponseEntity<Any> {
        val documentKey =
            dbc.read { it.getAssistanceNeedDecisionDocumentKey(decisionId) }
                ?: throw NotFound("No assistance need decision found with ID ($decisionId)")
        return documentClient.responseAttachment(bucket, documentKey, null)
    }

    fun generatePdf(sentDate: LocalDate, decision: AssistanceNeedDecision): ByteArray {
        return pdfGenerator.render(
            Page(
                Template(templateProvider.getAssistanceNeedDecisionPath()),
                Context().apply {
                    locale = decision.language.isoLanguage.toLocale()
                    setVariable("decision", decision)
                    setVariable("sentDate", sentDate)
                    setVariable(
                        "hasAssistanceServices",
                        decision.assistanceLevels.contains(
                            AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME
                        ),
                    )
                },
            )
        )
    }

    fun getDecisionMakerOptions(
        tx: Database.Read,
        id: AssistanceNeedDecisionId,
        roles: Set<UserRole>?,
    ): List<Employee> {
        val assistanceDecision = tx.getAssistanceNeedDecisionById(id)
        return if (roles == null) {
            tx.getEmployees().sortedBy { it.email }
        } else {
            tx.getEmployeesByRoles(roles, assistanceDecision.selectedUnit?.id)
        }
    }
}

private fun suomiFiDocumentFileName(lang: OfficialLanguage) =
    if (lang == OfficialLanguage.SV) {
        "Beslut_om_stöd.pdf"
    } else {
        "Päätös_tuesta.pdf"
    }
