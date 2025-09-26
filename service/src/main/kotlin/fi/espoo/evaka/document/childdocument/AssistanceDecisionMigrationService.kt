// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.assistanceneed.decision.AssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecision
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.getAssistanceNeedDecisionById
import fi.espoo.evaka.assistanceneed.decision.getAssistanceNeedDecisionDocumentKey
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecision
import fi.espoo.evaka.assistanceneed.preschooldecision.getAssistanceNeedPreschoolDecisionById
import fi.espoo.evaka.assistanceneed.preschooldecision.getAssistanceNeedPreschoolDecisionDocumentKey
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.getTemplate
import fi.espoo.evaka.document.getTemplateSummaries
import fi.espoo.evaka.shared.CaseProcessId
import fi.espoo.evaka.shared.ChildDocumentDecisionId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service

@Service
class AssistanceDecisionMigrationService(asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.MigrateDaycareAssistanceDecision> { db, _, msg ->
            migrateDaycareAssistanceDecision(db, msg)
        }
        asyncJobRunner.registerHandler<AsyncJob.MigratePreschoolAssistanceDecision> { db, _, msg ->
            migratePreschoolAssistanceDecision(db, msg)
        }
    }

    fun migrateDaycareAssistanceDecision(
        db: Database.Connection,
        msg: AsyncJob.MigrateDaycareAssistanceDecision,
    ) {
        val (decision, template) =
            db.read { tx ->
                val decision = tx.getAssistanceNeedDecisionById(msg.decisionId)
                val template =
                    tx.getTemplateSummaries()
                        .filter {
                            it.type ==
                                ChildDocumentType.MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION &&
                                it.language.isoLanguage == decision.language.isoLanguage &&
                                it.published
                        }
                        .also {
                            if (it.isEmpty()) {
                                throw IllegalStateException(
                                    "No template found for migrating daycare assistance decision in language ${decision.language}"
                                )
                            } else if (it.size > 1) {
                                throw IllegalStateException(
                                    "Multiple templates found for migrating daycare assistance decision in language ${decision.language}"
                                )
                            }
                        }
                        .first()
                        .let { tx.getTemplate(it.id)!! }
                decision to template
            }

        if (!decision.status.isDecided())
            throw IllegalStateException("Decision ${decision.id} is not in a decided state")
        if (!decision.hasDocument)
            throw IllegalStateException("Decision ${decision.id} has no document key")
        val childId =
            decision.child?.id
                ?: throw IllegalStateException("Decision ${decision.id} has no child")
        val decisionNumber =
            decision.decisionNumber
                ?: throw IllegalStateException("Decision ${decision.id} has no decision number")
        val decisionMaker =
            decision.decisionMaker?.employeeId
                ?: throw IllegalStateException("Decision ${decision.id} has no decision maker")
        val decidedAt =
            decision.decisionMade?.let { HelsinkiDateTime.atStartOfDay(it) }
                ?: throw IllegalStateException("Decision ${decision.id} has no decision made date")
        val documentId = ChildDocumentId(decision.id.raw)
        val processId =
            decision.processId
                ?: throw IllegalStateException("Decision ${decision.id} has no process id")

        val content = migrateDaycareContent(decision, getTranslations(decision.language))
        validateContentAgainstTemplate(content, template.content)

        db.transaction { tx ->
            tx.deletePreviouslyMigratedDecision(documentId)
            val documentKey = tx.getAssistanceNeedDecisionDocumentKey(decision.id)
            tx.insertMigratedDocument(
                id = documentId,
                type = ChildDocumentType.MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION,
                templateId = template.id,
                childId = childId,
                content = content,
                decidedAt = decidedAt,
                documentKey = documentKey,
                decisionMaker = decisionMaker,
                decisionStatus =
                    when (decision.status) {
                        AssistanceNeedDecisionStatus.ACCEPTED ->
                            ChildDocumentDecisionStatus.ACCEPTED
                        AssistanceNeedDecisionStatus.REJECTED ->
                            ChildDocumentDecisionStatus.REJECTED
                        AssistanceNeedDecisionStatus.ANNULLED ->
                            ChildDocumentDecisionStatus.ANNULLED
                        else ->
                            throw IllegalStateException(
                                "Decision ${decision.id} is not in a valid state for migration"
                            )
                    },
                decisionValidity = decision.validityPeriod,
                decisionNumber = decisionNumber,
                daycareId = decision.selectedUnit?.id,
                processId = processId,
            )
        }
    }

    fun migratePreschoolAssistanceDecision(
        db: Database.Connection,
        msg: AsyncJob.MigratePreschoolAssistanceDecision,
    ) {
        val (decision, template) =
            db.read { tx ->
                val decision = tx.getAssistanceNeedPreschoolDecisionById(msg.decisionId)
                val template =
                    tx.getTemplateSummaries()
                        .filter {
                            it.type ==
                                ChildDocumentType.MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION &&
                                it.language.isoLanguage == decision.form.language.isoLanguage &&
                                it.published
                        }
                        .also {
                            if (it.isEmpty()) {
                                throw IllegalStateException(
                                    "No template found for migrating preschool assistance decision in language ${decision.form.language}"
                                )
                            } else if (it.size > 1) {
                                throw IllegalStateException(
                                    "Multiple templates found for migrating preschool assistance decision in language ${decision.form.language}"
                                )
                            }
                        }
                        .first()
                        .let { tx.getTemplate(it.id)!! }
                decision to template
            }

        if (!decision.status.isDecided())
            throw IllegalStateException("Decision ${decision.id} is not in a decided state")
        val childId =
            decision.child?.id
                ?: throw IllegalStateException("Decision ${decision.id} has no child")
        val decisionNumber =
            decision.decisionNumber
                ?: throw IllegalStateException("Decision ${decision.id} has no decision number")
        val decisionMaker =
            decision.form.decisionMakerEmployeeId
                ?: throw IllegalStateException("Decision ${decision.id} has no decision maker")
        val decidedAt =
            decision.decisionMade?.let { HelsinkiDateTime.atStartOfDay(it) }
                ?: throw IllegalStateException("Decision ${decision.id} has no decision made date")
        val validFrom =
            decision.form.validFrom
                ?: throw IllegalStateException("Decision ${decision.id} has no validity start date")
        val processId =
            decision.processId
                ?: throw IllegalStateException("Decision ${decision.id} has no process id")
        val documentId = ChildDocumentId(decision.id.raw)

        val content = migratePreschoolContent(decision, getTranslations(decision.form.language))
        validateContentAgainstTemplate(content, template.content)

        db.transaction { tx ->
            tx.deletePreviouslyMigratedDecision(documentId)
            val documentKey = tx.getAssistanceNeedPreschoolDecisionDocumentKey(decision.id)
            tx.insertMigratedDocument(
                id = documentId,
                type = ChildDocumentType.MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION,
                templateId = template.id,
                childId = childId,
                content = content,
                decidedAt = decidedAt,
                documentKey = documentKey,
                decisionMaker = decisionMaker,
                decisionStatus =
                    when (decision.status) {
                        AssistanceNeedDecisionStatus.ACCEPTED ->
                            ChildDocumentDecisionStatus.ACCEPTED
                        AssistanceNeedDecisionStatus.REJECTED ->
                            ChildDocumentDecisionStatus.REJECTED
                        AssistanceNeedDecisionStatus.ANNULLED ->
                            ChildDocumentDecisionStatus.ANNULLED
                        else ->
                            throw IllegalStateException(
                                "Decision ${decision.id} is not in a valid state for migration"
                            )
                    },
                decisionValidity = DateRange(validFrom, decision.form.validTo),
                decisionNumber = decisionNumber,
                daycareId = decision.form.selectedUnit,
                processId = processId,
            )
        }
    }
}

