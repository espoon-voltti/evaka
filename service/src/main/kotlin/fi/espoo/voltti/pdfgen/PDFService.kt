// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.pdfgen

import com.lowagie.text.pdf.BaseFont
import org.springframework.stereotype.Component
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.context.Context
import org.xhtmlrenderer.pdf.ITextFontResolver
import org.xhtmlrenderer.pdf.ITextRenderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.file.Paths
import java.util.Locale

class Template(val value: String)
class Page(val template: Template, val context: Context)

fun ITemplateEngine.process(page: Page): String = this.process(page.template.value, page.context)

@Component
class PDFService(private val templateEngine: ITemplateEngine) {

    private fun getResourceFile(fileName: String): File {
        val res = javaClass.classLoader.getResource(fileName)
        return Paths.get(res.toURI()).toFile()
    }

    fun render(page: Page): ByteArray {
        val os = ByteArrayOutputStream()
        renderHtmlPageToPDF(templateEngine.process(page), os)
        return os.toByteArray()
    }

    private fun renderHtmlPageToPDF(pages: String, os: OutputStream) {
        with(ITextRenderer()) {
            fontResolver.addFontDirectory(getResourceFile("ttf"), BaseFont.IDENTITY_H, true)
            setDocumentFromString(pages)
            layout()
            createPDF(os, false)
            finishPDF()
        }
    }
}

/**
 * mostly copy from [ITextFontResolver.addFontDirectory] to add encoding support
 */
fun ITextFontResolver.addFontDirectory(f: File, encoding: String, embedded: Boolean) {
    if (f.isDirectory) {
        f.listFiles { _, name ->
            val lower = name.lowercase(Locale.getDefault())
            lower.endsWith(".otf") || lower.endsWith(".ttf")
        }.forEach { file -> addFont(file.absolutePath, encoding, embedded) }
    }
}
