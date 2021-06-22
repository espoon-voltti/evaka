// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.getProperty
import software.amazon.awssdk.services.ses.SesClient

@Configuration
class EmailConfig {
    @Bean
    fun emailClient(client: SesClient, env: Environment): IEmailClient =
        when (env.getProperty("application.email.enabled", Boolean::class.java, false)) {
            true -> EmailClient(
                client = client,
                whitelist = env.getProperty<List<String>>("application.email.whitelist")?.map { Regex(it) }
            )
            false -> MockEmailClient()
        }
}
