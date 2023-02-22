// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen

import fi.espoo.evaka.shared.domain.ISO_LANGUAGES_WITH_ALPHA2_CODE
import fi.espoo.evaka.shared.domain.IsoLanguage
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.intellij.lang.annotations.Language

private val logger = KotlinLogging.logger {}

fun generateLanguages(target: Path) {
    target.writeText(sequenceOf(fileHeader, languageFileBody()).joinToString("\n"))
    logger.info("Generated $target")
}

private fun languageFileBody(): String {
    val languages =
        ISO_LANGUAGES_WITH_ALPHA2_CODE.joinToString("\n") { lang ->
            "  ${lang.alpha2}: ${lang.toTypescript()},"
        }

    @Language("typescript")
    val body =
        """
export type IsoLanguage = { alpha2: string; nameFi: string }

const isoLanguages: { [alpha2: string]: IsoLanguage } = {
$languages
}
export { isoLanguages }
    """
    return body.trim()
}

private fun IsoLanguage.toTypescript(): String = """{ alpha2: "$alpha2", nameFi: "$nameFi" }"""

fun checkLanguages(target: Path) {
    if (languageFileBody() != target.readText().skipFileHeader()) {
        logger.error("File is not up to date: $target")
        exitProcess(1)
    }
}
