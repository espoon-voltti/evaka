// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {
    @Bean fun tracer(): Tracer = GlobalTracer.get()
}
