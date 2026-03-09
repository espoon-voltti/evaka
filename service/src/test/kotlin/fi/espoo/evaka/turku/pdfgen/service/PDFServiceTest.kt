// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.pdfgen.service

import fi.espoo.evaka.pdfgen.Page
import fi.espoo.evaka.pdfgen.PdfGenerator
import fi.espoo.evaka.pdfgen.Template
import fi.espoo.evaka.turku.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.thymeleaf.context.Context

internal class PDFServiceTest : AbstractIntegrationTest() {
    @Autowired private lateinit var pdfService: PdfGenerator

    @Test
    fun render() {
        pdfService.render(Page(Template("test"), Context()))
    }
}
