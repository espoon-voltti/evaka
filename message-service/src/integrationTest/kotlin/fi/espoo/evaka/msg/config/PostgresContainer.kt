// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.config

import org.testcontainers.containers.PostgreSQLContainer

class PostgresContainer : PostgreSQLContainer<PostgresContainer>("postgres:12-alpine") {
    companion object {
        private var instance: PostgresContainer? = null
        fun getInstance(): PostgresContainer = synchronized(this) {
            instance ?: PostgresContainer()
                .withDatabaseName("evaka_message_it")
                .withUsername("evaka_message_it")
                .withPassword("evaka_message_it")
                .also {
                    it.start()
                    it.waitUntilContainerStarted()
                    instance = it
                }
        }
    }
}
