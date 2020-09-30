// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Import(value = [fi.espoo.evaka.invoicing.service.PDFService::class, fi.espoo.voltti.pdfgen.PDFService::class])
@Configuration
class PDFConfig {
    @Bean
    fun templateEngine(): ITemplateEngine = PDFConfig.templateEngine()

    companion object {
        fun templateEngine(): ITemplateEngine = TemplateEngine().apply {
            setTemplateResolver(
                ClassLoaderTemplateResolver().apply {
                    prefix = "WEB-INF/templates/"
                    suffix = ".html"
                    setTemplateMode("HTML")
                }
            )
            addDialect(Java8TimeDialect())
        }
    }
}
