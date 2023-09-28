// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.ktlint

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.KtNodeTypes.CALL_EXPRESSION
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression

class NoPrint : Rule(RuleId("custom-ktlint-rules:no-println"), About()) {
    private val printFunctions = setOf("print", "println")

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == CALL_EXPRESSION) {
            val callExpression = node.psi as KtCallExpression
            val isPrintCall =
                callExpression.children.any { e ->
                    e is KtReferenceExpression && printFunctions.contains(e.text)
                }
            if (isPrintCall) {
                emit(node.startOffset, ERROR_MESSAGE, false)
            }
        }
    }

    companion object {
        const val ERROR_MESSAGE =
            "Do not use print or println for logging, use mu.KotlinLogging.logger instead"
    }
}
