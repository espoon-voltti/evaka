// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.pdfgen.service

import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.shared.config.PDFConfig
import fi.espoo.evaka.turku.TurkuTemplateProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.thymeleaf.context.Context

internal class PDFServiceTest {
    private lateinit var pdfService: PdfGenerator

    @BeforeEach
    fun setup() {
        val pdfConfig = PDFConfig()
        val turkuResolver =
            org.thymeleaf.templateresolver.ClassLoaderTemplateResolver().apply {
                prefix = "turku/templates/"
                suffix = ".html"
                setTemplateMode("HTML")
                checkExistence = true
                order = 1
            }
        val resolvers = setOf(pdfConfig.coreTemplateResolver(), turkuResolver)
        pdfService = PdfGenerator(TurkuTemplateProvider(), pdfConfig.templateEngine(resolvers))
    }

    @Test
    fun render() {
        pdfService.render(Page(Template("test"), Context()))
    }
}
