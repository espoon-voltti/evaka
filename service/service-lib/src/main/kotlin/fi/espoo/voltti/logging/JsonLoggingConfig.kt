// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import net.logstash.logback.decorate.JsonFactoryDecorator
import tools.jackson.core.JsonFactory
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature

class JsonLoggingConfig : JsonFactoryDecorator {
    override fun decorate(factory: JsonFactory): JsonFactory =
        factory.apply {
            val codec = factory.codec as? ObjectMapper
            codec?.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
}
