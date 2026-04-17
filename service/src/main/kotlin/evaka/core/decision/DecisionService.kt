// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.EmailEnv
import evaka.core.application.ApplicationDetails
import evaka.core.application.ServiceNeed
import evaka.core.application.fetchApplicationDetails
import evaka.core.application.getApplicationOtherGuardians
import evaka.core.daycare.UnitManager
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.daycare.getDaycare
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.identity.ExternalIdentifier
import evaka.core.pdfgen.Page
import evaka.core.pdfgen.PdfGenerator
import evaka.core.pdfgen.Template
import evaka.core.pis.EmailMessageType
import evaka.core.pis.getPersonById
import evaka.core.pis.service.PersonDTO
import evaka.core.pis.service.PersonService
import evaka.core.pis.service.getChildGuardiansAndFosterParents
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentLocation
import evaka.core.s3.DocumentService
import evaka.core.setting.SettingType
import evaka.core.setting.getSettings
import evaka.core.sficlient.SentSfiMessage
import evaka.core.sficlient.SfiMessage
import evaka.core.sficlient.storeSentSfiMessage
import evaka.core.shared.ApplicationId
import evaka.core.shared.DecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.OfficialLanguage
import evaka.core.shared.message.IMessageProvider
import evaka.core.shared.template.ITemplateProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context

val logger = KotlinLogging.logger {}

