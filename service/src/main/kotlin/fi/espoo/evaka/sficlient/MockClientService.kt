// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import fi.espoo.evaka.sficlient.ISfiClientService.MessageMetadata
import fi.espoo.evaka.sficlient.SfiResponse.Mapper.createOkResponse
import mu.KotlinLogging

class MockClientService : ISfiClientService {
    override fun sendMessage(metadata: MessageMetadata): SfiResponse {
        logger.info { "Sending mock message to $metadata.recipient" }
        return createOkResponse()
    }

    private val logger = KotlinLogging.logger {}
}
