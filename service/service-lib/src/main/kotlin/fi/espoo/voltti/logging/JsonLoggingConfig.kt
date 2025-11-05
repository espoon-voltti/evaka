// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import net.logstash.logback.decorate.MapperBuilderDecorator
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.MapperBuilder

class JsonLoggingConfig<M : ObjectMapper, B : MapperBuilder<M, B>> : MapperBuilderDecorator<M, B> {
    override fun decorate(decoratable: B): B {
        decoratable.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        return decoratable
    }
}
