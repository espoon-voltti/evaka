// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import mu.KotlinLogging

class MockSfiMessagesClient : SfiMessagesClient {
    private val logger = KotlinLogging.logger { }

    override fun send(msg: SfiMessage) {
        logger.info("Mock message client got $msg")
        data[msg.messageId] = msg
    }

    companion object {
        private val data = mutableMapOf<String, SfiMessage>()
        fun getMessages() = data.values.toList()
        fun clearMessages() = data.clear()
    }
}