private fun migrateDaycareContent(
    decision: AssistanceNeedDecision,
    translations: AssistanceDecisionTranslations,
): DocumentContent {
    val guardians = decision.guardianInfo.toList()
    val guardian1 = guardians.getOrNull(0)
    val guardian2 = guardians.getOrNull(1)

    return DocumentContent(
        listOf(
            AnsweredQuestion.TextAnswer(
                questionId = "pedagogical_motivation",
                answer = decision.pedagogicalMotivation ?: "",
            ),
            AnsweredQuestion.CheckboxGroupAnswer(
                questionId = "structural_motivation",
                answer =
                    listOfNotNull(
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_smaller_group"
                            )
                            .takeIf { decision.structuralMotivationOptions.smallerGroup },
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_special_group"
                            )
                            .takeIf { decision.structuralMotivationOptions.specialGroup },
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_small_group"
                            )
                            .takeIf { decision.structuralMotivationOptions.smallGroup },
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_group_assistant"
                            )
                            .takeIf { decision.structuralMotivationOptions.groupAssistant },
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_child_assistant"
                            )
                            .takeIf { decision.structuralMotivationOptions.childAssistant },
                        CheckboxGroupAnswerContent(
                                optionId = "structural_motivation_opt_additional_staff"
                            )
                            .takeIf { decision.structuralMotivationOptions.additionalStaff },
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "structural_motivation_description",
                answer = decision.structuralMotivationDescription ?: "",
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "care_motivation",
                answer = decision.careMotivation ?: "",
            ),
            AnsweredQuestion.CheckboxGroupAnswer(
                questionId = "services",
                answer =
                    listOfNotNull(
                        CheckboxGroupAnswerContent(optionId = "service_opt_consultation_special_ed")
                            .takeIf { decision.serviceOptions.consultationSpecialEd },
                        CheckboxGroupAnswerContent(optionId = "service_opt_part_time_special_ed")
                            .takeIf { decision.serviceOptions.partTimeSpecialEd },
                        CheckboxGroupAnswerContent(optionId = "service_opt_full_time_special_ed")
                            .takeIf { decision.serviceOptions.fullTimeSpecialEd },
                        CheckboxGroupAnswerContent(
                                optionId = "service_opt_interpretation_and_assistance_services"
                            )
                            .takeIf { decision.serviceOptions.interpretationAndAssistanceServices },
                        CheckboxGroupAnswerContent(optionId = "service_opt_special_aides").takeIf {
                            decision.serviceOptions.specialAides
                        },
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "services_motivation",
                answer = decision.servicesMotivation ?: "",
            ),
            AnsweredQuestion.DateAnswer(
                questionId = "guardians_heard_on",
                answer = decision.guardiansHeardOn,
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "guardian_1",
                answer =
                    listOf(
                        listOf(
                            guardian1?.name ?: "",
                            guardian1?.isHeard?.let { heard ->
                                if (heard) translations.yes else translations.no
                            } ?: "",
                        )
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "guardian_1_details",
                answer = guardian1?.details ?: "",
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "guardian_2",
                answer =
                    listOf(
                        listOf(
                            guardian2?.name ?: "",
                            guardian2?.isHeard?.let { heard ->
                                if (heard) translations.yes else translations.no
                            } ?: "",
                        )
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "guardian_2_details",
                answer = guardian2?.details ?: "",
            ),
            AnsweredQuestion.CheckboxAnswer(
                questionId = "other_representative_heard",
                answer = decision.otherRepresentativeHeard,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "other_representative_details",
                answer = decision.otherRepresentativeDetails ?: "",
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "view_of_guardians",
                answer = decision.viewOfGuardians ?: "",
            ),
            AnsweredQuestion.CheckboxGroupAnswer(
                questionId = "assistance_levels",
                answer =
                    decision.assistanceLevels.map { CheckboxGroupAnswerContent(optionId = it.name) },
            ),
            AnsweredQuestion.DateAnswer(
                questionId = "validity_period_start",
                answer = decision.validityPeriod.start,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "validity_period_end",
                answer =
                    when {
                        !decision.assistanceLevels.contains(
                            AssistanceLevel.ASSISTANCE_SERVICES_FOR_TIME
                        ) -> "-"
                        decision.endDateNotKnown || decision.validityPeriod.end == null ->
                            translations.endDateNotKnown
                        else ->
                            decision.validityPeriod.end.format(
                                DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            )
                    },
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "selected_unit_name",
                answer = decision.selectedUnit?.name ?: "",
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "motivation_for_decision",
                answer = decision.motivationForDecision ?: "",
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "preparers",
                listOf(
                    listOf(
                        decision.preparedBy1?.name ?: "",
                        decision.preparedBy1?.title ?: "",
                        decision.preparedBy1?.phoneNumber ?: "",
                    ),
                    listOf(
                        decision.preparedBy2?.name ?: "",
                        decision.preparedBy2?.title ?: "",
                        decision.preparedBy2?.phoneNumber ?: "",
                    ),
                ),
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "decision_maker",
                listOf(
                    listOf(decision.decisionMaker?.name ?: "", decision.decisionMaker?.title ?: "")
                ),
            ),
        )
    )
}

