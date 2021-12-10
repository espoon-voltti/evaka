// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.SfiEnv
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.SfiMessagesClient
import fi.espoo.evaka.sficlient.SfiMessagesSoapClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SfiConfig {
    @Bean
    fun sfiMessagesClient(env: ObjectProvider<SfiEnv>, documentService: DocumentService): SfiMessagesClient = env.ifAvailable?.let {
        SfiMessagesSoapClient(it, documentService)
    } ?: MockSfiMessagesClient()
}
