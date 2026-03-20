// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

@Configuration
class PDFConfig {
    @Bean
    @ConditionalOnMissingBean(ITemplateEngine::class)
    fun defaultTemplateEngine(): ITemplateEngine =
        PDFConfig.templateEngine(
            // Default to templates under WEB-INF/templates for backwards compatibility
            "WEB-INF"
        )

    companion object {
        fun templateEngine(municipality: String): ITemplateEngine =
            TemplateEngine().apply {
                setTemplateResolver(
                    ClassLoaderTemplateResolver().apply {
                        prefix = "$municipality/templates/"
                        suffix = ".html"
                        setTemplateMode("HTML")
                    }
                )
                addDialect(Java8TimeDialect())
                addDialect(LayoutDialect())
            }
    }
}
