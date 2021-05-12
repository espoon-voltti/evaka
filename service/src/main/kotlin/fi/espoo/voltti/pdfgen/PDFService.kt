// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.pdfgen

import com.lowagie.text.pdf.BaseFont
import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Paths

class Template(val value: String)
class Page(val template: Template, val context: Context)

fun ITemplateEngine.process(page: Page): String = this.process(page.template.value, page.context)


@Component
class PDFService(private val templateEngine: ITemplateEngine) {

    private fun getResourcePath(fileName: String): String {
        val res = javaClass.classLoader.getResource(fileName)
        val file: File = Paths.get(res.toURI()).toFile()
        return file.absolutePath
    }

    fun render(page: Page): ByteArray {
        val os = ByteArrayOutputStream()
        renderHtmlPageToPDF(templateEngine.process(page), os)
        return os.toByteArray()
    }

    private fun renderHtmlPageToPDF(pages: String, os: OutputStream) {
        with(ITextRenderer()) {
            val montserrat = getResourcePath("ttf/Montserrat-Regular.ttf")
            val montserratBold = getResourcePath("ttf/Montserrat-Regular.ttf")
            val openSans = getResourcePath("ttf/OpenSans-Regular.ttf")
            val openSansBold = getResourcePath("ttf/OpenSans-Bold.ttf")
            setDocumentFromString(pages)
            fontResolver.addFont(montserrat, BaseFont.IDENTITY_H, true)
            fontResolver.addFont(montserratBold, BaseFont.IDENTITY_H, true)
            fontResolver.addFont(openSans, BaseFont.IDENTITY_H, true)
            fontResolver.addFont(openSansBold, BaseFont.IDENTITY_H, true)
            layout()
            createPDF(os, false)
            finishPDF()
        }
    }
}
