// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

private fun KtOperationReferenceExpression.replaceOperator(token: KtSingleValueToken) =
    (getReferencedNameElement() as LeafPsiElement).rawReplaceWithList(
        LeafPsiElement(token, token.value)
    )

class NoTripleEquals : EvakaRule("no-triple-equals") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val expression = node.psi as? KtOperationReferenceExpression ?: return
        val correctOperator =
            when (expression.operationSignTokenType) {
                KtTokens.EQEQEQ -> KtTokens.EQEQ
                KtTokens.EXCLEQEQEQ -> KtTokens.EXCLEQ
                else -> return
            }
        emit(node.startOffset, ERROR_MESSAGE, true)
        if (autoCorrect) {
            expression.replaceOperator(correctOperator)
        }
    }

    companion object {
        const val ERROR_MESSAGE =
            "Do not use triple equals, which only checks if both sides are the exactly same object"
    }
}
