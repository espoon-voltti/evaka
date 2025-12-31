// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import net.logstash.logback.decorate.MapperBuilderDecorator
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper

class JsonLoggingConfig : MapperBuilderDecorator<JsonMapper, JsonMapper.Builder> {
    override fun decorate(decoratable: JsonMapper.Builder?): JsonMapper.Builder? {
        decoratable?.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        return decoratable
    }
}
