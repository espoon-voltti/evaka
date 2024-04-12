// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getUnitManager
import fi.espoo.evaka.daycare.service.DaycareManager
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.service.DocumentLang
import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentLocation
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.setting.SettingType
import fi.espoo.evaka.setting.getSettings
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.IMessageProvider
import fi.espoo.evaka.shared.template.ITemplateProvider
import java.util.Locale
import mu.KotlinLogging
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
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    env: BucketEnv
) {
    private val decisionBucket = env.decisions

    fun finalizeDecisions(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        clock: EvakaClock,
        applicationId: ApplicationId,
        sendAsMessage: Boolean
    ): List<DecisionId> {
        val decisionIds = tx.finalizeDecisions(applicationId, clock.today())
        asyncJobRunner.plan(
            tx,
            decisionIds.map { AsyncJob.NotifyDecisionCreated(it, user, sendAsMessage) },
            runAt = clock.now()
        )
        return decisionIds
    }

    fun createDecisionPdfs(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        decisionId: DecisionId
    ) {
        val settings = tx.getSettings()
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")
        val decisionLanguage = determineDecisionLanguage(decision, tx)
        val application =
            tx.fetchApplicationDetails(decision.applicationId)
                ?: throw NotFound("Application ${decision.applicationId} was not found")
        val guardian =
            tx.getPersonById(application.guardianId)
                ?: error("Guardian not found with id: ${application.guardianId}")
        val child =
            tx.getPersonById(application.childId)
                ?: error("Child not found with id: ${application.childId}")
        val unitManager =
            tx.getUnitManager(decision.unit.id)
                ?: throw NotFound("Daycare manager not found with daycare id: ${decision.unit.id}.")
        val guardianDecisionLocation =
            createAndUploadDecision(
                settings,
                decision,
                application,
                guardian,
                child,
                decisionLanguage,
                unitManager
            )

        tx.updateDecisionGuardianDocumentKey(decisionId, guardianDecisionLocation.key)

        if (
            application.otherGuardianId != null &&
                isDecisionForSecondGuardianRequired(decision, application, tx)
        ) {
            val otherGuardian =
                tx.getPersonById(application.otherGuardianId)
                    ?: throw NotFound(
                        "Other guardian not found with id: ${application.otherGuardianId}"
                    )

            val otherGuardianDecisionLocation =
                createAndUploadDecision(
                    settings,
                    decision,
                    application,
                    otherGuardian,
                    child,
                    decisionLanguage,
                    unitManager
                )
            tx.updateDecisionOtherGuardianDocumentKey(decisionId, otherGuardianDecisionLocation.key)
        }
    }

    private fun createAndUploadDecision(
        settings: Map<SettingType, String>,
        decision: Decision,
        application: ApplicationDetails,
        guardian: PersonDTO,
        child: PersonDTO,
        decisionLanguage: DocumentLang,
        unitManager: DaycareManager
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
                unitManager
            )

        return uploadPdfToS3(
            decisionBucket,
            constructObjectKey(decision, guardian, decisionLanguage),
            decisionBytes
        )
    }

    private fun isDecisionForSecondGuardianRequired(
        decision: Decision,
        application: ApplicationDetails,
        tx: Database.Transaction
    ) =
        decision.type != DecisionType.CLUB &&
            application.otherGuardianId != null &&
            !personService.personsLiveInTheSameAddress(
                tx,
                application.guardianId,
                application.otherGuardianId
            )

    private fun determineDecisionLanguage(
        decision: Decision,
        tx: Database.Transaction
    ): DocumentLang {
        return if (decision.type == DecisionType.CLUB) {
            DocumentLang.FI
        } else {
            tx.getDecisionLanguage(decision.id)
        }
    }

    private fun constructObjectKey(
        decision: Decision,
        guardian: PersonDTO,
        lang: DocumentLang
    ): String {
        return when (decision.type) {
            DecisionType.CLUB -> "clubdecision"
            DecisionType.DAYCARE,
            DecisionType.DAYCARE_PART_TIME -> "daycaredecision"
            DecisionType.PRESCHOOL -> "preschooldecision"
            DecisionType.PRESCHOOL_DAYCARE,
            DecisionType.PRESCHOOL_CLUB -> "connectingdaycaredecision"
            DecisionType.PREPARATORY_EDUCATION -> "preparatorydecision"
        }.let { "${it}_${decision.id}_${guardian.id}_$lang" }
    }

    private fun uploadPdfToS3(bucket: String, key: String, document: ByteArray): DocumentLocation =
        documentClient
            .upload(bucket, Document(name = key, bytes = document, contentType = "application/pdf"))
            .also { logger.debug { "PDF (object name: $key) uploaded to S3 with $it." } }

    fun deliverDecisionToGuardians(
        tx: Database.Transaction,
        clock: EvakaClock,
        decisionId: DecisionId
    ) {
        val decision =
            tx.getDecision(decisionId) ?: throw NotFound("No decision with id: $decisionId")

        val applicationId = decision.applicationId
        val application =
            tx.fetchApplicationDetails(applicationId)
                ?: throw NotFound("Application $applicationId was not found")

        val currentVtjGuardianIds =
            personService
                .getGuardians(tx, AuthenticatedUser.SystemInternalUser, decision.childId)
                .map { person -> person.id }

        val applicationGuardian =
            tx.getPersonById(application.guardianId)
                ?: error("Guardian not found with id: ${application.guardianId}")

        if (currentVtjGuardianIds.contains(applicationGuardian.id)) {
            deliverDecisionToGuardian(
                tx,
                clock,
                decision,
                applicationGuardian,
                decision.documentKey!!
            )
        } else {
            logger.warn(
                "Skipping sending decision $decisionId to application guardian ${applicationGuardian.id} - not a current VTJ guardian"
            )
        }

        if (
            application.otherGuardianId != null &&
                !decision.otherGuardianDocumentKey.isNullOrBlank() &&
                !applicationGuardian.restrictedDetailsEnabled
        ) {
            val otherGuardian =
                tx.getPersonById(application.otherGuardianId)
                    ?: error("Other guardian not found with id: ${application.otherGuardianId}")

            if (currentVtjGuardianIds.contains(application.otherGuardianId)) {
                deliverDecisionToGuardian(
                    tx,
                    clock,
                    decision,
                    otherGuardian,
                    decision.otherGuardianDocumentKey
                )
            } else {
                logger.warn(
                    "Skipping sending decision $decisionId to application other guardian ${application.otherGuardianId} - not a current VTJ guardian"
                )
            }
        }
        tx.markDecisionSent(decisionId, clock.today())
    }

    fun deliverDecisionToGuardian(
        tx: Database.Transaction,
        clock: EvakaClock,
        decision: Decision,
        guardian: PersonDTO,
        documentKey: String
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
        val message =
            SfiMessage(
                messageId = uniqueId,
                documentId = uniqueId,
                documentDisplayName = calculateDecisionFileName(decision, lang),
                documentBucket = decisionBucket,
                documentKey = documentKey,
                firstName = guardian.firstName,
                lastName = guardian.lastName,
                streetAddress = sendAddress.street,
                postalCode = sendAddress.postalCode,
                postOffice = sendAddress.postOffice,
                ssn = guardian.identity.ssn,
                language = lang.langCode,
                messageHeader = messageProvider.getDecisionHeader(lang.messageLang),
                messageContent = messageProvider.getDecisionContent(lang.messageLang)
            )

        asyncJobRunner.plan(tx, listOf(AsyncJob.SendMessage(message)), runAt = clock.now())
    }

    fun getDecisionPdf(dbc: Database.Connection, decision: Decision): ResponseEntity<Any> {
        val (documentKey, fileName) =
            dbc.read { tx ->
                val documentKey =
                    decision.documentKey
                        ?: throw NotFound("Document generation for ${decision.id} in progress")
                val lang = tx.getDecisionLanguage(decision.id)
                val fileName = calculateDecisionFileName(decision, lang)
                documentKey to fileName
            }
        return documentClient.responseAttachment(decisionBucket, documentKey, fileName)
    }

    private fun calculateDecisionFileName(decision: Decision, lang: DocumentLang): String {
        val decisionUniqueId = decision.decisionNumber
        val prefix = templateProvider.getLocalizedFilename(decision.type, lang)
        return "${prefix}_$decisionUniqueId.pdf".replace(" ", "_")
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
    lang: DocumentLang,
    unitManager: DaycareManager
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
            isPartTimeDecision,
            serviceNeed
        )

    return pdfService.render(pages)
}

