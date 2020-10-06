// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer

class RedisContainer : GenericContainer<RedisContainer>("redis:3.0.6") {
    companion object {
        private var instance: RedisContainer? = null
        fun getInstance(): RedisContainer = synchronized(this) {
            instance ?: RedisContainer().withExposedPorts(6379).also {
                it.start()
                it.waitUntilContainerStarted()
                instance = it
            }
        }
    }
}

class S3Container : GenericContainer<S3Container>("adobe/s3mock:2.1.18") {
    companion object {
        private var instance: S3Container? = null
        fun getInstance(): S3Container = synchronized(this) {
            instance ?: S3Container().withExposedPorts(9090).also {
                it.start()
                it.waitUntilContainerStarted()
                instance = it
            }
        }
    }
}

class PostgresContainer : PostgreSQLContainer<PostgresContainer>("postgres:12-alpine") {
    companion object {
        private var instance: PostgresContainer? = null
        fun getInstance(): PostgresContainer = synchronized(this) {
            instance ?: PostgresContainer()
                .withDatabaseName("evaka_it")
                .withUsername("evaka_it")
                .withPassword("evaka_it")
                .also {
                    it.start()
                    it.waitUntilContainerStarted()
                    instance = it
                }
        }
    }
}