@Service
class DecisionService(
    private val personService: PersonService,
    private val documentClient: DocumentService,
    private val templateProvider: ITemplateProvider,
    private val pdfGenerator: PdfGenerator,
    private val messageProvider: IMessageProvider,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailClient: EmailClient,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    fun finalizeDecisions(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        applicationId: ApplicationId,
        sendAsMessage: Boolean,
        skipGuardianApproval: Boolean,
    ): List<DecisionId> {
        val now = clock.now()
        val decisionIds = tx.finalizeDecisions(applicationId, now)
        asyncJobRunner.plan(
            tx,
            decisionIds.map { decisionId ->
                val decision = tx.getDecision(decisionId)!!
                AsyncJob.NotifyDecisionCreated(
                    decisionId,
                    user,
                    sendAsMessage,
                    skipGuardianApproval && decision.type == DecisionType.PRESCHOOL,
                )
            },
            runAt = now,
        )
        return decisionIds
    }

    fun createDecisionPdf(tx: Database.Transaction, decisionId: DecisionId) {
        val settings = tx.getSettings()
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")
        val decisionLanguage = determineDecisionLanguage(decision, tx)
        val application =
            tx.fetchApplicationDetails(decision.applicationId)
                ?: throw NotFound("Application ${decision.applicationId} was not found")
        val child =
            tx.getPersonById(application.childId)
                ?: error("Child not found with id: ${application.childId}")
        val unit = tx.getDaycare(decision.unit.id) ?: error("No unit with id: ${decision.unit.id}")
        val guardianDecisionLocation =
            createAndUploadDecision(
                settings,
                decision,
                application,
                child,
                decisionLanguage,
                unit.unitManager,
                unit.preschoolManager,
            )

        tx.updateDecisionGuardianDocumentKey(decisionId, guardianDecisionLocation.key)
    }

    private fun createAndUploadDecision(
        settings: Map<SettingType, String>,
        decision: Decision,
        application: ApplicationDetails,
        child: PersonDTO,
        decisionLanguage: OfficialLanguage,
        unitManager: UnitManager,
        preschoolManager: UnitManager,
    ): DocumentLocation {
        val decisionBytes =
            createDecisionPdf(
                templateProvider,
                pdfGenerator,
                settings,
                decision,
                child,
                application.transferApplication,
                application.form.preferences.serviceNeed,
                decisionLanguage,
                unitManager,
                preschoolManager,
            )

        return uploadPdfToS3(
            DocumentKey.Decision(decision.id, decision.type, decisionLanguage),
            decisionBytes,
        )
    }

    private fun Database.Read.isDecisionForSecondGuardianRequired(
        decision: Decision,
        application: ApplicationDetails,
        otherGuardian: PersonId,
    ) =
        decision.type != DecisionType.CLUB &&
            !personService.personsLiveInTheSameAddress(this, application.guardianId, otherGuardian)

    private fun determineDecisionLanguage(
        decision: Decision,
        tx: Database.Transaction,
    ): OfficialLanguage {
        return tx.getDecisionLanguage(decision.id)
    }

    private fun uploadPdfToS3(document: DocumentKey, bytes: ByteArray): DocumentLocation =
        documentClient.upload(document, bytes, "application/pdf").also {
            logger.debug { "PDF (object name: ${it.key}) uploaded to S3" }
        }

    fun deliverDecisionToGuardians(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: DecisionId,
        skipGuardianApproval: Boolean?,
    ) {
        val now = clock.now()
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")

        // make sure VTJ guardians are up-to-date
        personService.getGuardians(tx, AuthenticatedUser.SystemInternalUser, decision.childId)

        val applicationId = decision.applicationId
        val application =
            tx.fetchApplicationDetails(applicationId)
                ?: throw NotFound("Application $applicationId was not found")

        requireNotNull(decision.documentKey) {
            "Decision ${decision.id} PDF has not been generated"
        }

        val documentLocation = documentClient.locate(DocumentKey.Decision(decision.documentKey))
        val currentGuardians =
            tx.getChildGuardiansAndFosterParents(decision.childId, now.toLocalDate()).toSet()

        val applicationGuardian =
            tx.getPersonById(application.guardianId)
                ?: error("Guardian not found with id: ${application.guardianId}")
        if (currentGuardians.contains(applicationGuardian.id)) {
            deliverDecisionToGuardian(
                tx,
                now,
                decision,
                applicationGuardian,
                documentLocation,
                skipGuardianApproval,
            )
        } else {
            logger.warn {
                "Skipping sending decision $decisionId to application guardian ${applicationGuardian.id} - not a current guardian or foster parent"
            }
        }

        if (
            !applicationGuardian.restrictedDetailsEnabled && !decision.documentContainsContactInfo
        ) {
            val otherGuardians = tx.getApplicationOtherGuardians(applicationId)
            for (guardianId in otherGuardians) {
                val otherGuardian =
                    tx.getPersonById(guardianId)
                        ?: error("Other guardian not found with id: $guardianId")

                if (tx.isDecisionForSecondGuardianRequired(decision, application, guardianId)) {
                    if (currentGuardians.contains(guardianId)) {
                        deliverDecisionToGuardian(
                            tx,
                            now,
                            decision,
                            otherGuardian,
                            documentLocation,
                            skipGuardianApproval,
                        )
                    } else {
                        logger.warn {
                            "Skipping sending decision $decisionId to application other guardian $guardianId - not a current guardian or foster parent"
                        }
                    }
                }
            }
        }
        tx.markDecisionSent(decisionId, now)
    }

    fun deliverDecisionToGuardian(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        decision: Decision,
        guardian: PersonDTO,
        documentLocation: DocumentLocation,
        skipGuardianApproval: Boolean?,
    ) {
        if (guardian.identity !is ExternalIdentifier.SSN) {
            logger.info {
                "Cannot deliver daycare decision ${decision.id} to guardian. SSN is missing."
            }
            return
        }

        val lang = tx.getDecisionLanguage(decision.id)
        val sendAddress = getSendAddress(messageProvider, guardian, lang)
        // SFI expects unique string for each message so document.id is not suitable as it is NOT
        // string and NOT unique
        val uniqueId = "${decision.id}|${guardian.id}"

        val messageId =
            tx.storeSentSfiMessage(
                SentSfiMessage(guardianId = guardian.id, decisionId = decision.id)
            )

        val message =
            SfiMessage(
                messageId = messageId,
                documentId = uniqueId,
                documentDisplayName = calculateDecisionFileName(decision, lang),
                documentBucket = documentLocation.bucket,
                documentKey = documentLocation.key,
                firstName = guardian.firstName,
                lastName = guardian.lastName,
                streetAddress = sendAddress.street,
                postalCode = sendAddress.postalCode,
                postOffice = sendAddress.postOffice,
                ssn = guardian.identity.ssn,
                messageHeader = messageProvider.getDecisionHeader(lang),
                messageContent = messageProvider.getDecisionContent(lang, skipGuardianApproval),
            )

        asyncJobRunner.plan(tx, listOf(AsyncJob.SendMessage(message)), runAt = now)
    }

    fun sendNewDecisionEmail(
        db: Database.Connection,
        clock: EvakaClock,
        applicationId: ApplicationId,
    ) {
        val guardianId =
            db.transaction { tx ->
                val application =
                    tx.fetchApplicationDetails(applicationId)
                        ?: throw NotFound("Application $applicationId was not found")
                val childId = application.childId

                // make sure VTJ guardians are up-to-date
                personService.getGuardians(tx, AuthenticatedUser.SystemInternalUser, childId)

                val currentGuardians = tx.getChildGuardiansAndFosterParents(childId, clock.today())

                application.guardianId.takeIf { currentGuardians.contains(it) }
            }

        if (guardianId != null) {
            // simplified to get rid of superfluous language requirement
            val fromAddress = emailEnv.sender(Language.fi)
            val content = emailMessageProvider.decisionNotification()
            Email.create(
                    db,
                    guardianId,
                    EmailMessageType.DECISION_NOTIFICATION,
                    fromAddress,
                    content,
                    "$applicationId - $guardianId",
                )
                ?.also { emailClient.send(it) }
        } else {
            logger.warn {
                "Skipping sending decision for application $applicationId guardian - not a current guardian or foster parent"
            }
        }
    }

    fun getDecisionPdf(dbc: Database.Connection, decision: Decision): ResponseEntity<Any> {
        val (documentLocation, fileName) =
            dbc.read { tx ->
                val documentLocation =
                    documentClient.locate(
                        DocumentKey.Decision(
                            decision.documentKey
                                ?: throw NotFound(
                                    "Document generation for ${decision.id} in progress"
                                )
                        )
                    )
                val lang = tx.getDecisionLanguage(decision.id)
                val fileName = calculateDecisionFileName(decision, lang)
                documentLocation to fileName
            }
        return documentClient.responseAttachment(documentLocation, fileName)
    }

    private fun calculateDecisionFileName(decision: Decision, lang: OfficialLanguage): String {
        val prefix = templateProvider.getLocalizedFilename(decision.type, lang).replace(" ", "_")
        return "${prefix}_${decision.decisionNumber}_${decision.startDate}.pdf"
    }
}

