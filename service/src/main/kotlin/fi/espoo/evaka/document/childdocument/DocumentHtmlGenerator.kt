package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.document.Question
import fi.espoo.evaka.document.Section

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

fun generateHtml(document: ChildDocumentDetails): String {
    return """
        <html>
            <head>
                <style>
                    $childDocumentCss
                </style>
            </head>
            <body>
                <h1>${document.template.name}</h1>
                <h2>${document.child.firstName} ${document.child.lastName}</h2>
                <div class="sections">
                    ${document.template.content.sections.joinToString("\n") {
                        generateSectionHtml(it, document.content.answers)
                    }}
                </div>
            </body>
        </html>
    """
        .trimIndent()
}

private fun generateSectionHtml(section: Section, answers: List<AnsweredQuestion<*>>): String {
    return """
        <div class="section">
            <h2>${section.label}</h2>
            <div class="questions">
                ${section.questions.joinToString("\n") { generateQuestionHtml(it, answers) }}
            </div>
        </div>
    """
        .trimIndent()
}

private fun generateQuestionHtml(question: Question, answers: List<AnsweredQuestion<*>>): String {
    return answers.find { it.questionId == question.id }?.let { question.generateHtml(it) }
        ?: throw IllegalStateException("No answer found for question ${question.id}")
}
