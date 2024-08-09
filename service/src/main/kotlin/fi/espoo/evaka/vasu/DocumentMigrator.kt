// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.document.DocumentTemplateBasicsRequest
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.document.childdocument.AnsweredQuestion
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.insertTemplate
import fi.espoo.evaka.document.publishTemplate
import fi.espoo.evaka.document.updateDraftTemplateContent
import fi.espoo.evaka.process.ArchivedProcessState
import fi.espoo.evaka.process.insertProcess
import fi.espoo.evaka.process.insertProcessHistoryRow
import fi.espoo.evaka.shared.ArchivedProcessId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service

@Service
class VasuMigratorService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig
) {
    init {
        asyncJobRunner.registerHandler<AsyncJob.MigrateVasuDocument> { db, clock, msg ->
            if (msg.processDefinitionNumber == null) {
                return@registerHandler // old job
            }
            db.transaction { tx ->
                migrateVasu(
                    tx = tx,
                    asyncJobRunner = asyncJobRunner,
                    now = clock.now(),
                    id = msg.documentId,
                    processDefinitionNumber = msg.processDefinitionNumber,
                    archiveMetadataOrganization = featureConfig.archiveMetadataOrganization
                )
            }
        }
    }

    fun planMigrationJobs(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        vasuTemplateId: VasuTemplateId,
        processDefinitionNumber: String
    ) {
        val vasuDocumentIds =
            tx.createQuery {
                    sql(
                        """
            SELECT id FROM curriculum_document 
            WHERE template_id = ${bind(vasuTemplateId)}
        """
                    )
                }
                .toList<VasuDocumentId>()

        asyncJobRunner.plan(
            tx,
            payloads =
                vasuDocumentIds.map { AsyncJob.MigrateVasuDocument(it, processDefinitionNumber) },
            retryCount = 3,
            runAt = now
        )
    }
}

fun migrateVasu(
    tx: Database.Transaction,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    id: VasuDocumentId,
    processDefinitionNumber: String,
    archiveMetadataOrganization: String,
) {
    val vasuDocument =
        tx.getLatestPublishedVasuDocument(now.toLocalDate(), id)?.takeIf {
            it.documentState == VasuDocumentState.CLOSED && it.publishedAt != null
        } ?: return

    val templateBasics = toBasicsRequest(vasuDocument)
    val (templateContent, documentContent) = migrateContents(vasuDocument)

    val templateId = getOrCreateTemplate(tx, templateBasics, templateContent)

    val documentId = ChildDocumentId(vasuDocument.id.raw)
    tx.deletePreviouslyMigratedChildDocument(documentId)

    val processId =
        migrateMetadataProcess(
            tx = tx,
            vasuDocument = vasuDocument,
            processDefinitionNumber = processDefinitionNumber,
            archiveMetadataOrganization = archiveMetadataOrganization
        )

    tx.insertMigratedChildDocument(
        id = documentId,
        childId = vasuDocument.basics.child.id,
        templateId = templateId,
        content = documentContent,
        modifiedAt = vasuDocument.modifiedAt,
        publishedAt = vasuDocument.publishedAt!!,
        processId = processId
    )

    asyncJobRunner.plan(
        tx = tx,
        payloads = listOf(AsyncJob.CreateChildDocumentPdf(documentId)),
        retryCount = 5,
        runAt = now
    )
}

