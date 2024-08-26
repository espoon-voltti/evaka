// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import net.logstash.logback.decorate.JsonFactoryDecorator

class JsonLoggingConfig : JsonFactoryDecorator {
    override fun decorate(factory: JsonFactory): JsonFactory =
        factory.apply {
            val codec = factory.codec as? ObjectMapper
            codec?.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
}