fun createDecisionPdf(
    templateProvider: ITemplateProvider,
    pdfService: PdfGenerator,
    settings: Map<SettingType, String>,
    decision: Decision,
    child: PersonDTO,
    isTransferApplication: Boolean,
    serviceNeed: ServiceNeed?,
    lang: OfficialLanguage,
    unitManager: UnitManager,
    preschoolManager: UnitManager,
): ByteArray {
    val template = createTemplate(templateProvider, decision, isTransferApplication)
    val isPartTimeDecision: Boolean = decision.type == DecisionType.DAYCARE_PART_TIME

    val pages =
        generateDecisionPages(
            template,
            lang,
            settings,
            decision,
            child,
            unitManager,
            preschoolManager,
            isPartTimeDecision,
            serviceNeed,
        )

    return pdfService.render(pages)
}

private fun generateDecisionPages(
    template: String,
    lang: OfficialLanguage,
    settings: Map<SettingType, String>,
    decision: Decision,
    child: PersonDTO,
    unitManager: UnitManager,
    preschoolManager: UnitManager,
    isPartTimeDecision: Boolean,
    serviceNeed: ServiceNeed?,
): Page {
    return Page(
        Template(template),
        Context().apply {
            locale = lang.isoLanguage.toLocale()
            setVariable("decision", decision)
            setVariable("child", child)
            setVariable(
                "manager",
                when (decision.type) {
                    DecisionType.PRESCHOOL,
                    DecisionType.PREPARATORY_EDUCATION -> {
                        preschoolManager.let { if (it.name.isBlank()) unitManager else it }
                    }

                    else -> {
                        unitManager
                    }
                },
            )
            setVariable("isPartTimeDecision", isPartTimeDecision)
            setVariable("serviceNeed", serviceNeed)
            setVariable(
                "hideDaycareTime",
                decision.type == DecisionType.PRESCHOOL_DAYCARE ||
                    decision.type == DecisionType.PRESCHOOL_CLUB ||
                    decision.type == DecisionType.CLUB ||
                    decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER,
            )
            setVariable(
                "decisionUnitName",
                when (decision.type) {
                    DecisionType.DAYCARE,
                    DecisionType.DAYCARE_PART_TIME -> {
                        decision.unit.daycareDecisionName.takeUnless { it.isBlank() }
                    }

                    DecisionType.PRESCHOOL,
                    DecisionType.PRESCHOOL_DAYCARE,
                    DecisionType.PRESCHOOL_CLUB,
                    DecisionType.PREPARATORY_EDUCATION -> {
                        decision.unit.preschoolDecisionName.takeUnless { it.isBlank() }
                    }

                    else -> {
                        null
                    }
                } ?: decision.unit.name,
            )
            setVariable("decisionMakerName", settings[SettingType.DECISION_MAKER_NAME])
            setVariable("decisionMakerTitle", settings[SettingType.DECISION_MAKER_TITLE])
            setVariable("sentDate", decision.sentDate)
        },
    )
}

private fun createTemplate(
    templateProvider: ITemplateProvider,
    decision: Decision,
    isTransferApplication: Boolean,
): String {
    return when (decision.type) {
        DecisionType.CLUB -> {
            templateProvider.getClubDecisionPath()
        }

        DecisionType.DAYCARE,
        DecisionType.PRESCHOOL_DAYCARE,
        DecisionType.PRESCHOOL_CLUB,
        DecisionType.DAYCARE_PART_TIME -> {
            if (decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER) {
                templateProvider.getDaycareVoucherDecisionPath()
            } else {
                if (isTransferApplication) {
                    templateProvider.getDaycareTransferDecisionPath()
                } else {
                    templateProvider.getDaycareDecisionPath()
                }
            }
        }

        DecisionType.PRESCHOOL -> {
            templateProvider.getPreschoolDecisionPath()
        }

        DecisionType.PREPARATORY_EDUCATION -> {
            templateProvider.getPreparatoryDecisionPath()
        }
    }
}
