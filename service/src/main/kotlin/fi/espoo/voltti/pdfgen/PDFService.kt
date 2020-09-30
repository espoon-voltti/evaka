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

    fun render(pages: List<Page>): ByteArray {
        val os = ByteArrayOutputStream()
        renderHtmlPages(pages.map(templateEngine::process), os)
        return os.toByteArray()
    }

    private fun renderHtmlPages(pages: List<String>, os: OutputStream) {
        with(ITextRenderer()) {
            val head = pages.first()
            val tail = pages.drop(1)
            // First page
            setDocumentFromString(head)
            layout()
            createPDF(os, false)
            // Rest of the pages
            tail.forEach { page ->
                setDocumentFromString(page)
                layout()
                writeNextDocument()
            }
            finishPDF()
        }
    }
}
