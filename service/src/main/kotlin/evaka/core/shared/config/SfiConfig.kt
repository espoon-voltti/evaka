// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.SfiEnv
import evaka.core.s3.DocumentService
import evaka.core.sficlient.MockSfiMessagesClient
import evaka.core.sficlient.SfiMessagesClient
import evaka.core.sficlient.rest.AwsSsmPasswordStore
import evaka.core.sficlient.rest.SfiMessagesRestClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.ssm.SsmClient

@Configuration
class SfiConfig {
    private val logger = KotlinLogging.logger {}

    @Bean
    fun sfiMessagesClient(
        env: ObjectProvider<SfiEnv>,
        ssmClient: ObjectProvider<SsmClient>,
        documentClient: DocumentService,
    ): SfiMessagesClient =
        env.ifAvailable?.let {
            logger.info { "Using real REST Suomi.fi Messages API client. Configuration: $it" }
            SfiMessagesRestClient(
                it,
                documentClient::get,
                AwsSsmPasswordStore(ssmClient.getObject(), it),
            )
        }
            ?: MockSfiMessagesClient().also {
                logger.info { "Using mock Suomi.fi Messages API client" }
            }
}
