// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource

@Configuration
class ResourceBundleConfig {
    @Bean
    fun messageSource(): ResourceBundleMessageSource {
        val source = ResourceBundleMessageSource().apply {
            setBasename("i18n/messages")
            setDefaultEncoding("UTF-8")
        }
        return source
    }
}
