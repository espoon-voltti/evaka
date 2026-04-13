// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.ITemplateEngine
import org.thymeleaf.TemplateEngine
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

fun pdfTemplateEngine(municipality: String): ITemplateEngine =
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
