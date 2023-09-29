// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.ktlint

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective

class NoJUnit4Imports : EvakaRule("no-old-junit-imports") {
    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val importDirective = (node.psi as? KtImportDirective) ?: return
        val path = importDirective.importPath?.pathStr ?: return
        if (path.contains("org.junit") && !path.contains("jupiter")) {
            emit(
                node.startOffset,
                "Importing from JUnit 4, use org.junit.jupiter.* for JUnit 5 instead.",
                false
            )
        }

        if (path.contains("org.junit.jupiter.api.Assertions.")) {
            emit(
                node.startOffset,
                "Use kotlin.test assertions instead of junit assertions for better type-safety",
                false
            )
        }
    }
}
