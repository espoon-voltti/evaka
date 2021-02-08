// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class EmailConfig {
    @Bean
    fun emailClient(client: AmazonSimpleEmailService, env: Environment): IEmailClient =
        when (env.getProperty("application.email.enabled", Boolean::class.java, false)) {
            true -> EmailClient(client = client)
            false -> MockEmailClient()
        }
}