private fun generateDecisionPages(
    template: String,
    lang: DocumentLang,
    settings: Map<SettingType, String>,
    decision: Decision,
    child: PersonDTO,
    manager: DaycareManager,
    isPartTimeDecision: Boolean,
    serviceNeed: ServiceNeed?
): Page {
    return Page(
        Template(template),
        Context().apply {
            locale = Locale.Builder().setLanguage(lang.langCode).build()
            setVariable("decision", decision)
            setVariable("child", child)
            setVariable("manager", manager)
            setVariable("isPartTimeDecision", isPartTimeDecision)
            setVariable("serviceNeed", serviceNeed)
            setVariable(
                "hideDaycareTime",
                decision.type == DecisionType.PRESCHOOL_DAYCARE ||
                    decision.type == DecisionType.PRESCHOOL_CLUB ||
                    decision.type == DecisionType.CLUB ||
                    decision.unit.providerType == ProviderType.PRIVATE_SERVICE_VOUCHER
            )
            setVariable(
                "decisionUnitName",
                when (decision.type) {
                    DecisionType.DAYCARE,
                    DecisionType.DAYCARE_PART_TIME ->
                        decision.unit.daycareDecisionName.takeUnless { it.isBlank() }
                    DecisionType.PRESCHOOL,
                    DecisionType.PRESCHOOL_DAYCARE,
                    DecisionType.PRESCHOOL_CLUB,
                    DecisionType.PREPARATORY_EDUCATION ->
                        decision.unit.preschoolDecisionName.takeUnless { it.isBlank() }
                    else -> null
                } ?: decision.unit.name
            )
            setVariable("decisionMakerName", settings[SettingType.DECISION_MAKER_NAME])
            setVariable("decisionMakerTitle", settings[SettingType.DECISION_MAKER_TITLE])
            setVariable("sentDate", decision.sentDate)
        }
    )
}

private fun createTemplate(
    templateProvider: ITemplateProvider,
    decision: Decision,
    isTransferApplication: Boolean
): String {
    return when (decision.type) {
        DecisionType.CLUB -> templateProvider.getClubDecisionPath()
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
        DecisionType.PRESCHOOL -> templateProvider.getPreschoolDecisionPath()
        DecisionType.PREPARATORY_EDUCATION -> templateProvider.getPreparatoryDecisionPath()
    }
}
