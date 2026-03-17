// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.templateresolver.ITemplateResolver

@Configuration
class PDFConfig {
    @Bean
    fun coreTemplateResolver(): ITemplateResolver =
        ClassLoaderTemplateResolver().apply {
            prefix = "WEB-INF/templates/"
            suffix = ".html"
            setTemplateMode("HTML")
            checkExistence = true
            order = 100
        }

    @Bean
    fun templateEngine(resolvers: Set<ITemplateResolver>): ITemplateEngine =
        TemplateEngine().apply {
            setTemplateResolvers(resolvers)
            addDialect(Java8TimeDialect())
            addDialect(LayoutDialect())
        }
}
