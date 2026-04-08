// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.config

import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.job.JobSchedule
import evaka.core.shared.job.ScheduledJobRunner
import io.opentelemetry.api.trace.Tracer
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