private fun migrateMetadataProcess(
    tx: Database.Transaction,
    vasuDocument: VasuDocument,
    processDefinitionNumber: String,
    archiveMetadataOrganization: String
): ArchivedProcessId {
    val archiveDurationMonths = 120 * 12
    val processId =
        tx.insertProcess(
                processDefinitionNumber = processDefinitionNumber,
                year = vasuDocument.created.year,
                organization = archiveMetadataOrganization,
                archiveDurationMonths = archiveDurationMonths
            )
            .id
    tx.insertProcessHistoryRow(
        processId = processId,
        state = ArchivedProcessState.INITIAL,
        now = vasuDocument.created,
        userId = AuthenticatedUser.SystemInternalUser.evakaUserId
    )
    vasuDocument.events
        .firstOrNull { it.eventType == VasuDocumentEventType.MOVED_TO_CLOSED }
        ?.also {
            tx.insertProcessHistoryRow(
                processId = processId,
                state = ArchivedProcessState.COMPLETED,
                now = it.created,
                userId = it.createdBy
            )
        }
    return processId
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val checkboxTrue = "[x]"
private val checkboxFalse = "[ ]"
private val emptyField = "___________"

private fun getOrCreateTemplate(
    tx: Database.Transaction,
    templateBasics: DocumentTemplateBasicsRequest,
    templateContent: DocumentTemplateContent
): DocumentTemplateId {
    tx.createUpdate { sql("LOCK TABLE document_template") }.execute()
    return getMatchingTemplate(tx, templateBasics, templateContent)
        ?: tx.insertTemplate(templateBasics)
            .id
            .also { tx.updateDraftTemplateContent(it, templateContent) }
            .also { tx.publishTemplate(it) }
}

private fun getMatchingTemplate(
    tx: Database.Transaction,
    templateBasics: DocumentTemplateBasicsRequest,
    templateContent: DocumentTemplateContent
): DocumentTemplateId? =
    tx.createQuery {
            sql(
                """
    SELECT id FROM document_template
    WHERE name = ${bind(templateBasics.name)}
     AND type = ${bind(templateBasics.type)}
     AND language = ${bind(templateBasics.language)}
     AND confidential = ${bind(templateBasics.confidential)}
     AND legal_basis = ${bind(templateBasics.legalBasis)}
     AND validity = ${bind(templateBasics.validity)}
     AND content = ${bind(templateContent)}::jsonb
"""
            )
        }
        .exactlyOneOrNull()

private fun Database.Transaction.deletePreviouslyMigratedChildDocument(id: ChildDocumentId) {
    val type =
        createQuery {
                sql(
                    """
        SELECT dt.type 
        FROM child_document cd
        JOIN document_template dt ON dt.id = cd.template_id
        WHERE cd.id = ${bind(id)}
    """
                )
            }
            .exactlyOneOrNull<DocumentType>()

    when (type) {
        null -> return // no document found
        DocumentType.MIGRATED_VASU,
        DocumentType.MIGRATED_LEOPS -> {
            execute {
                sql(
                    """
                DELETE FROM archived_process ap
                WHERE id = (SELECT process_id FROM child_document cd WHERE cd.id = ${bind(id)});
                
                DELETE FROM child_document cd WHERE id = ${bind(id)};
            """
                )
            }
        }
        else -> throw IllegalArgumentException("document $id is not a migrated document")
    }
}

private fun Database.Transaction.insertMigratedChildDocument(
    id: ChildDocumentId,
    childId: ChildId,
    templateId: DocumentTemplateId,
    content: DocumentContent,
    modifiedAt: HelsinkiDateTime,
    publishedAt: HelsinkiDateTime,
    processId: ArchivedProcessId
) {
    createUpdate {
            sql(
                """
INSERT INTO child_document(id, child_id, template_id, status, content, published_content, modified_at, published_at, content_modified_at, content_modified_by, created_by, process_id)
VALUES (${bind(id)}, ${bind(childId)}, ${bind(templateId)}, 'COMPLETED', ${bind(content)}, ${bind(content)}, ${bind(modifiedAt)}, ${bind(publishedAt)}, ${bind(modifiedAt)}, null, null, ${bind(processId)})
"""
            )
        }
        .execute()
}

private fun toBasicsRequest(vasu: VasuDocument): DocumentTemplateBasicsRequest {
    val translations = getTranslations(vasu.language)
    return DocumentTemplateBasicsRequest(
        name = vasu.templateName,
        type =
            when (vasu.type) {
                CurriculumType.DAYCARE -> DocumentType.MIGRATED_VASU
                CurriculumType.PRESCHOOL -> DocumentType.MIGRATED_LEOPS
            },
        language =
            when (vasu.language) {
                OfficialLanguage.FI -> OfficialLanguage.FI
                OfficialLanguage.SV -> OfficialLanguage.SV
            },
        confidential = true,
        legalBasis =
            when (vasu.type) {
                CurriculumType.DAYCARE -> translations.lawVasu
                CurriculumType.PRESCHOOL -> translations.lawLeops
            },
        validity = vasu.templateRange.asDateRange(),
        processDefinitionNumber = null,
        archiveDurationMonths = null
    )
}

private fun migrateContents(
    vasuDocument: VasuDocument
): Pair<DocumentTemplateContent, DocumentContent> {
    val sections =
        if (vasuDocument.content.hasDynamicFirstSection == true) {
            vasuDocument.content.sections
        } else {
            val basicsSection =
                VasuSection(
                    name = getTranslations(vasuDocument.language).basicsTitle,
                    questions = listOf(VasuQuestion.StaticInfoSubSection())
                )
            listOf(basicsSection) + vasuDocument.content.sections
        }
    val sectionsAndAnswers =
        sections.mapIndexed { i, s ->
            migrateTemplateSection(document = vasuDocument, vasuSection = s, sectionIndex = i)
        }
    val templateContent = DocumentTemplateContent(sections = sectionsAndAnswers.map { it.first })
    val documentContent = DocumentContent(answers = sectionsAndAnswers.flatMap { it.second })
    return templateContent to documentContent
}

private fun migrateTemplateSection(
    document: VasuDocument,
    vasuSection: VasuSection,
    sectionIndex: Int
): Pair<Section, List<AnsweredQuestion<*>>> {
    val questionsAndAnswer =
        vasuSection.questions.flatMapIndexed { i, q ->
            migrateTemplateQuestion(
                document = document,
                vasuQuestion = q,
                sectionIndex = sectionIndex,
                questionIndex = i
            )
        }
    return Section(
        id = "section-$sectionIndex",
        label = vasuSection.name,
        questions = questionsAndAnswer.map { it.first }
    ) to questionsAndAnswer.map { it.second }
}

/** maps vasu question to one or more question-answer pairs */
private fun migrateTemplateQuestion(
    document: VasuDocument,
    vasuQuestion: VasuQuestion,
    sectionIndex: Int,
    questionIndex: Int
): List<Pair<Question, AnsweredQuestion<*>>> {
    val id = "section-$sectionIndex-question-$questionIndex"
    return when (vasuQuestion) {
        is VasuQuestion.TextQuestion -> {
            val question =
                Question.TextQuestion(
                    id = id,
                    label = vasuQuestion.name,
                    infoText = vasuQuestion.info,
                    multiline = vasuQuestion.multiline
                )
            val answer = AnsweredQuestion.TextAnswer(questionId = id, answer = vasuQuestion.value)
            listOf(question to answer)
        }
        is VasuQuestion.CheckboxQuestion -> {
            val questionAndAnswer =
                if (vasuQuestion.label.isNullOrBlank()) {
                    Question.CheckboxQuestion(
                        id = id,
                        label = vasuQuestion.name,
                        infoText = vasuQuestion.info
                    ) to
                        AnsweredQuestion.CheckboxAnswer(
                            questionId = id,
                            answer = vasuQuestion.value
                        )
                } else if (vasuQuestion.name.isBlank()) {
                    Question.CheckboxQuestion(
                        id = id,
                        label = vasuQuestion.label,
                        infoText = vasuQuestion.info
                    ) to
                        AnsweredQuestion.CheckboxAnswer(
                            questionId = id,
                            answer = vasuQuestion.value
                        )
                } else {
                    Question.TextQuestion(
                        id = id,
                        label = vasuQuestion.label,
                        infoText = vasuQuestion.info,
                        multiline = false
                    ) to
                        AnsweredQuestion.TextAnswer(
                            questionId = id,
                            answer =
                                if (vasuQuestion.value) "$checkboxTrue ${vasuQuestion.name}"
                                else "$checkboxFalse ${vasuQuestion.name}"
                        )
                }
            listOf(questionAndAnswer)
        }
        is VasuQuestion.RadioGroupQuestion -> {
            val question =
                Question.TextQuestion(
                    id = id,
                    label = vasuQuestion.name,
                    infoText = vasuQuestion.info,
                    multiline = true
                )

            val answerText =
                vasuQuestion.value
                    ?.let { vasuQuestion.options.find { opt -> opt.key == it } }
                    ?.let { opt ->
                        listOfNotNull(
                                opt.name,
                                if (opt.dateRange) {
                                    vasuQuestion.dateRange?.let {
                                        "${it.start.format(dateFormatter)} - ${it.end.format(dateFormatter)}"
                                    } ?: emptyField
                                } else null
                            )
                            .joinToString(separator = " : ")
                    } ?: "-"

            val answer = AnsweredQuestion.TextAnswer(questionId = id, answer = answerText)

            listOf(question to answer)
        }
        is VasuQuestion.MultiSelectQuestion -> {
            val question =
                Question.TextQuestion(
                    id = id,
                    label = vasuQuestion.name,
                    infoText = vasuQuestion.info,
                    multiline = true
                )
            val optionAnswers =
                vasuQuestion.options.map { opt ->
                    if (opt.isIntervention) return@map "\n${opt.name}\n"
                    val selected = vasuQuestion.value.contains(opt.key)
                    val answerParts =
                        listOfNotNull(
                            " ",
                            if (selected) checkboxTrue else checkboxFalse,
                            opt.name,
                            if (selected && opt.dateRange) {
                                vasuQuestion.dateRangeValue?.get(opt.key)?.let {
                                    "${it.start.format(dateFormatter)} - ${it.end.format(dateFormatter)}"
                                } ?: emptyField
                            } else null,
                            if (selected && opt.date) {
                                vasuQuestion.dateValue?.get(opt.key)?.format(dateFormatter)
                                    ?: emptyField
                            } else null,
                            if (selected && opt.textAnswer) {
                                vasuQuestion.textValue[opt.key]?.takeIf { it.isNotBlank() }
                                    ?: emptyField
                            } else null
                        )
                    answerParts.joinToString(separator = " ")
                }
            val answer =
                AnsweredQuestion.TextAnswer(
                    questionId = id,
                    answer = optionAnswers.joinToString(separator = "\n")
                )
            listOf(question to answer)
        }
        is VasuQuestion.MultiField -> {
            if (vasuQuestion.separateRows) {
                val header =
                    Question.StaticTextDisplayQuestion(
                        id = "$id-header",
                        label = vasuQuestion.name,
                        infoText = vasuQuestion.info
                    ) to
                        AnsweredQuestion.StaticTextDisplayAnswer(
                            questionId = "$id-header",
                            answer = null
                        )
                val subQuestions =
                    vasuQuestion.keys.mapIndexed { i, field ->
                        Question.TextQuestion(
                            id = "$id-$i",
                            label = field.name,
                            infoText = field.info ?: "",
                            multiline = false
                        ) to
                            AnsweredQuestion.TextAnswer(
                                questionId = "$id-$i",
                                answer = vasuQuestion.value[i]
                            )
                    }
                listOf(header) + subQuestions
            } else {
                val question =
                    Question.GroupedTextFieldsQuestion(
                        id = id,
                        label = vasuQuestion.name,
                        fieldLabels = vasuQuestion.keys.map { it.name },
                        infoText = vasuQuestion.info,
                        allowMultipleRows = false
                    )
                val answer =
                    AnsweredQuestion.GroupedTextFieldsAnswer(
                        questionId = id,
                        answer = listOf(vasuQuestion.value)
                    )
                listOf(question to answer)
            }
        }
        is VasuQuestion.MultiFieldList -> {
            val question =
                Question.GroupedTextFieldsQuestion(
                    id = id,
                    label = vasuQuestion.name,
                    fieldLabels = vasuQuestion.keys.map { it.name },
                    infoText = vasuQuestion.info,
                    allowMultipleRows = true
                )
            val answer =
                AnsweredQuestion.GroupedTextFieldsAnswer(
                    questionId = id,
                    answer = vasuQuestion.value
                )
            listOf(question to answer)
        }
        is VasuQuestion.DateQuestion -> {
            val question =
                Question.DateQuestion(
                    id = id,
                    label = vasuQuestion.name,
                    infoText = vasuQuestion.info
                )
            val answer = AnsweredQuestion.DateAnswer(questionId = id, answer = vasuQuestion.value)
            listOf(question to answer)
        }
        is VasuQuestion.Followup -> {
            val question =
                Question.TextQuestion(
                    id = id,
                    label = vasuQuestion.title,
                    infoText = vasuQuestion.info,
                    multiline = true
                )
            val answer =
                AnsweredQuestion.TextAnswer(
                    questionId = id,
                    answer =
                        if (vasuQuestion.value.isEmpty()) "-"
                        else {
                            vasuQuestion.value.joinToString("\n\n") {
                                "${it.date.format(dateFormatter)}: ${it.text.trim()}"
                            }
                        }
                )

            listOf(question to answer)
        }
        is VasuQuestion.Paragraph -> {
            listOf(
                Question.StaticTextDisplayQuestion(
                    id = id,
                    label = vasuQuestion.title,
                    text = vasuQuestion.paragraph
                ) to AnsweredQuestion.StaticTextDisplayAnswer(questionId = id, answer = null)
            )
        }
        is VasuQuestion.StaticInfoSubSection -> {
            val translations = getTranslations(document.language)
            listOf(
                Question.TextQuestion(id = "$id-0", label = translations.name) to
                    AnsweredQuestion.TextAnswer(
                        questionId = "$id-0",
                        answer =
                            "${document.basics.child.firstName} ${document.basics.child.lastName}"
                    ),
                Question.TextQuestion(id = "$id-1", label = translations.dateOfBirth) to
                    AnsweredQuestion.TextAnswer(
                        questionId = "$id-1",
                        answer = document.basics.child.dateOfBirth.format(dateFormatter)
                    ),
                Question.TextQuestion(
                    id = "$id-2",
                    label = translations.guardians,
                    multiline = true
                ) to
                    AnsweredQuestion.TextAnswer(
                        questionId = "$id-2",
                        answer =
                            document.basics.guardians.joinToString("\n") {
                                "${it.firstName} ${it.lastName}"
                            }
                    ),
                Question.TextQuestion(
                    id = "$id-3",
                    label =
                        when (document.type) {
                            CurriculumType.DAYCARE -> translations.placementsVasu
                            CurriculumType.PRESCHOOL -> translations.placementsLeops
                        },
                    multiline = true
                ) to
                    AnsweredQuestion.TextAnswer(
                        questionId = "$id-3",
                        answer =
                            document.basics.placements?.joinToString("\n") {
                                "${it.unitName} ${it.groupName} ${it.range.start.format(dateFormatter)} - ${it.range.end.format(dateFormatter)}"
                            } ?: "-"
                    )
            )
        }
    }
}

private data class Translations(
    val basicsTitle: String,
    val name: String,
    val dateOfBirth: String,
    val guardians: String,
    val placementsVasu: String,
    val placementsLeops: String,
    val lawVasu: String,
    val lawLeops: String,
)

private val translationsFi =
    Translations(
        basicsTitle = "Perustiedot",
        name = "Lapsen nimi",
        dateOfBirth = "Lapsen syntymäaika",
        guardians = "Huoltaja(t) tai muu laillinen edustaja",
        placementsVasu = "Varhaiskasvatusyksikkö ja ryhmä",
        placementsLeops = "Esiopetusyksikkö ja ryhmä",
        lawVasu = "Varhaiskasvatuslaki (540/2018) 40§:n 3 mom.",
        lawLeops = "JulkL 24.1 §:n kohdat 25 ja 30"
    )

private val translationsSv =
    Translations(
        basicsTitle = "Basuppgifter",
        name = "Barnets namn",
        dateOfBirth = "Barnets födelsedatum",
        guardians = "Vårdnadshavare eller annan laglig företrädare",
        placementsVasu = "Enhet och grupp inom småbarnspedagogiken",
        placementsLeops = "Enhet och grupp inom förskoleundervisningen",
        lawVasu = "40 § 3 mom. i lagen om småbarnspedagogik (540/2018)",
        lawLeops = "OffentlighetsL 24.1 §§ punkt 25 och 30"
    )

private fun getTranslations(language: OfficialLanguage) =
    when (language) {
        OfficialLanguage.FI -> translationsFi
        OfficialLanguage.SV -> translationsSv
    }
