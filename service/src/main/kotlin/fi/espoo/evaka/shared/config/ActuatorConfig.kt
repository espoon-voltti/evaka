// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.shared.db.Database
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
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
    fun databaseHealthEndpoint(jdbi: Jdbi, tracer: Tracer): HealthIndicator {
        return DatabaseHealthIndicator(jdbi, tracer)
    }
}

class DatabaseHealthIndicator(private val jdbi: Jdbi, private val tracer: Tracer) :
    AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder) {
        try {
            Database(jdbi, tracer).connect { db ->
                db.read { tx -> tx.createQuery { sql("SELECT 1") }.exactlyOne<Int>() }
            }
            builder.up()
        } catch (e: JdbiException) {
            builder.down(e)
        }
    }
}