private fun migratePreschoolContent(
    decision: AssistanceNeedPreschoolDecision,
    translations: AssistanceDecisionTranslations,
): DocumentContent {
    val guardians = decision.form.guardianInfo.toList()
    val guardian1 = guardians.getOrNull(0)
    val guardian2 = guardians.getOrNull(1)

    return DocumentContent(
        listOf(
            AnsweredQuestion.RadioButtonGroupAnswer(
                questionId = "type",
                answer = decision.form.type?.name,
            ),
            AnsweredQuestion.DateAnswer(
                questionId = "valid_from",
                answer = decision.form.validFrom,
            ),
            AnsweredQuestion.CheckboxAnswer(
                questionId = "extended_compulsory_education",
                answer = decision.form.extendedCompulsoryEducation,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "extended_compulsory_education_info",
                answer = decision.form.extendedCompulsoryEducationInfo,
            ),
            AnsweredQuestion.CheckboxGroupAnswer(
                questionId = "granted_services",
                answer =
                    listOfNotNull(
                        CheckboxGroupAnswerContent("granted_assistance_service").takeIf {
                            decision.form.grantedAssistanceService
                        },
                        CheckboxGroupAnswerContent("granted_interpretation_service").takeIf {
                            decision.form.grantedInterpretationService
                        },
                        CheckboxGroupAnswerContent("granted_assistive_devices").takeIf {
                            decision.form.grantedAssistiveDevices
                        },
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "granted_services_basis",
                answer = decision.form.grantedServicesBasis,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "selected_unit_name",
                answer = decision.unitName ?: "",
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "primary_group",
                answer = decision.form.primaryGroup,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "decision_basis",
                answer = decision.form.decisionBasis,
            ),
            AnsweredQuestion.CheckboxGroupAnswer(
                questionId = "basis_documents",
                answer =
                    listOfNotNull(
                        CheckboxGroupAnswerContent(
                                "basis_document_pedagogical_report",
                                decision.form.basisDocumentPedagogicalReportDate?.format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                ) ?: "",
                            )
                            .takeIf { decision.form.basisDocumentPedagogicalReport },
                        CheckboxGroupAnswerContent(
                                "basis_document_psychologist_statement",
                                decision.form.basisDocumentPsychologistStatementDate?.format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                ) ?: "",
                            )
                            .takeIf { decision.form.basisDocumentPsychologistStatement },
                        CheckboxGroupAnswerContent(
                                "basis_document_social_report",
                                decision.form.basisDocumentSocialReportDate?.format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                ) ?: "",
                            )
                            .takeIf { decision.form.basisDocumentSocialReport },
                        CheckboxGroupAnswerContent(
                                "basis_document_doctor_statement",
                                decision.form.basisDocumentDoctorStatementDate?.format(
                                    DateTimeFormatter.ofPattern("dd.MM.yyyy")
                                ) ?: "",
                            )
                            .takeIf { decision.form.basisDocumentDoctorStatement },
                        CheckboxGroupAnswerContent(
                                "basis_document_other_or_missing",
                                decision.form.basisDocumentOtherOrMissingInfo,
                            )
                            .takeIf { decision.form.basisDocumentOtherOrMissing },
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "basis_documents_info",
                answer = decision.form.basisDocumentsInfo,
            ),
            AnsweredQuestion.DateAnswer(
                questionId = "guardians_heard_on",
                answer = decision.form.guardiansHeardOn,
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "guardian_1",
                answer =
                    listOf(
                        listOf(
                            guardian1?.name ?: "",
                            guardian1?.isHeard?.let { heard ->
                                if (heard) translations.yes else translations.no
                            } ?: "",
                        )
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "guardian_1_details",
                answer = guardian1?.details ?: "",
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "guardian_2",
                answer =
                    listOf(
                        listOf(
                            guardian2?.name ?: "",
                            guardian2?.isHeard?.let { heard ->
                                if (heard) translations.yes else translations.no
                            } ?: "",
                        )
                    ),
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "guardian_2_details",
                answer = guardian2?.details ?: "",
            ),
            AnsweredQuestion.CheckboxAnswer(
                questionId = "other_representative_heard",
                answer = decision.form.otherRepresentativeHeard,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "other_representative_details",
                answer = decision.form.otherRepresentativeDetails,
            ),
            AnsweredQuestion.TextAnswer(
                questionId = "view_of_guardians",
                answer = decision.form.viewOfGuardians,
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "preparers",
                listOf(
                    listOf(
                        decision.preparer1Name ?: "",
                        decision.form.preparer1Title,
                        decision.form.preparer1PhoneNumber,
                    ),
                    listOf(
                        decision.preparer2Name ?: "",
                        decision.form.preparer2Title,
                        decision.form.preparer2PhoneNumber,
                    ),
                ),
            ),
            AnsweredQuestion.GroupedTextFieldsAnswer(
                questionId = "decision_maker",
                listOf(listOf(decision.decisionMakerName ?: "", decision.form.decisionMakerTitle)),
            ),
        )
    )
}

