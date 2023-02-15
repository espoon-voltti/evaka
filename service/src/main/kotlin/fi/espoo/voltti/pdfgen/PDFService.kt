// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.pdfgen

import fi.espoo.evaka.pdfgen.PdfGenerator
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context

@Deprecated("renamed to fi.espoo.evaka.pdfgen.Template") class Template(val value: String)

@Deprecated("renamed to fi.espoo.evaka.pdfgen.Page")
@Suppress("DEPRECATION")
class Page(val template: Template, val context: Context)

@Deprecated("renamed to fi.espoo.evaka.pdfgen.PdfGenerator")
@Component
class PDFService(private val pdfGenerator: PdfGenerator) {
    @Deprecated("use PdfGenerator.render instead")
    @Suppress("DEPRECATION")
    fun render(page: Page): ByteArray =
        pdfGenerator.render(
            fi.espoo.evaka.pdfgen.Page(
                fi.espoo.evaka.pdfgen.Template(page.template.value),
                page.context
            )
        )
}
