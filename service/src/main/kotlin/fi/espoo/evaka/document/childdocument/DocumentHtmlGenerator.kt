// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.DocumentLanguage
import fi.espoo.evaka.document.DocumentTemplate
import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.shared.HtmlBuilder
import fi.espoo.evaka.shared.HtmlElement
import java.time.format.DateTimeFormatter

// language=css
private val childDocumentCss =
    """
    * {
        line-height: 1.4;
        font-family: "Open Sans", sans-serif;
    }
    
    body {
        margin: 0;
        font-size: 13px;
    }
    
    @page {
        size: A4 portrait;
        margin: 21mm 21mm 21mm 21mm;
    }

    label {
        font-weight: 600;
    }
    
    .header-section {
        width: 100%;
    }
    
    .header-section>h1 {
        display: inline-block;
        width: 65%;
    }
    
    .header-section>.legal-info {
        display: inline-block;
        width: 30%;
        text-align: right;
    }
    
    .legal-basis {
        margin-bottom: 8px;
    }
    
    .child-info {
        margin-bottom: 48px;
    }
    
    .sections {
        display: flex;
        flex-direction: column;
    }
    
    .section {
        margin-bottom: 48px;
    }
    
    .questions {
        display: flex;
        flex-direction: column;
    }
    
    .question {
        margin-bottom: 24px;
    }
    
    .question>label {
        display: block;
        margin-bottom: 4px;
    }
    
    .question ul {
        margin-top: 0;
    }
    
    .question-grouped-text-fields th, .question-grouped-text-fields td {
        white-space: nowrap;
        overflow: hidden;
        padding-right: 24px;
    }
"""
        .trimIndent()

fun generateChildDocumentHtml(document: ChildDocumentDetails): String {
    return """
        <html>
            <head>
                <style>$childDocumentCss</style>
            </head>
            ${generateBody(document).toHtml()}
        </html>
    """
        .trimIndent()
}

private fun generateBody(document: ChildDocumentDetails): HtmlElement {
    return HtmlBuilder.body {
        listOf(
            generateHeader(document.template),
            h2(
                text =
                    "${document.child.firstName} ${document.child.lastName} " +
                        (document.child.dateOfBirth?.let {
                            "(s. ${it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))})"
                        } ?: ""),
                className = "child-info"
            ),
            div(className = "sections") {
                document.template.content.sections.map {
                    generateSectionHtml(it, document.content.answers, document.template.language)
                }
            }
        )
    }
}

private fun generateHeader(template: DocumentTemplate): HtmlElement {
    return HtmlBuilder.div(className = "header-section") {
        listOf(
            h1(template.name),
            div(className = "legal-info") {
                listOfNotNull(
                    template.legalBasis
                        .takeIf { it.isNotBlank() }
                        ?.let { div(text = it, className = "legal-basis") },
                    if (template.confidential) div(getTranslations(template.language).confidential)
                    else null
                )
            }
        )
    }
}

private fun generateSectionHtml(
    section: Section,
    answers: List<AnsweredQuestion<*>>,
    language: DocumentLanguage
): HtmlElement {
    return HtmlBuilder.div(className = "section") {
        listOf(
            h2(section.label),
            div(className = "questions") {
                section.questions.map { generateQuestionHtml(it, answers, language) }
            }
        )
    }
}

private fun generateQuestionHtml(
    question: Question,
    answers: List<AnsweredQuestion<*>>,
    language: DocumentLanguage
): HtmlElement {
    return question.generateHtml(
        answeredQuestion = answers.find { it.questionId == question.id },
        language = language
    )
}

private data class Translations(val confidential: String)

private val translationsFi = Translations(confidential = "Salassapidettävä")

private val translationsSv = Translations(confidential = "Konfidentiellt")

private fun getTranslations(language: DocumentLanguage) =
    when (language) {
        DocumentLanguage.FI -> translationsFi
        DocumentLanguage.SV -> translationsSv
    }
