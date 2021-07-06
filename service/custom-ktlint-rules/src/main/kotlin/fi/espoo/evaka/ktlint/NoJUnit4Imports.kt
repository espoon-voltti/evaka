// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.ktlint

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoJUnit4Imports : Rule("no-old-junit-imports") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (
            offset: Int,
            errorMessage: String,
            canBeAutoCorrected:
                    Boolean
        ) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val path = importDirective.importPath?.pathStr
            if (path != null && path.contains("org.junit") && !path.contains("jupiter")) {
                emit(node.startOffset, "Importing from JUnit 4, use org.junit.jupiter.* for JUnit 5 instead.",
                    false)
            }

            if (path != null && path.contains("org.junit.jupiter.api.Assertions.")) {
                emit(node.startOffset, "Use kotlin.test assertions instead of junit assertions for better type-safety",
                    false)
            }
        }
    }
}
