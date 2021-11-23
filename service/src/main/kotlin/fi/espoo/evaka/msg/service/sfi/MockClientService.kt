// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.sfi

import fi.espoo.evaka.msg.service.sfi.ISfiClientService.MessageMetadata
import fi.espoo.evaka.msg.service.sfi.SfiResponse.Mapper.createOkResponse
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("dev", "local")
class MockClientService : ISfiClientService {
    override fun sendMessage(metadata: MessageMetadata): SfiResponse {
        logger.info { "Sending mock message to $metadata.recipient" }
        return createOkResponse()
    }

    private val logger = KotlinLogging.logger {}
}
