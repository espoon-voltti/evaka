// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.job.JobSchedule
import fi.espoo.evaka.shared.job.ScheduledJobRunner
import io.opentracing.Tracer
import javax.sql.DataSource
import org.jdbi.v3.core.Jdbi
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener

@Configuration
class ScheduledJobConfig {
    @Bean
    fun scheduledJobRunner(
        jdbi: Jdbi,
        tracer: Tracer,
        asyncJobRunner: AsyncJobRunner<AsyncJob>,
        dataSource: DataSource,
        schedules: List<JobSchedule>,
    ) = ScheduledJobRunner(jdbi, tracer, asyncJobRunner, schedules, dataSource)

    @Bean
    @Profile("production")
    fun scheduledJobRunnerStart(runner: ScheduledJobRunner) =
        object {
            @EventListener
            fun onApplicationReady(@Suppress("UNUSED_PARAMETER") event: ApplicationReadyEvent) {
                runner.scheduler.start()
            }
        }
}
