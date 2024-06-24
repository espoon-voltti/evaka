// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.EmailEnv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.ses.SesClient

@Configuration
class EmailConfig {
    @Bean
    fun emailClient(
        client: SesClient,
        env: EmailEnv
    ): EmailClient =
        when (env.enabled) {
            true ->
                SESEmailClient(
                    client = client,
                    whitelist = env.whitelist,
                    subjectPostfix = env.subjectPostfix
                )
            false -> MockEmailClient()
        }
}
