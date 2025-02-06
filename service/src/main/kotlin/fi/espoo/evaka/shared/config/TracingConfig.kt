// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {
    @Bean fun openTelemetry(): OpenTelemetry = GlobalOpenTelemetry.get()

    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer = openTelemetry.getTracer("evaka-service")

    @Bean fun meter(openTelemetry: OpenTelemetry): Meter = openTelemetry.getMeter("evaka-service")
}
