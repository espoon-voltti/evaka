// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.pdfgen

import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class Template(val value: String)
class Page(val template: Template, val context: Context)

fun ITemplateEngine.process(page: Page): String = this.process(page.template.value, page.context)

@Component
class PDFService(private val templateEngine: ITemplateEngine) {

    fun render(page: Page): ByteArray {
        val os = ByteArrayOutputStream()
        renderHtmlPageToPDF(templateEngine.process(page), os)
        return os.toByteArray()
    }

    private fun renderHtmlPageToPDF(pages: String, os: OutputStream) {
        with(ITextRenderer()) {
            setDocumentFromString(pages)
            layout()
            createPDF(os, false)
            finishPDF()
        }
    }
}