private fun Database.Transaction.deletePreviouslyMigratedDecision(id: ChildDocumentId) {
    val type =
        createQuery { sql("SELECT type FROM child_document WHERE id = ${bind(id)}") }
            .exactlyOneOrNull<ChildDocumentType>() ?: return

    if (
        type != ChildDocumentType.MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION &&
            type != ChildDocumentType.MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION
    )
        throw IllegalStateException(
            "Found an existing document with id $id that is not a migrated assistance need decision"
        )

    execute { sql("DELETE FROM child_document_read WHERE document_id = ${bind(id)}") }
    // set archived_at to bypass deletion prevention trigger
    execute { sql("UPDATE child_document SET archived_at = now() WHERE id = ${bind(id)}") }
    createQuery { sql("DELETE FROM child_document WHERE id = ${bind(id)} RETURNING decision_id") }
        .exactlyOneOrNull<ChildDocumentDecisionId>()
        ?.also { decisionId ->
            execute { sql("DELETE FROM child_document_decision WHERE id = ${bind(decisionId)}") }
        }
}

private fun Database.Transaction.insertMigratedDocument(
    id: ChildDocumentId,
    type: ChildDocumentType,
    templateId: DocumentTemplateId,
    childId: ChildId,
    content: DocumentContent,
    decidedAt: HelsinkiDateTime,
    documentKey: String?,
    decisionMaker: EmployeeId,
    decisionStatus: ChildDocumentDecisionStatus,
    decisionValidity: DateRange,
    decisionNumber: Long,
    daycareId: DaycareId?,
    processId: CaseProcessId?,
) {
    val userId = EvakaUserId(decisionMaker.raw)
    val systemUser = AuthenticatedUser.SystemInternalUser.evakaUserId

    // constraint: valid_from must be null when status is REJECTED
    val validFrom =
        when (decisionStatus) {
            ChildDocumentDecisionStatus.ACCEPTED,
            ChildDocumentDecisionStatus.ANNULLED -> decisionValidity.start
            ChildDocumentDecisionStatus.REJECTED -> null
        }

    val childDocumentDecisionId =
        createQuery {
                sql(
                    """
        INSERT INTO child_document_decision (created_at, created_by, modified_at, modified_by, status, valid_from, valid_to, decision_number, daycare_id) 
        VALUES (${bind(decidedAt)}, ${bind(userId)}, ${bind(decidedAt)}, ${bind(userId)}, ${bind(decisionStatus)}, ${bind(validFrom)}, ${bind(decisionValidity.end)}, ${bind(decisionNumber)}, ${bind(daycareId)})
        RETURNING id
    """
                )
            }
            .exactlyOne<ChildDocumentDecisionId>()

    createUpdate {
            sql(
                """
INSERT INTO child_document (id, child_id, template_id, content, published_at, status, published_content, modified_at, document_key, content_locked_at, content_locked_by, process_id, created_by, type, decision_maker, decision_id, published_by) 
VALUES (${bind(id)}, ${bind(childId)}, ${bind(templateId)}, ${bind(content)}, ${bind(decidedAt)}, 'COMPLETED', ${bind(content)}, ${bind(decidedAt)}, ${bind(documentKey)}, ${bind(decidedAt)}, ${bind(userId)}, ${bind(processId)}, ${bind(systemUser)}, ${bind(type)}, ${bind(decisionMaker)}, ${bind(childDocumentDecisionId)}, ${bind(systemUser)})
"""
            )
        }
        .updateExactlyOne()
}

private data class AssistanceDecisionTranslations(
    val yes: String,
    val no: String,
    val endDateNotKnown: String,
)

private val translationsFi =
    AssistanceDecisionTranslations(
        yes = "Kyllä",
        no = "Ei",
        endDateNotKnown = "Tukipalvelun päättymisajankohta ei tiedossa",
    )

private val translationsSv =
    AssistanceDecisionTranslations(
        yes = "Ja",
        no = "Nej",
        endDateNotKnown = "Slutdatumet för stödtjänster är okänt",
    )

private fun getTranslations(lang: OfficialLanguage) =
    when (lang) {
        OfficialLanguage.FI -> translationsFi
        OfficialLanguage.SV -> translationsSv
    }
