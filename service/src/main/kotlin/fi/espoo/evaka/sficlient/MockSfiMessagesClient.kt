// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import mu.KotlinLogging

private typealias MessageId = String

class MockSfiMessagesClient : SfiMessagesClient {
    private val logger = KotlinLogging.logger {}

    override fun send(msg: SfiMessage, bytes: ByteArray?) {
        logger.info("Mock message client got $msg")
        lock.write {
            data[msg.messageId] = msg
            if (bytes != null) {
                dataBytes[msg.messageId] = bytes
            }
        }
    }

    companion object {
        private val data = mutableMapOf<MessageId, SfiMessage>()
        private val dataBytes = mutableMapOf<MessageId, ByteArray?>()
        private val lock = ReentrantReadWriteLock()

        fun getMessages() =
            lock.read { data.values.toList().map { Pair(it, dataBytes[it.messageId]) } }

        fun clearMessages() = lock.write { data.clear() }
    }
}
