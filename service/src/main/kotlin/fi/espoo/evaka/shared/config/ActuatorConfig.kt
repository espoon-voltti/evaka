// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ActuatorConfig {

    @Bean
    @Autowired
    fun databaseHealthEndpoint(jdbi: Jdbi): HealthIndicator {
        return DatabaseHealthIndicator(jdbi)
    }
}

class DatabaseHealthIndicator(private val jdbi: Jdbi) : AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) {
        try {
            Database(jdbi).read { tx -> tx.createQuery("SELECT 1").mapTo<Int>().first() }
            builder.up()
        } catch (e: JdbiException) {
            builder.down(e)
        }
    }
}
