// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section
import fi.espoo.evaka.shared.HtmlElement

// language=css
private val childDocumentCss =
    """
    .sections {
        display: flex;
        flex-direction: column;
    }
    
    .section {
        margin-bottom: 64px;
    }
    
    .questions {
        display: flex;
        flex-direction: column;
    }
    
    .question {
        margin-bottom: 32px;
    }
    
    label {
        font-weight: 600;
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
    return HtmlElement.body(
        children =
            listOf(
                HtmlElement.h1(document.template.name),
                HtmlElement.h2("${document.child.firstName} ${document.child.lastName}"),
                HtmlElement.div(
                    className = "sections",
                    children =
                        document.template.content.sections.map {
                            generateSectionHtml(it, document.content.answers)
                        }
                )
            )
    )
}

private fun generateSectionHtml(section: Section, answers: List<AnsweredQuestion<*>>): HtmlElement {
    return HtmlElement.div(
        className = "section",
        children =
            listOf(
                HtmlElement.h2(section.label),
                HtmlElement.div(
                    className = "questions",
                    children = section.questions.map { generateQuestionHtml(it, answers) }
                )
            )
    )
}

private fun generateQuestionHtml(
    question: Question,
    answers: List<AnsweredQuestion<*>>
): HtmlElement {
    return answers.find { it.questionId == question.id }?.let { question.generateHtml(it) }
        ?: throw IllegalStateException("No answer found for question ${question.id}")
}
